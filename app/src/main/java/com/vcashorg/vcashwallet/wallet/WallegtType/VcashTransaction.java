package com.vcashorg.vcashwallet.wallet.WallegtType;

import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
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
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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

    public byte[]calculateTokenFinalExcess(){
        ArrayList<byte[]> negativeCommits = new ArrayList<byte[]>();
        ArrayList<byte[]> positiveCommits = new ArrayList<byte[]>();
        for (TokenInput input : body.token_inputs){
            negativeCommits.add(input.commit);
        }
        for (TokenOutput output : body.token_outputs){
            positiveCommits.add(output.commit);
        }

        return NativeSecp256k1.instance().commitSum(positiveCommits, negativeCommits);
    }

    public boolean setTokenTxExcessAndandTxSig(byte[] excess, byte[] sig){
        if (body.token_kernels.size() == 1){
            TokenTxKernel kernel = body.token_kernels.get(0);
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
        Collections.sort(body.token_inputs,comparator);
        Collections.sort(body.outputs,comparator);
        Collections.sort(body.token_outputs,comparator);
        Collections.sort(body.kernels,comparator);
        Collections.sort(body.token_kernels,comparator);
    }

    public class VcashTransactionTypeAdapter extends TypeAdapter<VcashTransaction> {
        @Override
        public void write(JsonWriter jsonWriter, VcashTransaction tx) throws IOException {
            String offsetStr = AppUtil.hex(tx.offset);
            Gson gson1 = new GsonBuilder().registerTypeAdapter(TransactionBody.class, tx.body.new TransactionBodyTypeAdapter()).create();
            String bodyStr = gson1.toJson(tx.body, TransactionBody.class);
            jsonWriter.beginObject();
            jsonWriter.name("offset").value(offsetStr);
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
                        String offset_str = jsonReader.nextString();
                        tx.offset = AppUtil.decode(offset_str);
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
        ByteBuffer buf = ByteBuffer.allocate(200000);
        buf.put(offset);
        buf.put(body.computePayload(isForHash));

        buf.flip();
        return AppUtil.BufferToByteArr(buf);
    }

    public class TransactionBody extends VcashTxBaseObject{
        public ArrayList<Input> inputs = new ArrayList<>();
        public ArrayList<Output> outputs = new ArrayList<>();
        public ArrayList<TxKernel> kernels = new ArrayList<>();
        public ArrayList<TokenInput> token_inputs = new ArrayList<>();
        public ArrayList<TokenOutput> token_outputs = new ArrayList<>();
        public ArrayList<TokenTxKernel> token_kernels = new ArrayList<>();

        public byte[] computePayload(boolean isForHash){
            //max 2 Output
            ByteBuffer buf = ByteBuffer.allocate(200000);
            buf.order(BIG_ENDIAN);
            buf.putLong(inputs.size());
            buf.putLong(token_inputs.size());
            buf.putLong(outputs.size());
            buf.putLong(token_outputs.size());
            buf.putLong(kernels.size());
            buf.putLong(token_kernels.size());
            for (Input item: inputs){
                buf.put(item.computePayload(isForHash));
            }
            for (TokenInput item: token_inputs){
                buf.put(item.computePayload(isForHash));
            }
            for (Output item: outputs){
                buf.put(item.computePayload(isForHash));
            }
            for (TokenOutput item: token_outputs){
                buf.put(item.computePayload(isForHash));
            }
            for (TxKernel item: kernels){
                buf.put(item.computePayload(isForHash));
            }
            for (TokenTxKernel item: token_kernels){
                buf.put(item.computePayload(isForHash));
            }

            buf.flip();
            return AppUtil.BufferToByteArr(buf);
        }

        public class TransactionBodyTypeAdapter extends TypeAdapter<TransactionBody> {
            @Override
            public void write(JsonWriter jsonWriter, TransactionBody body) throws IOException {
                Input input = new Input();
                Gson inputGson = new GsonBuilder().registerTypeAdapter(Input.class, input.new InputTypeAdapter()).create();
                String inputsStr = inputGson.toJson(body.inputs, new TypeToken<ArrayList<Input>>(){}.getType());
                //inputsStr = StringEscapeUtils.unescapeJson(inputsStr);

                TokenInput token_input = new TokenInput();
                Gson tokenInputGson = new GsonBuilder().registerTypeAdapter(TokenInput.class, token_input.new TokenInputTypeAdapter()).create();
                String tokenInputsStr = tokenInputGson.toJson(body.token_inputs, new TypeToken<ArrayList<TokenInput>>(){}.getType());

                Output output = new Output();
                Gson outputGson = new GsonBuilder().registerTypeAdapter(Output.class, output.new OutputTypeAdapter()).create();
                String outputsStr = outputGson.toJson(body.outputs, new TypeToken<ArrayList<Output>>(){}.getType());
                //outputsStr = StringEscapeUtils.unescapeJson(outputsStr);

                TokenOutput token_output = new TokenOutput();
                Gson tokenOutputGson = new GsonBuilder().registerTypeAdapter(TokenOutput.class, token_output.new TokenOutputTypeAdapter()).create();
                String tokenOutputsStr = tokenOutputGson.toJson(body.token_outputs, new TypeToken<ArrayList<TokenOutput>>(){}.getType());

                TxKernel kernel = new TxKernel();
                Gson kernelGson = new GsonBuilder().registerTypeAdapter(TxKernel.class, kernel.new TxKernelTypeAdapter()).create();
                String kernelsStr = kernelGson.toJson(body.kernels, new TypeToken<ArrayList<TxKernel>>(){}.getType());
                //kernelsStr = StringEscapeUtils.unescapeJson(kernelsStr);

                TokenTxKernel token_kernel = new TokenTxKernel();
                Gson tokenKernelGson = new GsonBuilder().registerTypeAdapter(TokenTxKernel.class, token_kernel.new TokenTxKernelTypeAdapter()).create();
                String tokenKernelsStr = tokenKernelGson.toJson(body.token_kernels, new TypeToken<ArrayList<TokenTxKernel>>(){}.getType());

                jsonWriter.beginObject();
                jsonWriter.name("inputs").jsonValue(inputsStr);
                jsonWriter.name("token_inputs").jsonValue(tokenInputsStr);
                jsonWriter.name("outputs").jsonValue(outputsStr);
                jsonWriter.name("token_outputs").jsonValue(tokenOutputsStr);
                jsonWriter.name("kernels").jsonValue(kernelsStr);
                jsonWriter.name("token_kernels").jsonValue(tokenKernelsStr);
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
                        case "token_inputs": {
                            jsonReader.beginArray();
                            while(jsonReader.hasNext()){
                                TokenInput input = new TokenInput();
                                Gson datagson = new GsonBuilder().registerTypeAdapter(TokenInput.class, input.new TokenInputTypeAdapter()).create();
                                input = datagson.fromJson(jsonReader, TokenInput.class);
                                body.token_inputs.add(input);
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
                        case "token_outputs": {
                            jsonReader.beginArray();
                            while(jsonReader.hasNext()){
                                TokenOutput output = new TokenOutput();
                                Gson datagson = new GsonBuilder().registerTypeAdapter(TokenOutput.class, output.new TokenOutputTypeAdapter()).create();
                                output = datagson.fromJson(jsonReader, TokenOutput.class);
                                body.token_outputs.add(output);
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
                        case "token_kernels": {
                            jsonReader.beginArray();
                            while(jsonReader.hasNext()){
                                TokenTxKernel kernel = new TokenTxKernel();
                                Gson datagson = new GsonBuilder().registerTypeAdapter(TokenTxKernel.class, kernel.new TokenTxKernelTypeAdapter()).create();
                                kernel = datagson.fromJson(jsonReader, TokenTxKernel.class);
                                body.token_kernels.add(kernel);
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
        public byte[] excess = AppUtil.zeroByteArray(33);
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
                    buf.put((byte)features.code());
                    buf.putLong(fee);
                    break;
                case KernelFeatureCoinbase:
                    buf = ByteBuffer.allocate(1);
                    buf.order(BIG_ENDIAN);
                    buf.put((byte)features.code());
                    break;
                case KernelFeatureHeightLocked:
                    buf = ByteBuffer.allocate(17);
                    buf.order(BIG_ENDIAN);
                    buf.put((byte)features.code());
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
            ByteBuffer buf = ByteBuffer.allocate(256);
            buf.order(BIG_ENDIAN);
            byte feature = (byte)features.code();
            buf.put(feature);
            if (features == KernelFeatures.KernelFeaturePlain) {
                buf.putLong(fee);
            } else if (features == KernelFeatures.KernelFeatureHeightLocked) {
                buf.putLong(fee);
                buf.putLong(lock_height);
            }
            buf.put(excess);
            buf.put(excess_sig);

            buf.flip();
            return AppUtil.BufferToByteArr(buf);
        }

        public class TxKernelTypeAdapter extends TypeAdapter<TxKernel> {
            @Override
            public void write(JsonWriter jsonWriter, TxKernel kernel) throws IOException {
                String excessStr = AppUtil.hex(kernel.excess);
                byte[] compact_excess_sig = NativeSecp256k1.instance().signatureToCompactData(kernel.excess_sig);
                String excessSigStr = AppUtil.hex(compact_excess_sig);
                String featureStr = null;
                if (kernel.features == KernelFeatures.KernelFeaturePlain) {
                    HashMap<String, Map> featureMap = new HashMap<>();
                    HashMap<String, Long> feeMap = new HashMap<>();
                    feeMap.put("fee", kernel.fee);
                    featureMap.put("Plain", feeMap);
                    Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
                    featureStr = gson.toJson(featureMap);
                } else if (kernel.features == KernelFeatures.KernelFeatureCoinbase) {
                    featureStr = "Coinbase";
                } else if (kernel.features == KernelFeatures.KernelFeatureHeightLocked) {
                    HashMap<String, Map> featureMap = new HashMap<>();
                    HashMap<String, Long> feeMap = new HashMap<>();
                    feeMap.put("fee", kernel.fee);
                    feeMap.put("lock_height", kernel.lock_height);
                    featureMap.put("HeightLocked", feeMap);
                    Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
                    featureStr = gson.toJson(featureMap);
                }
                jsonWriter.beginObject();
                jsonWriter.name("excess").value(excessStr);
                jsonWriter.name("excess_sig").value(excessSigStr);
                jsonWriter.name("features").jsonValue(featureStr);
//                jsonWriter.name("fee").value(kernel.fee);
//                jsonWriter.name("lock_height").value(kernel.lock_height);
                jsonWriter.endObject();
            }

            @Override
            public TxKernel read(JsonReader jsonReader) throws IOException {
                TxKernel kernel = new TxKernel();
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    switch (jsonReader.nextName()) {
                        case "excess":
                            String excess_str = jsonReader.nextString();
                            kernel.excess = AppUtil.decode(excess_str);
                            break;

                        case "excess_sig":
                            String excess_sig_str = jsonReader.nextString();
                            byte[] compact_excess_sig = AppUtil.decode(excess_sig_str);
                            kernel.excess_sig = NativeSecp256k1.instance().compactDataToSignature(compact_excess_sig);
                            break;

                        case "features":
                            try {
                                String featureStr = jsonReader.nextString();
                                if (featureStr.equals("Coinbase")) {
                                    kernel.features = KernelFeatures.KernelFeatureCoinbase;
                                }
                            } catch (Exception e) {
                                Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
                                Type type = new TypeToken<Map<String, Map<String, Long>>>() {}.getType();
                                Map<String, Map<String, Long>> featureMap = gson.fromJson(jsonReader, type);
                                //String featureStr = jsonReader.nextString();
                                if (featureMap.get("Plain") != null) {
                                    kernel.features = KernelFeatures.KernelFeaturePlain;
                                    Map<String, Long> feeMap = featureMap.get("Plain");
//                                Type type2 = new TypeToken<Map<String, Long>>() {}.getType();
//                                Map<String, Long> feeMap = gson2.fromJson(feeStr, type2);
                                    kernel.fee = feeMap.get("fee");

                                } else {
                                    kernel.features = KernelFeatures.KernelFeatureHeightLocked;
                                    Map<String, Long> feeMap = featureMap.get("HeightLocked");
                                    kernel.fee = feeMap.get("fee");
                                    kernel.lock_height = feeMap.get("lock_height");
                                }
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
            byte feature = (byte)features.code();
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
                String commitStr = AppUtil.hex(output.commit);
                String proofStr = AppUtil.hex(output.proof);
                String featureStr = null;
                if (output.features == OutputFeatures.OutputFeaturePlain){
                    featureStr = "Plain";
                }
                else if(output.features == OutputFeatures.OutputFeatureCoinbase){
                    featureStr = "Coinbase";
                }
                jsonWriter.beginObject();
                jsonWriter.name("commit").value(commitStr);
                jsonWriter.name("features").value(featureStr);
                jsonWriter.name("proof").value(proofStr);
                jsonWriter.endObject();
            }

            @Override
            public Output read(JsonReader jsonReader) throws IOException {
                Output output = new Output();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "commit":
                            String commit_str = jsonReader.nextString();
                            output.commit = AppUtil.decode(commit_str);
                            break;

                        case "proof":
                            String proof_str = jsonReader.nextString();
                            output.proof = AppUtil.decode(proof_str);
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
            byte feature = (byte)features.code();
            buf.put(feature);
            buf.put(commit);

            return buf.array();
        }

        public class InputTypeAdapter extends TypeAdapter<Input> {
            @Override
            public void write(JsonWriter jsonWriter, Input input) throws IOException {
                String commitStr = AppUtil.hex(input.commit);
                String featureStr = null;
                if (input.features == OutputFeatures.OutputFeaturePlain){
                    featureStr = "Plain";
                }
                else if(input.features == OutputFeatures.OutputFeatureCoinbase){
                    featureStr = "Coinbase";
                }
                jsonWriter.beginObject();
                jsonWriter.name("commit").value(commitStr);
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
                            String commit_str = jsonReader.nextString();
                            input.commit = AppUtil.decode(commit_str);
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

    public class TokenTxKernel extends VcashTxBaseObject{
        public TokenKernelFeatures features;
        public String token_type;
        public long lock_height;
        public byte[] excess = AppUtil.zeroByteArray(32);
        public byte[] excess_sig = AppUtil.zeroByteArray(64);;

        public TokenKernelFeatures featureWithLockHeight(long lock_height){
            return lock_height>0?TokenKernelFeatures.KernelFeatureHeightLockedToken:TokenKernelFeatures.KernelFeaturePlainToken;
        }

        public byte[] kernelMsgToSign(){
            ByteBuffer buf = null;
            switch (features){
                case KernelFeatureHeightLockedToken:
                    buf = ByteBuffer.allocate(1+32+8);
                    buf.order(BIG_ENDIAN);
                    buf.put((byte)features.code());
                    buf.put(AppUtil.decode(token_type));
                    if (lock_height == 0){
                        buf.putLong(lock_height);
                    }
                    break;
                default:
                    buf = ByteBuffer.allocate(1+32);
                    buf.order(BIG_ENDIAN);
                    buf.put((byte)features.code());
                    buf.put(AppUtil.decode(token_type));
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
            ByteBuffer buf = ByteBuffer.allocate(256);
            buf.order(BIG_ENDIAN);
            byte feature = (byte)features.code();
            buf.put(feature);
            if (features == TokenKernelFeatures.KernelFeatureHeightLockedToken) {
                buf.putLong(lock_height);
            }
            buf.put(AppUtil.decode(token_type));
            buf.put(excess);
            buf.put(excess_sig);

            buf.flip();
            return AppUtil.BufferToByteArr(buf);
        }

        public class TokenTxKernelTypeAdapter extends TypeAdapter<TokenTxKernel> {
            @Override
            public void write(JsonWriter jsonWriter, TokenTxKernel kernel) throws IOException {
                String excessStr = AppUtil.hex(kernel.excess);
                byte[] compact_excess_sig = NativeSecp256k1.instance().signatureToCompactData(kernel.excess_sig);
                String excessSigStr = AppUtil.hex(compact_excess_sig);
                String featureStr = null;
                if (kernel.features == TokenKernelFeatures.KernelFeaturePlainToken) {
                    featureStr = "PlainToken";
                } else if (kernel.features == TokenKernelFeatures.KernelFeatureIssueToken) {
                    featureStr = "IssueToken";
                } else if (kernel.features == TokenKernelFeatures.KernelFeatureHeightLockedToken) {
                    featureStr = "HeightLockedToken";
                }
                jsonWriter.beginObject();
                jsonWriter.name("excess").value(excessStr);
                jsonWriter.name("excess_sig").value(excessSigStr);
                jsonWriter.name("features").value(featureStr);
                jsonWriter.name("token_type").value(kernel.token_type);
                jsonWriter.endObject();
            }

            @Override
            public TokenTxKernel read(JsonReader jsonReader) throws IOException {
                TokenTxKernel kernel = new TokenTxKernel();
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    switch (jsonReader.nextName()) {
                        case "token_type":
                            String token_type = jsonReader.nextString();
                            kernel.token_type = token_type;
                            break;

                        case "excess":
                            String excess_str = jsonReader.nextString();
                            kernel.excess = AppUtil.decode(excess_str);
                            break;

                        case "excess_sig":
                            String excess_sig_str = jsonReader.nextString();
                            byte[] compact_excess_sig = AppUtil.decode(excess_sig_str);
                            kernel.excess_sig = NativeSecp256k1.instance().compactDataToSignature(compact_excess_sig);
                            break;

                        case "features":
                            String featureStr = jsonReader.nextString();
                            if (featureStr.equals("PlainToken")) {
                                kernel.features = TokenKernelFeatures.KernelFeaturePlainToken;
                            } else if (featureStr.equals("IssueToken")) {
                                kernel.features = TokenKernelFeatures.KernelFeatureIssueToken;
                            } else {
                                kernel.features = TokenKernelFeatures.KernelFeatureHeightLockedToken;
                            }
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

    public class TokenOutput extends VcashTxBaseObject{
        public TokenOutputFeatures features;
        public String token_type;
        public byte[] commit;
        public byte[] proof;

        public byte[] computePayload(boolean isForHash){
            int length = 1+32+PEDERSEN_COMMITMENT_SIZE+8+MAX_PROOF_SIZE;
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(BIG_ENDIAN);
            byte feature = (byte)features.code();
            buf.put(feature);
            buf.put(AppUtil.decode(token_type));
            buf.put(commit);
            if (!isForHash){
                buf.putLong(proof.length);
                buf.put(proof);
            }

            buf.flip();
            return AppUtil.BufferToByteArr(buf);
        }

        public class TokenOutputTypeAdapter extends TypeAdapter<TokenOutput> {
            @Override
            public void write(JsonWriter jsonWriter, TokenOutput output) throws IOException {
                String commitStr = AppUtil.hex(output.commit);
                String proofStr = AppUtil.hex(output.proof);
                String featureStr = null;
                if (output.features == TokenOutputFeatures.OutputFeatureToken){
                    featureStr = "Token";
                }
                else if(output.features == TokenOutputFeatures.OutputFeatureTokenIssue){
                    featureStr = "TokenIssue";
                }
                jsonWriter.beginObject();
                jsonWriter.name("commit").value(commitStr);
                jsonWriter.name("token_type").value(output.token_type);
                jsonWriter.name("features").value(featureStr);
                jsonWriter.name("proof").value(proofStr);
                jsonWriter.endObject();
            }

            @Override
            public TokenOutput read(JsonReader jsonReader) throws IOException {
                TokenOutput output = new TokenOutput();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "commit":
                            String commit_str = jsonReader.nextString();
                            output.commit = AppUtil.decode(commit_str);
                            break;

                        case "token_type":
                            String token_type = jsonReader.nextString();
                            output.token_type = token_type;
                            break;

                        case "proof":
                            String proof_str = jsonReader.nextString();
                            output.proof = AppUtil.decode(proof_str);
                            break;

                        case "features":
                            String featureStr = jsonReader.nextString();
                            if (featureStr.equals("TokenIssue")){
                                output.features = TokenOutputFeatures.OutputFeatureTokenIssue;
                            }
                            else {
                                output.features = TokenOutputFeatures.OutputFeatureToken;
                            }
                            break;
                    }
                }
                jsonReader.endObject();
                return output;
            }
        }
    }

    public class TokenInput extends VcashTxBaseObject{
        public TokenOutputFeatures features;
        public String token_type;
        public byte[] commit;

        public byte[] computePayload(boolean isForHash){
            ByteBuffer buf = ByteBuffer.allocate(PEDERSEN_COMMITMENT_SIZE+1+32);
            byte feature = (byte)features.code();
            buf.put(feature);
            buf.put(AppUtil.decode(token_type));
            buf.put(commit);

            return buf.array();
        }

        public class TokenInputTypeAdapter extends TypeAdapter<TokenInput> {
            @Override
            public void write(JsonWriter jsonWriter, TokenInput input) throws IOException {
                String commitStr = AppUtil.hex(input.commit);
                String featureStr = null;
                if (input.features == TokenOutputFeatures.OutputFeatureToken){
                    featureStr = "Token";
                }
                else if(input.features == TokenOutputFeatures.OutputFeatureTokenIssue){
                    featureStr = "TokenIssue";
                }
                jsonWriter.beginObject();
                jsonWriter.name("commit").value(commitStr);
                jsonWriter.name("token_type").value(input.token_type);
                jsonWriter.name("features").value(featureStr);
                jsonWriter.endObject();
            }

            @Override
            public TokenInput read(JsonReader jsonReader) throws IOException {
                TokenInput input = new TokenInput();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "commit":
                            String commit_str = jsonReader.nextString();
                            input.commit = AppUtil.decode(commit_str);
                            break;

                        case "token_type":
                            String token_type = jsonReader.nextString();
                            input.token_type = token_type;
                            break;

                        case "features":
                            String featureStr = jsonReader.nextString();
                            if (featureStr.equals("Token")){
                                input.features = TokenOutputFeatures.OutputFeatureToken;
                            }
                            else {
                                input.features = TokenOutputFeatures.OutputFeatureTokenIssue;
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
        KernelFeaturePlain(0),
        KernelFeatureCoinbase(1),
        KernelFeatureHeightLocked(2);

        private final int code;

        KernelFeatures(int code) {
            this.code = code;
        }

        public static KernelFeatures locateEnum(int code) {
            for (KernelFeatures type: KernelFeatures.values()){
                if (code == type.code()){
                    return type;
                }
            }
            return null;
        }

        public int code() {
            return code;
        }
    }

    public enum TokenKernelFeatures{
        KernelFeaturePlainToken(0),
        KernelFeatureIssueToken(1),
        KernelFeatureHeightLockedToken(2);

        private final int code;

        TokenKernelFeatures(int code) {
            this.code = code;
        }

        public static TokenKernelFeatures locateEnum(int code) {
            for (TokenKernelFeatures type: TokenKernelFeatures.values()){
                if (code == type.code()){
                    return type;
                }
            }
            return null;
        }

        public int code() {
            return code;
        }
    }

    public enum OutputFeatures{
        OutputFeaturePlain(0),
        OutputFeatureCoinbase(1);

        private final int code;

        OutputFeatures(int code) {
            this.code = code;
        }

        public static OutputFeatures locateEnum(int code) {
            for (OutputFeatures type: OutputFeatures.values()){
                if (code == type.code()){
                    return type;
                }
            }
            return null;
        }

        public int code() {
            return code;
        }

    }

    public enum TokenOutputFeatures{
        OutputFeatureTokenIssue(98),
        OutputFeatureToken(99);

        private final int code;

        TokenOutputFeatures(int code) {
            this.code = code;
        }

        public static TokenOutputFeatures locateEnum(int code) {
            for (TokenOutputFeatures type: TokenOutputFeatures.values()){
                if (code == type.code()){
                    return type;
                }
            }
            return null;
        }

        public int code() {
            return code;
        }

    }
}
