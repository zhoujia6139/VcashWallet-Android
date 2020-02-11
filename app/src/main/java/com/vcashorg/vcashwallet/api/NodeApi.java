package com.vcashorg.vcashwallet.api;

import android.util.Log;

import com.google.gson.JsonObject;
import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshTokenOutput;
import com.vcashorg.vcashwallet.net.CommonObserver;
import com.vcashorg.vcashwallet.net.RetrofitUtils;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.VcashWallet;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenOutput;
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
                Log.i(Tag, String.format("getOutputsByPmmrIndex:last_retrieved_index=%d, highest_index=%d, size=%d", result.last_retrieved_index, result.highest_index, result.outputs.size()));
                for (NodeOutputs.NodeOutput item :result.outputs){
                    VcashOutput output = (VcashOutput)VcashWallet.getInstance().identifyUtxoOutput(item);
                    if (output != null){
                        retArr.add(output);
                    }
                }
                if (result.highest_index > result.last_retrieved_index){
                    NodeApi.getOutputsByPmmrIndex(result.last_retrieved_index, retArr, callback);
                    double percent = (double)result.last_retrieved_index / (double)result.highest_index;
                    if (callback != null){
                        callback.onCall(true, percent);
                    }
                }
                else {
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

    public static void getTokenOutputsByPmmrIndex(long startHeight, final ArrayList<VcashTokenOutput> retArr, final WalletCallback callback){
        RetrofitUtils.getNodeApiUrl().getTokenOutputs(startHeight)
                .compose(RxHelper.<NodeOutputs>io2main())
                .subscribe(new CommonObserver<NodeOutputs>() {
                    @Override
                    public void onSuccess(NodeOutputs result) {
                        for (NodeOutputs.NodeOutput item :result.outputs){
                            VcashTokenOutput output = (VcashTokenOutput)VcashWallet.getInstance().identifyUtxoOutput(item);
                            if (output != null){
                                retArr.add(output);
                            }
                        }
                        if (result.highest_index > result.last_retrieved_index){
                            NodeApi.getTokenOutputsByPmmrIndex(result.last_retrieved_index, retArr, callback);
                            double percent = (double)result.last_retrieved_index / (double)result.highest_index;
                            if (callback != null){
                                callback.onCall(true, percent);
                            }
                        }
                        else{
                            if (callback != null){
                                callback.onCall(true, retArr);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("getTokenOutputsByPmmrIndex failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, null);
                        }
                    }
                });
    }

    public static void getOutputsByCommitArr(ArrayList<String> commitArr, final WalletCallback callback){
        if(commitArr == null || commitArr.size() == 0){
            if(callback != null){
                callback.onCall(false,null);
            }
            return;
        }

        StringBuilder sb = new StringBuilder();

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

    public static void getTokenOutputsByCommitArr(String tokenType, ArrayList<String> commitArr, final WalletCallback callback){
        StringBuilder sb = new StringBuilder();
        for (String item :commitArr){
            sb.append(item);
            sb.append(",");
        }
        String param = sb.toString();
        param = param.substring(0,  param.length()-1);

        RetrofitUtils.getNodeApiUrl().getTokenOutputsByCommitArr(tokenType, param)
                .compose(RxHelper.<ArrayList<NodeRefreshTokenOutput>>io2main())
                .subscribe(new CommonObserver<ArrayList<NodeRefreshTokenOutput>>() {
                    @Override
                    public void onSuccess(ArrayList<NodeRefreshTokenOutput> result) {
                        if (callback != null){
                            callback.onCall(true, result);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("getTokenOutputsByCommitArr failed:%s", errorMsg));
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
