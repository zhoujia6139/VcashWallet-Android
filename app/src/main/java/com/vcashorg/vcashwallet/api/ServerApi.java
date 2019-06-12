package com.vcashorg.vcashwallet.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vcashorg.vcashwallet.api.bean.FinalizeTxInfo;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.net.CommonObserver;
import com.vcashorg.vcashwallet.net.RetrofitUtils;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;
import com.vcashorg.vcashwallet.wallet.VcashWallet;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;

import okhttp3.ResponseBody;

public class ServerApi {
    static private String Tag = "------ServerApi";
    public static void checkStatus(String userId, final WalletCallback callback) {
        RetrofitUtils.getServerApiUrl().checkStatus(userId)
                .compose(RxHelper.<ArrayList<JsonElement>>io2main())
                .subscribe(new CommonObserver<ArrayList<JsonElement>>() {
                    @Override
                    public void onSuccess(ArrayList<JsonElement> result) {
                        ArrayList<ServerTransaction> txs = new ArrayList<>();
                        ServerTransaction tx = new ServerTransaction();
                        Gson gson = new GsonBuilder().registerTypeAdapter(ServerTransaction.class, tx.new ServerTransactionTypeAdapter()).create();
                        for (JsonElement item :result){
                            txs.add(gson.fromJson(item, ServerTransaction.class));
                        }
                        if (callback != null){
                            callback.onCall(true, txs);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("checkStatus failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });
    }

    public static void sendTransaction(ServerTransaction tx, final WalletCallback callback) {
        tx.msg_sig = AppUtil.hex(NativeSecp256k1.instance().ecdsaSign(tx.msgToSign(), VcashWallet.getInstance().getSignerKey()));

        Gson gson = new GsonBuilder().registerTypeAdapter(ServerTransaction.class, tx.new ServerTransactionTypeAdapter()).create();
        JsonElement jsonStr = gson.toJsonTree(tx);


        RetrofitUtils.getServerApiUrl().sendTransaction(jsonStr)
                .compose(RxHelper.<ResponseBody>io2main())
                .subscribe(new CommonObserver<ResponseBody>() {
                    @Override
                    public void onSuccess(ResponseBody result) {
                        Log.d(Tag, "sendTransaction suc");
                        if (callback != null){
                            callback.onCall(true, null);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("sendTransaction failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });
    }

    public static void receiveTransaction(ServerTransaction tx, final WalletCallback callback) {
        tx.msg_sig = AppUtil.hex(NativeSecp256k1.instance().ecdsaSign(tx.msgToSign(), VcashWallet.getInstance().getSignerKey()));
        tx.tx_sig = AppUtil.hex(NativeSecp256k1.instance().ecdsaSign(tx.txDataToSign(), VcashWallet.getInstance().getSignerKey()));

        Gson gson = new GsonBuilder().registerTypeAdapter(ServerTransaction.class, tx.new ServerTransactionTypeAdapter()).create();
        JsonElement jsonStr = gson.toJsonTree(tx);
        RetrofitUtils.getServerApiUrl().receiveTransaction(jsonStr)
                .compose(RxHelper.<ResponseBody>io2main())
                .subscribe(new CommonObserver<ResponseBody>() {
                    @Override
                    public void onSuccess(ResponseBody result) {
                        Log.d(Tag, "receiveTransaction suc");
                        if (callback != null){
                            callback.onCall(true, null);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("receiveTransaction failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });
    }

    public static void filanizeTransaction(String tx_id, final WalletCallback callback) {
        FinalizeTxInfo tx = new FinalizeTxInfo();
        tx.code = ServerTxStatus.TxFinalized;
        tx.tx_id = tx_id;
        tx.msg_sig = AppUtil.hex(NativeSecp256k1.instance().ecdsaSign(tx.msgToSign(), VcashWallet.getInstance().getSignerKey()));

        Gson gson = new GsonBuilder().registerTypeAdapter(FinalizeTxInfo.class, tx.new FinalizeTxInfoTypeAdapter()).create();
        JsonElement jsonStr = gson.toJsonTree(tx);
        RetrofitUtils.getServerApiUrl().filanizeTransaction(jsonStr)
                .compose(RxHelper.<ResponseBody>io2main())
                .subscribe(new CommonObserver<ResponseBody>() {
                    @Override
                    public void onSuccess(ResponseBody result) {
                        Log.d(Tag, "filanizeTransaction suc");
                        if (callback != null){
                            callback.onCall(true, null);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("filanizeTransaction failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });
    }

    public static void cancelTransaction(String tx_id, final WalletCallback callback) {
        FinalizeTxInfo tx = new FinalizeTxInfo();
        tx.code = ServerTxStatus.TxCanceled;
        tx.tx_id = tx_id;
        tx.msg_sig = AppUtil.hex(NativeSecp256k1.instance().ecdsaSign(tx.msgToSign(), VcashWallet.getInstance().getSignerKey()));

        Gson gson = new GsonBuilder().registerTypeAdapter(FinalizeTxInfo.class, tx.new FinalizeTxInfoTypeAdapter()).create();
        JsonElement jsonStr = gson.toJsonTree(tx);
        RetrofitUtils.getServerApiUrl().cancelTransaction(jsonStr)
                .compose(RxHelper.<ResponseBody>io2main())
                .subscribe(new CommonObserver<ResponseBody>() {
                    @Override
                    public void onSuccess(ResponseBody result) {
                        Log.d(Tag, "cancelTransaction suc");
                        if (callback != null){
                            callback.onCall(true, null);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("cancelTransaction failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });
    }

    public static void closeTransaction(String tx_id, final WalletCallback callback) {
        FinalizeTxInfo tx = new FinalizeTxInfo();
        tx.code = ServerTxStatus.TxClosed;
        tx.tx_id = tx_id;
        tx.msg_sig = AppUtil.hex(NativeSecp256k1.instance().ecdsaSign(tx.msgToSign(), VcashWallet.getInstance().getSignerKey()));

        Gson gson = new GsonBuilder().registerTypeAdapter(FinalizeTxInfo.class, tx.new FinalizeTxInfoTypeAdapter()).create();
        JsonElement jsonStr = gson.toJsonTree(tx);
        RetrofitUtils.getServerApiUrl().closeTransaction(jsonStr)
                .compose(RxHelper.<ResponseBody>io2main())
                .subscribe(new CommonObserver<ResponseBody>() {
                    @Override
                    public void onSuccess(ResponseBody result) {
                        Log.d(Tag, "closeTransaction suc");
                        if (callback != null){
                            callback.onCall(true, null);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("closeTransaction failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });
    }
}
