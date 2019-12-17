package com.vcashorg.vcashwallet.wallet;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.vcashorg.vcashwallet.api.NodeApi;
import com.vcashorg.vcashwallet.api.ServerApi;
import com.vcashorg.vcashwallet.api.bean.JsonRpcRes;
import com.vcashorg.vcashwallet.api.bean.JsonRpcRq;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshTokenOutput;
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
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletNoParamCallBack;

import org.apache.commons.lang3.StringEscapeUtils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                            tx.confirm_height = item.height;
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

    public static void checkWalletTokenUtxo(final WalletCallback callback){
        ArrayList<VcashTokenOutput> arr = new ArrayList<>();
        NodeApi.getTokenOutputsByPmmrIndex(0, arr, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data){
                if (yesOrNo){
                    if (data instanceof ArrayList){
                        ArrayList<VcashTokenTxLog> txArr = new ArrayList<>();
                        for (VcashTokenOutput item :(ArrayList<VcashTokenOutput>)data){
                            VcashTokenTxLog tx = new VcashTokenTxLog();
                            tx.tx_id = VcashWallet.getInstance().getNextLogId();
                            tx.create_time = AppUtil.getCurrentTimeSecs();
                            tx.confirm_height = item.height;
                            tx.confirm_state = VcashTxLog.TxLogConfirmType.NetConfirmed;
                            tx.token_amount_credited = item.value;
                            tx.tx_type = item.is_token_issue? VcashTokenTxLog.TokenTxLogEntryType.TokenIssue: VcashTokenTxLog.TokenTxLogEntryType.TokenTxReceived;
                            tx.server_status = ServerTxStatus.TxClosed;
                            item.tx_log_id = tx.tx_id;
                            txArr.add(tx);
                        }
                        VcashWallet.getInstance().setChainTokenOutputs((ArrayList<VcashTokenOutput>)data);
                        EncryptedDBHelper.getsInstance().saveTokenTxDataArr(txArr);
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

    public static void createSendTokenTransaction(String token_type, long amount, final WalletCallback callback){
        VcashWallet.getInstance().sendTokenTransaction(token_type, amount, callback);
    }

    public static void sendTransactionForUser(final VcashSlate slate, final String user, final WalletCallback callback){
        Log.w(Tag, String.format("sendTransaction for userid:%s", user));
        EncryptedDBHelper.getsInstance().beginDatabaseTransaction();

        final WalletNoParamCallBack rollbackBlock = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                EncryptedDBHelper.getsInstance().rollbackDataTransaction();
                VcashWallet.getInstance().reloadOutputInfo();
                VcashWallet.getInstance().reloadTokenOutputInfo();
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
                VcashWallet.getInstance().reloadTokenOutputInfo();
            }
        };

        final Handler handler=new Handler();
        sendTransaction(slate, new WalletCallback(){
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    JsonRpcRq rq = new JsonRpcRq(slate);
                    final Gson rq_gson = new GsonBuilder().registerTypeAdapter(JsonRpcRq.class, rq.new JsonRpcRqTypeAdapter()).serializeNulls().create();
                    String req_str = rq_gson.toJson(rq);
                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), req_str);
                    try{
                        final String full_url = String.format("%s/v2/foreign", url);
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
                            public void onResponse(final Call call, Response response) throws IOException {
                                try {
                                    String json = response.body().string();
                                    JsonRpcRes res = new JsonRpcRes();
                                    Gson res_gson = new GsonBuilder().registerTypeAdapter(JsonRpcRes.class, res.new JsonRpcResTypeAdapter()).serializeNulls().create();
                                    res = res_gson.fromJson(json, JsonRpcRes.class);
                                    if (res.resSlate == null){
                                        Log.e(Tag, String.format("sendTransaction post to %s failed!", full_url));
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                rollbackBlock.onCall();
                                                if(callback != null){
                                                    callback.onCall(false, null);
                                                }
                                            }
                                        });
                                    } else {
                                        Log.w(Tag, String.format("sendTransaction post to %s suc!", full_url));
                                        final VcashSlate resSlate = res.resSlate;
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
                                }catch (Exception e){
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            rollbackBlock.onCall();
                                            if(callback != null){
                                                callback.onCall(false, null);
                                            }
                                        }
                                    });
                                }

                            }
                        });
                    } catch (Exception exc){
                        Log.e(Tag, "Url is illegal");
                        rollbackBlock.onCall();
                        callback.onCall(false, null);
                    }
                }
                else{
                    Log.e(Tag, "sendTx error!");
                    rollbackBlock.onCall();
                    callback.onCall(false, data);
                }
            }
        });
    }

    public static void sendTransactionByFile(final VcashSlate slate, final WalletCallback callback){
        Log.w(Tag, String.format("start sendTransaction by file"));
        EncryptedDBHelper.getsInstance().beginDatabaseTransaction();

        final WalletNoParamCallBack rollbackBlock = new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                EncryptedDBHelper.getsInstance().rollbackDataTransaction();
                VcashWallet.getInstance().reloadOutputInfo();
                VcashWallet.getInstance().reloadTokenOutputInfo();
            }
        };

        sendTransaction(slate, new WalletCallback(){
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    Log.w(Tag, String.format("sendTransaction by file suc"));
                    EncryptedDBHelper.getsInstance().commitDatabaseTransaction();
                    final Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, slate.new VcashSlateTypeAdapter()).create();
                    String slate_str = gson.toJson(slate);
                    callback.onCall(true, slate_str);
                }
                else{
                    Log.e(Tag, "sendTransaction by file error!");
                    rollbackBlock.onCall();
                    callback.onCall(false, data);
                }
            }
        });
    }

    private static void sendTransaction(VcashSlate slate, final WalletCallback callback){
        if (slate.lockOutputsFn != null){
            slate.lockOutputsFn.onCall();
        }
        if (slate.lockTokenOutputsFn != null){
            slate.lockTokenOutputsFn.onCall();
        }
        if (slate.createNewOutputsFn != null){
            slate.createNewOutputsFn.onCall();
        }
        if (slate.createNewTokenOutputsFn != null){
            slate.createNewTokenOutputsFn.onCall();
        }

        //save txLog
        if (slate.txLog != null) {
            slate.txLog.server_status = ServerTxStatus.TxDefaultStatus;
            if (!EncryptedDBHelper.getsInstance().saveTx(slate.txLog)){
                if (callback != null){
                    callback.onCall(false, "Db error:saveTx failed");
                    return;
                }
            }
        } else {
            slate.tokenTxLog.server_status = ServerTxStatus.TxDefaultStatus;
            if (!EncryptedDBHelper.getsInstance().saveTokenTx(slate.tokenTxLog)){
                if (callback != null){
                    callback.onCall(false, "Db error:saveTokenTx failed");
                    return;
                }
            }
        }

        //save output status
        VcashWallet.getInstance().syncOutputInfo();
        VcashWallet.getInstance().syncTokenOutputInfo();

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

    public static void isValidSlateConentForReceive(String fileContent, final WalletCallback callback){
        Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, (new VcashSlate()).new VcashSlateTypeAdapter()).create();
        VcashSlate slate;
        try {
             slate = gson.fromJson(fileContent, VcashSlate.class);
        }catch (Exception e){
            if (callback != null){
                callback.onCall(false, "Wrong Data Format");
            }
            return;
        }
        if (slate == null || !slate.isValidForReceive()){
            if (callback != null){
                callback.onCall(false, "Wrong Data Format");
            }
            return;
        }

        VcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
        if (txLog != null){
            if (callback != null){
                callback.onCall(false, "Duplicate transaction");
            }
            return;
        }

        VcashTokenTxLog tokenTxLog = EncryptedDBHelper.getsInstance().getTokenTxBySlateId(slate.uuid);
        if (tokenTxLog != null){
            if (callback != null){
                callback.onCall(false, "Duplicate transaction");
            }
            return;
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
                VcashWallet.getInstance().reloadTokenOutputInfo();
            }
        };



        Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, (new VcashSlate()).new VcashSlateTypeAdapter()).create();
        String slateStr = gson.toJson(slate);

        if (slate.token_type != null) {
            slate.createNewTokenOutputsFn.onCall();
            //save txLog
            slate.tokenTxLog.server_status = ServerTxStatus.TxReceiverd;
            if (serverTx != null){
                slate.tokenTxLog.parter_id = serverTx.sender_id;
            }
            slate.tokenTxLog.signed_slate_msg = slateStr;
            if (!EncryptedDBHelper.getsInstance().saveTokenTx(slate.tokenTxLog)){
                rollbackBlock.onCall();
                Log.e(Tag, "VcashDataManager saveTokenTx failed");
                callback.onCall(false, null);
                return;
            }

            //save output status
            VcashWallet.getInstance().syncTokenOutputInfo();
        } else {
            slate.createNewOutputsFn.onCall();
            //save txLog
            slate.txLog.server_status = ServerTxStatus.TxReceiverd;
            if (serverTx != null){
                slate.txLog.parter_id = serverTx.sender_id;
            }
            slate.txLog.signed_slate_msg = slateStr;
            if (!EncryptedDBHelper.getsInstance().saveTx(slate.txLog)){
                rollbackBlock.onCall();
                Log.e(Tag, "VcashDataManager saveTx failed");
                callback.onCall(false, null);
                return;
            }

            //save output status
            VcashWallet.getInstance().syncOutputInfo();
        }

        if (serverTx != null){
            serverTx.slate = slateStr;
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
            EncryptedDBHelper.getsInstance().commitDatabaseTransaction();
            if (callback != null){
                callback.onCall(true, slateStr);
            }
        }

    }

    public static void isValidSlateConentForFinalize(String fileContent, final WalletCallback callback){
        Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, (new VcashSlate()).new VcashSlateTypeAdapter()).create();
        VcashSlate slate;
        try {
            slate = gson.fromJson(fileContent, VcashSlate.class);
        }catch (Exception e){
            if (callback != null){
                callback.onCall(false, "Wrong Data Format");
            }
            return;
        }
        if (slate == null || !slate.isValidForFinalize()){
            if (callback != null){
                callback.onCall(false, "Wrong Data Format");
            }
            return;
        }

        if (slate.token_type != null) {
            VcashTokenTxLog txLog = EncryptedDBHelper.getsInstance().getTokenTxBySlateId(slate.uuid);
            if (txLog == null){
                if (callback != null){
                    callback.onCall(false, "Tx missed");
                }
                return;
            }
        } else {
            VcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
            if (txLog == null){
                if (callback != null){
                    callback.onCall(false, "Tx missed");
                }
                return;
            }
        }

        if (callback != null){
            callback.onCall(true, slate);
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

    public static void finalizeTransaction(final VcashSlate slate, final WalletCallback callback){
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
                    if (slate.token_type != null) {
                        VcashTokenTxLog txLog = EncryptedDBHelper.getsInstance().getTokenTxBySlateId(slate.uuid);
                        if (txLog != null){
                            txLog.confirm_state = VcashTxLog.TxLogConfirmType.LoalConfirmed;
                            txLog.server_status = ServerTxStatus.TxFinalized;
                            EncryptedDBHelper.getsInstance().saveTokenTx(txLog);
                        }
                    } else {
                        VcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
                        if (txLog != null){
                            txLog.confirm_state = VcashTxLog.TxLogConfirmType.LoalConfirmed;
                            txLog.server_status = ServerTxStatus.TxFinalized;
                            EncryptedDBHelper.getsInstance().saveTx(txLog);
                        }
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

        if (txLog != null && !UIUtils.isEmpty(txLog.parter_id)){
            ServerApi.cancelTransaction(tx_id, new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    if (!yesOrNo){
                        Log.e(Tag, "cancel tx to Server failed");
                    }
                }
            });
        }

        return true;
    }

    public static ArrayList<VcashTxLog> getTransationArr(){
        return EncryptedDBHelper.getsInstance().getTxData();
    }

    public static ArrayList<VcashTokenTxLog> getTokenTransationArr(){
        return EncryptedDBHelper.getsInstance().getTokenTxData();
    }

    public static VcashTxLog getTxByTxid(String txid){
        return EncryptedDBHelper.getsInstance().getTxBySlateId(txid);
    }

    public static VcashTokenTxLog getTokenTxByTxid(String txid){
        return EncryptedDBHelper.getsInstance().getTokenTxBySlateId(txid);
    }

    public static boolean deleteTxByTxid(String txid){
        return  EncryptedDBHelper.getsInstance().deleteTxBySlateId(txid);
    }

    public static boolean deleteTokenTxByTxid(String txid){
        return  EncryptedDBHelper.getsInstance().deleteTokenTxBySlateId(txid);
    }

    public static ArrayList<VcashTxLog> getFileReceiveTxArr(){
        ArrayList<VcashTxLog> txArr = getTransationArr();
        ArrayList<VcashTxLog> retArr = new ArrayList<>();
        if(txArr != null){
            for (VcashTxLog txLog: txArr){
                if (txLog.tx_type == VcashTxLog.TxLogEntryType.TxReceived
                        && UIUtils.isEmpty(txLog.parter_id)
                        && !UIUtils.isEmpty(txLog.signed_slate_msg)){
                    retArr.add(txLog);
                }
            }
        }
        return retArr;
    }

    public static ArrayList<VcashTokenTxLog> getFileReceiveTokenTxArr(){
        ArrayList<VcashTokenTxLog> txArr = getTokenTransationArr();
        ArrayList<VcashTokenTxLog> retArr = new ArrayList<>();
        if(txArr != null){
            for (VcashTokenTxLog txLog: txArr){
                if (txLog.tx_type == VcashTokenTxLog.TokenTxLogEntryType.TokenTxReceived
                        && UIUtils.isEmpty(txLog.parter_id)
                        && !UIUtils.isEmpty(txLog.signed_slate_msg)){
                    retArr.add(txLog);
                }
            }
        }
        return retArr;
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
                                    tx.confirm_height = nodeOutput.height;
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
                                    final VcashTxLog callback_tx = tx;
                                    NodeApi.getOutputsByCommitArr(tx.outputs, new WalletCallback() {
                                        @Override
                                        public void onCall(boolean yesOrNo, Object data) {
                                            if (yesOrNo){
                                                ArrayList<NodeRefreshOutput> apiOutputs = (ArrayList<NodeRefreshOutput>)data;
                                                if (apiOutputs.size() > 0){
                                                    NodeRefreshOutput output = apiOutputs.get(0);
                                                    callback_tx.confirm_height = output.height;
                                                    EncryptedDBHelper.getsInstance().saveTx(callback_tx);
                                                }
                                            }
                                        }
                                    });
                                }
                                item.status = VcashOutput.OutputStatus.Spent;
                                hasChange = true;
                            }
                        }
                    }

                    if (hasChange){
                        EncryptedDBHelper.getsInstance().saveTxDataArr(txs);
                        VcashWallet.getInstance().syncOutputInfo();
                    }
                }

                callback.onCall(yesOrNo, null);
            }
        });
    }

    public static void updateTokenOutputStatusWithComplete(final WalletCallback callback) {
        Map<String, ArrayList<VcashTokenOutput>> dic = VcashWallet.getInstance().token_outputs_dic;
        Set<String> set = dic.keySet();
        String[] token_types = set.toArray(new String[0]);
        List<String> tokensList = Arrays.asList(token_types);
        Collections.sort(tokensList);
        if (tokensList.size() > 0) {
            updateTokenOutputStatus(tokensList.get(0), callback);
        } else {
            if (callback != null){
                callback.onCall(true, null);
            }
        }

    }

    private static void updateTokenOutputStatus(final String token_type, final WalletCallback callback) {
        if (token_type == null) {
            if (callback != null){
                callback.onCall(true, null);
            }
            return;
        }

        implUpdateTokenOutputStatus(token_type,  new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo) {
                    Map<String, ArrayList<VcashTokenOutput>> dic = VcashWallet.getInstance().token_outputs_dic;
                    Set<String> set = dic.keySet();
                    String[] token_types = set.toArray(new String[0]);
                    List<String> tokensList = Arrays.asList(token_types);
                    Collections.sort(tokensList);
                    int index = tokensList.indexOf(token_type);
                    if (index + 1 >= tokensList.size()){
                        if (callback != null){
                            callback.onCall(true, null);
                        }
                        return;
                    }

                    String nextTokenType = tokensList.get(index+1);
                    updateTokenOutputStatus(nextTokenType, null);
                } else {
                    if (callback != null){
                        callback.onCall(false, null);
                    }
                    return;
                }
            }
        });
    }

    private static void implUpdateTokenOutputStatus(String token_type, final WalletCallback callback) {
        ArrayList<String> strArr = new ArrayList<>();
        final ArrayList<VcashTokenOutput> token_arr = VcashWallet.getInstance().token_outputs_dic.get(token_type);
        for (VcashTokenOutput item: token_arr){
            strArr.add(item.commitment);
        }

        if (strArr.size() == 0){
            callback.onCall(true, null);
            return;
        }

        NodeApi.getTokenOutputsByCommitArr(token_type, strArr, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    ArrayList<NodeRefreshTokenOutput> apiOutputs = (ArrayList<NodeRefreshTokenOutput>)data;
                    ArrayList<VcashTokenTxLog> txs = getTokenTransationArr();
                    boolean hasChange = false;
                    for (VcashTokenOutput item: token_arr){
                        NodeRefreshTokenOutput nodeOutput = null;
                        for (NodeRefreshTokenOutput output: apiOutputs){
                            if (item.commitment.equals(output.commit)){
                                nodeOutput = output;
                            }
                        }

                        if (nodeOutput!=null){
                            //should not be coinbase
                            if (item.is_token_issue && item.status == VcashOutput.OutputStatus.Unconfirmed){

                            }
                            else if(!item.is_token_issue && item.status == VcashOutput.OutputStatus.Unconfirmed){
                                VcashTokenTxLog tx = null;
                                for (VcashTokenTxLog txLog :txs){
                                    if (txLog.tx_id == item.tx_log_id){
                                        tx = txLog;
                                    }
                                }
                                if (tx != null){
                                    tx.confirm_state = VcashTxLog.TxLogConfirmType.NetConfirmed;
                                    tx.confirm_time = AppUtil.getCurrentTimeSecs();
                                    tx.confirm_height = nodeOutput.height;
                                    tx.server_status = ServerTxStatus.TxFinalized;
                                }
                                item.height = nodeOutput.height;
                                item.status = VcashOutput.OutputStatus.Unspent;

                                hasChange = true;
                            }
                        }
                        else{
                            if (item.status == VcashOutput.OutputStatus.Locked || item.status == VcashOutput.OutputStatus.Unspent){
                                VcashTokenTxLog tx = null;
                                for (VcashTokenTxLog txLog :txs){
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
                                    final VcashTokenTxLog callback_tx = tx;
                                    NodeApi.getOutputsByCommitArr(tx.outputs, new WalletCallback() {
                                        @Override
                                        public void onCall(boolean yesOrNo, Object data) {
                                            if (yesOrNo){
                                                ArrayList<NodeRefreshOutput> apiOutputs = (ArrayList<NodeRefreshOutput>)data;
                                                if (apiOutputs.size() > 0){
                                                    NodeRefreshOutput output = apiOutputs.get(0);
                                                    callback_tx.confirm_height = output.height;
                                                    EncryptedDBHelper.getsInstance().saveTokenTx(callback_tx);
                                                }
                                            }
                                        }
                                    });
                                }
                                item.status = VcashOutput.OutputStatus.Spent;
                                hasChange = true;
                            }
                        }
                    }

                    if (hasChange){
                        EncryptedDBHelper.getsInstance().saveTokenTxDataArr(txs);
                        VcashWallet.getInstance().syncOutputInfo();
                        VcashWallet.getInstance().syncTokenOutputInfo();
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
