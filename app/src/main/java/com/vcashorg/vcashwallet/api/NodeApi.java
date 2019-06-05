package com.vcashorg.vcashwallet.api;

import android.util.Log;

import com.google.gson.JsonObject;
import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.net.CommonObserver;
import com.vcashorg.vcashwallet.net.RetrofitUtils;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.VcashWallet;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;

public class NodeApi {
    static private String Tag = "------NodeApi";
    static private long curHeight = 0;
    static private long lastFetch = 0;

    public static void getOutputsByPmmrIndex(long startHeight, final ArrayList<VcashOutput> retArr, final WalletCallback callback){
        RetrofitUtils.getNodeApiUrl().getOutputs(startHeight)
                .compose(RxHelper.<NodeOutputs>io2main())
                .subscribe(new CommonObserver<NodeOutputs>() {
            @Override
            public void onSuccess(NodeOutputs result) {
                for (NodeOutputs.NodeOutput item :result.outputs){
                    VcashOutput output = VcashWallet.getInstance().identifyUtxoOutput(item);
                    if (output != null){
                        retArr.add(output);
                    }
                }
                if (result.highest_index > result.last_retrieved_index){
                    NodeApi.getOutputsByPmmrIndex(result.last_retrieved_index, retArr, callback);
                }
                else if(result.highest_index == result.last_retrieved_index){
                    if (callback != null){
                        callback.onCall(true, retArr);
                    }
                }
            }

            @Override
            public void onFailure(Throwable e, String errorMsg) {
                Log.e(Tag, String.format("getOutputsByPmmrIndex failed:%s", errorMsg));
                if (callback != null){
                    callback.onCall(false, null);
                }
            }
        });
    }

    public static void getOutputsByCommitArr(ArrayList<String> commitArr, final WalletCallback callback){
        StringBuffer sb = new StringBuffer();
        for (String item :commitArr){
            sb.append(item);
            sb.append(",");
        }
        String param = sb.toString();
        param = param.substring(0,  param.length()-1);

        RetrofitUtils.getNodeApiUrl().getOutputsByCommitArr(param)
                .compose(RxHelper.<ArrayList<NodeRefreshOutput>>io2main())
                .subscribe(new CommonObserver<ArrayList<NodeRefreshOutput>>() {
                    @Override
                    public void onSuccess(ArrayList<NodeRefreshOutput> result) {
                        if (callback != null){
                            callback.onCall(true, result);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("getOutputsByCommitArr failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });

    }

    public static long getChainHeight(final WalletCallback callback){
        if (AppUtil.getCurrentTimeSecs() - lastFetch > 10){
            RetrofitUtils.getNodeApiUrl().getChainHeight()
                    .compose(RxHelper.<NodeChainInfo>io2main())
                    .subscribe(new CommonObserver<NodeChainInfo>() {
                        @Override
                        public void onSuccess(NodeChainInfo result) {
                            lastFetch = AppUtil.getCurrentTimeSecs();
                            curHeight = result.height;
                            if (callback != null){
                                callback.onCall(true, result);
                            }
                        }

                        @Override
                        public void onFailure(Throwable e, String errorMsg) {
                            Log.e(Tag, String.format("getChainHeight failed:%s", errorMsg));
                            if (callback != null){
                                callback.onCall(false, null);
                            }
                            lastFetch = 0;
                        }
                    });
        }

        return curHeight;
    }

    public static void postTx(String tx, final WalletCallback callback){
//        Map<String, String> map = new HashMap<>();
//        map.put("tx_hex", tx);
        JsonObject body = new JsonObject();
        body.addProperty("tx_hex",tx);
        RetrofitUtils.getNodeApiUrl().postTx(body)
                .compose(RxHelper.<ResponseBody>io2main())
                .subscribe(new CommonObserver<ResponseBody>() {
                    @Override
                    public void onSuccess(ResponseBody result) {
                        if (callback != null){
                            callback.onCall(true, null);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("postTx failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });
    }
}
