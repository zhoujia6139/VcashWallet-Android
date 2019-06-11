package com.vcashorg.vcashwallet.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.VcashWallet;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerTxManager {
    final private static long TimerInterval = 30;
    private static ServerTxManager instance;
    static private String Tag = "------ServerTxManager";
    private long lastFetch = 0;


    private Map<String,ServerTransaction> txMap = new LinkedHashMap<>();
    private Map<String,ServerTransaction> txBlackListMap = new LinkedHashMap<>();
    private LinkedBlockingQueue<ServerTransaction> txQueue = new LinkedBlockingQueue<>();

    private ServerTxCallBack callBack;


    public static ServerTxManager getInstance() {
        if (instance == null) {
            instance = new ServerTxManager();
        }
        return instance;
    }

    public void addNewTxCallBack(ServerTxCallBack callBack) {
        this.callBack = callBack;
    }

    private Timer timer;
    private TimerTask timerTask;

    class ServerTxTimer extends TimerTask {

        @Override
        public void run() {
            fetchTxStatus(false);
        }
    }

    public void startWork() {
        timer = new Timer();
        timerTask = new ServerTxTimer();
        timer.schedule(timerTask, 0, 30000);

    }

    public void stopWork() {
        timer.cancel();
        timerTask.cancel();
    }

    public void fetchTxStatus(final boolean force) {
        if (force || (AppUtil.getCurrentTimeSecs() - lastFetch) >= (TimerInterval - 1)) {
            ServerApi.checkStatus(VcashWallet.getInstance().mUserId, new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    if (yesOrNo) {
                        ArrayList<ServerTransaction> txs = (ArrayList<ServerTransaction>) data;
                        Log.i(Tag, String.format("check status ret %d tx", txs.size()));

                        lastFetch = AppUtil.getCurrentTimeSecs();
                        boolean hasNewData = false;
                        txMap.clear();

                        for (ServerTransaction item : txs) {
                            Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, (new VcashSlate()).new VcashSlateTypeAdapter()).create();
                            item.slateObj = gson.fromJson(item.slate, VcashSlate.class);
                            if (item.slateObj != null) {
                                VcashTxLog txLog = EncryptedDBHelper.getsInstance().getTxBySlateId(item.slateObj.uuid);

                                //check as receiver
                                if (item.receiver_id.equals(VcashWallet.getInstance().mUserId)) {
                                    item.isSend = false;
                                    if (item.status == ServerTxStatus.TxFinalized ||
                                            item.status == ServerTxStatus.TxCanceled) {
                                        if (txLog != null && txLog.confirm_state == VcashTxLog.TxLogConfirmType.DefaultState) {
                                            switch (item.status) {
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
                                        ServerApi.closeTransaction(item.tx_id, null);
                                        continue;
                                    }
                                }
                                //check as sender
                                else if (item.sender_id.equals(VcashWallet.getInstance().mUserId)) {
                                    item.isSend = true;
                                    //check is cancelled
                                    if (txLog.server_status == ServerTxStatus.TxCanceled) {
                                        ServerApi.cancelTransaction(txLog.tx_slate_id, null);
                                        continue;
                                    }

                                    //check is finalized
                                    if (txLog.server_status == ServerTxStatus.TxFinalized) {
                                        ServerApi.filanizeTransaction(txLog.tx_slate_id, null);
                                        continue;
                                    }

                                    if (item.status == ServerTxStatus.TxReceiverd) {
                                        txLog.server_status = item.status;
                                        EncryptedDBHelper.getsInstance().saveTx(txLog);
                                    }
                                }

                                //if goes here item.status would be TxDefaultStatus or TxReceiverd

                                //process special case here
                                //if tx confirmed by net, finalize directly
                                if (txLog != null && txLog.confirm_state == VcashTxLog.TxLogConfirmType.NetConfirmed){
                                    ServerApi.filanizeTransaction(txLog.tx_slate_id, null);
                                    continue;
                                }

                                txMap.put(item.tx_id,item);
                                if(!txBlackListMap.containsKey(item.tx_id)){
                                    txQueue.offer(item);
                                    hasNewData = true;
                                }

                            } else {
                                Log.e(Tag, String.format("receive a illegal tx"));
                            }
                        }

                        if (hasNewData && callBack != null) {
                            callBack.onChecked();
                        }else{
                            if(force && callBack != null){
                                callBack.onForceRefresh();
                            }
                        }
                    } else {
                        lastFetch = 0;
                        if(force && callBack != null){
                            callBack.onForceRefresh();
                        }
                    }
                }
            });
        }
    }

    public interface ServerTxCallBack {
        void onChecked();

        void onForceRefresh();
    }

    public ServerTransaction getRecentTx() {
        return txQueue.poll();
    }

    public ServerTransaction getServerTxByTxId(String slateId){
        return txMap.get(slateId);
    }

    public void addBlackList(ServerTransaction serverTx){
        if(serverTx != null){
            txBlackListMap.put(serverTx.tx_id,serverTx);
        }
    }

    public boolean inServerTxList(String tx_id){
        return txMap.containsKey(tx_id);
    }

    public void removeServerTx(String tx_id){
        txMap.remove(tx_id);
    }

    public List<ServerTransaction> getSeverTxList(){
        List<ServerTransaction> list = new ArrayList<>();
        //List<ServerTransaction> blacklist = new ArrayList<>();
        List<ServerTransaction> normallist = new ArrayList<>();

        Set<Map.Entry<String, ServerTransaction>> entrySet = txMap.entrySet();

        for (Map.Entry<String,ServerTransaction> entry : entrySet){

            normallist.add(entry.getValue());

        }

        //Collections.reverse(blacklist);
        Collections.reverse(normallist);

        //list.addAll(blacklist);
        list.addAll(normallist);

        return list;
    }


}
