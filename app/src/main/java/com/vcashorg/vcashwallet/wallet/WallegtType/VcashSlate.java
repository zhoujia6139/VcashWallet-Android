package com.vcashorg.vcashwallet.wallet.WallegtType;

import android.util.Log;

import com.fasterxml.uuid.Generators;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;
import com.vcashorg.vcashwallet.wallet.VcashKeychain;
import com.vcashorg.vcashwallet.wallet.VcashKeychainPath;
import com.vcashorg.vcashwallet.wallet.VcashWallet;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Locked;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Unconfirmed;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Unspent;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTransaction.OutputFeatures;

public class VcashSlate implements Serializable {
    private VersionCompatInfo version_info = new VersionCompatInfo(3, 3, 2);
    public String uuid = Generators.randomBasedGenerator().generate().toString();
    public short num_participants;
    public long amount;
    public String token_type;
    public long fee;
    public long height;
    public long lock_height;
    public VcashTransaction tx = new VcashTransaction();
    private ArrayList<ParticipantData> participant_data = new ArrayList<>();

    @Expose(serialize = false, deserialize = false)
    public VcashTxLog txLog;
    @Expose(serialize = false, deserialize = false)
    public VcashTokenTxLog tokenTxLog;
    @Expose(serialize = false, deserialize = false)
    public WalletNoParamCallBack lockOutputsFn;
    @Expose(serialize = false, deserialize = false)
    public WalletNoParamCallBack lockTokenOutputsFn;
    @Expose(serialize = false, deserialize = false)
    public WalletNoParamCallBack createNewOutputsFn;
    @Expose(serialize = false, deserialize = false)
    public WalletNoParamCallBack createNewTokenOutputsFn;
    @Expose(serialize = false, deserialize = false)
    public VcashContext context;

    public class VersionCompatInfo implements Serializable {
        private int version;
        private int orig_version;
        private int block_header_version;

        VersionCompatInfo(int ver, int orig, int header_version){
            version = ver;
            orig_version = orig;
            block_header_version = header_version;
        }
    }

    public byte[]addTxElement(ArrayList<VcashOutput> outputs, long change, boolean isForToken){
        VcashTransaction.TxKernel kernel = new VcashTransaction.TxKernel();
        ArrayList<byte[]> positiveArr = new ArrayList<>();
        ArrayList<byte[]> negativeArr = new ArrayList<>();

        //1,fee
        kernel.fee = fee;

        //2,input
        final ArrayList<VcashOutput> lockOutput = new ArrayList<>();
        for (VcashOutput item : outputs){
            if (item.status != Unspent){
                continue;
            }

            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(item.keyPath));
            byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(item.value, keypath, VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular);
            VcashTransaction.Input input = new VcashTransaction.Input();
            input.commit = commitment;
            input.features = (item.is_coinbase?OutputFeatures.OutputFeatureCoinbase:OutputFeatures.OutputFeaturePlain);

            tx.body.inputs.add(input);
            byte[] secKey = VcashWallet.getInstance().mKeyChain.deriveBindKey(item.value, keypath, VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular);
            negativeArr.add(secKey);

            lockOutput.add(item);
            if (isForToken) {
                tokenTxLog.appendInput(item.commitment);
            } else {
                txLog.appendInput(item.commitment);
            }
        }
        lockOutputsFn = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                for (VcashOutput item : lockOutput){
                    item.status = Locked;
                }
            }
        };

        //output
        if (change > 0){
            byte[] secKey = createTxOutputWithAmount(change, isForToken);
            positiveArr.add(secKey);
        }

        //lockheight
        kernel.setLock_height(lock_height);
        tx.body.kernels.add(kernel);

        return NativeSecp256k1.instance().bindSum(positiveArr, negativeArr);
    }

    public byte[]addTokenTxElement(ArrayList<VcashTokenOutput> token_outputs, long change){
        VcashTransaction.TokenTxKernel txKernel = new VcashTransaction.TokenTxKernel();
        ArrayList<byte[]> positiveArr = new ArrayList<>();
        ArrayList<byte[]> negativeArr = new ArrayList<>();

        final ArrayList<VcashTokenOutput> lockOutput = new ArrayList<>();
        for (VcashTokenOutput item : token_outputs){
            if (item.status != Unspent){
                continue;
            }

            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(item.keyPath));
            byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(item.value, keypath, VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular);
            VcashTransaction.TokenInput input = new VcashTransaction.TokenInput();
            input.token_type = item.token_type;
            input.commit = commitment;
            input.features = (item.is_token_issue? VcashTransaction.TokenOutputFeatures.OutputFeatureTokenIssue:VcashTransaction.TokenOutputFeatures.OutputFeatureToken);

            tx.body.token_inputs.add(input);
            byte[] secKey = VcashWallet.getInstance().mKeyChain.deriveBindKey(item.value, keypath, VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular);
            negativeArr.add(secKey);

            lockOutput.add(item);
            tokenTxLog.appendTokenInput(item.commitment);
            txKernel.token_type = item.token_type;

        }
        lockTokenOutputsFn = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                for (VcashTokenOutput item : lockOutput){
                    item.status = Locked;
                }
            }
        };

        //output
        if (change > 0){
            byte[] secKey = createTxTokenOutput(txKernel.token_type, change);
            positiveArr.add(secKey);
        }

        //lockheight
        txKernel.setLock_height(lock_height);
        tx.body.token_kernels.add(txKernel);

        return NativeSecp256k1.instance().bindSum(positiveArr, negativeArr);
    }

    public byte[] addReceiverTxOutput(){
        byte[] secKey;
        if (token_type != null) {
            secKey = createTxTokenOutput(token_type, amount);
        } else {
            secKey = createTxOutputWithAmount(amount, false);
        }
        tx.sortTx();
        return secKey;
    }

    public boolean fillRound1(VcashContext context, int participant_id, String message){
        if (tx.offset == null){
            generateOffset(context);
        }

        addParticipantInfo(context, participant_id, message);
        return true;
    }

    public boolean fillRound2(VcashContext context, int participant_id) {
        ArrayList<byte[]> pubNonceArr = new ArrayList<>();
        ArrayList<byte[]> pubBlindArr = new ArrayList<>();
        for (ParticipantData item: participant_data){
            pubNonceArr.add(item.public_nonce);
            pubBlindArr.add(item.public_blind_excess);
        }
        byte[] nonceSum = NativeSecp256k1.instance().combinationPubkey(pubNonceArr);
        byte[] keySum = NativeSecp256k1.instance().combinationPubkey(pubBlindArr);
        byte[] msgData;
        if (token_type != null) {
            msgData = createTokenMsgToSign();
        } else {
            msgData = createMsgToSign();
        }
        if (nonceSum == null || keySum == null || msgData == null){
            return false;
        }

        //1, verify part sig
        for (ParticipantData item: participant_data){
            if (item.part_sig != null){
                if (!NativeSecp256k1.instance().verifySingleSignature(item.part_sig, item.public_blind_excess, nonceSum, keySum, msgData)){
                    Log.e("------VcashSlate", String.format("verifySingleSignature failed! pId = %d", item.pId));
                    return false;
                }
            }
        }

        //2, calcluate part sig
        String sec_key;
        if (token_type != null) {
            sec_key = context.token_sec_key;
        } else {
            sec_key = context.sec_key;
        }
        byte[] sig = NativeSecp256k1.instance().calculateSingleSignature(AppUtil.decode(sec_key), AppUtil.decode(context.sec_nounce), nonceSum, keySum, msgData);
        if (sig == null){
            return false;
        }
        ParticipantData participantData = null;
        for (ParticipantData item : participant_data){
            if (item.pId == participant_id){
                participantData = item;
            }
        }
        participantData.part_sig = sig;
        if (!NativeSecp256k1.instance().verifySingleSignature(participantData.part_sig, participantData.public_blind_excess, nonceSum, keySum, msgData)){
            Log.e("------VcashSlate", String.format("verifySingleSignature failed! pId = %d", participantData.pId));
            return false;
        }

        return true;
    }

    public byte[] finalizeSignature(){
        ArrayList<byte[]> pubNonceArr = new ArrayList<>();
        ArrayList<byte[]> pubBlindArr = new ArrayList<>();
        ArrayList<byte[]> sigsArr = new ArrayList<>();
        for (ParticipantData item: participant_data){
            pubNonceArr.add(item.public_nonce);
            pubBlindArr.add(item.public_blind_excess);
            sigsArr.add(item.part_sig);
        }

        byte[] nonceSum = NativeSecp256k1.instance().combinationPubkey(pubNonceArr);
        byte[] keySum = NativeSecp256k1.instance().combinationPubkey(pubBlindArr);
        byte[] finalSig = NativeSecp256k1.instance().combinationSignatureAndNonceSum(sigsArr, nonceSum);
        byte[] msgData;
        if (token_type != null) {
            msgData = createTokenMsgToSign();
        } else {
            msgData = createMsgToSign();
        }
        if (finalSig != null && msgData != null){
            if (NativeSecp256k1.instance().verifySingleSignature(finalSig, keySum, null, keySum, msgData)){
                return finalSig;
            }
        }

        return null;
    }

    public boolean finalizeTx(byte[] finalSig){
        if (token_type != null) {
            byte[] final_excess = tx.calculateFinalExcess();
            byte[] pubKey = NativeSecp256k1.instance().commitToPubkey(final_excess);
            byte[] msgData = createMsgToSign();
            byte[] sig = NativeSecp256k1.instance().calculateSingleSignature(AppUtil.decode(context.sec_key), null, null, pubKey, msgData);
            if (!tx.setTxExcessAndandTxSig(final_excess, sig)){
                return false;
            }
            byte[] final_token_excess = tx.calculateTokenFinalExcess();
            if (tx.setTokenTxExcessAndandTxSig(final_token_excess, finalSig)){
                return true;
            }
        } else {
            byte[] final_excess = tx.calculateFinalExcess();
            if (tx.setTxExcessAndandTxSig(final_excess, finalSig)){
                return true;
            }
        }

        return false;
    }

    private void generateOffset(VcashContext context){
        byte[] seed = AppUtil.randomBytes(32);
        while (!NativeSecp256k1.instance().verifyEcSecretKey(seed)){
            seed = AppUtil.randomBytes(32);
        }

        tx.offset = seed;
        ArrayList<byte[]> positiveArr = new ArrayList<byte[]>();
        ArrayList<byte[]> negativeArr = new ArrayList<byte[]>();
        positiveArr.add(AppUtil.decode(context.sec_key));
        negativeArr.add(tx.offset);
        context.sec_key = AppUtil.hex(NativeSecp256k1.instance().bindSum(positiveArr, negativeArr));
    }

    private byte[] createMsgToSign(){
        VcashTransaction.TxKernel kernel = tx.body.kernels.get(0);
        return kernel.kernelMsgToSign();
    }

    private byte[] createTokenMsgToSign(){
        VcashTransaction.TokenTxKernel kernel = tx.body.token_kernels.get(0);
        return kernel.kernelMsgToSign();
    }

    private void addParticipantInfo(VcashContext context, int participant_id, String message){
        String sec_key;
        if (token_type != null) {
            sec_key = context.token_sec_key;
        } else {
            sec_key = context.sec_key;
        }
        byte[] pub_key = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(sec_key));
        byte[] pub_nonce = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(context.sec_nounce));
        ParticipantData partiData = new ParticipantData();
        partiData.pId = (short) participant_id;
        partiData.public_nonce = pub_nonce;
        partiData.public_blind_excess = pub_key;
        partiData.message = message;

        participant_data.add(partiData);
    }

    public boolean isValidForReceive(){
        if (participant_data.size() != 1){
            return false;
        }

        if (tx.body.inputs.size() == 0 || tx.body.kernels.size() == 0){
            return false;
        }

        return true;
    }

    public boolean isValidForFinalize(){
        if (participant_data.size() != 2){
            return false;
        }

        if (tx.body.inputs.size() == 0 || tx.body.kernels.size() == 0){
            return false;
        }

        return true;
    }

    private byte[] createTxOutputWithAmount(final long amount, final boolean isForToken){
        final VcashKeychainPath keypath = VcashWallet.getInstance().nextChild();
        final byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(amount, keypath, VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular);
        byte[] proof = VcashWallet.getInstance().mKeyChain.createRangeProof(amount, keypath);
        VcashTransaction.Output output = new VcashTransaction.Output();
        output.features = OutputFeatures.OutputFeaturePlain;
        output.commit = commitment;
        output.proof = proof;
        tx.body.outputs.add(output);

        createNewOutputsFn = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                VcashOutput output = new VcashOutput();
                output.commitment = AppUtil.hex(commitment);
                output.keyPath = AppUtil.hex(keypath.pathData());
                output.value = amount;
                output.height = height;
                output.lock_height = 0;
                output.is_coinbase = false;
                output.status = Unconfirmed;
                if (isForToken) {
                    output.tx_log_id = tokenTxLog.tx_id;
                    tokenTxLog.appendOutput(output.commitment);
                } else {
                    output.tx_log_id = txLog.tx_id;
                    txLog.appendOutput(output.commitment);
                }

                VcashWallet.getInstance().addNewTxChangeOutput(output);
            }
        };

        return VcashWallet.getInstance().mKeyChain.deriveBindKey(amount, keypath, VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular);
    }

    private byte[] createTxTokenOutput(final String tokenType, final long amount){
        final VcashKeychainPath keypath = VcashWallet.getInstance().nextChild();
        final byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(amount, keypath, VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular);
        byte[] proof = VcashWallet.getInstance().mKeyChain.createRangeProof(amount, keypath);
        VcashTransaction.TokenOutput output = new VcashTransaction.TokenOutput();
        output.token_type = tokenType;
        output.features = VcashTransaction.TokenOutputFeatures.OutputFeatureToken;
        output.commit = commitment;
        output.proof = proof;
        tx.body.token_outputs.add(output);

        createNewTokenOutputsFn = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                VcashTokenOutput output = new VcashTokenOutput();
                output.token_type = tokenType;
                output.commitment = AppUtil.hex(commitment);
                output.keyPath = AppUtil.hex(keypath.pathData());
                output.value = amount;
                output.height = height;
                output.lock_height = 0;
                output.is_token_issue = false;
                output.status = Unconfirmed;
                output.tx_log_id = tokenTxLog.tx_id;
                tokenTxLog.appendTokenOutput(output.commitment);

                VcashWallet.getInstance().addNewTokenTxChangeOutput(output);
            }
        };

        return VcashWallet.getInstance().mKeyChain.deriveBindKey(amount, keypath, VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular);
    }

    public static class VcashSlateTypeAdapter extends TypeAdapter<VcashSlate> {
        @Override
        public void write(JsonWriter jsonWriter, VcashSlate slate) throws IOException {
            if (slate != null) {
                jsonWriter.beginObject();
                jsonWriter.name("id").value(slate.uuid);
                jsonWriter.name("num_participants").value(slate.num_participants);
                jsonWriter.name("amount").value(slate.amount);
                jsonWriter.name("token_type").value(slate.token_type);
                jsonWriter.name("fee").value(slate.fee);
                jsonWriter.name("height").value(slate.height);
                jsonWriter.name("lock_height").value(slate.lock_height);
                Gson version_gson = new Gson();
                String version_str = version_gson.toJson(slate.version_info);
                jsonWriter.name("version_info").jsonValue(version_str);
                Gson gson = new GsonBuilder().registerTypeAdapter(VcashTransaction.class, new VcashTransaction.VcashTransactionTypeAdapter()).create();
                String jsonStr = gson.toJson(slate.tx);
                jsonWriter.name("tx").jsonValue(jsonStr);

                ParticipantData data = slate.participant_data.get(0);
                Gson gson1 = new GsonBuilder().serializeNulls().registerTypeAdapter(ParticipantData.class, new ParticipantData.ParticipantDataTypeAdapter()).create();
                String jsonStr1 = gson1.toJson(slate.participant_data, new TypeToken<ArrayList<ParticipantData>>(){}.getType());
                jsonWriter.name("participant_data").jsonValue(jsonStr1);
                jsonWriter.endObject();
            } else {
                jsonWriter.nullValue();
            }
        }

        @Override
        public VcashSlate read(JsonReader jsonReader) throws IOException {
            VcashSlate slate = new VcashSlate();
            jsonReader.beginObject();
            while (jsonReader.hasNext()){
                switch (jsonReader.nextName()){
                    case "id":
                        slate.uuid = jsonReader.nextString();
                        break;
                    case "num_participants":
                        slate.num_participants = (short) jsonReader.nextInt();
                        break;
                    case "amount":
                        slate.amount = jsonReader.nextLong();
                        break;
                    case "token_type":
                        //maybe null
                        try {
                            slate.token_type = jsonReader.nextString();
                        } catch (Exception e) {
                            jsonReader.nextNull();
                        }
                        break;
                    case "fee":
                        slate.fee = jsonReader.nextLong();
                        break;
                    case "height":
                        slate.height = jsonReader.nextLong();
                        break;
                    case "lock_height":
                        slate.lock_height = jsonReader.nextLong();
                        break;
                    case "version_info":
                        Gson version_gson = new Gson();
                        slate.version_info = version_gson.fromJson(jsonReader, VersionCompatInfo.class);
                        break;
                    case "tx":
                        Gson gson = new GsonBuilder().registerTypeAdapter(VcashTransaction.class, new VcashTransaction.VcashTransactionTypeAdapter()).create();
                        slate.tx = gson.fromJson(jsonReader, VcashTransaction.class);
                        break;
                    case "participant_data":
                        jsonReader.beginArray();
                        while(jsonReader.hasNext()){
                            Gson datagson = new GsonBuilder().registerTypeAdapter(ParticipantData.class, new ParticipantData.ParticipantDataTypeAdapter()).create();
                            ParticipantData pData = datagson.fromJson(jsonReader, ParticipantData.class);
                            slate.participant_data.add(pData);
                        }
                        jsonReader.endArray();
                        break;
                }
            }
            jsonReader.endObject();
            return slate;
        }
    }

    public static class ParticipantData implements Serializable{
        short pId;
        byte[] public_blind_excess;
        byte[] public_nonce;
        byte[] part_sig;
        public String message;
        byte[] message_sig;

        public static class ParticipantDataTypeAdapter extends TypeAdapter<ParticipantData> {
            @Override
            public void write(JsonWriter jsonWriter, ParticipantData data) throws IOException {
                jsonWriter.beginObject();
                jsonWriter.name("id").value(data.pId);
                jsonWriter.name("message").value(data.message);
                byte[] compressKey = NativeSecp256k1.instance().getCompressedPubkey(data.public_blind_excess);
                String public_blind_excessStr = AppUtil.hex(compressKey);
                jsonWriter.name("public_blind_excess").value(public_blind_excessStr);
                compressKey = NativeSecp256k1.instance().getCompressedPubkey(data.public_nonce);
                String public_nonceStr = AppUtil.hex(compressKey);
                jsonWriter.name("public_nonce").value(public_nonceStr);

                byte[] compressSig = NativeSecp256k1.instance().signatureToCompactData(data.part_sig);
                String part_sigStr = AppUtil.hex(compressSig);
                jsonWriter.name("part_sig").value(part_sigStr);
                compressSig = NativeSecp256k1.instance().signatureToCompactData(data.message_sig);
                String message_sigStr = AppUtil.hex(compressSig);
                jsonWriter.name("message_sig").value(message_sigStr);
                jsonWriter.endObject();
            }

            @Override
            public ParticipantData read(JsonReader jsonReader) throws IOException {
                ParticipantData data = new ParticipantData();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "id":
                            data.pId = (short) jsonReader.nextInt();
                            break;
                        case "message":
                            if (jsonReader.peek() != JsonToken.NULL) {
                                data.message = jsonReader.nextString();
                            }
                            else{
                                jsonReader.nextNull();
                            }
                            break;
                        case "public_blind_excess":
                            String bind_excess_str = jsonReader.nextString();
                            byte[] compressed_bind_excess = AppUtil.decode(bind_excess_str);
                            data.public_blind_excess = NativeSecp256k1.instance().pubkeyFromCompressedKey(compressed_bind_excess);
                            break;
                        case "public_nonce":
                            String public_nonce_str = jsonReader.nextString();
                            byte[] compressed_nonce = AppUtil.decode(public_nonce_str);
                            data.public_nonce = NativeSecp256k1.instance().pubkeyFromCompressedKey(compressed_nonce);
                            break;

                        case "part_sig":
                            if (jsonReader.peek() != JsonToken.NULL) {
                                String part_sig_str = jsonReader.nextString();
                                data.part_sig = AppUtil.decode(part_sig_str);
                                data.part_sig = NativeSecp256k1.instance().compactDataToSignature(data.part_sig);
                            }
                            else{
                                jsonReader.nextNull();
                            }

                            break;
                        case "message_sig":
                            if (jsonReader.peek() != JsonToken.NULL) {
                                String message_sig_str = jsonReader.nextString();
                                data.message_sig = AppUtil.decode(message_sig_str);
                                data.message_sig = NativeSecp256k1.instance().compactDataToSignature(data.message_sig);
                            }
                            else{
                                jsonReader.nextNull();
                            }
                            break;
                    }
                }
                jsonReader.endObject();
                return data;
            }
        }
    }
}
