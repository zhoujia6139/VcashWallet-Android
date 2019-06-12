package com.vcashorg.vcashwallet.wallet;

import android.util.Log;

import com.vcashorg.vcashwallet.api.NodeApi;
import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashContext;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashProofInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashWalletInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletNoParamCallBack;

import org.bitcoinj.crypto.DeterministicKey;

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogConfirmType.DefaultState;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogEntryType.TxReceived;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogEntryType.TxSent;

public class VcashWallet {
    static private String Tag = "------VcashWallet";
    public ArrayList<VcashOutput> outputs = new ArrayList<>();

    final private long DEFAULT_BASE_FEE = 1000000;
    private static VcashWallet instance = null;
    public VcashKeychain mKeyChain;
    private VcashKeychainPath mKeyPath = new VcashKeychainPath(3, 0, 0, 0, 0);
    private long mChainHeight;
    private short mCurTxLogId;
    public String mUserId = null;
    private ArrayList<WalletNoParamCallBack> heightListener = new ArrayList<>();

    private VcashWallet(VcashKeychain keychain){
        mKeyChain = keychain;
        mUserId = this.createUserId();
        VcashWalletInfo info = EncryptedDBHelper.getsInstance().loadWalletInfo();
        if (info != null){
            mKeyPath = new VcashKeychainPath(3, AppUtil.decode(info.curKeyPath));
            mChainHeight = info.curHeight;
            mCurTxLogId = info.curTxLogId;
        }
        reloadOutputInfo();
        SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.USER_ID,mUserId);
    }

    public static void createVcashWallet(VcashKeychain keychain){
        if (instance == null){
            instance = new VcashWallet(keychain);
        }
    }

    public static VcashWallet getInstance(){
        return instance;
    }

    public byte[] getSignerKey(){
        DeterministicKey key = this.mKeyChain.deriveKey(new VcashKeychainPath(4, 0, 0, 0,0));
        return key.getPrivKeyBytes();
    }

    public long getChainHeight(){
        long height = NodeApi.getChainHeight(new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                NodeChainInfo info = (NodeChainInfo)data;
                if (yesOrNo && info.height > mChainHeight){
                    mChainHeight = info.height;
                    saveBaseInfo();
                    notifyChainHeightListener();
                }
            }
        });
        if (height > mChainHeight){
            mChainHeight = height;
        }
        return mChainHeight;
    }

    public void addChainHeightListener(WalletNoParamCallBack listener){
        heightListener.add(listener);
    }

    private void notifyChainHeightListener(){
        for (WalletNoParamCallBack listener:heightListener){
            listener.onCall();
        }
    }

    public VcashKeychainPath nextChild(){
        if (mKeyPath != null){
            mKeyPath = mKeyPath.nextPath();
            saveBaseInfo();
        }
        else{
            mKeyPath = new VcashKeychainPath(3, 0, 0, 0, 0);
        }

        return mKeyPath;
    }

    public short getNextLogId(){
        mCurTxLogId += 1;
        saveBaseInfo();
        return mCurTxLogId;
    }

    public void setChainOutputs(ArrayList<VcashOutput> chainOutputs){
        outputs = chainOutputs;

        VcashKeychainPath maxKeyPath = new VcashKeychainPath(3, 0, 0, 0, 0);
        for (VcashOutput item :outputs){
            VcashKeychainPath keyPath = new VcashKeychainPath(3, AppUtil.decode(item.keyPath));
            if (keyPath.compareTo(maxKeyPath) > 0){
                maxKeyPath = keyPath;
            }
        }
        mKeyPath = maxKeyPath;
        saveBaseInfo();
        syncOutputInfo();
    }

    public void addNewTxChangeOutput(VcashOutput output){
        outputs.add(output);
    }

    public void syncOutputInfo(){
        ArrayList<VcashOutput> arrayList = new ArrayList<VcashOutput>();
        for (VcashOutput output:outputs){
            if (output.status == VcashOutput.OutputStatus.Spent){
                Log.w(Tag, String.format("Output commit:%s has been spend, remove from wallet", output.commitment));
            }
            else {
                arrayList.add(output);
            }
        }
        outputs = arrayList;
        EncryptedDBHelper.getsInstance().saveOutputData(outputs);
    }

    public void reloadOutputInfo(){
        ArrayList<VcashOutput> outputData = EncryptedDBHelper.getsInstance().getActiveOutputData();
        if(outputData != null){
            outputs = EncryptedDBHelper.getsInstance().getActiveOutputData();
        }
    }

    public VcashOutput identifyUtxoOutput(NodeOutputs.NodeOutput nodeOutput){
        byte[] commit = AppUtil.decode(nodeOutput.commit);
        byte[] proof = AppUtil.decode(nodeOutput.proof);
        VcashProofInfo info = mKeyChain.rewindProof(commit, proof);
        if (info != null){
            VcashOutput output = new VcashOutput();
            output.commitment = nodeOutput.commit;
            output.keyPath = AppUtil.hex(info.msg);
            output.mmr_index = nodeOutput.mmr_index;
            output.value = info.value;
            output.height = nodeOutput.block_height;
            output.is_coinbase = (nodeOutput.output_type.equals("Coinbase"));
            if (output.is_coinbase){
                output.lock_height = nodeOutput.block_height + 144;
            }
            else {
                output.lock_height = nodeOutput.block_height;
            }
            output.status = VcashOutput.OutputStatus.Unspent;
            return output;
        }
        return null;
    }

    public void sendTransaction(long amount, long fee, final WalletCallback callback){
        long total = 0;
        ArrayList<VcashOutput> spendable = new ArrayList<VcashOutput>();
        for (VcashOutput item : outputs){
            if (item.isSpendable()){
                spendable.add(item);
                total += item.value;
            }
        }

        //1 Compute Fee and output
        // 1.1First attempt to spend without change
        long actualFee = fee;
        if (fee == 0){
            actualFee = calcuteFee(spendable.size(), 1);
        }
        long amount_with_fee = amount + actualFee;
        if (total < amount_with_fee){
            String errMsg = String.format("Not enough funds, available:%f, needed:%f", WalletApi.nanoToVcash(total), WalletApi.nanoToVcash(amount_with_fee));
            if (callback!=null){
                callback.onCall(false, errMsg);
            }
            return;
        }

        // 1.2Second attempt to spend with change
        if (total != amount_with_fee) {
            actualFee = calcuteFee(spendable.size(), 2);
        }
        amount_with_fee = amount + actualFee;
        long change = total - amount_with_fee;

        //2 fill txLog and slate
        VcashSlate slate = new VcashSlate();
        slate.num_participants = 2;
        slate.amount = amount;
        slate.height = mChainHeight;
        slate.lock_height = mChainHeight;
        slate.fee = actualFee;

        VcashTxLog txLog = new VcashTxLog();
        txLog.tx_id = getNextLogId();
        txLog.tx_slate_id = slate.uuid;
        txLog.tx_type = TxSent;
        txLog.create_time = AppUtil.getCurrentTimeSecs();
        txLog.fee = slate.fee;
        txLog.amount_credited = change;
        txLog.amount_debited = total;
        txLog.confirm_state = DefaultState;
        slate.txLog = txLog;

        byte[] blind = slate.addTxElement(spendable, change);
        if (blind == null){
            Log.e(Tag, "sender addTxElement failed");
            if (callback!=null){
                callback.onCall(false, null);
            }
            return;
        }

        //3 construct sender Context
        VcashContext context = new VcashContext();
        context.sec_key = AppUtil.hex(blind);
        context.slate_id = slate.uuid;
        slate.context = context;

        //4 sender fill round 1
        if (!slate.fillRound1(context, 0, null)){
            Log.e(Tag, "sender fillRound1 failed");
            if (callback!=null){
                callback.onCall(false, null);
            }
            return;
        }

        if (callback!=null){
            callback.onCall(true, slate);
        }
    }

    public boolean receiveTransaction(VcashSlate slate){
        //5, fill slate with receiver output
        VcashTxLog txLog = new VcashTxLog();
        txLog.tx_id = VcashWallet.getInstance().getNextLogId();
        txLog.tx_slate_id = slate.uuid;
        txLog.tx_type = TxReceived;
        txLog.create_time = AppUtil.getCurrentTimeSecs();
        txLog.fee = slate.fee;
        txLog.amount_credited = slate.amount;
        txLog.amount_debited = 0;
        txLog.confirm_state = DefaultState;
        slate.txLog = txLog;

        byte[] blind = slate.addReceiverTxOutput();
        if (blind == null){
            Log.e("", "--------receiver addReceiverTxOutput failed");
            return false;
        }

        //6, construct receiver Context
        VcashContext context = new VcashContext();
        context.sec_key = AppUtil.hex(blind);
        context.slate_id = slate.uuid;
        slate.context = context;

        //7, receiver fill round 1
        if (!slate.fillRound1(context, 1, null)){
            Log.e(Tag, "--------receiver fillRound1 failed");
            return false;
        }

        //8, receiver fill round 2
        if (!slate.fillRound2(context, 1)){
            Log.e(Tag, "--------receiver fillRound2 failed");
            return false;
        }

        return true;
    }

    public boolean finalizeTransaction(VcashSlate slate){
        //9, sender fill round 2
        if (!slate.fillRound2(slate.context, 0)){
            Log.e(Tag, "--------sender fillRound2 failed");
            return false;
        }

        //10, create group signature
        byte[] groupSig = slate.finalizeSignature();
        if (groupSig == null){
            Log.e(Tag, "--------sender create group signature failed");
            return false;
        }

        if (!slate.finalizeTx(groupSig)){
            Log.e(Tag, "--------sender finalize tx failed");
            return false;
        }

        return true;
    }


    private long calcuteFee(int inputCount, int outputCount){
        int tx_weight = outputCount*4 + 1 - inputCount;
        if (tx_weight < 1){
            tx_weight = 1;
        }

        return DEFAULT_BASE_FEE*tx_weight;
    }

    private String createUserId(){
        DeterministicKey key = this.mKeyChain.deriveKey(new VcashKeychainPath(4, 0, 0, 0,0));
        return AppUtil.hex(key.getPubKey());
    }

    private void saveBaseInfo(){
        VcashWalletInfo info = new VcashWalletInfo();
        info.curHeight = mChainHeight;
        info.curKeyPath = AppUtil.hex(mKeyPath.pathData());
        info.curTxLogId = mCurTxLogId;
        EncryptedDBHelper.getsInstance().saveWalletInfo(info);
    }
}
