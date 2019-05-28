package com.vcashorg.vcashwallet.api;

import android.util.Log;

import com.vcashorg.vcashwallet.api.bean.FinalizeTxInfo;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.net.CommonObserver;
import com.vcashorg.vcashwallet.net.RetrofitUtils;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import java.util.ArrayList;

public class ServerApi {
    static private String Tag = "------ServerApi";
    public static void checkStatus(String userId, final WalletCallback callback) {
        RetrofitUtils.getServerApiUrl().checkStatus(userId)
                .compose(RxHelper.<ArrayList<ServerTransaction>>io2main())
                .subscribe(new CommonObserver<ArrayList<ServerTransaction>>() {
                    @Override
                    public void onSuccess(ArrayList<ServerTransaction> result) {
                        if (callback != null){
                            callback.onCall(true, result);
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
        RetrofitUtils.getServerApiUrl().sendTransaction(tx)
                .compose(RxHelper.io2main())
                .subscribe(new CommonObserver() {
                    @Override
                    public void onSuccess(Object result) {
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
        RetrofitUtils.getServerApiUrl().receiveTransaction(tx)
                .compose(RxHelper.io2main())
                .subscribe(new CommonObserver() {
                    @Override
                    public void onSuccess(Object result) {
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
        RetrofitUtils.getServerApiUrl().filanizeTransaction(tx)
                .compose(RxHelper.io2main())
                .subscribe(new CommonObserver() {
                    @Override
                    public void onSuccess(Object result) {
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
        RetrofitUtils.getServerApiUrl().cancelTransaction(tx)
                .compose(RxHelper.io2main())
                .subscribe(new CommonObserver() {
                    @Override
                    public void onSuccess(Object result) {
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
        RetrofitUtils.getServerApiUrl().closeTransaction(tx)
                .compose(RxHelper.io2main())
                .subscribe(new CommonObserver() {
                    @Override
                    public void onSuccess(Object result) {
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
