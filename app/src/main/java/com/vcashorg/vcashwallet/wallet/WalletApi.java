package com.vcashorg.vcashwallet.wallet;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vcashorg.vcashwallet.api.NodeApi;
import com.vcashorg.vcashwallet.api.ServerApi;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.payload.PayloadUtil;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.utils.CoinUtils;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashContext;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletNoParamCallBack;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WalletApi {
    static private String Tag = "------WalletApi";
    final public static long VCASH_BASE = 1000000000;
    private static  Context context;
    private static OkHttpClient okClient = new OkHttpClient();

    public static void setWalletContext(Context con){
        context = con;
        EncryptedDBHelper.setDbContext(con);
        AppUtil.getInstance(context).applyPRNGFixes();
    }

    public static List<String> getAllPhraseWords(){
        return MnemonicHelper.instance(context).getWordList();
    }

    public static List<String> generateMnemonicPassphrase(){
        List<String> strList = null;
        try {
            byte[] seed = AppUtil.randomBytes(32);
            strList = MnemonicHelper.instance(context).mnemoicFromBytes(seed);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        }

        return strList;
    }


    public static boolean createWallet(List<String> wordsArr, String password){
        if (wordsArr == null){
            return false;
        }
        byte[] entropy = MnemonicHelper.instance(context).toEntropy(wordsArr);
        if (entropy != null){
            DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(entropy);
            byte[] secret = masterKey.getPrivKeyBytes();
            String temp = AppUtil.hex(secret);
            Log.d(Tag, temp);
            VcashKeychain keyChain = new VcashKeychain(masterKey);
            VcashWallet.createVcashWallet(keyChain);
            return true;
        }

        return false;
    }

    public static void clearWallet(){
        EncryptedDBHelper.getsInstance().clearAllData();
    }

    public static String getWalletUserId(){
        if (VcashWallet.getInstance() != null){
            return VcashWallet.getInstance().mUserId;
        }

        return null;
    }

    public static WalletBalanceInfo getWalletBalanceInfo(){
        long total = 0;
        long locked = 0;
        long unconfirmed = 0;
        long spendable = 0;

        for (VcashOutput output :VcashWallet.getInstance().outputs){
            switch (output.status){
                case Unconfirmed:{
                    total += output.value;
                    unconfirmed += output.value;
                    break;
                }
                case Unspent:{
                    total += output.value;
                    if (output.isSpendable()){
                        spendable += output.value;
                    }
                    break;
                }

                case Locked:{
                    locked += output.value;
                    break;
                }

                default:
                    break;
            }
        }
        WalletBalanceInfo info = new WalletApi.WalletBalanceInfo();
        info.total = total;
        info.spendable = spendable;
        info.locked = locked;
        info.unconfirmed = unconfirmed;
        return info;
    }

    public static long getCurChainHeight(){
        return VcashWallet.getInstance().getChainHeight();
    }

    public static void checkWalletUtxo(final WalletCallback callback){
        ArrayList<VcashOutput> arr = new ArrayList<>();
        NodeApi.getOutputsByPmmrIndex(0, arr, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data){
                if (yesOrNo){
                    if (data instanceof ArrayList){
                        ArrayList<VcashTxLog> txArr = new ArrayList<VcashTxLog>();
                        for (VcashOutput item :(ArrayList<VcashOutput>)data){
                            VcashTxLog tx = new VcashTxLog();
                            tx.tx_id = VcashWallet.getInstance().getNextLogId();
                            tx.create_time = AppUtil.getCurrentTimeSecs();
                            tx.confirm_state = VcashTxLog.TxLogConfirmType.NetConfirmed;
                            tx.amount_credited = item.value;
                            tx.tx_type = item.is_coinbase? VcashTxLog.TxLogEntryType.ConfirmedCoinbase: VcashTxLog.TxLogEntryType.TxReceived;
                            tx.server_status = ServerTxStatus.TxClosed;
                            item.tx_log_id = tx.tx_id;
                            txArr.add(tx);
                        }
                        VcashWallet.getInstance().setChainOutputs((ArrayList<VcashOutput>)data);
                        EncryptedDBHelper.getsInstance().saveTxDataArr(txArr);
                        if (callback != null){
                            callback.onCall(true, null);
                        }
                    }
                    else {
                        if (callback != null){
                            callback.onCall(true, data);
                        }
                    }
                }
                else {
                    if (callback != null){
                        callback.onCall(false, null);
                    }
                }
            }
        });
    }

    public static void createSendTransaction(long amount, long fee, final WalletCallback callback){
        VcashWallet.getInstance().sendTransaction(amount, fee, callback);
    }

    public static void sendTransactionForUser(final VcashSlate slate, final String user, final WalletCallback callback){
        Log.w(Tag, String.format("sendTransaction for userid:%s", user));
        EncryptedDBHelper.getsInstance().beginDatabaseTransaction();

        final WalletNoParamCallBack rollbackBlock = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                EncryptedDBHelper.getsInstance().rollbackDataTransaction();
                VcashWallet.getInstance().reloadOutputInfo();
            }
        };

        slate.txLog.parter_id = user;

        sendTransaction(slate, new WalletCallback(){
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    ServerTransaction server_tx = new ServerTransaction(slate);
                    server_tx.sender_id = VcashWallet.getInstance().mUserId;
                    server_tx.receiver_id = user;
                    server_tx.status = ServerTxStatus.TxDefaultStatus;

                    ServerApi.sendTransaction(server_tx, new WalletCallback() {
                        @Override
                        public void onCall(boolean yesOrNo, Object data) {
                            if (yesOrNo){
                                Log.d(Tag, "sendTransaction to server suc");
                                EncryptedDBHelper.getsInstance().commitDatabaseTransaction();
                                callback.onCall(true, null);
                            }
                            else{
                                Log.e(Tag, "sendTransaction to server failed! roll back database");
                                rollbackBlock.onCall();
                                callback.onCall(false, "Tx send to server failed");
                            }
                        }
                    });
                }
                else{
                    Log.e(Tag, "sendTx error!");
                    rollbackBlock.onCall();
                    callback.onCall(false, data);
                }
            }
        });
    }

    public static void sendTransactionForUrl(final VcashSlate slate, final String url, final WalletCallback callback){
        Log.w(Tag, String.format("sendTransaction for url:%s", url));
        EncryptedDBHelper.getsInstance().beginDatabaseTransaction();

        final WalletNoParamCallBack rollbackBlock = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                EncryptedDBHelper.getsInstance().rollbackDataTransaction();
                VcashWallet.getInstance().reloadOutputInfo();
            }
        };

        final Handler handler=new Handler();
        sendTransaction(slate, new WalletCallback(){
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    final Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, slate.new VcashSlateTypeAdapter()).create();
                    String slate_str = gson.toJson(slate);
                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), slate_str);
                    final String full_url = String.format("%s/v1/wallet/foreign/receive_tx", url);
                    Request req = new Request.Builder().url(full_url).post(body).build();
                    okClient.newCall(req).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e(Tag, String.format("sendTransaction post to %s fail!", full_url));
                                    rollbackBlock.onCall();
                                    callback.onCall(false, null);
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            Log.w(Tag, String.format("sendTransaction post to %s suc!", full_url));
                            String json = response.body().string();
                            if (json.startsWith("\"")){
                                if (json.endsWith("\"")){
                                    json = json.substring(0, json.length()-1);
                                }
                                json = json.replaceFirst("\"", "");
                                json = json.replaceAll("\\\\", "");

                            }
                            final VcashSlate resSlate = gson.fromJson(json, VcashSlate.class);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    finalizeTransaction(resSlate, new WalletCallback() {
                                        @Override
                                        public void onCall(boolean yesOrNo, Object data) {
                                            if (yesOrNo){
                                                Log.w(Tag, "finalizeTransaction sec!");
                                                EncryptedDBHelper.getsInstance().commitDatabaseTransaction();
                                                if (callback != null){
                                                    callback.onCall(true, null);
                                                }
                                            }
                                            else{
                                                Log.e(Tag, "finalizeTransaction failed!");
                                                rollbackBlock.onCall();
                                                if (callback != null){
                                                    callback.onCall(false, data);
                                                }
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
                else{
                    Log.e(Tag, "sendTx error!");
                    rollbackBlock.onCall();
                    callback.onCall(false, data);
                }
            }
        });
    }

    private static void sendTransaction(VcashSlate slate, final WalletCallback callback){
        slate.lockOutputsFn.onCall();
        if (slate.createNewOutputsFn != null){
            slate.createNewOutputsFn.onCall();
        }

        //save txLog

        slate.txLog.server_status = ServerTxStatus.TxDefaultStatus;
        if (!EncryptedDBHelper.getsInstance().saveTx(slate.txLog)){
            if (callback != null){
                callback.onCall(false, "Db error:saveTx failed");
                return;
            }
        }

        //save output status
        VcashWallet.getInstance().syncOutputInfo();

        //save context
        if (!EncryptedDBHelper.getsInstance().saveContext(slate.context)){
            if (callback != null){
                callback.onCall(false, "Db error:save context failed");
                return;
            }
        }

        if (callback != null){
            callback.onCall(true, null);
        }
    }

    public static void isValidSlateConent(String fileContent, final WalletCallback callback){
        Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, (new VcashSlate()).new VcashSlateTypeAdapter()).create();
        VcashSlate slate = gson.fromJson(fileContent, VcashSlate.class);
        if (slate == null || !slate.isValidForReceive()){
            if (callback != null){
                callback.onCall(false, "Wrong Data Format");
            }
        }

        VcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
        if (txLog != null){
            if (callback != null){
                callback.onCall(false, "Duplicate Tx");
            }
        }

        if (callback != null){
            callback.onCall(true, slate);
        }
    }

    public static void receiveTransactionBySlate(VcashSlate slate, final WalletCallback callback){
        receiveTx(slate, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (callback != null){
                    callback.onCall(yesOrNo, data);
                }
            }
        });
    }

    public static void receiveTransaction(ServerTransaction tx, final WalletCallback callback){
        receiveTx(tx, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (callback != null){
                    callback.onCall(yesOrNo, data);
                }
            }
        });
    }

    private static void receiveTx(Object obj, final WalletCallback callback){
        VcashSlate slate = null;
        ServerTransaction serverTx = null;
        if (obj instanceof VcashSlate){
            slate = (VcashSlate)obj;
        }
        else if(obj instanceof  ServerTransaction){
            serverTx = (ServerTransaction)obj;
            slate = serverTx.slateObj;
        }
        else {
            if (callback != null){
                callback.onCall(false, "obj instance error");
            }
            return;
        }

        if (!VcashWallet.getInstance().receiveTransaction(slate)){
            Log.e(Tag, "VcashWallet receiveTransaction failed");
            callback.onCall(false, null);
            return;
        }

        EncryptedDBHelper.getsInstance().beginDatabaseTransaction();

        final WalletNoParamCallBack rollbackBlock = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                EncryptedDBHelper.getsInstance().rollbackDataTransaction();
                VcashWallet.getInstance().reloadOutputInfo();
            }
        };

        slate.createNewOutputsFn.onCall();
        //save txLog
        slate.txLog.server_status = ServerTxStatus.TxReceiverd;
        if (serverTx != null){
            slate.txLog.parter_id = serverTx.sender_id;
        }
        if (!EncryptedDBHelper.getsInstance().saveTx(slate.txLog)){
            rollbackBlock.onCall();
            Log.e(Tag, "VcashDataManager saveAppendTx failed");
            callback.onCall(false, null);
            return;
        }

        //save output status
        VcashWallet.getInstance().syncOutputInfo();

        Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, (new VcashSlate()).new VcashSlateTypeAdapter()).create();
        if (serverTx != null){
            serverTx.slate = gson.toJson(slate);
            serverTx.status = ServerTxStatus.TxReceiverd;
            ServerApi.receiveTransaction(serverTx, new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    if (yesOrNo){
                        Log.d(Tag, "send receiveTransaction suc");
                        EncryptedDBHelper.getsInstance().commitDatabaseTransaction();
                        if (callback != null) {
                            callback.onCall(true, null);
                        }
                    }
                    else{
                        Log.e(Tag, "send receiveTransaction to server failed! roll back database");
                        rollbackBlock.onCall();
                        if (callback != null) {
                            callback.onCall(false, null);
                        }
                    }
                }
            });
        }
        else {
            String slate_str = gson.toJson(slate);
            EncryptedDBHelper.getsInstance().commitDatabaseTransaction();
            if (callback != null){
                callback.onCall(true, slate_str);
            }
        }

    }

    public static void finalizeServerTransaction(final ServerTransaction tx, final WalletCallback callback){
        finalizeTransaction(tx.slateObj, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    tx.status = ServerTxStatus.TxFinalized;
                    ServerApi.filanizeTransaction(tx.tx_id, new WalletCallback() {
                        @Override
                        public void onCall(boolean yesOrNo, Object data) {
                            if (!yesOrNo){
                                Log.e(Tag, "filalize tx to Server failed, cache tx state");
                            }
                        }
                    });
                    if (callback != null){
                        callback.onCall(true, null);
                    }
                }
                else{
                    if (callback != null){
                        callback.onCall(false, data);
                    }
                }
            }
        });
    }

    private static void finalizeTransaction(final VcashSlate slate, final WalletCallback callback){
        VcashContext context = EncryptedDBHelper.getsInstance().getContextBySlateId(slate.uuid);
        if (context == null){
            Log.e(Tag, "database record is broke, cannot finalize tx");
            callback.onCall(false, null);
            return;
        }
        slate.context = context;

        if (!VcashWallet.getInstance().finalizeTransaction(slate)){
            Log.e(Tag, "finalizeTransaction failed");
            callback.onCall(false, null);
            return;
        }

        slate.tx.sortTx();
        byte[] txPayload = slate.tx.computePayload(false);
        NodeApi.postTx(AppUtil.hex(txPayload), new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    callback.onCall(true, null);
                    VcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
                    if (txLog != null){
                        txLog.confirm_state = VcashTxLog.TxLogConfirmType.LoalConfirmed;
                        txLog.server_status = ServerTxStatus.TxFinalized;
                        EncryptedDBHelper.getsInstance().saveTx(txLog);
                    }
                }
                else {
                    callback.onCall(false, "post tx to node failed");
                }
            }
        });
    }

    public static boolean cancelTransaction(String tx_id){
        VcashTxLog txLog = WalletApi.getTxByTxid(tx_id);
        if (txLog != null && !txLog.isCanBeCanneled()){
            return false;
        }
        if (txLog != null){
            txLog.cancelTxlog();
            EncryptedDBHelper.getsInstance().saveTx(txLog);
        }
        ServerApi.cancelTransaction(tx_id, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (!yesOrNo){
                    Log.e(Tag, "cancel tx to Server failed");
                }
            }
        });
        return true;
    }

    public static ArrayList<VcashTxLog> getTransationArr(){
        return EncryptedDBHelper.getsInstance().getTxData();
    }

    public static VcashTxLog getTxByTxid(String txid){
        return EncryptedDBHelper.getsInstance().getTxBySlateId(txid);
    }

    public static boolean deleteTxByTxid(String txid){
        return  EncryptedDBHelper.getsInstance().deleteTxBySlateId(txid);
    }

    public static void updateOutputStatusWithComplete(final WalletCallback callback){
        ArrayList<String> strArr = new ArrayList<>();
        for (VcashOutput item: VcashWallet.getInstance().outputs){
            strArr.add(item.commitment);
        }

        if (strArr.size() == 0){
            callback.onCall(true, null);
            return;
        }

        NodeApi.getOutputsByCommitArr(strArr, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    ArrayList<NodeRefreshOutput> apiOutputs = (ArrayList<NodeRefreshOutput>)data;
                    ArrayList<VcashTxLog> txs = getTransationArr();
                    boolean hasChange = false;
                    for (VcashOutput item: VcashWallet.getInstance().outputs){
                        NodeRefreshOutput nodeOutput = null;
                        for (NodeRefreshOutput output: apiOutputs){
                            if (item.commitment.equals(output.commit)){
                                nodeOutput = output;
                            }
                        }

                        if (nodeOutput!=null){
                            //should not be coinbase
                            if (item.is_coinbase && item.status == VcashOutput.OutputStatus.Unconfirmed){

                            }
                            else if(!item.is_coinbase && item.status == VcashOutput.OutputStatus.Unconfirmed){
                                VcashTxLog tx = null;
                                for (VcashTxLog txLog :txs){
                                    if (txLog.tx_id == item.tx_log_id){
                                        tx = txLog;
                                    }
                                }
                                if (tx != null){
                                    tx.confirm_state = VcashTxLog.TxLogConfirmType.NetConfirmed;
                                    tx.confirm_time = AppUtil.getCurrentTimeSecs();
                                    tx.server_status = ServerTxStatus.TxFinalized;
                                }
                                item.height = nodeOutput.height;
                                item.status = VcashOutput.OutputStatus.Unspent;

                                hasChange = true;
                            }
                        }
                        else{
                            if (item.status == VcashOutput.OutputStatus.Locked || item.status == VcashOutput.OutputStatus.Unspent){
                                VcashTxLog tx = null;
                                for (VcashTxLog txLog :txs){
                                    if (txLog.confirm_state == VcashTxLog.TxLogConfirmType.LoalConfirmed){
                                        for (String commitStr :txLog.inputs){
                                            if (commitStr.equals(item.commitment)){
                                                tx = txLog;
                                                break;
                                            }
                                        }
                                    }
                                    if (tx != null){
                                        break;
                                    }

                                }
                                if (tx != null){
                                    tx.confirm_state = VcashTxLog.TxLogConfirmType.NetConfirmed;
                                    tx.confirm_time = AppUtil.getCurrentTimeSecs();
                                    tx.server_status = ServerTxStatus.TxFinalized;
                                }
                                item.status = VcashOutput.OutputStatus.Spent;
                                hasChange = true;
                            }
                        }
                    }

                    if (hasChange){
                        EncryptedDBHelper.getsInstance().saveTxDataArr(txs);
                        getTransationArr();
                        VcashWallet.getInstance().syncOutputInfo();
                    }
                }

                callback.onCall(yesOrNo, null);
            }
        });
    }

    public static void addChainHeightListener(WalletNoParamCallBack listener){
        VcashWallet.getInstance().addChainHeightListener(listener);
    }

    public static void addTxDataListener(WalletNoParamCallBack listener){
        EncryptedDBHelper.getsInstance().addTxDataListener(listener);
    }

    public static double nanoToVcash(long nano){
        return (double)nano/VCASH_BASE;
    }

    public static String nanoToVcashString(long nano){
        return CoinUtils.format(nanoToVcash(nano));
    }

    public static String nanoToVcashWithUnit(long nano){
        return nanoToVcashString(nano) + " VCash";
    }


    public static long vcashToNano(double vcash){
        return (long)(vcash*VCASH_BASE);
    }


    public static class WalletBalanceInfo {
        public long total;
        public long locked;
        public long unconfirmed;
        public long spendable;
    }
}
