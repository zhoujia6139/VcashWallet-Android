package com.vcashorg.vcashwallet.wallet.WallegtType;

import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;

import org.apache.commons.lang3.StringEscapeUtils;
import org.bitcoin.protocols.payments.Protos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static java.nio.ByteOrder.BIG_ENDIAN;

public class VcashTransaction extends VcashTxBaseObject {
    final public static int PEDERSEN_COMMITMENT_SIZE = 33;
    final public static int SIGNATURE_SIZE = 64;
    final public static int MAX_PROOF_SIZE = 5134;
    public byte[] offset;
    public TransactionBody body = new TransactionBody();

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

        byte[] feeCommit = NativeSecp256k1.instance().getCommitment(fee, AppUtil.zeroByteArray(32));
        positiveCommits.add(feeCommit);

        byte[] offsetCommit = NativeSecp256k1.instance().getCommitment(0,   offset);
        negativeCommits.add(offsetCommit);

        return NativeSecp256k1.instance().commitSum(positiveCommits, negativeCommits);
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
                    int byte1 = hash1[i] & 0xff;
                    int byte2 = hash2[i] & 0xff;
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

        Collections.sort(body.inputs,comparator);
        Collections.sort(body.outputs,comparator);
        Collections.sort(body.kernels,comparator);
    }

    public class VcashTransactionTypeAdapter extends TypeAdapter<VcashTransaction> {
        @Override
        public void write(JsonWriter jsonWriter, VcashTransaction tx) throws IOException {
            String offsetStr = AppUtil.getByteStrFromByteArr(tx.offset);
            offsetStr = StringEscapeUtils.unescapeJson(offsetStr);
            Gson gson1 = new GsonBuilder().registerTypeAdapter(TransactionBody.class, tx.body.new TransactionBodyTypeAdapter()).create();
            String bodyStr = gson1.toJson(tx.body, TransactionBody.class);
            bodyStr = StringEscapeUtils.unescapeJson(bodyStr);
            jsonWriter.beginObject();
            jsonWriter.name("offset").jsonValue(offsetStr);
            jsonWriter.name("body").jsonValue(bodyStr);
            jsonWriter.endObject();
        }

        @Override
        public VcashTransaction read(JsonReader jsonReader) throws IOException {
            VcashTransaction tx = new VcashTransaction();
            jsonReader.beginObject();
            while (jsonReader.hasNext()){
                switch (jsonReader.nextName()){
                    case "offset":
                        if (jsonReader.peek() != JsonToken.NULL){
                            jsonReader.beginArray();
                            ArrayList<Integer> arrayList3 = new ArrayList<>();
                            while(jsonReader.hasNext()){
                                arrayList3.add(jsonReader.nextInt());
                            }
                            jsonReader.endArray();
                            tx.offset = AppUtil.intArrToByteArr(arrayList3);
                        }
                        else{
                            jsonReader.nextNull();
                        }
                        break;

                    case "body":
                        TransactionBody body = new TransactionBody();
                        Gson gson = new GsonBuilder().registerTypeAdapter(TransactionBody.class, body.new TransactionBodyTypeAdapter()).create();
                        tx.body = gson.fromJson(jsonReader, TransactionBody.class);
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

        buf.flip();
        return AppUtil.BufferToByteArr(buf);
    }

    public class TransactionBody extends VcashTxBaseObject{
        public ArrayList<Input> inputs = new ArrayList<Input>();
        public ArrayList<Output> outputs = new ArrayList<>();
        public ArrayList<TxKernel> kernels = new ArrayList<>();

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

            buf.flip();
            return AppUtil.BufferToByteArr(buf);
        }

        public class TransactionBodyTypeAdapter extends TypeAdapter<TransactionBody> {
            @Override
            public void write(JsonWriter jsonWriter, TransactionBody body) throws IOException {
                Input input = body.inputs.get(0);
                Gson inputGson = new GsonBuilder().registerTypeAdapter(Input.class, input.new InputTypeAdapter()).create();
                String inputsStr = inputGson.toJson(body.inputs, new TypeToken<ArrayList<Input>>(){}.getType());
                inputsStr = StringEscapeUtils.unescapeJson(inputsStr);

                Output output = body.outputs.get(0);
                Gson outputGson = new GsonBuilder().registerTypeAdapter(Output.class, output.new OutputTypeAdapter()).create();
                String outputsStr = outputGson.toJson(body.outputs, new TypeToken<ArrayList<Output>>(){}.getType());
                outputsStr = StringEscapeUtils.unescapeJson(outputsStr);

                TxKernel kernel = body.kernels.get(0);
                Gson kernelGson = new GsonBuilder().registerTypeAdapter(TxKernel.class, kernel.new TxKernelTypeAdapter()).create();
                String kernelsStr = kernelGson.toJson(body.kernels, new TypeToken<ArrayList<TxKernel>>(){}.getType());
                kernelsStr = StringEscapeUtils.unescapeJson(kernelsStr);
                jsonWriter.beginObject();
                jsonWriter.name("inputs").jsonValue(inputsStr);
                jsonWriter.name("outputs").jsonValue(outputsStr);
                jsonWriter.name("kernels").jsonValue(kernelsStr);
                jsonWriter.endObject();
            }

            @Override
            public TransactionBody read(JsonReader jsonReader) throws IOException {
                TransactionBody body = new TransactionBody();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "inputs": {
                            jsonReader.beginArray();
                            while(jsonReader.hasNext()){
                                Input input = new Input();
                                Gson datagson = new GsonBuilder().registerTypeAdapter(Input.class, input.new InputTypeAdapter()).create();
                                input = datagson.fromJson(jsonReader, Input.class);
                                body.inputs.add(input);
                            }
                            jsonReader.endArray();
                        }
                            break;
                        case "outputs": {
                            jsonReader.beginArray();
                            while(jsonReader.hasNext()){
                                Output output = new Output();
                                Gson datagson = new GsonBuilder().registerTypeAdapter(Output.class, output.new OutputTypeAdapter()).create();
                                output = datagson.fromJson(jsonReader, Output.class);
                                body.outputs.add(output);
                            }
                            jsonReader.endArray();
                        }
                            break;
                        case "kernels": {
                            jsonReader.beginArray();
                            while(jsonReader.hasNext()){
                                TxKernel kernel = new TxKernel();
                                Gson datagson = new GsonBuilder().registerTypeAdapter(TxKernel.class, kernel.new TxKernelTypeAdapter()).create();
                                kernel = datagson.fromJson(jsonReader, TxKernel.class);
                                body.kernels.add(kernel);
                            }
                            jsonReader.endArray();
                        }
                            break;
                    }
                }
                jsonReader.endObject();
                return body;
            }
        }
    }

    public class TxKernel extends VcashTxBaseObject{
        public KernelFeatures features;
        public long fee;
        public long lock_height;
        public byte[] excess = AppUtil.zeroByteArray(32);
        public byte[] excess_sig = AppUtil.zeroByteArray(64);;

        public KernelFeatures featureWithLockHeight(long lock_height){
            return lock_height>0?KernelFeatures.KernelFeatureHeightLocked:KernelFeatures.KernelFeaturePlain;
        }

        public byte[] kernelMsgToSign(){
            ByteBuffer buf = null;
            switch (features){
                case KernelFeaturePlain:
                    buf = ByteBuffer.allocate(9);
                    buf.order(BIG_ENDIAN);
                    if (lock_height == 0){
                        buf.put((byte)features.ordinal());
                        buf.putLong(fee);
                    }
                    break;
                case KernelFeatureCoinbase:
                    buf = ByteBuffer.allocate(1);
                    buf.order(BIG_ENDIAN);
                    if (fee == 0 && lock_height == 0){
                        buf.put((byte)features.ordinal());
                    }
                    break;
                case KernelFeatureHeightLocked:
                    buf = ByteBuffer.allocate(17);
                    buf.order(BIG_ENDIAN);
                    buf.put((byte)features.ordinal());
                    buf.putLong(fee);
                    buf.putLong(lock_height);
                    break;
            }

            return NativeSecp256k1.instance().blake2b(buf.array(), null);
        }

        public void setLock_height(long height){
            lock_height = height;
            features = featureWithLockHeight(height);
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
                String excessStr = AppUtil.getByteStrFromByteArr(kernel.excess);
                excessStr = StringEscapeUtils.unescapeJson(excessStr);
                String excessSigStr = AppUtil.getByteStrFromByteArr(kernel.excess_sig);
                excessSigStr = StringEscapeUtils.unescapeJson(excessSigStr);
                String featureStr = null;
                if (kernel.features == KernelFeatures.KernelFeaturePlain) {
                    featureStr = "Plain";
                } else if (kernel.features == KernelFeatures.KernelFeatureCoinbase) {
                    featureStr = "Coinbase";
                } else if (kernel.features == KernelFeatures.KernelFeatureHeightLocked) {
                    featureStr = "HeightLocked";
                }
                jsonWriter.beginObject();
                jsonWriter.name("excess").jsonValue(excessStr);
                jsonWriter.name("excess_sig").jsonValue(excessSigStr);
                jsonWriter.name("features").value(featureStr);
                jsonWriter.name("fee").value(kernel.fee);
                jsonWriter.name("lock_height").value(kernel.lock_height);
                jsonWriter.endObject();
            }

            @Override
            public TxKernel read(JsonReader jsonReader) throws IOException {
                TxKernel kernel = new TxKernel();
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    switch (jsonReader.nextName()) {
                        case "excess":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList3 = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList3.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                kernel.excess = AppUtil.intArrToByteArr(arrayList3);
                            }
                            else{
                                jsonReader.nextNull();
                            }
                            break;

                        case "excess_sig":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList3 = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList3.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                kernel.excess_sig = AppUtil.intArrToByteArr(arrayList3);
                            }
                            else{
                                jsonReader.nextNull();
                            }
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
            int length = 1+PEDERSEN_COMMITMENT_SIZE+8+MAX_PROOF_SIZE;
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(BIG_ENDIAN);
            byte feature = (byte)features.ordinal();
            buf.put(feature);
            buf.put(commit);
            if (!isForHash){
                buf.putLong(proof.length);
                buf.put(proof);
            }

            buf.flip();
            return AppUtil.BufferToByteArr(buf);
        }

        public class OutputTypeAdapter extends TypeAdapter<Output> {
            @Override
            public void write(JsonWriter jsonWriter, Output output) throws IOException {
                String commitStr = AppUtil.getByteStrFromByteArr(output.commit);
                commitStr = StringEscapeUtils.unescapeJson(commitStr);
                String proofStr = AppUtil.getByteStrFromByteArr(output.proof);
                proofStr = StringEscapeUtils.unescapeJson(proofStr);
                String featureStr = null;
                if (output.features == OutputFeatures.OutputFeaturePlain){
                    featureStr = "Plain";
                }
                else if(output.features == OutputFeatures.OutputFeatureCoinbase){
                    featureStr = "Coinbase";
                }
                jsonWriter.beginObject();
                jsonWriter.name("commit").jsonValue(commitStr);
                jsonWriter.name("features").value(featureStr);
                jsonWriter.name("proof").jsonValue(proofStr);
                jsonWriter.endObject();
            }

            @Override
            public Output read(JsonReader jsonReader) throws IOException {
                Output output = new Output();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "commit":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList3 = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList3.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                output.commit = AppUtil.intArrToByteArr(arrayList3);
                            }
                            else{
                                jsonReader.nextNull();
                            }
                            break;

                        case "proof":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList3 = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList3.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                output.proof = AppUtil.intArrToByteArr(arrayList3);
                            }
                            else{
                                jsonReader.nextNull();
                            }
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
            ByteBuffer buf = ByteBuffer.allocate(PEDERSEN_COMMITMENT_SIZE+1);
            byte feature = (byte)features.ordinal();
            buf.put(feature);
            buf.put(commit);

            return buf.array();
        }

        public class InputTypeAdapter extends TypeAdapter<Input> {
            @Override
            public void write(JsonWriter jsonWriter, Input input) throws IOException {
                String commitStr = AppUtil.getByteStrFromByteArr(input.commit);
                commitStr = StringEscapeUtils.unescapeJson(commitStr);
                String featureStr = null;
                if (input.features == OutputFeatures.OutputFeaturePlain){
                    featureStr = "Plain";
                }
                else if(input.features == OutputFeatures.OutputFeatureCoinbase){
                    featureStr = "Coinbase";
                }
                jsonWriter.beginObject();
                jsonWriter.name("commit").jsonValue(commitStr);
                jsonWriter.name("features").value(featureStr);
                jsonWriter.endObject();
            }

            @Override
            public Input read(JsonReader jsonReader) throws IOException {
                Input input = new Input();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "commit":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList3 = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList3.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                input.commit = AppUtil.intArrToByteArr(arrayList3);
                            }
                            else{
                                jsonReader.nextNull();
                            }
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
