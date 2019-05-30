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

    private byte[] createTxOutputWithAmount(long amount){
        return null;
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
