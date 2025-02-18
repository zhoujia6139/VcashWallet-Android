package com.vcashorg.vcashwallet.wallet;

import android.util.Log;

import com.vcashorg.vcashwallet.api.NodeApi;
import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.AbstractVcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashCommitId;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashContext;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashProofInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashWalletInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletNoParamCallBack;

import org.bitcoinj.crypto.DeterministicKey;

import static com.vcashorg.vcashwallet.wallet.VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeNone;
import static com.vcashorg.vcashwallet.wallet.VcashKeychain.SwitchCommitmentType.SwitchCommitmentTypeRegular;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate.SlateState.Standard1;
import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogConfirmType.DefaultState;

public class VcashWallet {
    static private String Tag = "------VcashWallet";
    public ArrayList<VcashOutput> outputs = new ArrayList<>();
    private ArrayList<VcashTokenOutput> token_outputs = new ArrayList<>();
    public HashMap<String, ArrayList<VcashTokenOutput>> token_outputs_dic = new HashMap<>();
    ConcurrentHashMap<String, ArrayList<VcashTokenOutput>> tokenoutputs_dic = new ConcurrentHashMap<>();

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
        reloadTokenOutputInfo();
        SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.USER_ID,mUserId);
    }

    static void createVcashWallet(VcashKeychain keychain){
        if (instance == null){
            instance = new VcashWallet(keychain);
        }
    }

    public static VcashWallet getInstance(){
        return instance;
    }

    public String getSignerKey(){
//        DeterministicKey key = this.mKeyChain.deriveKey(new VcashKeychainPath(4, 0, 0, 0,0));
//        return key.getPrivKeyBytes();
        byte[] data = this.getPaymentProofKey();
        return AppUtil.hex(data);
    }

    public byte[] getPaymentProofKey(){
        byte[] key_data = this.mKeyChain.deriveBindKey(0, new VcashKeychainPath(3, 0, 1, 0,0), SwitchCommitmentTypeNone);
        byte[] key_hash = NativeSecp256k1.instance().blake2b(key_data, null);
        return key_hash;
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

    void addChainHeightListener(WalletNoParamCallBack listener){
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

    short getNextLogId(){
        mCurTxLogId += 1;
        saveBaseInfo();
        return mCurTxLogId;
    }

    void setChainOutputs(ArrayList<VcashOutput> chainOutputs){
        outputs = chainOutputs;

        VcashKeychainPath maxKeyPath = new VcashKeychainPath(3, 0, 0, 0, 0);
        if (mKeyPath != null) {
            maxKeyPath = mKeyPath;
        }
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

    void setChainTokenOutputs(ArrayList<VcashTokenOutput> chainOutputs){
        if(chainOutputs != null){
            token_outputs = chainOutputs;
        }

        VcashKeychainPath maxKeyPath = new VcashKeychainPath(3, 0, 0, 0, 0);
        if (mKeyPath != null) {
            maxKeyPath = mKeyPath;
        }
        for (VcashTokenOutput item :token_outputs){
            VcashKeychainPath keyPath = new VcashKeychainPath(3, AppUtil.decode(item.keyPath));
            if (keyPath.compareTo(maxKeyPath) > 0){
                maxKeyPath = keyPath;
            }
        }
        mKeyPath = maxKeyPath;
        saveBaseInfo();
        syncTokenOutputInfo();
        tokenOutputToDic();
    }

    public void addNewTxChangeOutput(VcashOutput output){
        outputs.add(output);
    }

    public void addNewTokenTxChangeOutput(VcashTokenOutput output){
        token_outputs.add(output);
        tokenOutputToDic();
    }

    public void syncOutputInfo(){
        ArrayList<VcashOutput> arrayList = new ArrayList<>();
        if(outputs != null){
            for (VcashOutput output:outputs){
                if (output.status == VcashOutput.OutputStatus.Spent){
                    Log.w(Tag, String.format("Output commit:%s has been spend, remove from wallet", output.commitment));
                }
                else {
                    arrayList.add(output);
                }
            }
        }

        outputs = arrayList;
        EncryptedDBHelper.getsInstance().saveOutputData(outputs);
    }

    public void syncTokenOutputInfo(){
        ArrayList<VcashTokenOutput> arrayList = new ArrayList<>();
        for (VcashTokenOutput output:token_outputs){
            if (output.status == VcashOutput.OutputStatus.Spent){
                Log.w(Tag, String.format("Token Output commit:%s has been spend, remove from wallet", output.commitment));
            }
            else {
                arrayList.add(output);
            }
        }
        token_outputs = arrayList;
        tokenOutputToDic();
        EncryptedDBHelper.getsInstance().saveTokenOutputData(token_outputs);
    }

    void reloadOutputInfo(){
        outputs = EncryptedDBHelper.getsInstance().getActiveOutputData();
    }

    void reloadTokenOutputInfo(){
        token_outputs = EncryptedDBHelper.getsInstance().getActiveTokenOutputData();
        tokenOutputToDic();
    }

    public VcashOutput findOutputByCommit(String commit) {
        for (VcashOutput output : this.outputs) {
            if (output.commitment.equals(commit)) {
                return output;
            }
        }

        return null;
    }

    public VcashTokenOutput findTokenOutputByCommit(String commit) {
        for (VcashTokenOutput output : this.token_outputs) {
            if (output.commitment.equals(commit)) {
                return output;
            }
        }

        return null;
    }

    public Object identifyUtxoOutput(NodeOutputs.NodeOutput nodeOutput){
        byte[] commit = AppUtil.decode(nodeOutput.commit);
        byte[] proof = AppUtil.decode(nodeOutput.proof);
        VcashProofInfo info = mKeyChain.rewindProof(commit, proof);
        if (info != null && info.msg.length == 20){
            VcashKeychain.SwitchCommitmentType commitmentType = SwitchCommitmentTypeRegular;
            byte[] keyPathMsg = Arrays.copyOfRange(info.msg, 4, 20);
            if (info.version == 1){
                int type = info.msg[2];
                commitmentType = VcashKeychain.SwitchCommitmentType.locateEnum(type);
            }
            byte[] retCommit = mKeyChain.createCommitment(info.value, new VcashKeychainPath(3, keyPathMsg), commitmentType);
            if (!Arrays.equals(commit, retCommit)){
                Log.e(Tag, String.format("rewindProof suc, but message data is invalid. commit = %s", nodeOutput.commit));
                return null;
            }

            if (nodeOutput.token_type != null){
                VcashTokenOutput output = new VcashTokenOutput();
                output.token_type = nodeOutput.token_type;
                output.commitment = nodeOutput.commit;
                output.keyPath = AppUtil.hex(keyPathMsg);
                output.mmr_index = nodeOutput.mmr_index;
                output.value = info.value;
                output.height = nodeOutput.block_height;
                output.is_token_issue = (nodeOutput.output_type.equals("TokenIsuue"));
                output.lock_height = nodeOutput.block_height;
                output.status = VcashOutput.OutputStatus.Unspent;
                return output;
            } else {
                VcashOutput output = new VcashOutput();
                output.commitment = nodeOutput.commit;
                output.keyPath = AppUtil.hex(keyPathMsg);
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
        }
        return null;
    }

    void sendTransaction(long amount, long fee, final WalletCallback callback){
        long total = 0;
        ArrayList<VcashOutput> spendable = new ArrayList<>();
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
            actualFee = calcuteFee(spendable.size(), 1, 1, 0, 0, 0);
        }
        long amount_with_fee = amount + actualFee;
        if (total < amount_with_fee){
            String errMsg = String.format(Locale.getDefault(), "Not enough funds, available:%f, needed:%f", WalletApi.nanoToVcash(total), WalletApi.nanoToVcash(amount_with_fee));
            if (callback!=null){
                callback.onCall(false, errMsg);
            }
            return;
        }

        // 1.2Second attempt to spend with change
        if (total != amount_with_fee) {
            actualFee = calcuteFee(spendable.size(), 2, 1, 0, 0, 0);
        }
        amount_with_fee = amount + actualFee;
        if (total < amount_with_fee){
            String errMsg = String.format(Locale.getDefault(), "Not enough funds, available:%f, needed:%f", WalletApi.nanoToVcash(total), WalletApi.nanoToVcash(amount_with_fee));
            if (callback!=null){
                callback.onCall(false, errMsg);
            }
            return;
        }
        long change = total - amount_with_fee;

        //2 fill txLog and slate
        VcashSlate slate = new VcashSlate();
        slate.num_participants = 2;
        slate.amount = amount;
        slate.fee = actualFee;
        slate.kernel_features = 0;
        slate.state = Standard1;

        VcashTxLog txLog = new VcashTxLog();
        txLog.tx_id = getNextLogId();
        txLog.tx_slate_id = slate.uuid;
        txLog.tx_type = AbstractVcashTxLog.TxLogEntryType.TxSent;
        txLog.create_time = AppUtil.getCurrentTimeSecs();
        txLog.fee = slate.fee;
        txLog.amount_credited = change;
        txLog.amount_debited = total;
        txLog.confirm_state = DefaultState;
        slate.txLog = txLog;

        VcashCommitId changeOutputid = new VcashCommitId();
        byte[] blind = slate.addTxElement(spendable, change, changeOutputid, false);
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
        context.amout = amount;
        context.fee = actualFee;

        for (VcashOutput item: spendable) {
            VcashCommitId outputid = new VcashCommitId();
            outputid.keyPath = item.keyPath;
            outputid.mmr_index = item.mmr_index;
            outputid.value = item.value;
            context.input_ids.add(outputid);
        }
        if (change > 0) {
            context.output_ids.add(changeOutputid);
        }

        slate.context = context;

        //4 sender fill round 1
        if (!slate.fillRound1(context)){
            Log.e(Tag, "sender fillRound1 failed");
            if (callback!=null){
                callback.onCall(false, null);
            }
            return;
        }

        slate.tx = null;

        if (callback!=null){
            callback.onCall(true, slate);
        }
    }

    void sendTokenTransaction(String token_type, long amount, final WalletCallback callback){
        long total = 0;
        ArrayList<VcashTokenOutput> spendable = new ArrayList<>();
        ArrayList<VcashTokenOutput> tokens = token_outputs_dic.get(token_type);
        if (tokens != null) {
            for (VcashTokenOutput item : tokens){
                if (item.isSpendable()){
                    spendable.add(item);
                    total += item.value;
                }
            }
        }

        if (total < amount) {
            String errMsg = String.format(Locale.getDefault(), "Not enough funds, available:%f, needed:%f", WalletApi.nanoToVcash(total), WalletApi.nanoToVcash(amount));
            if (callback!=null){
                callback.onCall(false, errMsg);
            }
            return;
        }

        long change = total - amount;

        long vcash_total = 0;
        ArrayList<VcashOutput> vcash_spendable = new ArrayList<>();
        if(outputs != null){
            for (VcashOutput item: outputs){
                if (item.isSpendable()){
                    vcash_spendable.add(item);
                    vcash_total += item.value;
                }
            }
        }

        //assume spend all vcash input as fee
        int token_output_count = change > 0? 2:1;
        long fee1 = calcuteFee(vcash_spendable.size(), 1, 1, spendable.size(), token_output_count, 1);
        if (vcash_total < fee1) {
            String errMsg = String.format(Locale.getDefault(), "Not enough funds, available:%f, needed:%f", WalletApi.nanoToVcash(vcash_total), WalletApi.nanoToVcash(fee1));
            if (callback!=null){
                callback.onCall(false, errMsg);
            }
            return;
        }

        //assume 1 vcash input and 1 vcash output, spend all token input with 1 token chang output
        long fee2 = calcuteFee(1, 1, 1, spendable.size(), token_output_count, 1);
        Comparator<VcashOutput> comparator = new Comparator<VcashOutput>(){
            @Override
            public int compare(VcashOutput arg1, VcashOutput arg2) {
                if (arg1.value > arg2.value) {
                    return 1;
                } else if (arg1.value < arg2.value) {
                    return -1;
                }
                return 0;
            }
        };
        Collections.sort(vcash_spendable, comparator);

        VcashOutput input = null;
        for (VcashOutput item: vcash_spendable) {
            if (item.value >= fee2) {
                input = item;
                break;
            }
        }

        long actualFee = 0;
        long vcash_input_total = 0;
        ArrayList<VcashOutput> vcash_actual_spend = null;
        if (input != null) {
            vcash_input_total = input.value;
            actualFee = fee2;
            vcash_actual_spend = new ArrayList<>();
            vcash_actual_spend.add(input);
        }else {
            vcash_input_total = vcash_total;
            actualFee = fee1;
            vcash_actual_spend = vcash_spendable;
        }
        long vcash_change = vcash_input_total - actualFee;

        //2 fill txLog and slate
        VcashSlate slate = new VcashSlate();
        slate.num_participants = 2;
        slate.token_type = token_type;
        slate.amount = amount;
        slate.fee = actualFee;
        slate.kernel_features = 0;
        slate.token_kernel_features = 0;
        slate.state = Standard1;

        VcashTokenTxLog txLog = new VcashTokenTxLog();
        txLog.tx_id = getNextLogId();
        txLog.token_type = token_type;
        txLog.tx_slate_id = slate.uuid;
        txLog.tx_type = AbstractVcashTxLog.TxLogEntryType.TxSent;
        txLog.create_time = AppUtil.getCurrentTimeSecs();
        txLog.fee = slate.fee;
        txLog.amount_credited = vcash_change;
        txLog.amount_debited = vcash_input_total;
        txLog.token_amount_credited = change;
        txLog.token_amount_debited = total;
        txLog.confirm_state = DefaultState;
        slate.tokenTxLog = txLog;

        VcashCommitId changeOutputid = new VcashCommitId();
        byte[] blind = slate.addTxElement(vcash_actual_spend, vcash_change, changeOutputid,true);
        if (blind == null){
            Log.e(Tag, "sender addTxElement failed");
            if (callback!=null){
                callback.onCall(false, null);
            }
            return;
        }

        VcashCommitId tokenChangeOutputid = new VcashCommitId();
        byte[] token_blind = slate.addTokenTxElement(spendable, change, tokenChangeOutputid);
        if (token_blind == null){
            Log.e(Tag, "sender addTokenTxElement failed");
            if (callback!=null){
                callback.onCall(false, null);
            }
            return;
        }

        //3 construct sender Context
        VcashContext context = new VcashContext();
        context.sec_key = AppUtil.hex(blind);
        context.token_sec_key = AppUtil.hex(token_blind);
        context.slate_id = slate.uuid;
        context.amout = amount;
        context.fee = slate.fee;

        for (VcashOutput item: vcash_actual_spend) {
            VcashCommitId outputid = new VcashCommitId();
            outputid.keyPath = item.keyPath;
            outputid.mmr_index = item.mmr_index;
            outputid.value = item.value;
            context.input_ids.add(outputid);
        }
        if (vcash_change > 0) {
            context.output_ids.add(changeOutputid);
        }

        for (VcashTokenOutput item: spendable) {
            VcashCommitId outputid = new VcashCommitId();
            outputid.keyPath = item.keyPath;
            outputid.mmr_index = item.mmr_index;
            outputid.value = item.value;
            context.token_input_ids.add(outputid);
        }
        if (change > 0) {
            context.token_output_ids.add(tokenChangeOutputid);
        }

        slate.context = context;

        //4 sender fill round 1
        if (!slate.fillRound1(context)){
            Log.e(Tag, "sender fillRound1 failed");
            if (callback!=null){
                callback.onCall(false, null);
            }
            return;
        }

        slate.tx = null;

        if (callback!=null){
            callback.onCall(true, slate);
        }
    }

    boolean receiveTransaction(VcashSlate slate){
        //5, fill slate with receiver output
        if (slate.token_type != null) {
            VcashTokenTxLog txLog = new VcashTokenTxLog();
            txLog.tx_id = VcashWallet.getInstance().getNextLogId();
            txLog.tx_slate_id = slate.uuid;
            txLog.tx_type = AbstractVcashTxLog.TxLogEntryType.TxReceived;
            txLog.create_time = AppUtil.getCurrentTimeSecs();
            txLog.token_type = slate.token_type;
            txLog.fee = slate.fee;
            txLog.token_amount_credited = slate.amount;
            txLog.amount_debited = 0;
            txLog.confirm_state = DefaultState;
            slate.tokenTxLog = txLog;
        } else {
            VcashTxLog txLog = new VcashTxLog();
            txLog.tx_id = VcashWallet.getInstance().getNextLogId();
            txLog.tx_slate_id = slate.uuid;
            txLog.tx_type = AbstractVcashTxLog.TxLogEntryType.TxReceived;
            txLog.create_time = AppUtil.getCurrentTimeSecs();
            txLog.fee = slate.fee;
            txLog.amount_credited = slate.amount;
            txLog.amount_debited = 0;
            txLog.confirm_state = DefaultState;
            slate.txLog = txLog;
        }

        VcashCommitId commitId = new VcashCommitId();
        byte[] blind = slate.addReceiverTxOutput(commitId);
        if (blind == null){
            Log.e("", "--------receiver addReceiverTxOutput failed");
            return false;
        }

        //6, construct receiver Context
        VcashContext context = new VcashContext();
        if (slate.token_type != null) {
            context.token_sec_key = AppUtil.hex(blind);
        } else {
            context.sec_key = AppUtil.hex(blind);
        }
        context.slate_id = slate.uuid;
        context.amout = slate.amount;
        context.fee = slate.fee;
        if (slate.token_type != null) {
            context.token_output_ids.add(commitId);
        } else {
            context.output_ids.add(commitId);
        }
        slate.context = context;

        //7, receiver fill round 1
        if (!slate.fillRound1(context)){
            Log.e(Tag, "--------receiver fillRound1 failed");
            return false;
        }

        //8, receiver fill round 2
        if (!slate.fillRound2(context)){
            Log.e(Tag, "--------receiver fillRound2 failed");
            return false;
        }

        return true;
    }

    boolean finalizeTransaction(VcashSlate slate){
        //compute offset
        slate.subInputFromOffset(slate.context);

        //construct tx
        slate.repopulateTx(slate.context);

        //9, sender fill round 2
        if (!slate.fillRound2(slate.context)){
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

    private void tokenOutputToDic() {
        token_outputs_dic = new HashMap<>();
        if (token_outputs != null) {
            for (VcashTokenOutput item : token_outputs) {
                ArrayList<VcashTokenOutput> arr = token_outputs_dic.get(item.token_type);
                if (arr == null) {
                    arr = new ArrayList<>();
                    token_outputs_dic.put(item.token_type, arr);
                }
                arr.add(item);
            }
        }
    }


    private long calcuteFee(int inputCount, int outputCount, int kernelCount, int tokeninputCount, int tokenoutputCount, int tokenkernelCount){
        int tx_weight = outputCount*4 + kernelCount - inputCount;
        if (tx_weight < 0) {
            tx_weight = 0;
        }
        int token_tx_weight = tokenoutputCount*4 + tokenkernelCount - tokeninputCount;
        if (token_tx_weight < 0) {
            token_tx_weight = 0;
        }
        int total_weight = tx_weight + token_tx_weight;
        if (total_weight < 1){
            total_weight = 1;
        }

        return DEFAULT_BASE_FEE*total_weight;
    }

    private String createUserId(){
//        DeterministicKey key = this.mKeyChain.deriveKey(new VcashKeychainPath(4, 0, 0, 0,0));
//        return AppUtil.hex(key.getPubKey());
        byte[] secData = this.getPaymentProofKey();
        String secStr = PaymentProof.getPaymentProofAddress(secData);
        return secStr;
    }

    private void saveBaseInfo(){
        VcashWalletInfo info = new VcashWalletInfo();
        info.curHeight = mChainHeight;
        info.curKeyPath = AppUtil.hex(mKeyPath.pathData());
        info.curTxLogId = mCurTxLogId;
        EncryptedDBHelper.getsInstance().saveWalletInfo(info);
    }
}
