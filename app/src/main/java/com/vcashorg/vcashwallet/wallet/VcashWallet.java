package com.vcashorg.vcashwallet.wallet;

import android.util.Log;

import com.vcashorg.vcashwallet.api.NodeApi;
import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
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

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogConfirmType.DefaultState;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogEntryType.TxSent;

public class VcashWallet {
    public ArrayList<VcashOutput> outputs;

    final private long DEFAULT_BASE_FEE = 1000000;
    private static VcashWallet instance = null;
    public VcashKeychain mKeyChain;
    private VcashKeychainPath mKeyPath;
    private long mChainHeight;
    private short mCurTxLogId;
    public String mUserId = null;

    private VcashWallet(VcashKeychain keychain){
        mKeyChain = keychain;
        mUserId = this.createUserId();
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

    public long getChainHeight(){
        long height = NodeApi.getChainHeight(new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                NodeChainInfo info = (NodeChainInfo)data;
                if (yesOrNo && info.height > mChainHeight){
                    mChainHeight = info.height;
                    saveBaseInfo();
                }
            }
        });
        if (height > mChainHeight){
            mChainHeight = height;
        }
        return mChainHeight;
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
                Log.w("", String.format("Output commit:%s has been spend, remove from wallet", output.commitment));
            }
            else {
                arrayList.add(output);
            }
        }
        outputs = arrayList;
        EncryptedDBHelper.getsInstance().saveOutputData(outputs);
    }

    public void reloadOutputInfo(){
        outputs = EncryptedDBHelper.getsInstance().getActiveOutputData();
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
        }
        return null;
    }

    public void sendTransaction(String targetUserId, long amount, long fee, final WalletCallback callback){
        long total = 0;
        ArrayList<VcashOutput> arrayList = new ArrayList<VcashOutput>();
        for (VcashOutput item : outputs){
            if (item.isSpendable()){
                arrayList.add(item);
                total += item.value;
            }
        }

        //1 Compute Fee and output
        // 1.1First attempt to spend without change
        long actualFee = fee;
        if (fee == 0){
            actualFee = calcuteFee(arrayList.size(), 1);
        }
        long amount_with_fee = amount + actualFee;
        if (total < amount_with_fee){
            String errMsg = String.format("Not enough funds, available:%d, needed:%d", WalletApi.nanoToVcash(total), WalletApi.nanoToVcash(amount_with_fee));
            if (callback!=null){
                callback.onCall(false, errMsg);
            }
            return;
        }

        // 1.2Second attempt to spend with change
        if (total != amount_with_fee) {
            actualFee = calcuteFee(arrayList.size(), 2);
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
        txLog.create_time = Calendar.getInstance().getTimeInMillis()/1000;
        txLog.fee = slate.fee;
        txLog.amount_credited = change;
        txLog.amount_debited = total;
        txLog.confirm_state = DefaultState;
        slate.txLog = txLog;
    }

    public boolean receiveTransaction(VcashSlate slate){
        return false;
    }

    public boolean finalizeTransaction(VcashSlate slate){
        return false;
    }


    private long calcuteFee(int inputCount, int outputCount){
        int tx_weight = outputCount*4 + 1 - inputCount;
        if (tx_weight < 1){
            tx_weight = 1;
        }

        return DEFAULT_BASE_FEE*tx_weight;
    }

    private String createUserId(){
        byte[] key = this.mKeyChain.deriveBindKey(0, new VcashKeychainPath(4, 0, 0, 0,0));
        return AppUtil.hex(key);
    }

    private void saveBaseInfo(){
        VcashWalletInfo info = new VcashWalletInfo();
        info.curHeight = mChainHeight;
        info.curKeyPath = AppUtil.hex(mKeyPath.pathData());
        info.curTxLogId = mCurTxLogId;
    }
}
