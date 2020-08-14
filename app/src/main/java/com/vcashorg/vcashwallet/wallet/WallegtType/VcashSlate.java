package com.vcashorg.vcashwallet.wallet.WallegtType;

import android.util.Log;

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
import com.vcashorg.vcashwallet.wallet.PaymentProof;
import com.vcashorg.vcashwallet.wallet.VcashKeychain;
import com.vcashorg.vcashwallet.wallet.VcashKeychainPath;
import com.vcashorg.vcashwallet.wallet.VcashWallet;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.vcashorg.vcashwallet.wallet.VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Locked;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Unconfirmed;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Unspent;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTransaction.OutputFeatures;
import static java.nio.ByteOrder.BIG_ENDIAN;

public class VcashSlate implements Serializable {
    private VersionCompatInfo version_info = new VersionCompatInfo(4, 3);
    public short num_participants = 2;
    public String uuid = PaymentProof.createUUID();
    public SlateState state;
    public long amount;
    public String token_type;
    public long fee;
    public long ttl_cutoff_height;
    public byte kernel_features;
    public byte token_kernel_features;
    public byte[] offset = new byte[32];
    private ArrayList<ParticipantData> participant_data = new ArrayList<>();
    public PaymentInfo payment_proof;
    public KernelFeaturesArgs kernel_features_args;
    public KernelFeaturesArgs token_kernel_features_args;

    @Expose(serialize = false, deserialize = false)
    public VcashTransaction tx = new VcashTransaction();
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
    @Expose(serialize = false, deserialize = false)
    public String partnerAddress;

    public static class VersionCompatInfo implements Serializable {
        private int version;
        private int block_header_version;

        VersionCompatInfo(int ver, int header_version){
            version = ver;
            block_header_version = header_version;
        }
    }

    public enum SlateState{
        Unknown(0, "NA"),
        Standard1(1, "S1"),
        Standard2(2, "S2"),
        Standard3(3, "S3"),
        Invoice1(4, "I1"),
        Invoice2(5, "I2"),
        Invoice3(6, "I3");

        private final int code;
        private final String tag;

        SlateState(int code, String tag) {
            this.code = code;
            this.tag = tag;
        }

        public static SlateState locateEnum(int code) {
            for (SlateState type: SlateState.values()){
                if (code == type.code()){
                    return type;
                }
            }
            return null;
        }

        public static SlateState locateEnumByTag(String tag) {
            for (SlateState type: SlateState.values()){
                if (tag.equals(type.tag)){
                    return type;
                }
            }
            return null;
        }

        public int code() {
            return code;
        }

        public String tag() {
            return tag;
        }
    }

    static public VcashSlate parseSlateFromData(byte[] binData) {
        if (binData.length < 10) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap(binData);
        VcashSlate slate = new VcashSlate();

        //version
        short version = buf.getShort();
        short header_version = buf.getShort();
        VersionCompatInfo info = new VersionCompatInfo(version, header_version);

        //uuid
        byte[] uuid_data = new byte[16];
        buf.get(uuid_data, 0, 16);
        slate.uuid = PaymentProof.UUIDFromBytes(uuid_data);

        //state
        slate.state = SlateState.locateEnum(buf.get());

        //offset
        byte[] offset_data = new byte[32];
        buf.get(offset_data, 0, 32);
        slate.offset = offset_data;

        //SlateOptFields
        byte status = buf.get();
        if ((status & 0x01) > 0){
            slate.num_participants = buf.get();
        }
        if ((status & 0x02) > 0){
            slate.amount = buf.getLong();
        }
        if ((status & 0x04) > 0){
            slate.fee = buf.getLong();
        }
        if ((status & 0x08) > 0){
            slate.kernel_features = buf.get();
        }
        if ((status & 0x10) > 0){
            slate.ttl_cutoff_height = buf.getLong();
        }
        if ((status & 0x20) > 0){
            slate.token_kernel_features = buf.get();
        }

        //SigsWrapRef
        byte pDatalen = buf.get();
        for (byte i=0; i<pDatalen; i++) {
            ParticipantData pData = new ParticipantData();
            byte has_partial = buf.get();

            byte[] public_blind_excess = new byte[33];
            buf.get(public_blind_excess, 0, 33);
            pData.public_blind_excess = NativeSecp256k1.instance().pubkeyFromCompressedKey(public_blind_excess);

            byte[] public_nonce = new byte[33];
            buf.get(public_nonce, 0, 33);
            pData.public_nonce = NativeSecp256k1.instance().pubkeyFromCompressedKey(public_nonce);

            if (has_partial > 0) {
                byte[] sig_data = new byte[64];
                buf.get(sig_data, 0, 64);
                pData.part_sig = sig_data;
            }
            slate.participant_data.add(pData);
        }

        //SlateOptStructsRef
        byte struct_status = buf.get();
        if ((struct_status & 0x01) > 0){
            short commit_len = buf.getShort();

            for (short i=0; i<commit_len; i++) {
                byte has_proof = buf.get();

                byte features = buf.get();

                byte[] commit = new byte[33];
                buf.get(commit, 0, 33);

                if (has_proof > 0) {
                    int proof_len = (int)buf.getLong();

                    byte[] proof = new byte[proof_len];
                    buf.get(proof, 0, proof_len);

                    VcashTransaction.Output output = new VcashTransaction.Output();
                    output.proof = proof;
                    output.features = OutputFeatures.locateEnum((int)features);
                    output.commit = commit;
                    slate.tx.body.outputs.add(output);
                } else {
                    VcashTransaction.Input input = new VcashTransaction.Input();
                    input.features = OutputFeatures.locateEnum((int)features);
                    input.commit = commit;
                    slate.tx.body.inputs.add(input);
                }
            }
        }
        if ((struct_status & 0x02) > 0){
            PaymentInfo paymentInfo = new PaymentInfo();

            byte[] sender_address = new byte[32];
            buf.get(sender_address, 0, 32);
            paymentInfo.sender_address = AppUtil.hex(sender_address);

            byte[] receiver_address = new byte[32];
            buf.get(receiver_address, 0, 32);
            paymentInfo.receiver_address = AppUtil.hex(receiver_address);

            byte has_sig = buf.get();
            if (has_sig > 0) {
                byte[] signature = new byte[64];
                buf.get(signature, 0, 64);
                paymentInfo.receiver_signature = AppUtil.hex(signature);
            }
            slate.payment_proof = paymentInfo;
        }
        if ((struct_status & 0x04) > 0){
            byte[] token_type = new byte[32];
            buf.get(token_type, 0, 32);
            slate.token_type = AppUtil.hex(token_type);
        }
        if ((struct_status & 0x08) > 0){
            short commit_len = buf.getShort();

            for (short i=0; i<commit_len; i++) {
                byte has_proof = buf.get();

                byte[] token_type = new byte[32];
                buf.get(token_type, 0, 32);

                byte features = buf.get();

                byte[] commit = new byte[33];
                buf.get(commit, 0, 33);

                if (has_proof > 0) {
                    int proof_len = (int)buf.getLong();

                    byte[] proof = new byte[proof_len];
                    buf.get(proof, 0, proof_len);

                    VcashTransaction.TokenOutput output = new VcashTransaction.TokenOutput();
                    output.token_type = AppUtil.hex(token_type);
                    output.proof = proof;
                    output.features = VcashTransaction.TokenOutputFeatures.locateEnum((int)features);
                    output.commit = commit;
                    slate.tx.body.token_outputs.add(output);
                } else {
                    VcashTransaction.TokenInput input = new VcashTransaction.TokenInput();
                    input.token_type = AppUtil.hex(token_type);
                    input.features = VcashTransaction.TokenOutputFeatures.locateEnum((int)features);
                    input.commit = commit;
                    slate.tx.body.token_inputs.add(input);
                }
            }
        }

        //feat
        if (slate.kernel_features == 2) {
            slate.kernel_features_args = new KernelFeaturesArgs();
            slate.kernel_features_args.lock_height = buf.getLong();
        }

        //token feat
        if (slate.token_kernel_features == 2) {
            slate.token_kernel_features_args = new KernelFeaturesArgs();
            slate.token_kernel_features_args.lock_height = buf.getLong();
        }

        return slate;
    }

    public byte[] selializeAsData(){
        ByteBuffer buf = ByteBuffer.allocate(200000);
        buf.order(BIG_ENDIAN);

        //version
        buf.putShort((short) this.version_info.version);

        //header version
        buf.putShort((short) this.version_info.block_header_version);

        //uuid
        byte[] uuidData = PaymentProof.bytesFromUUID(this.uuid);
        buf.put(uuidData);

        //state
        buf.put((byte) this.state.code());

        //offset
        buf.put(this.offset);

        //SlateOptFields
        byte status = 0;
        if (this.num_participants != 2) {
            status |= 0x01;
        }
        if (this.amount > 0) {
            status |= 0x02;
        }
        if (this.fee > 0) {
            status |= 0x04;
        }
        if (this.kernel_features > 0) {
            status |= 0x08;
        }
        if (this.ttl_cutoff_height > 0) {
            status |= 0x10;
        }
        if (this.token_kernel_features > 0) {
            status |= 0x20;
        }
        buf.put(status);
        if (this.num_participants != 2) {
            buf.put((byte)this.num_participants);
        }
        if (this.amount > 0) {
            buf.putLong(this.amount);
        }
        if (this.fee > 0) {
            buf.putLong(this.fee);
        }
        if (this.kernel_features > 0) {
            buf.put(this.kernel_features);
        }
        if (this.ttl_cutoff_height > 0) {
            buf.putLong(this.ttl_cutoff_height);
        }
        if (this.token_kernel_features > 0) {
            buf.put(this.token_kernel_features);
        }

        //SigsWrapRef
        buf.put((byte) this.participant_data.size());
        for (ParticipantData item: this.participant_data) {
            byte has_sig = 0;
            if (item.part_sig != null) {
                has_sig = 1;
            }
            buf.put(has_sig);
            buf.put(NativeSecp256k1.instance().getCompressedPubkey(item.public_blind_excess));
            buf.put(NativeSecp256k1.instance().getCompressedPubkey(item.public_nonce));
            if (item.part_sig != null) {
                buf.put(item.part_sig);
            }
        }

        //SlateOptStructsRef
        byte struct_status = 0;
        {
            //coms
            struct_status |= 0x01;

            //proof
            if (this.payment_proof != null) {
                struct_status |= 0x02;
            }
            //token_type
            if (this.token_type != null) {
                struct_status |= 0x04;
            }

            //token coms
            struct_status |= 0x08;
        }
        buf.put(struct_status);
        {
            //coms
            int commitCount = 0;
            if (this.tx != null) {
                commitCount = this.tx.body.inputs.size() + this.tx.body.outputs.size();
                buf.putShort((short)commitCount);

                for (VcashTransaction.Input input : this.tx.body.inputs) {
                    byte hasProof = 0;
                    buf.put(hasProof);
                    buf.put((byte) input.features.code());
                    buf.put(input.commit);
                }

                for (VcashTransaction.Output output : this.tx.body.outputs) {
                    byte hasProof = 1;
                    buf.put(hasProof);
                    buf.put((byte) output.features.code());
                    buf.put(output.commit);
                    buf.putLong(output.proof.length);
                    buf.put(output.proof);
                }
            } else {
                buf.putShort((short)commitCount);
            }

            //proof
            if (this.payment_proof != null) {
                buf.put(AppUtil.decode(this.payment_proof.sender_address));
                buf.put(AppUtil.decode(this.payment_proof.receiver_address));
                byte has_sig = 0;
                if (this.payment_proof.receiver_signature != null) {
                    has_sig = 1;
                    buf.put(has_sig);
                    buf.put(AppUtil.decode(this.payment_proof.receiver_signature));
                } else {
                    buf.put(has_sig);
                }
            }

            //token_type
            if (this.token_type != null) {
                buf.put(AppUtil.decode(this.token_type));
            }

            //token coms
            int tokenCommitCount = 0;
            if (this.tx != null) {
                tokenCommitCount = this.tx.body.token_inputs.size() + this.tx.body.token_outputs.size();
                buf.putShort((short)tokenCommitCount);

                for (VcashTransaction.TokenInput input : this.tx.body.token_inputs) {
                    byte hasProof = 0;
                    buf.put(hasProof);
                    buf.put(AppUtil.decode(input.token_type));
                    buf.put((byte) input.features.code());
                    buf.put(input.commit);
                }

                for (VcashTransaction.TokenOutput output : this.tx.body.token_outputs) {
                    byte hasProof = 1;
                    buf.put(hasProof);
                    buf.put(AppUtil.decode(output.token_type));
                    buf.put((byte) output.features.code());
                    buf.put(output.commit);
                    buf.putLong(output.proof.length);
                    buf.put(output.proof);
                }
            } else {
                buf.putShort((short)tokenCommitCount);
            }
        }

        //feat
        if (this.kernel_features == 2) {
            buf.putLong(this.kernel_features_args.lock_height);
        }

        //token feat
        if (this.token_kernel_features == 2) {
            buf.putLong(this.token_kernel_features_args.lock_height);
        }

        buf.flip();
        return AppUtil.BufferToByteArr(buf);
    }

    public byte[]addTxElement(ArrayList<VcashOutput> outputs, long change, VcashCommitId changeCommitId, boolean isForToken){
        VcashTransaction.TxKernel kernel = new VcashTransaction.TxKernel();
        ArrayList<byte[]> positiveArr = new ArrayList<>();
        ArrayList<byte[]> negativeArr = new ArrayList<>();

        //2,input
        final ArrayList<VcashOutput> lockOutput = new ArrayList<>();
        for (VcashOutput item : outputs){
            if (item.status != Unspent){
                continue;
            }

            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(item.keyPath));
            byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(item.value, keypath, SwitchCommitmentTypeRegular);
            VcashTransaction.Input input = new VcashTransaction.Input();
            input.commit = commitment;
            input.features = (item.is_coinbase?OutputFeatures.OutputFeatureCoinbase:OutputFeatures.OutputFeaturePlain);

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
            byte[] secKey = createTxOutputWithAmount(change, changeCommitId, isForToken);
            positiveArr.add(secKey);
        }

        return NativeSecp256k1.instance().bindSum(positiveArr, negativeArr);
    }

    public byte[]addTokenTxElement(ArrayList<VcashTokenOutput> token_outputs, long change, VcashCommitId changeCommitId){
        ArrayList<byte[]> positiveArr = new ArrayList<>();
        ArrayList<byte[]> negativeArr = new ArrayList<>();

        final ArrayList<VcashTokenOutput> lockOutput = new ArrayList<>();
        for (VcashTokenOutput item : token_outputs){
            if (item.status != Unspent){
                continue;
            }

            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(item.keyPath));
            byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(item.value, keypath, SwitchCommitmentTypeRegular);
            VcashTransaction.TokenInput input = new VcashTransaction.TokenInput();
            input.token_type = item.token_type;
            input.commit = commitment;
            input.features = (item.is_token_issue? VcashTransaction.TokenOutputFeatures.OutputFeatureTokenIssue:VcashTransaction.TokenOutputFeatures.OutputFeatureToken);

            byte[] secKey = VcashWallet.getInstance().mKeyChain.deriveBindKey(item.value, keypath, SwitchCommitmentTypeRegular);
            negativeArr.add(secKey);

            lockOutput.add(item);
            tokenTxLog.appendTokenInput(item.commitment);
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
            byte[] secKey = createTxTokenOutput(this.token_type, change, changeCommitId);
            positiveArr.add(secKey);
        }

        return NativeSecp256k1.instance().bindSum(positiveArr, negativeArr);
    }

    public byte[] addReceiverTxOutput(VcashCommitId changeCommitId){
        byte[] secKey;
        if (token_type != null) {
            secKey = createTxTokenOutput(token_type, amount, changeCommitId);
        } else {
            secKey = createTxOutputWithAmount(amount, changeCommitId, false);
        }
        return secKey;
    }

    public boolean fillRound1(VcashContext context){
        if (this.token_type == null || this.participant_data.size() == 0){
            generateOffset(context);
        }

        addParticipantInfo(context);
        return true;
    }

    public boolean fillRound2(VcashContext context) {
        byte[] nonceSum = this.participantNounceSum();
        byte[] keySum = this.participantPublicBlindSum();
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
                    Log.e("------VcashSlate", String.format("verifySingleSignature failed! "));
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

        byte[] pub_key = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(sec_key));
        byte[] pub_nonce = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(context.sec_nounce));
        for (ParticipantData item : participant_data){
            if (Arrays.equals(pub_key, item.public_blind_excess) &&
            Arrays.equals(pub_nonce, item.public_nonce)){
                item.part_sig = sig;
                if (!NativeSecp256k1.instance().verifySingleSignature(item.part_sig, item.public_blind_excess, nonceSum, keySum, msgData)){
                    Log.e("------VcashSlate", "verifySingleSignature failed!");
                    return false;
                }
            }
        }

        return true;
    }

    public void removeOtherSigdata() {
        String sec_key;
        if (token_type != null) {
            sec_key = context.token_sec_key;
        } else {
            sec_key = context.sec_key;
        }

        byte[] pub_key = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(sec_key));
        byte[] pub_nonce = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(context.sec_nounce));

        ParticipantData myData = null;
        for (ParticipantData item : this.participant_data) {
            if (Arrays.equals(pub_key, item.public_blind_excess) &&
                    Arrays.equals(pub_nonce, item.public_nonce)){
                myData = item;
            }
        }

        this.participant_data.clear();
        this.participant_data.add(myData);
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
            byte[] final_token_excess = this.calculateExcess();
            if (tx.setTokenTxExcessAndandTxSig(final_token_excess, finalSig)){
                return true;
            }
        } else {
            byte[] final_excess = this.calculateExcess();
            if (tx.setTxExcessAndandTxSig(final_excess, finalSig)){
                return true;
            }
        }

        return false;
    }

    public void subInputFromOffset(VcashContext context){
        ArrayList<byte[]> positiveArr = new ArrayList<>();
        ArrayList<byte[]> negativeArr = new ArrayList<>();

        positiveArr.add(this.offset);

        for (VcashCommitId commitId: context.input_ids) {
            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(commitId.keyPath));
            byte[] key = VcashWallet.getInstance().mKeyChain.deriveBindKey(commitId.value, keypath, SwitchCommitmentTypeRegular);
            negativeArr.add(key);
        }

        byte[] newOffset = NativeSecp256k1.instance().bindSum(positiveArr, negativeArr);
        this.offset = newOffset;
        this.tx.offset = offset;
    }

    public void repopulateTx(VcashContext context){
        this.amount = context.amout;
        this.fee = context.fee;

        String sec_key;
        if (token_type != null) {
            sec_key = context.token_sec_key;
        } else {
            sec_key = context.sec_key;
        }

        if (this.participant_data.size() == 1) {
            byte[] pub_key = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(sec_key));
            byte[] pub_nonce = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(context.sec_nounce));

            ParticipantData item = new ParticipantData();
            item.public_blind_excess = pub_key;
            item.public_nonce = pub_nonce;
            this.participant_data.add(item);
        }

        for (VcashCommitId commitId: context.input_ids) {
            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(commitId.keyPath));
            byte[] commit = VcashWallet.getInstance().mKeyChain.createCommitment(commitId.value, keypath, SwitchCommitmentTypeRegular);

            VcashOutput output = VcashWallet.getInstance().findOutputByCommit(AppUtil.hex(commit));

            VcashTransaction.Input input = new VcashTransaction.Input();
            input.commit = commit;
            input.features = output.is_coinbase?OutputFeatures.OutputFeatureCoinbase:OutputFeatures.OutputFeaturePlain;
            this.tx.body.inputs.add(input);
        }

        for (VcashCommitId commitId: context.token_input_ids) {
            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(commitId.keyPath));
            byte[] commit = VcashWallet.getInstance().mKeyChain.createCommitment(commitId.value, keypath, SwitchCommitmentTypeRegular);

            VcashTokenOutput output = VcashWallet.getInstance().findTokenOutputByCommit(AppUtil.hex(commit));

            VcashTransaction.TokenInput input = new VcashTransaction.TokenInput();
            input.token_type = output.token_type;
            input.commit = commit;
            input.features = output.is_token_issue? VcashTransaction.TokenOutputFeatures.OutputFeatureTokenIssue: VcashTransaction.TokenOutputFeatures.OutputFeatureToken;
            this.tx.body.token_inputs.add(input);
        }

        for (VcashCommitId commitId: context.output_ids) {
            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(commitId.keyPath));
            byte[] commit = VcashWallet.getInstance().mKeyChain.createCommitment(commitId.value, keypath, SwitchCommitmentTypeRegular);
            byte[] proof = VcashWallet.getInstance().mKeyChain.createRangeProof(commitId.value, keypath);

            VcashTransaction.Output output = new VcashTransaction.Output();
            output.commit = commit;
            output.features = OutputFeatures.OutputFeaturePlain;
            output.proof = proof;

            this.tx.body.outputs.add(output);
        }

        for (VcashCommitId commitId: context.token_output_ids) {
            VcashKeychainPath keypath = new VcashKeychainPath(3, AppUtil.decode(commitId.keyPath));
            byte[] commit = VcashWallet.getInstance().mKeyChain.createCommitment(commitId.value, keypath, SwitchCommitmentTypeRegular);
            byte[] proof = VcashWallet.getInstance().mKeyChain.createRangeProof(commitId.value, keypath);

            VcashTransaction.TokenOutput output = new VcashTransaction.TokenOutput();
            output.token_type = this.token_type;
            output.commit = commit;
            output.features = VcashTransaction.TokenOutputFeatures.OutputFeatureToken;
            output.proof = proof;

            this.tx.body.token_outputs.add(output);
        }

        VcashTransaction.TxKernel kernel = new VcashTransaction.TxKernel();
        kernel.features = VcashTransaction.KernelFeatures.locateEnum((int)this.kernel_features);
        kernel.fee = this.fee;
        if (this.kernel_features_args != null) {
            kernel.lock_height = this.kernel_features_args.lock_height;
        }
        this.tx.body.kernels.add(kernel);

        if (this.token_type != null) {
            VcashTransaction.TokenTxKernel tokenKernel = new VcashTransaction.TokenTxKernel();
            tokenKernel.features = VcashTransaction.TokenKernelFeatures.locateEnum((int)this.token_kernel_features);
            tokenKernel.token_type = this.token_type;
            if (this.token_kernel_features_args != null) {
                tokenKernel.lock_height = this.token_kernel_features_args.lock_height;
            }
            this.tx.body.token_kernels.add(tokenKernel);
        }
    }

    private void generateOffset(VcashContext context){
        byte[] seed = AppUtil.randomBytes(32);
        while (!NativeSecp256k1.instance().verifyEcSecretKey(seed)){
            seed = AppUtil.randomBytes(32);
        }

        ArrayList<byte[]> offsetArr = new ArrayList<byte[]>();
        offsetArr.add(this.offset);
        offsetArr.add(seed);
        byte[] totalOffset = NativeSecp256k1.instance().bindSum(offsetArr, null);
        this.offset = totalOffset;

        ArrayList<byte[]> positiveArr = new ArrayList<byte[]>();
        ArrayList<byte[]> negativeArr = new ArrayList<byte[]>();
        positiveArr.add(AppUtil.decode(context.sec_key));
        negativeArr.add(seed);
        context.sec_key = AppUtil.hex(NativeSecp256k1.instance().bindSum(positiveArr, negativeArr));
    }

    private byte[] createMsgToSign(){
        VcashTransaction.TxKernel kernel = new VcashTransaction.TxKernel();
        kernel.features = VcashTransaction.KernelFeatures.locateEnum(this.kernel_features);
        kernel.fee = this.fee;
        if (this.kernel_features_args != null) {
            kernel.lock_height = this.kernel_features_args.lock_height;
        }
        return kernel.kernelMsgToSign();
    }

    private byte[] createTokenMsgToSign(){
        VcashTransaction.TokenTxKernel kernel = new VcashTransaction.TokenTxKernel();
        kernel.features = VcashTransaction.TokenKernelFeatures.locateEnum(this.token_kernel_features);
        kernel.token_type = this.token_type;
        if (this.token_kernel_features_args != null) {
            kernel.lock_height = this.token_kernel_features_args.lock_height;
        }
        return kernel.kernelMsgToSign();
    }

    private void addParticipantInfo(VcashContext context){
        String sec_key;
        if (token_type != null) {
            sec_key = context.token_sec_key;
        } else {
            sec_key = context.sec_key;
        }
        byte[] pub_key = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(sec_key));
        byte[] pub_nonce = NativeSecp256k1.instance().getPubkeyFromSecretKey(AppUtil.decode(context.sec_nounce));
        ParticipantData partiData = new ParticipantData();
        partiData.public_nonce = pub_nonce;
        partiData.public_blind_excess = pub_key;

        participant_data.add(partiData);
    }

    public byte[] calculateExcess(){
        byte[] keySum = this.participantPublicBlindSum();
        byte[] commit = NativeSecp256k1.instance().pubkeyToCommit(keySum);

        return commit;
    }

    private byte[] participantPublicBlindSum(){
        ArrayList<byte[]> pubBlindArr = new ArrayList<>();
        for (ParticipantData item: this.participant_data) {
            pubBlindArr.add(item.public_blind_excess);
        }
        return NativeSecp256k1.instance().combinationPubkey(pubBlindArr);
    }

    private byte[] participantNounceSum(){
        ArrayList<byte[]> pubNonceArr = new ArrayList<>();
        for (ParticipantData item: this.participant_data) {
            pubNonceArr.add(item.public_nonce);
        }
        return NativeSecp256k1.instance().combinationPubkey(pubNonceArr);
    }


    public boolean isValidForReceive(){
        if (participant_data.size() != 1){
            return false;
        }

        if (this.amount == 0 || this.fee == 0){
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

    private byte[] createTxOutputWithAmount(final long amount, VcashCommitId changeCommitId, final boolean isForToken){
        final VcashKeychainPath keypath = VcashWallet.getInstance().nextChild();
        final byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(amount, keypath, SwitchCommitmentTypeRegular);
        byte[] proof = VcashWallet.getInstance().mKeyChain.createRangeProof(amount, keypath);
        VcashTransaction.Output output = new VcashTransaction.Output();
        output.features = OutputFeatures.OutputFeaturePlain;
        output.commit = commitment;
        output.proof = proof;
        tx.body.outputs.add(output);

        changeCommitId.keyPath = AppUtil.hex(keypath.pathData());;
        changeCommitId.value = amount;

        createNewOutputsFn = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                VcashOutput output = new VcashOutput();
                output.commitment = AppUtil.hex(commitment);
                output.keyPath = AppUtil.hex(keypath.pathData());
                output.value = amount;
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

        return VcashWallet.getInstance().mKeyChain.deriveBindKey(amount, keypath, SwitchCommitmentTypeRegular);
    }

    private byte[] createTxTokenOutput(final String tokenType, final long amount, VcashCommitId changeCommitId){
        final VcashKeychainPath keypath = VcashWallet.getInstance().nextChild();
        final byte[] commitment = VcashWallet.getInstance().mKeyChain.createCommitment(amount, keypath, SwitchCommitmentTypeRegular);
        byte[] proof = VcashWallet.getInstance().mKeyChain.createRangeProof(amount, keypath);
        VcashTransaction.TokenOutput output = new VcashTransaction.TokenOutput();
        output.token_type = tokenType;
        output.features = VcashTransaction.TokenOutputFeatures.OutputFeatureToken;
        output.commit = commitment;
        output.proof = proof;
        tx.body.token_outputs.add(output);

        changeCommitId.keyPath = AppUtil.hex(keypath.pathData());;
        changeCommitId.value = amount;

        createNewTokenOutputsFn = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                VcashTokenOutput output = new VcashTokenOutput();
                output.token_type = tokenType;
                output.commitment = AppUtil.hex(commitment);
                output.keyPath = AppUtil.hex(keypath.pathData());
                output.value = amount;
                output.lock_height = 0;
                output.is_token_issue = false;
                output.status = Unconfirmed;
                output.tx_log_id = tokenTxLog.tx_id;
                tokenTxLog.appendTokenOutput(output.commitment);

                VcashWallet.getInstance().addNewTokenTxChangeOutput(output);
            }
        };

        return VcashWallet.getInstance().mKeyChain.deriveBindKey(amount, keypath, SwitchCommitmentTypeRegular);
    }

    public static class VcashSlateTypeAdapter extends TypeAdapter<VcashSlate> {
        @Override
        public void write(JsonWriter jsonWriter, VcashSlate slate) throws IOException {
            if (slate != null) {
                Gson common_gson = new GsonBuilder().serializeNulls().create();
                jsonWriter.beginObject();
                jsonWriter.name("ver").value("4:3");
                jsonWriter.name("id").value(slate.uuid);
                jsonWriter.name("sta").value(slate.state.tag);
                jsonWriter.name("off").value(AppUtil.hex(slate.offset));
                if (slate.num_participants != 2) {
                    jsonWriter.name("num_parts").value(slate.num_participants);
                }
                if (slate.amount != 0) {
                    jsonWriter.name("amt").value(slate.amount);
                }
                if (slate.token_type != null) {
                    jsonWriter.name("token_type").value(slate.token_type);
                }
                if (slate.fee != 0) {
                    jsonWriter.name("fee").value(slate.fee);
                }
                if (slate.kernel_features != 0) {
                    jsonWriter.name("feat").value(slate.kernel_features);
                }
                if (slate.token_kernel_features != 0) {
                    jsonWriter.name("token_feat").value(slate.token_kernel_features);
                }
                if (slate.ttl_cutoff_height != 0) {
                    jsonWriter.name("ttl").value(slate.ttl_cutoff_height);
                }
                if (slate.participant_data.size() > 0) {
                    ParticipantData data = slate.participant_data.get(0);
                    Gson gson1 = new GsonBuilder().serializeNulls().registerTypeAdapter(ParticipantData.class, new ParticipantData.ParticipantDataTypeAdapter()).create();
                    String jsonStr1 = gson1.toJson(slate.participant_data, new TypeToken<ArrayList<ParticipantData>>(){}.getType());
                    jsonWriter.name("sigs").jsonValue(jsonStr1);
                }
                if (slate.tx != null) {
                    int commitCount = slate.tx.body.inputs.size() + slate.tx.body.token_inputs.size();
                    if (commitCount > 0) {
                        ArrayList<HashMap> arrayList = new ArrayList<>();
                        for (VcashTransaction.Input input : slate.tx.body.inputs) {
                            HashMap map = new HashMap();
                            map.put("c", AppUtil.hex(input.commit));
                            if (input.features != OutputFeatures.OutputFeaturePlain) {
                                map.put("f", input.features.code());
                            }
                            arrayList.add(map);
                        }
                        for (VcashTransaction.Output output : slate.tx.body.outputs) {
                            HashMap map = new HashMap();
                            map.put("c", AppUtil.hex(output.commit));
                            map.put("p", AppUtil.hex(output.proof));
                            if (output.features != OutputFeatures.OutputFeaturePlain) {
                                map.put("f", output.features.code());
                            }
                            arrayList.add(map);
                        }
                        String coms = common_gson.toJson(arrayList);
                        jsonWriter.name("coms").value(coms);
                    }

                    int tokenCommitCount = slate.tx.body.token_inputs.size() + slate.tx.body.token_outputs.size();
                    if (tokenCommitCount > 0) {
                        ArrayList<HashMap> arrayList = new ArrayList<>();
                        for (VcashTransaction.TokenInput input : slate.tx.body.token_inputs) {
                            HashMap map = new HashMap();
                            map.put("k", input.token_type);
                            map.put("c", AppUtil.hex(input.commit));
                            if (input.features != VcashTransaction.TokenOutputFeatures.OutputFeatureToken) {
                                map.put("f", input.features.code());
                            }
                            arrayList.add(map);
                        }
                        for (VcashTransaction.TokenOutput output : slate.tx.body.token_outputs) {
                            HashMap map = new HashMap();
                            map.put("k", output.token_type);
                            map.put("c", AppUtil.hex(output.commit));
                            map.put("p", AppUtil.hex(output.proof));
                            if (output.features != VcashTransaction.TokenOutputFeatures.OutputFeatureToken) {
                                map.put("f", output.features.code());
                            }
                            arrayList.add(map);
                        }
                        String coms = common_gson.toJson(arrayList);
                        jsonWriter.name("token_coms").value(coms);
                    }
                }

                if (slate.payment_proof != null) {
                    Gson paymentproof_gson = new GsonBuilder().serializeNulls().create();
                    String paymentproof_str = paymentproof_gson.toJson(slate.payment_proof);
                    jsonWriter.name("proof").jsonValue(paymentproof_str);
                }

                if (slate.kernel_features_args != null) {
                    HashMap map = new HashMap();
                    map.put("lock_hgt", slate.kernel_features_args.lock_height);
                    String feat_str = common_gson.toJson(map);
                    jsonWriter.name("feat_args").jsonValue(feat_str);
                }

                if (slate.token_kernel_features_args != null) {
                    HashMap map = new HashMap();
                    map.put("lock_hgt", slate.token_kernel_features_args.lock_height);
                    String feat_str = common_gson.toJson(map);
                    jsonWriter.name("token_feat_args").jsonValue(feat_str);
                }
                jsonWriter.endObject();
            } else {
                jsonWriter.nullValue();
            }
        }

        @Override
        public VcashSlate read(JsonReader jsonReader) throws IOException {
            VcashSlate slate = new VcashSlate();
            Gson common_gson = new GsonBuilder().serializeNulls().create();
            jsonReader.beginObject();
            while (jsonReader.hasNext()){
                switch (jsonReader.nextName()){
                    case "ver":
                        String ver = jsonReader.nextString();
                        break;
                    case "id":
                        slate.uuid = jsonReader.nextString();
                        break;
                    case "sta":
                        String sta = jsonReader.nextString();
                        slate.state = SlateState.locateEnumByTag(sta);
                        break;
                    case "off":
                        String offset = jsonReader.nextString();
                        slate.offset = AppUtil.decode(offset);
                        break;
                    case "num_parts":
                        slate.num_participants = (short) jsonReader.nextInt();
                        break;
                    case "amt":
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
                    case "feat":
                        slate.kernel_features = (byte) jsonReader.nextInt();
                        break;
                    case "token_feat":
                        slate.token_kernel_features = (byte) jsonReader.nextInt();
                        break;
                    case "ttl":
                        try {
                            slate.ttl_cutoff_height = jsonReader.nextLong();
                        } catch (Exception e) {
                            jsonReader.nextNull();
                        }
                        break;
                    case "sigs":
                        jsonReader.beginArray();
                        while(jsonReader.hasNext()){
                            Gson datagson = new GsonBuilder().registerTypeAdapter(ParticipantData.class, new ParticipantData.ParticipantDataTypeAdapter()).create();
                            ParticipantData pData = datagson.fromJson(jsonReader, ParticipantData.class);
                            slate.participant_data.add(pData);
                        }
                        jsonReader.endArray();
                        break;

                    case "coms":
                        jsonReader.beginArray();
                        while(jsonReader.hasNext()){
                            jsonReader.beginObject();
                            byte[] commit = null;
                            int feature = 0;
                            byte[] proof = null;
                            while (jsonReader.hasNext()){
                                switch (jsonReader.nextName()) {
                                    case "c":
                                        commit = AppUtil.decode(jsonReader.nextString());
                                        break;
                                    case "f":
                                        feature = jsonReader.nextInt();
                                        break;
                                    case "p":
                                        proof = AppUtil.decode(jsonReader.nextString());
                                        break;
                                }
                            }
                            jsonReader.endObject();
                            if (proof != null) {
                                VcashTransaction.Output output = new  VcashTransaction.Output();
                                output.commit = commit;
                                output.proof = proof;
                                output.features = OutputFeatures.locateEnum(feature);
                                slate.tx.body.outputs.add(output);
                            } else {
                                VcashTransaction.Input input = new  VcashTransaction.Input();
                                input.commit = commit;
                                input.features = OutputFeatures.locateEnum(feature);
                                slate.tx.body.inputs.add(input);
                            }
                        }
                        jsonReader.endArray();
                        break;
                    case "token_coms":
                        jsonReader.beginArray();
                        while(jsonReader.hasNext()){
                            jsonReader.beginObject();
                            byte[] commit = null;
                            int feature = 0;
                            byte[] proof = null;
                            String token_type = null;
                            while (jsonReader.hasNext()){
                                switch (jsonReader.nextName()) {
                                    case "c":
                                        commit = AppUtil.decode(jsonReader.nextString());
                                        break;
                                    case "f":
                                        feature = jsonReader.nextInt();
                                        break;
                                    case "p":
                                        proof = AppUtil.decode(jsonReader.nextString());
                                        break;
                                    case "k":
                                        token_type = jsonReader.nextString();
                                        break;
                                }
                            }
                            jsonReader.endObject();
                            if (proof != null) {
                                VcashTransaction.TokenOutput output = new  VcashTransaction.TokenOutput();
                                output.token_type = token_type;
                                output.commit = commit;
                                output.proof = proof;
                                output.features = VcashTransaction.TokenOutputFeatures.locateEnum(feature);
                                slate.tx.body.token_outputs.add(output);
                            } else {
                                VcashTransaction.TokenInput input = new  VcashTransaction.TokenInput();
                                input.token_type = token_type;
                                input.commit = commit;
                                input.features = VcashTransaction.TokenOutputFeatures.locateEnum(feature);
                                slate.tx.body.token_inputs.add(input);
                            }
                        }
                        jsonReader.endArray();
                        break;
                    case "proof":
                        slate.payment_proof = common_gson.fromJson(jsonReader, PaymentInfo.class);
                        break;
                    case "feat_args":
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()){
                            switch (jsonReader.nextName()) {
                                case "lock_hgt":
                                    long lock_height = jsonReader.nextLong();
                                    KernelFeaturesArgs args = new KernelFeaturesArgs();
                                    args.lock_height = lock_height;
                                    slate.kernel_features_args = args;
                                    break;
                            }
                        }
                        jsonReader.endObject();
                        break;
                    case "token_feat_args":
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()){
                            switch (jsonReader.nextName()) {
                                case "lock_hgt":
                                    long lock_height = jsonReader.nextLong();
                                    KernelFeaturesArgs args = new KernelFeaturesArgs();
                                    args.lock_height = lock_height;
                                    slate.token_kernel_features_args = args;
                                    break;
                            }
                        }
                        jsonReader.endObject();
                        break;
                }
            }
            jsonReader.endObject();
            return slate;
        }
    }

    public static class ParticipantData implements Serializable{
        byte[] public_blind_excess;
        byte[] public_nonce;
        byte[] part_sig;

        public static class ParticipantDataTypeAdapter extends TypeAdapter<ParticipantData> {
            @Override
            public void write(JsonWriter jsonWriter, ParticipantData data) throws IOException {
                jsonWriter.beginObject();
                byte[] compressKey = NativeSecp256k1.instance().getCompressedPubkey(data.public_blind_excess);
                String public_blind_excessStr = AppUtil.hex(compressKey);
                jsonWriter.name("xs").value(public_blind_excessStr);

                compressKey = NativeSecp256k1.instance().getCompressedPubkey(data.public_nonce);
                String public_nonceStr = AppUtil.hex(compressKey);
                jsonWriter.name("nonce").value(public_nonceStr);

                if (data.part_sig != null) {
                    byte[] compressSig = NativeSecp256k1.instance().signatureToCompactData(data.part_sig);
                    String part_sigStr = AppUtil.hex(compressSig);
                    jsonWriter.name("part").value(part_sigStr);
                }
                jsonWriter.endObject();
            }

            @Override
            public ParticipantData read(JsonReader jsonReader) throws IOException {
                ParticipantData data = new ParticipantData();
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    switch (jsonReader.nextName()){
                        case "xs":
                            String bind_excess_str = jsonReader.nextString();
                            byte[] compressed_bind_excess = AppUtil.decode(bind_excess_str);
                            data.public_blind_excess = NativeSecp256k1.instance().pubkeyFromCompressedKey(compressed_bind_excess);
                            break;
                        case "nonce":
                            String public_nonce_str = jsonReader.nextString();
                            byte[] compressed_nonce = AppUtil.decode(public_nonce_str);
                            data.public_nonce = NativeSecp256k1.instance().pubkeyFromCompressedKey(compressed_nonce);
                            break;

                        case "part":
                            if (jsonReader.peek() != JsonToken.NULL) {
                                String part_sig_str = jsonReader.nextString();
                                data.part_sig = AppUtil.decode(part_sig_str);
                                data.part_sig = NativeSecp256k1.instance().compactDataToSignature(data.part_sig);
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

    public static class PaymentInfo implements Serializable{
        public String sender_address;
        public String receiver_address;
        public String receiver_signature;


    }
}
