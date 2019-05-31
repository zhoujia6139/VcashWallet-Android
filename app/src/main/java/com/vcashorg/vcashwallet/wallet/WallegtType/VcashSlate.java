package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;
import com.vcashorg.vcashwallet.wallet.VcashKeychain;
import com.vcashorg.vcashwallet.wallet.VcashKeychainPath;
import com.vcashorg.vcashwallet.wallet.VcashWallet;

import java.util.ArrayList;

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Locked;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Unconfirmed;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput.OutputStatus.Unspent;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTransaction.OutputFeatures.OutputFeatureCoinbase;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTransaction.OutputFeatures.OutputFeaturePlain;

public class VcashSlate {
    @SerializedName("id")
    public String uuid;
    public short num_participants;
    public long amount;
    public long fee;
    public long height;
    public long lock_height;
    @SerializedName("version")
    public long slate_version;
    public VcashTransaction tx;
    public ArrayList<ParticipantData> participant_data;

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
        kernel.lock_height = lock_height;
        tx.body.kernels.add(kernel);

        byte[][] positives = (byte[][])positiveArr.toArray();
        byte[][] negatives = (byte[][])negativeArr.toArray();
        return NativeSecp256k1.instance().bindSum(positives, negatives);
    }

    public byte[] addReceiverTxOutput(){
        byte[] secKey = createTxOutputWithAmount(amount);
        tx.sortTx();
        return secKey;
    }

    public boolean fillRound1(VcashContext context, int participant_id, String message){
        return true;
    }

    public boolean fillRound2(VcashContext context, int participant_id) {
        return true;
    }

    public byte[] finalizeSignature(){
        return null;
    }

    public boolean finalizeTx(byte[] finalSig){
        return false;
    }

    public void generateOffset(VcashContext context){
        byte[] seed = AppUtil.randomBytes(32);
        while (!NativeSecp256k1.instance().verifyEcSecretKey(seed)){
            seed = AppUtil.randomBytes(32);
        }

        tx.offset = seed;
        byte[][] positiveArr = new byte[1][];
        byte[][] negativeArr = new byte[1][];
        positiveArr[0] = AppUtil.decode(context.sec_key);
        negativeArr[0] = tx.offset;
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

    public class ParticipantData{
        @SerializedName("id")
        public short pId;
        public byte[] public_blind_excess;
        public byte[] public_nonce;
        public byte[] part_sig;
        public String message;
        public byte[] message_sig;
    }
}
