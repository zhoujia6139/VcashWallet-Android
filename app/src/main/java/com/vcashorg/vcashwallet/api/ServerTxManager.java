package com.vcashorg.vcashwallet.api;

import android.util.Log;

import com.google.gson.Gson;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.VcashWallet;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import java.util.ArrayList;

public class ServerTxManager {
    final private static long TimerInterval = 30;
    private static ServerTxManager instance;
    static private String Tag = "------ServerTxManager";
    private ArrayList<ServerTransaction> txArr = new ArrayList<ServerTransaction>();
    private long lastFetch = 0;

    public static ServerTxManager getInstance(){
        if (instance == null){
            instance = new ServerTxManager();
        }
        return instance;
    }

    public void startWork(){

    }

    public void stopWork(){

    }

    public void fetchTxStatus(boolean force){
        if (force || (AppUtil.getCurrentTimeSecs() - lastFetch) >= (TimerInterval-1)){
            ServerApi.checkStatus(VcashWallet.getInstance().mUserId, new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    ArrayList<ServerTransaction> txs = (ArrayList<ServerTransaction>)data;
                    Log.i(Tag, String.format("check status ret %d tx", txs.size()));

                    if (yesOrNo){
                        lastFetch = AppUtil.getCurrentTimeSecs();
                        for (ServerTransaction item:txs){
                            Gson gson = new Gson();
                            item.slateObj = gson.fromJson(item.slate, VcashSlate.class);
                            if (item.slateObj != null){
                                VcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(item.slateObj.uuid);

                                //check as receiver
                                if (item.receiver_id.equals(VcashWallet.getInstance().mUserId)){
                                    if (item.status == ServerTxStatus.TxFinalized ||
                                    item.status == ServerTxStatus.TxCanceled){
                                        if (txLog != null && txLog.confirm_state == VcashTxLog.TxLogConfirmType.DefaultState){
                                            switch (item.status){
                                                case TxFinalized:
                                                    txLog.confirm_state = VcashTxLog.TxLogConfirmType.LoalConfirmed;
                                                    break;
                                                case TxCanceled:
                                                    txLog.tx_type = VcashTxLog.TxLogEntryType.TxReceivedCancelled;
                                                    break;

                                                    default:
                                                        break;
                                            }
                                            txLog.server_status = item.status;
                                            EncryptedDBHelper.getsInstance().saveTx(txLog);
                                        }
                                    }
                                    ServerApi.closeTransaction(item.tx_id, null);
                                    continue;
                                }
                                //check as sender
                                else if (item.sender_id.equals(VcashWallet.getInstance().mUserId)){
                                    //check is cancelled
                                    if (txLog.server_status == ServerTxStatus.TxCanceled){
                                        ServerApi.cancelTransaction(txLog.tx_slate_id, null);
                                        continue;
                                    }

                                    //check is finalized
                                    if (txLog.server_status == ServerTxStatus.TxFinalized){
                                        ServerApi.filanizeTransaction(txLog.tx_slate_id, null);
                                        continue;
                                    }

                                    if (item.status == ServerTxStatus.TxReceiverd){
                                        txLog.server_status = item.status;
                                        EncryptedDBHelper.getsInstance().saveTx(txLog);
                                    }
                                }

                                //if goes here item.status would be TxDefaultStatus or TxReceiverd
                                boolean isRepeat = false;
                                for (ServerTransaction tx :txArr){
                                    if (tx.tx_id.equals(item.tx_id)){
                                        isRepeat = true;
                                        break;
                                    }
                                }

                                if (!isRepeat){
                                    txArr.add(item);
                                }
                            }
                            else {
                                Log.e(Tag, String.format("receive a illegal tx"));
                            }
                        }
                    } else {
                        lastFetch = 0;
                    }
                }
            });
        }
    }

    private void handleServerTx(){

    }
}
