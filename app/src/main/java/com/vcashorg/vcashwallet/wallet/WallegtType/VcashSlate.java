package com.vcashorg.vcashwallet.wallet.WallegtType;

import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;
import com.vcashorg.vcashwallet.wallet.VcashKeychainPath;
import com.vcashorg.vcashwallet.wallet.VcashWallet;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Locked;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Unconfirmed;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Unspent;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTransaction.OutputFeatures.OutputFeatureCoinbase;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTransaction.OutputFeatures.OutputFeaturePlain;

public class VcashSlate implements Serializable {
    public String uuid = AppUtil.hex(AppUtil.randomBytes(16));
    public short num_participants;
    public long amount;
    public long fee;
    public long height;
    public long lock_height;
    public long slate_version = 1;
    public VcashTransaction tx = new VcashTransaction();
    public ArrayList<ParticipantData> participant_data = new ArrayList<>();

    @Expose(serialize = false, deserialize = false)
    public VcashTxLog txLog;
    @Expose(serialize = false, deserialize = false)
    public WalletNoParamCallBack lockOutputsFn;
    @Expose(serialize = false, deserialize = false)
    public WalletNoParamCallBack createNewOutputsFn;
    @Expose(serialize = false, deserialize = false)
    public VcashContext context;

    public byte[]addTxElement(ArrayList<VcashOutput> outputs, long change){
        VcashTransaction.TxKernel kernel = tx.new TxKernel();
        ArrayList<byte[]> positiveArr = new ArrayList<byte[]>();
        ArrayList<byte[]> negativeArr = new ArrayList<byte[]>();

        //1,fee
        kernel.fee = fee;

        //2,input
        final ArrayList<VcashOutput> lockOutput = new ArrayList<VcashOutput>();
        for (VcashOutput item : outputs){
            if (item.status != Unspent){
                continue;
            }

            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(item.keyPath));
            byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(item.value, keypath);
            String temp = AppUtil.hex(commitment);
            VcashTransaction.Input input = tx.new Input();
            input.commit = commitment;
            input.features = (item.is_coinbase?OutputFeatureCoinbase:OutputFeaturePlain);

            tx.body.inputs.add(input);
            byte[] secKey = VcashWallet.getInstance().mKeyChain.deriveBindKey(item.value, keypath);
            negativeArr.add(secKey);

            lockOutput.add(item);
            txLog.appendInput(item.commitment);
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
            byte[] secKey = createTxOutputWithAmount(change);
            positiveArr.add(secKey);
        }

        //lockheight
        kernel.setLock_height(lock_height);
        tx.body.kernels.add(kernel);

        return NativeSecp256k1.instance().bindSum(positiveArr, negativeArr);
    }

    public byte[] addReceiverTxOutput(){
        byte[] secKey = createTxOutputWithAmount(amount);
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
        byte[] msgData = createMsgToSign();
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
        byte[] sig = NativeSecp256k1.instance().calculateSingleSignature(AppUtil.decode(context.sec_key), AppUtil.decode(context.sec_nounce), nonceSum, keySum, msgData);
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
        byte[] msgData = createMsgToSign();
        if (finalSig != null && msgData != null){
            if (NativeSecp256k1.instance().verifySingleSignature(finalSig, keySum, null, keySum, msgData)){
                return finalSig;
            }
        }

        return null;
    }

    public boolean finalizeTx(byte[] finalSig){
        byte[] final_excess = tx.calculateFinalExcess();
        if (tx.setTxExcessAndandTxSig(final_excess, finalSig)){
            return true;
        }
        return false;
    }

    public void generateOffset(VcashContext context){
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

    public byte[] createMsgToSign(){
        VcashTransaction.TxKernel kernel = tx.body.kernels.get(0);
        return kernel.kernelMsgToSign();
    }

    public void addParticipantInfo(VcashContext context, int participant_id, String message){
        byte[] pub_key = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(context.sec_key));
        byte[] pub_nonce = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(context.sec_nounce));
        ParticipantData partiData = new ParticipantData();
        partiData.pId = (short) participant_id;
        partiData.public_nonce = pub_nonce;
        partiData.public_blind_excess = pub_key;
        partiData.message = message;

        participant_data.add(partiData);
    }

    private byte[] createTxOutputWithAmount(final long amount){
        final VcashKeychainPath keypath = VcashWallet.getInstance().nextChild();
        final byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(amount, keypath);
        byte[] proof = VcashWallet.getInstance().mKeyChain.createRangeProof(amount, keypath);
        VcashTransaction.Output output = tx.new Output();
        output.features = OutputFeaturePlain;
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
                output.tx_log_id = txLog.tx_id;
                txLog.appendOutput(output.commitment);

                VcashWallet.getInstance().addNewTxChangeOutput(output);
                VcashWallet.getInstance().syncOutputInfo();
            }
        };

        return VcashWallet.getInstance().mKeyChain.deriveBindKey(amount, keypath);
    }

    public class VcashSlateTypeAdapter extends TypeAdapter<VcashSlate> {
        @Override
        public void write(JsonWriter jsonWriter, VcashSlate slate) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name("id").value(slate.uuid);
            jsonWriter.name("num_participants").value(slate.num_participants);
            jsonWriter.name("amount").value(slate.amount);
            jsonWriter.name("fee").value(slate.fee);
            jsonWriter.name("height").value(slate.height);
            jsonWriter.name("lock_height").value(slate.lock_height);
            jsonWriter.name("version").value(slate.slate_version);
            Gson gson = new GsonBuilder().registerTypeAdapter(VcashTransaction.class, tx.new VcashTransactionTypeAdapter()).create();
            String jsonStr = gson.toJson(slate.tx);
            jsonStr = StringEscapeUtils.unescapeJson(jsonStr);
            jsonWriter.name("tx").jsonValue(jsonStr);

            ParticipantData data = slate.participant_data.get(0);
            Gson gson1 = new GsonBuilder().registerTypeAdapter(ParticipantData.class, data.new ParticipantDataTypeAdapter()).create();
            String jsonStr1 = gson1.toJson(slate.participant_data, new TypeToken<ArrayList<ParticipantData>>(){}.getType());
            jsonStr1 = StringEscapeUtils.unescapeJson(jsonStr1);
            jsonWriter.name("participant_data").jsonValue(jsonStr1);
            jsonWriter.endObject();
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
                    case "fee":
                        slate.fee = jsonReader.nextLong();
                        break;
                    case "height":
                        slate.height = jsonReader.nextLong();
                        break;
                    case "lock_height":
                        slate.lock_height = jsonReader.nextLong();
                        break;
                    case "version":
                        slate.slate_version = jsonReader.nextLong();
                        break;
                    case "tx":
                        Gson gson = new GsonBuilder().registerTypeAdapter(VcashTransaction.class, tx.new VcashTransactionTypeAdapter()).create();
                        slate.tx = gson.fromJson(jsonReader, VcashTransaction.class);
                        break;
                    case "participant_data":
                        jsonReader.beginArray();
                        while(jsonReader.hasNext()){
                            ParticipantData pData = new ParticipantData();
                            Gson datagson = new GsonBuilder().registerTypeAdapter(ParticipantData.class, pData.new ParticipantDataTypeAdapter()).create();
                            pData = datagson.fromJson(jsonReader, ParticipantData.class);
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

    public class ParticipantData implements Serializable{
        public short pId;
        public byte[] public_blind_excess;
        public byte[] public_nonce;
        public byte[] part_sig;
        public String message;
        public byte[] message_sig;

        public class ParticipantDataTypeAdapter extends TypeAdapter<ParticipantData> {
            @Override
            public void write(JsonWriter jsonWriter, ParticipantData data) throws IOException {
                jsonWriter.beginObject();
                jsonWriter.name("id").value(data.pId);
                jsonWriter.name("message").jsonValue(data.message);
                byte[] compressKey = NativeSecp256k1.instance().getCompressedPubkey(data.public_blind_excess);
                String public_blind_excessStr = AppUtil.getByteStrFromByteArr(compressKey);
                jsonWriter.name("public_blind_excess").jsonValue(public_blind_excessStr);
                compressKey = NativeSecp256k1.instance().getCompressedPubkey(data.public_nonce);
                String public_nonceStr = AppUtil.getByteStrFromByteArr(compressKey);
                jsonWriter.name("public_nonce").jsonValue(public_nonceStr);

                byte[] compressSig = NativeSecp256k1.instance().signatureToCompactData(data.part_sig);
                String part_sigStr = AppUtil.getByteStrFromByteArr(compressSig);
                jsonWriter.name("part_sig").jsonValue(part_sigStr);
                compressSig = NativeSecp256k1.instance().signatureToCompactData(data.message_sig);
                String message_sigStr = AppUtil.getByteStrFromByteArr(compressSig);
                jsonWriter.name("message_sig").jsonValue(message_sigStr);
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
                            data.message = jsonReader.nextString();
                            break;
                        case "public_blind_excess":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                byte[] compressedKey = AppUtil.intArrToByteArr(arrayList);
                                data.public_blind_excess = NativeSecp256k1.instance().pubkeyFromCompressedKey(compressedKey);
                            }
                            else{
                                jsonReader.nextNull();
                            }

                            break;
                        case "public_nonce":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList1 = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList1.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                byte[] compressedKey1 = AppUtil.intArrToByteArr(arrayList1);
                                data.public_nonce = NativeSecp256k1.instance().pubkeyFromCompressedKey(compressedKey1);
                            }
                            else{
                                jsonReader.nextNull();
                            }

                            break;
                        case "part_sig":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList2 = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList2.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                byte[] compressedData = AppUtil.intArrToByteArr(arrayList2);
                                data.part_sig = NativeSecp256k1.instance().compactDataToSignature(compressedData);
                            }
                            else{
                                jsonReader.nextNull();
                            }

                            break;
                        case "message_sig":
                            if (jsonReader.peek() != JsonToken.NULL){
                                jsonReader.beginArray();
                                ArrayList<Integer> arrayList3 = new ArrayList<>();
                                while(jsonReader.hasNext()){
                                    arrayList3.add(jsonReader.nextInt());
                                }
                                jsonReader.endArray();
                                byte[] compressedData1 = AppUtil.intArrToByteArr(arrayList3);
                                data.message_sig = NativeSecp256k1.instance().compactDataToSignature(compressedData1);
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
