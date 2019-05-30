package com.vcashorg.vcashwallet.wallet.WallegtType;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;

import org.bitcoin.protocols.payments.Protos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static java.nio.ByteOrder.BIG_ENDIAN;

public class VcashTransaction extends VcashTxBaseObject {
    final public static int COMMITMENT_SIZE = 32;
    final public static int PEDERSEN_COMMITMENT_SIZE = 33;
    final public static int SIGNATURE_SIZE = 64;
    final public static int MAX_PROOF_SIZE = 5134;
    public byte[] offset;
    public TransactionBody body;

    public byte[]calculateFinalExcess(){
        ArrayList<byte[]> negativeCommits = new ArrayList<byte[]>();
        ArrayList<byte[]> positiveCommits = new ArrayList<byte[]>();
        for (Input input : body.inputs){
            negativeCommits.add(input.commit);
        }
        for (Output output : body.outputs){
            positiveCommits.add(output.commit);
        }

        long fee = 0;
        for (TxKernel kernel: body.kernels){
            fee += kernel.fee;
        }
        byte[] zeroKey = new byte[32];
        for (int j=0; j<32; j++){
            zeroKey[j] = 0;
        }
        byte[] feeCommit = NativeSecp256k1.instance().getCommitment(fee, zeroKey);
        positiveCommits.add(feeCommit);

        byte[] offsetCommit = NativeSecp256k1.instance().getCommitment(0,   offset);
        negativeCommits.add(offsetCommit);

        byte[][] positiveArr = (byte[][])positiveCommits.toArray();
        byte[][] negativeArr = (byte[][])negativeCommits.toArray();
        return NativeSecp256k1.instance().commitSum(positiveArr, negativeArr);
    }

    public boolean setTxExcessAndandTxSig(byte[] excess, byte[] sig){
        if (body.kernels.size() == 1){
            TxKernel kernel = body.kernels.get(0);
            kernel.excess = excess;
            kernel.excess_sig = sig;
            if (kernel.verify()){
                return true;
            }
        }
        return false;
    }

    public void sortTx(){
        Comparator<VcashTxBaseObject> comparator = new Comparator<VcashTxBaseObject>(){
            @Override
            public int compare(VcashTxBaseObject arg1, VcashTxBaseObject arg2) {
                byte[] hash1 = arg1.blake2bHash();
                byte[] hash2 = arg2.blake2bHash();
                for (int i=0; i<hash1.length; i++){
                    byte byte1 = hash1[i];
                    byte byte2 = hash2[i];
                    if (byte1 > byte2){
                        return 1;
                    }
                    else if (byte1 < byte2){
                        return -1;
                    }
                    else {
                        continue;
                    }
                }
                Log.e("", "hash value can not be equal");
                return 0;
            }
        };
        Input[] inputs = (Input[])body.inputs.toArray();
        Arrays.sort(inputs, comparator);
        body.inputs = new ArrayList<Input>();
        for (int i=0; i<inputs.length; i++){
            body.inputs.add(inputs[i]);
        }
        Output[] outputs = (Output[])body.outputs.toArray();
        Arrays.sort(outputs, comparator);
        body.outputs = new ArrayList<Output>();
        for (int i=0; i<outputs.length; i++){
            body.outputs.add(outputs[i]);
        }
        TxKernel[] kernels = (TxKernel[])body.kernels.toArray();
        Arrays.sort(kernels, comparator);
        body.kernels = new ArrayList<TxKernel>();
        for (int i=0; i<kernels.length; i++){
            body.kernels.add(kernels[i]);
        }
    }

    public class VcashTransactionTypeAdapter extends TypeAdapter<VcashTransaction> {
        @Override
        public void write(JsonWriter jsonWriter, VcashTransaction tx) throws IOException {
            Gson gson = new Gson();
            String offsetStr = gson.toJson(tx.offset, byte[].class);
            String bodyStr = gson.toJson(tx.body, TransactionBody.class);
            jsonWriter.beginObject();
            jsonWriter.name("offset").value(offsetStr);
            jsonWriter.name("body").value(bodyStr);
            jsonWriter.endObject();
        }

        @Override
        public VcashTransaction read(JsonReader jsonReader) throws IOException {
            VcashTransaction tx = new VcashTransaction();
            Gson gson = new Gson();
            jsonReader.beginObject();
            while (jsonReader.hasNext()){
                switch (jsonReader.nextName()){
                    case "offset":
                        String offsetStr = jsonReader.nextString();
                        tx.offset = gson.fromJson(offsetStr, byte[].class);
                        break;

                    case "body":
                        String bodyStr = jsonReader.nextString();
                        tx.body = gson.fromJson(bodyStr, TransactionBody.class);
                        break;
                }
            }
            jsonReader.endObject();
            return tx;
        }
    }

    public byte[] computePayload(boolean isForHash){
        //max 2 Output
        ByteBuffer buf = ByteBuffer.allocate(20000);
        buf.put(offset);
        buf.put(body.computePayload(isForHash));

        return buf.array();
    }

    public class TransactionBody extends VcashTxBaseObject{
        public ArrayList<Input> inputs;
        public ArrayList<Output> outputs;
        public ArrayList<TxKernel> kernels;

        public byte[] computePayload(boolean isForHash){
            //max 2 Output
            ByteBuffer buf = ByteBuffer.allocate(20000);
            buf.order(BIG_ENDIAN);
            buf.putLong(inputs.size());
            buf.putLong(outputs.size());
            buf.putLong(kernels.size());
            for (Input item: inputs){
                buf.put(item.computePayload(isForHash));
            }
            for (Output item: outputs){
                buf.put(item.computePayload(isForHash));
            }
            for (TxKernel item: kernels){
                buf.put(item.computePayload(isForHash));
            }

            return buf.array();
        }
    }

    public class TxKernel extends VcashTxBaseObject{
        public KernelFeatures features;
        public long fee;
        public long lock_height;
        public byte[] excess;
        public byte[] excess_sig;

        public KernelFeatures featureWithLockHeight(long lock_height){
            return lock_height>0?KernelFeatures.KernelFeatureHeightLocked:KernelFeatures.KernelFeaturePlain;
        }

        public byte[] kernelMsgToSign(){
            ByteBuffer buf = ByteBuffer.allocate(20);
            buf.order(BIG_ENDIAN);
            switch (features){
                case KernelFeaturePlain:
                    if (lock_height == 0){
                        buf.put((byte)features.ordinal());
                        buf.putLong(fee);
                    }
                    break;
                case KernelFeatureCoinbase:
                    if (fee == 0 && lock_height == 0){
                        buf.put((byte)features.ordinal());
                    }
                    break;
                case KernelFeatureHeightLocked:
                    buf.put((byte)features.ordinal());
                    buf.putLong(fee);
                    buf.putLong(lock_height);
                    break;
            }

            return NativeSecp256k1.instance().blake2b(buf.array(), null);
        }

        public boolean verify(){
            byte[] pubkey = NativeSecp256k1.instance().commitToPubkey(excess);
            if (pubkey != null){
                return NativeSecp256k1.instance().verifySingleSignature(excess_sig, pubkey, null, pubkey, kernelMsgToSign());
            }
            return false;
        }

        public byte[] computePayload(boolean isForHash){
            ByteBuffer buf = ByteBuffer.allocate(1+8+8+PEDERSEN_COMMITMENT_SIZE+SIGNATURE_SIZE);
            buf.order(BIG_ENDIAN);
            byte feature = (byte)features.ordinal();
            buf.put(feature);
            buf.putLong(fee);
            buf.putLong(lock_height);
            buf.put(excess);
            buf.put(excess_sig);

            return buf.array();
        }

        public class TxKernelTypeAdapter extends TypeAdapter<TxKernel> {
            @Override
            public void write(JsonWriter jsonWriter, TxKernel kernel) throws IOException {
                Gson gson = new Gson();
                String excessStr = gson.toJson(kernel.excess, byte[].class);
                String excessSigStr = gson.toJson(kernel.excess_sig, byte[].class);
                String featureStr = null;
                if (kernel.features == KernelFeatures.KernelFeaturePlain) {
                    featureStr = "Plain";
                } else if (kernel.features == KernelFeatures.KernelFeatureCoinbase) {
                    featureStr = "Coinbase";
                } else if (kernel.features == KernelFeatures.KernelFeatureHeightLocked) {
                    featureStr = "HeightLocked";
                }
                jsonWriter.beginObject();
                jsonWriter.name("excess").value(excessStr);
                jsonWriter.name("excess_sig").value(excessSigStr);
                jsonWriter.name("features").value(featureStr);
                jsonWriter.name("fee").value(kernel.fee);
                jsonWriter.name("lock_height").value(kernel.lock_height);
                jsonWriter.endObject();
            }

            @Override
            public TxKernel read(JsonReader jsonReader) throws IOException {
                TxKernel kernel = new TxKernel();
                jsonReader.beginObject();
                Gson gson = new Gson();
                while (jsonReader.hasNext()) {
                    switch (jsonReader.nextName()) {
                        case "excess":
                            String excessStr = jsonReader.nextString();
                            kernel.excess = gson.fromJson(excessStr, byte[].class);
                            break;

                        case "excess_sig":
                            String excessSigStr = jsonReader.nextString();
                            kernel.excess_sig = gson.fromJson(excessSigStr, byte[].class);
                            break;

                        case "features":
                            String featureStr = jsonReader.nextString();
                            if (featureStr.equals("Plain")) {
                                kernel.features = KernelFeatures.KernelFeaturePlain;
                            } else if (featureStr.equals("Coinbase")) {
                                kernel.features = KernelFeatures.KernelFeatureCoinbase;
                            } else {
                                kernel.features = KernelFeatures.KernelFeatureHeightLocked;
                            }
                            break;

                        case "fee":
                            kernel.fee = jsonReader.nextLong();
                            break;

                        case "lock_height":
                            kernel.lock_height = jsonReader.nextLong();
                            break;
                    }
                }
                jsonReader.endObject();
                return kernel;
            }
        }
    }

    public class Output extends VcashTxBaseObject{
        public OutputFeatures features;
        public byte[] commit;
        public byte[] proof;

        public byte[] computePayload(boolean isForHash){
            ByteBuffer buf = ByteBuffer.allocate(1+COMMITMENT_SIZE+8+MAX_PROOF_SIZE);
            buf.order(BIG_ENDIAN);
            byte feature = (byte)features.ordinal();
            buf.put(feature);
            buf.put(commit);
            if (!isForHash){
                buf.putLong(proof.length);
                buf.put(proof);
            }

            return buf.array();
        }

        public class OutputTypeAdapter extends TypeAdapter<Output> {
            @Override
            public void write(JsonWriter jsonWriter, Output output) throws IOException {
                Gson gson = new Gson();
                String commitStr = gson.toJson(output.commit, byte[].class);
                String proofStr = gson.toJson(output.proof, byte[].class);
                String featureStr = null;
                if (output.features == OutputFeatures.OutputFeaturePlain){
                    featureStr = "Plain";
                }
                else if(output.features == OutputFeatures.OutputFeatureCoinbase){
                    featureStr = "Coinbase";
                }
                jsonWriter.beginObject();
                jsonWriter.name("commit").value("commitStr");
                jsonWriter.name("features").value("featureStr");
                jsonWriter.name("proof").value("proofStr");
                jsonWriter.endObject();
            }

            @Override
            public Output read(JsonReader jsonReader) throws IOException {
                Output output = new Output();
                jsonReader.beginObject();
                Gson gson = new Gson();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "commit":
                            String commitStr = jsonReader.nextString();
                            output.commit = gson.fromJson(commitStr, byte[].class);
                            break;

                        case "proof":
                            String proofStr = jsonReader.nextString();
                            output.proof = gson.fromJson(proofStr, byte[].class);
                            break;

                        case "features":
                            String featureStr = jsonReader.nextString();
                            if (featureStr.equals("Plain")){
                                output.features = OutputFeatures.OutputFeaturePlain;
                            }
                            else {
                                output.features = OutputFeatures.OutputFeatureCoinbase;
                            }
                            break;
                    }
                }
                jsonReader.endObject();
                return output;
            }
        }
    }

    public class Input extends VcashTxBaseObject{
        public OutputFeatures features;
        public byte[] commit;

        public byte[] computePayload(boolean isForHash){
            ByteBuffer buf = ByteBuffer.allocate(COMMITMENT_SIZE+1);
            byte feature = (byte)features.ordinal();
            buf.put(feature);
            buf.put(commit);

            return buf.array();
        }

        public class InputTypeAdapter extends TypeAdapter<Input> {
            @Override
            public void write(JsonWriter jsonWriter, Input input) throws IOException {
                Gson gson = new Gson();
                String commitStr = gson.toJson(input.commit, byte[].class);
                String featureStr = null;
                if (input.features == OutputFeatures.OutputFeaturePlain){
                    featureStr = "Plain";
                }
                else if(input.features == OutputFeatures.OutputFeatureCoinbase){
                    featureStr = "Coinbase";
                }
                jsonWriter.beginObject();
                jsonWriter.name("commit").value("commitStr");
                jsonWriter.name("features").value("featureStr");
                jsonWriter.endObject();
            }

            @Override
            public Input read(JsonReader jsonReader) throws IOException {
                Input input = new Input();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "commit":
                            String commitStr = jsonReader.nextString();
                            Gson gson = new Gson();
                            input.commit = gson.fromJson(commitStr, byte[].class);
                            break;

                        case "features":
                            String featureStr = jsonReader.nextString();
                            if (featureStr.equals("Plain")){
                                input.features = OutputFeatures.OutputFeaturePlain;
                            }
                            else {
                                input.features = OutputFeatures.OutputFeatureCoinbase;
                            }
                            break;
                    }
                }
                jsonReader.endObject();
                return input;
            }
        }
    }

    public enum KernelFeatures{
        KernelFeaturePlain,
        KernelFeatureCoinbase,
        KernelFeatureHeightLocked,
    }

    public enum OutputFeatures{
        OutputFeaturePlain,
        OutputFeatureCoinbase,
    }
}
