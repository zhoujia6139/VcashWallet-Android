package com.vcashorg.vcashwallet.wallet;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.vcashorg.vcashwallet.api.NodeApi;
import com.vcashorg.vcashwallet.api.ServerApi;
import com.vcashorg.vcashwallet.api.bean.JsonRpcRes;
import com.vcashorg.vcashwallet.api.bean.JsonRpcRq;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshTokenOutput;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.utils.CoinUtils;
import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.AbstractVcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.ExportPaymentInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashContext;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletNoParamCallBack;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate.SlateState.Standard2;
import static java.nio.ByteOrder.BIG_ENDIAN;

public class WalletApi {
    static {
        System.loadLibrary("secp256k1_wrapper");
        System.loadLibrary("paymentproof");
    }
    static private String Tag = "------WalletApi";
    final public static long VCASH_BASE = 1000000000;
    private static  Context context;
    private static OkHttpClient okClient = new OkHttpClient();
    private static Map<String, VcashTokenInfo> tokenInfoMap;
    private static Set addedToken;

    public static void initTokenInfos() {
        if (tokenInfoMap == null) {
            tokenInfoMap = new HashMap<>();
            addedToken = new HashSet();
            readTokenInfoFromFile();
            readAddedTokenFromSp();
            updateTokenInfos(null);
        }
    }

    public static void updateTokenInfos(final WalletNoParamCallBack callback) {
        try{
            final String full_url = "https://s.vcashwallet.app/token_static/VCashTokenInfo.json";
            Request req = new Request.Builder().url(full_url).get().build();
            okClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(Tag, "fetch Tokeninfos failed...");

                    if(callback != null){
                        callback.onCall();
                    }
                }

                @Override
                public void onResponse(final Call call, Response response) {
                    try {
                        String json = response.body().string();
                        Type type = new TypeToken<ArrayList<VcashTokenInfo>>() {}.getType();
                        ArrayList<VcashTokenInfo> vcashTokenInfos = new Gson().fromJson(json, type);
                        if (vcashTokenInfos.size() > 0) {
                            tokenInfoMap = new HashMap<>();
                            for (VcashTokenInfo vcashTokenInfo : vcashTokenInfos) {
                                //VcashTokenInfo info = new Gson().fromJson(jsonO, VcashTokenInfo.class);
                                tokenInfoMap.put(vcashTokenInfo.TokenId, vcashTokenInfo);
                            }

                        }

                    }catch (Exception e){
                        Log.e(Tag, "parse tokeninfo response catch exception...");
                    }

                    writeTokenInfoToFile();

                    if(callback != null){
                        callback.onCall();
                    }
                }
            });
        } catch (Exception exc){
            Log.e(Tag, "fetch Tokeninfos catch exception...");
        }
    }

    private static void writeTokenInfoToFile() {
        if(tokenInfoMap != null){
            SPUtil.getInstance(UIUtils.getContext()).putHashMapData(SPUtil.TOKEN_ALL,tokenInfoMap);
        }
    }

    private static void readTokenInfoFromFile() {
        Map<String, VcashTokenInfo> map = SPUtil.getInstance(UIUtils.getContext()).getHashMapData(SPUtil.TOKEN_ALL,VcashTokenInfo.class);
        if(map != null){
            tokenInfoMap = map;
        }
    }

    private static void writeAddedTokenToSp() {
        if(addedToken != null){
            SPUtil.getInstance(UIUtils.getContext()).setStringListValue(SPUtil.TOKEN_ADDED_TYPE,addedToken);
        }
    }

    private static void readAddedTokenFromSp() {
        Set<String> listValue = SPUtil.getInstance(UIUtils.getContext()).getStringListValue(SPUtil.TOKEN_ADDED_TYPE);
        if(listValue != null){
            addedToken = listValue;
        }
    }

    public static Set getAllTokens() {
        return tokenInfoMap.keySet();
    }

    public static VcashTokenInfo getTokenInfo(String tokenType) {
        if(tokenType.equals("VCash")){
            VcashTokenInfo info = new VcashTokenInfo();
            info.Balance = WalletApi.getWalletBalanceInfo();
            info.Name = "VCash";
            info.FullName = "--";
            info.TokenId = tokenType;
            return info;
        }

        VcashTokenInfo info = tokenInfoMap.get(tokenType);
        if (info == null && tokenType.length() == 64) {
            info = new VcashTokenInfo();
            info.TokenId = tokenType;
            info.Name = tokenType.substring(0, 8);
            info.FullName = "--";
        }

        if(info != null){
            info.TokenId = tokenType;
            info.Balance = WalletApi.getWalletTokenBalanceInfo(tokenType);
        }

        return info;
    }

    public static Set getAddedTokens() {
        return addedToken;
    }

    public static void addAddedToken(String tokenType) {
        addedToken.add(tokenType);
        writeAddedTokenToSp();
    }

    public static void deleteAddedToken(String tokenType) {
        addedToken.remove(tokenType);
        writeAddedTokenToSp();
    }

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

        if(VcashWallet.getInstance().outputs != null){
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
        }

        WalletBalanceInfo info = new WalletApi.WalletBalanceInfo();
        info.total = total;
        info.spendable = spendable;
        info.locked = locked;
        info.unconfirmed = unconfirmed;
        return info;
    }

    public static WalletBalanceInfo getWalletTokenBalanceInfo(String tokenType){
        long total = 0;
        long locked = 0;
        long unconfirmed = 0;
        long spendable = 0;

        ArrayList<VcashTokenOutput> tokenOutputs = VcashWallet.getInstance().token_outputs_dic.get(tokenType);
        if(tokenOutputs != null){
            for (VcashTokenOutput output :tokenOutputs){
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
        }

        WalletBalanceInfo info = new WalletApi.WalletBalanceInfo();
        info.total = total;
        info.spendable = spendable;
        info.locked = locked;
        info.unconfirmed = unconfirmed;
        return info;
    }

    public static Set getBalancedToken() {
        return VcashWallet.getInstance().token_outputs_dic.keySet();
    }


    public static long getCurChainHeight(){
        return VcashWallet.getInstance().getChainHeight();
    }

    public static void checkWalletUtxo(long startHeight, final WalletCallback callback){
        NodeApi.getOutputsByPmmrIndex(startHeight, new WalletCallback() {
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
                            tx.tx_type = item.is_coinbase? VcashTxLog.TxLogEntryType.ConfirmedCoinbaseOrTokenIssue: VcashTxLog.TxLogEntryType.TxReceived;
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
                        callback.onCall(false, data);
                    }
                }
            }
        });
    }

    public static void checkWalletTokenUtxo(long startHeight, final WalletCallback callback){
        NodeApi.getTokenOutputsByPmmrIndex(startHeight, new WalletCallback() {
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
                            tx.tx_type = item.is_token_issue? VcashTxLog.TxLogEntryType.ConfirmedCoinbaseOrTokenIssue: VcashTxLog.TxLogEntryType.TxReceived;
                            tx.server_status = ServerTxStatus.TxClosed;
                            tx.token_type = item.token_type;
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
                        callback.onCall(false, data);
                    }
                }
            }
        });
    }

    public static void createSendTransaction(String token_type, long amount, final String proofAddress, final WalletCallback callback){
        VcashSlate.PaymentInfo info = null;
        if (WalletApi.isValidSlatePackAddress(proofAddress)) {
            String receiverAddr = WalletApi.getPubkeyFromProofAddress(proofAddress);
            info = new VcashSlate.PaymentInfo();
            info.sender_address = WalletApi.getPubkeyFromProofAddress(VcashWallet.getInstance().mUserId);
            info.receiver_address = receiverAddr;
        }

        final VcashSlate.PaymentInfo final_info = info;
        if (token_type != null) {
            VcashWallet.getInstance().sendTokenTransaction(token_type, amount, new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    if (yesOrNo) {
                        VcashSlate slate = (VcashSlate)data;
                        slate.partnerAddress = proofAddress;
                        slate.payment_proof = final_info;
                    }
                    callback.onCall(yesOrNo, data);
                }
            });
        } else {
            VcashWallet.getInstance().sendTransaction(amount, 0, new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    if (yesOrNo) {
                        VcashSlate slate = (VcashSlate)data;
                        slate.partnerAddress = proofAddress;
                        slate.payment_proof = final_info;
                    }
                    callback.onCall(yesOrNo, data);
                }
            });
        }
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

        if (slate.token_type != null) {
            slate.tokenTxLog.parter_id = user;
        } else {
            slate.txLog.parter_id = user;
        }

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
                    final Gson rq_gson = new GsonBuilder().registerTypeAdapter(JsonRpcRq.class, new JsonRpcRq.JsonRpcRqTypeAdapter()).serializeNulls().create();
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
                            public void onResponse(final Call call, Response response) {
                                try {
                                    String json = response.body().string();
                                    Gson res_gson = new GsonBuilder().registerTypeAdapter(JsonRpcRes.class, new JsonRpcRes.JsonRpcResTypeAdapter()).serializeNulls().create();
                                    JsonRpcRes res = res_gson.fromJson(json, JsonRpcRes.class);
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
                    String slate_str = WalletApi.encryptSlateForParter(slate);
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
        String slateStr = AppUtil.hex(slate.selializeAsData());
        if (slate.txLog != null) {
            slate.txLog.server_status = ServerTxStatus.TxDefaultStatus;
            slate.txLog.signed_slate_msg = slateStr;
            if (!EncryptedDBHelper.getsInstance().saveTx(slate.txLog)){
                if (callback != null){
                    callback.onCall(false, "Db error:saveTx failed");
                    return;
                }
            }
        } else {
            slate.tokenTxLog.server_status = ServerTxStatus.TxDefaultStatus;
            slate.tokenTxLog.signed_slate_msg = slateStr;
            if (!EncryptedDBHelper.getsInstance().saveTx(slate.tokenTxLog)){
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
        VcashSlate slate = WalletApi.parseSlateFromEncrypedSlatePackStr(fileContent);
        if (slate == null || !slate.isValidForReceive()){
            if (callback != null){
                callback.onCall(false, "Wrong Data Format");
            }
            return;
        }

        AbstractVcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
        if (txLog != null){
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
        VcashSlate slate;
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

        if (slate.ttl_cutoff_height > 0 && slate.ttl_cutoff_height <= VcashWallet.getInstance().getChainHeight()) {
            Log.e(Tag, "Transaction Expired!");
            if (callback != null){
                callback.onCall(false, "Transaction Expired!");
            }
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

        if (!VcashWallet.getInstance().receiveTransaction(slate)){
            Log.e(Tag, "VcashWallet receiveTransaction failed");
            callback.onCall(false, null);
            return;
        }

        if (slate.payment_proof != null) {
            if (slate.payment_proof.sender_address == null || slate.payment_proof.sender_address.length() != 64 ||
            slate.payment_proof.receiver_address == null || slate.payment_proof.receiver_address.length() != 64) {
                rollbackBlock.onCall();
                Log.e(Tag, "Tx payment proof address is invalid!");
                callback.onCall(false, "Tx payment proof address is invalid!");
                return;
            }

            String selfAddress = WalletApi.getPubkeyFromProofAddress(VcashWallet.getInstance().mUserId);
            if (!selfAddress.equals(slate.payment_proof.receiver_address)) {
                rollbackBlock.onCall();
                Log.e(Tag, "Tx is not for me!");
                callback.onCall(false, "Tx is not for me!");
                return;
            }

            byte[] excess = slate.calculateExcess();
            byte[] key = VcashWallet.getInstance().getPaymentProofKey();
            String signature = WalletApi.createPaymentProofSignature(slate.token_type, slate.amount, AppUtil.hex(excess), slate.payment_proof.sender_address, AppUtil.hex(key));
            if (signature == null) {
                rollbackBlock.onCall();
                Log.e(Tag, "Create Tx payment proof failed");
                callback.onCall(false, "Create Tx payment proof failed");
                return;
            }
            Log.w(Tag, String.format("-------------signature:%s", signature));

            boolean isValid = WalletApi.verifyPaymentProof(slate.token_type, slate.amount, AppUtil.hex(excess), slate.payment_proof.sender_address, slate.payment_proof.receiver_address, signature);
            if (!isValid) {
                rollbackBlock.onCall();
                Log.e(Tag, "Create Tx payment proof failed");
                callback.onCall(false, "Create Tx payment proof failed");
                return;
            }

            slate.payment_proof.receiver_signature = signature;
        }

        // Can remove amount and fee now
        // as well as sender's sig data
        slate.amount = 0;
        slate.fee = 0;
        slate.removeOtherSigdata();
        slate.state = Standard2;

        String slateStr = WalletApi.encryptSlateForParter(slate);

        if (slate.token_type != null) {
            slate.createNewTokenOutputsFn.onCall();
            //save txLog
            slate.tokenTxLog.server_status = ServerTxStatus.TxReceiverd;
            if (serverTx != null){
                slate.tokenTxLog.parter_id = serverTx.sender_id;
            }
            slate.tokenTxLog.signed_slate_msg = slateStr;
            if (!EncryptedDBHelper.getsInstance().saveTx(slate.tokenTxLog)){
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
        VcashSlate slate = WalletApi.parseSlateFromEncrypedSlatePackStr(fileContent);
        if (slate == null || !slate.isValidForFinalize()){
            if (callback != null){
                callback.onCall(false, "Wrong Data Format");
            }
            return;
        }

        AbstractVcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
        if (txLog == null){
            if (callback != null){
                callback.onCall(false, "Tx missed");
            }
            return;
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
        AbstractVcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
        if (txLog == null || txLog.signed_slate_msg == null) {
            Log.e(Tag, "database record is broke, cannot find tx record");
            callback.onCall(false, null);
            return;
        }

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

        VcashSlate origSlate = VcashSlate.parseSlateFromData(AppUtil.decode(txLog.signed_slate_msg));
        if (origSlate.payment_proof != null) {
            if (slate.payment_proof == null) {
                Log.e(Tag, "Expected Payment Proof for this Transaction is not present");
                callback.onCall(false, null);
                return;
            }

            if (!origSlate.payment_proof.sender_address.equals(slate.payment_proof.sender_address) ||
                    !origSlate.payment_proof.receiver_address.equals(slate.payment_proof.receiver_address)) {
                Log.e(Tag, "Payment Proof address does not match original Payment Proof address");
                callback.onCall(false, null);
                return;
            }

            byte[] excess = slate.calculateExcess();
            boolean isValid = WalletApi.verifyPaymentProof(slate.token_type, slate.amount, AppUtil.hex(excess), slate.payment_proof.sender_address, slate.payment_proof.receiver_address, slate.payment_proof.receiver_signature);
            if (!isValid) {
                Log.e(Tag, "Recipient did not provide requested proof signature");
                callback.onCall(false, "Recipient did not provide requested proof signature");
                return;
            }
        }



        slate.tx.sortTx();
        byte[] txPayload = slate.tx.computePayload(false);
        NodeApi.postTx(AppUtil.hex(txPayload), new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if (yesOrNo){
                    callback.onCall(true, null);
                    AbstractVcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(slate.uuid);
                    if (txLog != null){
                        txLog.confirm_state = VcashTxLog.TxLogConfirmType.LoalConfirmed;
                        txLog.server_status = ServerTxStatus.TxFinalized;
                        txLog.signed_slate_msg = AppUtil.hex(slate.selializeAsData());
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
        AbstractVcashTxLog txLog = WalletApi.getTxByTxid(tx_id);
        if (txLog != null && !txLog.isCanBeCanneled()){
            return false;
        }
        if (txLog != null){
            txLog.cancelTxlog();
            EncryptedDBHelper.getsInstance().saveTx(txLog);
        }

        //if (txLog != null && !UIUtils.isEmpty(txLog.parter_id)){
        if (txLog != null){
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

    public static ArrayList<VcashTokenTxLog> getTokenTransationArr(String tokenType){
        return EncryptedDBHelper.getsInstance().getTokenTxData(tokenType);
    }

    public static AbstractVcashTxLog getTxByTxid(String txid){
        return EncryptedDBHelper.getsInstance().getTxBySlateId(txid);
    }

    public static boolean deleteTxByTxid(String txid){
        return  EncryptedDBHelper.getsInstance().deleteTxBySlateId(txid);
    }

    public static boolean deleteTokenTxByTxid(String txid){
        return  EncryptedDBHelper.getsInstance().deleteTokenTxBySlateId(txid);
    }

    public static ArrayList<AbstractVcashTxLog> getFileReceiveTxArr(){
        ArrayList<VcashTxLog> txArr = getTransationArr();
        ArrayList<AbstractVcashTxLog> retArr = new ArrayList<>();
        if(txArr != null){
            for (VcashTxLog txLog: txArr){
                if (txLog.tx_type == VcashTxLog.TxLogEntryType.TxReceived
                        && UIUtils.isEmpty(txLog.parter_id)
                        && !UIUtils.isEmpty(txLog.signed_slate_msg)){
                    retArr.add(txLog);
                }
            }
        }

        ArrayList<VcashTokenTxLog> tokenTxArr = getTokenTransationArr(null);
        if (tokenTxArr != null) {
            for (VcashTokenTxLog tokenLog: tokenTxArr) {
                if (tokenLog.tx_type == VcashTxLog.TxLogEntryType.TxReceived
                        && UIUtils.isEmpty(tokenLog.parter_id)
                        && !UIUtils.isEmpty(tokenLog.signed_slate_msg)){
                    retArr.add(tokenLog);
                }
            }
        }

        return retArr;
    }

    public static void updateTxStatus() {
        ArrayList<VcashTxLog> txArr = EncryptedDBHelper.getsInstance().getTxData();
        for (VcashTxLog tx :txArr) {
            WalletApi.checkSingleTxStatus(tx);
        }

        ArrayList<VcashTokenTxLog> tokenTxArr = EncryptedDBHelper.getsInstance().getTokenTxData(null);
        for (VcashTokenTxLog tx: tokenTxArr) {
            WalletApi.checkSingleTxStatus(tx);
        }
    }

    static void checkSingleTxStatus(AbstractVcashTxLog tx) {
        if (!tx.isCanBeAutoCanneled()) {
            return;
        }

        if (tx.signed_slate_msg != null) {
            try {
                Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, new VcashSlate.VcashSlateTypeAdapter()).serializeNulls().create();
                VcashSlate slate = gson.fromJson(tx.signed_slate_msg, VcashSlate.class);
                if (slate != null &&
                        slate.ttl_cutoff_height > 0 &&
                        slate.ttl_cutoff_height <= VcashWallet.getInstance().getChainHeight()) {
                    tx.cancelTxlog();
                    EncryptedDBHelper.getsInstance().saveTx(tx);
                }
            } catch (JsonSyntaxException exc) {

            }
        }
    }

    public static void updateOutputStatusWithComplete(final WalletCallback callback){
        ArrayList<String> strArr = new ArrayList<>();

        if(VcashWallet.getInstance().outputs != null){
            for (VcashOutput item: VcashWallet.getInstance().outputs){
                strArr.add(item.commitment);
            }
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
                    updateTokenOutputStatus(nextTokenType, callback);
                } else {
                    if (callback != null){
                        callback.onCall(false, null);
                    }
                    return;
                }
            }
        });
    }

    private static void implUpdateTokenOutputStatus(final String token_type, final WalletCallback callback) {
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
                    ArrayList<VcashTokenTxLog> txs = getTokenTransationArr(token_type);
                    if(txs == null){
                        txs = new ArrayList<>();
                    }
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
                                        for (String commitStr :txLog.token_inputs){
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

    public static Boolean isValidSlatePackAddress(String address) {
        String pubkey = WalletApi.getPubkeyFromProofAddress(address);
        return (pubkey != null && pubkey.length() == 64);
    }

    public static String getPubkeyFromProofAddress(String proofAddress) {
        return PaymentProof.getPubkeyFromProofAddress(proofAddress);
    }

    public static String createPaymentProofSignature(String token_type, long amount, String excess, String senderPubkey, String secKey) {
        return PaymentProof.createPaymentProofSignature(token_type, amount, excess, senderPubkey, secKey);
    }

    public static boolean verifyPaymentProof(String token_type, long amount, String excess, String senderPubkey, String verifyPubkey, String signature) {
        return PaymentProof.verifyPaymentProof(token_type, amount, excess, senderPubkey, verifyPubkey, signature);
    }

    public static String exportPaymentProof(VcashSlate slate) {
        if (slate.payment_proof != null) {
            ExportPaymentInfo proof = new ExportPaymentInfo();
            proof.token_type = slate.token_type;
            proof.amount = String.valueOf(slate.amount);
            byte[] excessData = slate.calculateExcess();
            proof.excess = AppUtil.hex(excessData);

            proof.recipient_address = PaymentProof.getProofAddressFromPubkey(slate.payment_proof.receiver_address);
            proof.recipient_sig = slate.payment_proof.receiver_signature;
            proof.sender_address = VcashWallet.getInstance().mUserId;
            byte[] key = VcashWallet.getInstance().getPaymentProofKey();
            String signature = PaymentProof.createPaymentProofSignature(slate.token_type, slate.amount, proof.excess, slate.payment_proof.sender_address, AppUtil.hex(key));
            if (signature != null) {
                boolean isValid = WalletApi.verifyPaymentProof(slate.token_type, slate.amount, proof.excess, slate.payment_proof.sender_address, slate.payment_proof.sender_address, signature);
                if (isValid) {
                    proof.sender_sig = signature;
                    Gson gson = new Gson();
                    return gson.toJson(proof);
                }
            }
        }

        return null;
    }

    public static void verifyPaymentProof(String proofStr, final WalletCallback callback){
        try{
            Gson gson = new Gson();
            ExportPaymentInfo proof = gson.fromJson(proofStr, ExportPaymentInfo.class);
            if (proof != null) {
                String senderAddr;
                if (proof.sender_address.length() == 64) {
                    senderAddr = proof.sender_address;
                } else if (proof.sender_address.length() == 56) {
                    senderAddr = PaymentProof.getPubkeyFromProofAddress(proof.sender_address);
                } else {
                    if(callback != null){
                        callback.onCall(false, "Sender address format is invalid.");
                    }
                    return;
                }

                String receiverAddr;
                if (proof.recipient_address.length() == 64) {
                    receiverAddr = proof.recipient_address;
                } else if (proof.recipient_address.length() == 56) {
                    receiverAddr = PaymentProof.getPubkeyFromProofAddress(proof.recipient_address);
                } else {
                    if(callback != null){
                        callback.onCall(false, "Recipient address format is invalid.");
                    }
                    return;
                }

                if (proof.sender_sig.length() != 128) {
                    if(callback != null){
                        callback.onCall(false, "Sender signature format is invalid.");
                    }
                    return;
                }

                if (proof.recipient_sig.length() != 128) {
                    if(callback != null){
                        callback.onCall(false, "Recipient signature format is invalid.");
                    }
                    return;
                }

                boolean isSenderSigValid = WalletApi.verifyPaymentProof(proof.token_type, Long.parseLong(proof.amount), proof.excess, senderAddr, senderAddr, proof.sender_sig);
                if (!isSenderSigValid) {
                    if(callback != null){
                        callback.onCall(false, "Invalid sender signature.");
                    }
                    return;
                }

                boolean isReceiverSigValid = WalletApi.verifyPaymentProof(proof.token_type, Long.parseLong(proof.amount), proof.excess, senderAddr, receiverAddr, proof.recipient_sig);
                if (!isReceiverSigValid) {
                    if(callback != null){
                        callback.onCall(false, "Invalid recipient signature.");
                    }
                    return;
                }

                if (proof.token_type != null) {
                    NodeApi.getTokenKernel(proof.excess, new WalletCallback() {
                        @Override
                        public void onCall(boolean yesOrNo, Object data) {
                            if (yesOrNo) {
                                if(callback != null){
                                    callback.onCall(true, "Signature is valid.");
                                }
                            } else {
                                if(callback != null){
                                    callback.onCall(false, "Transaction not found on chain.");
                                }
                            }
                        }
                    });
                } else {
                    NodeApi.getKernel(proof.excess, new WalletCallback() {
                        @Override
                        public void onCall(boolean yesOrNo, Object data) {
                            if (yesOrNo) {
                                if(callback != null){
                                    callback.onCall(true, "Signature is valid.");
                                }
                            } else {
                                if(callback != null){
                                    callback.onCall(false, "Transaction not found on chain.");
                                }
                            }
                        }
                    });
                }

                return;
            }
        }catch (Exception e) {

        }

        if(callback != null){
            callback.onCall(false, "Proof format is invalid.");
        }
    }

    public static VcashSlate parseSlateFromEncrypedSlatePackStr(String slateStr) {
        byte[] key = VcashWallet.getInstance().getPaymentProofKey();
        byte[] slate_bin = PaymentProof.slateFromSlatePackMessage(slateStr, AppUtil.hex(key));
        if (slate_bin == null) {
            return null;
        }

        VcashSlate slate = VcashSlate.parseSlateFromData(slate_bin);
        if (slate == null) {
            return null;
        }

        String slateAddress = PaymentProof.senderAddrFromSlatePackMessage(slateStr, AppUtil.hex(key));

        slate.partnerAddress = slateAddress;

        VcashContext context = EncryptedDBHelper.getsInstance().getContextBySlateId(slate.uuid);
        if (context != null) {
            slate.amount = context.amout;
            slate.fee = context.fee;
        }

        return slate;
    }

    public static String encryptSlateForParter(VcashSlate slate) {
        byte[] slate_bin = slate.selializeAsData();
        String slateStr = PaymentProof.createSlatePackMsg(slate_bin, slate.partnerAddress, VcashWallet.getInstance().mUserId);

        return slateStr;
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
