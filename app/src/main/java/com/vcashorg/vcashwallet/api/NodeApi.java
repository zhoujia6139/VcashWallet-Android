package com.vcashorg.vcashwallet.api;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.vcashorg.vcashwallet.api.bean.NodeJsonRpcRes;
import com.vcashorg.vcashwallet.api.bean.NodeJsonRpcRq;
import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshTokenOutput;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.net.CommonObserver;
import com.vcashorg.vcashwallet.net.RetrofitUtils;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.VcashWallet;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTransaction;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NodeApi {
    static private String Tag = "------NodeApi";
    static private long curHeight = 0;
    static private long lastFetch = 0;
    static private ArrayList<VcashOutput> outputArr = new ArrayList<>();
    static private ArrayList<VcashTokenOutput> tokenOutputArr = new ArrayList<>();
    static private OkHttpClient okClient = new OkHttpClient();

    public static void getOutputsByPmmrIndex(final long startHeight, final WalletCallback callback){
        if (startHeight == 0) {
            outputArr.clear();
        }
        RetrofitUtils.getNodeApiUrl().getOutputs(startHeight)
                .compose(RxHelper.<NodeOutputs>io2main())
                .subscribe(new CommonObserver<NodeOutputs>() {
            @Override
            public void onSuccess(NodeOutputs result) {
                Log.i(Tag, String.format("getOutputsByPmmrIndex:last_retrieved_index=%d, highest_index=%d, size=%d", result.last_retrieved_index, result.highest_index, result.outputs.size()));
                for (NodeOutputs.NodeOutput item :result.outputs){
                    VcashOutput output = (VcashOutput)VcashWallet.getInstance().identifyUtxoOutput(item);
                    if (output != null){
                        outputArr.add(output);
                    }
                }
                if (result.highest_index > result.last_retrieved_index){
                    NodeApi.getOutputsByPmmrIndex(result.last_retrieved_index, callback);
                    double percent = (double)result.last_retrieved_index / (double)result.highest_index;
                    if (callback != null){
                        callback.onCall(true, percent);
                    }
                }
                else {
                    if (callback != null){
                        callback.onCall(true, outputArr);
                    }
                    outputArr.clear();
                }
            }

            @Override
            public void onFailure(Throwable e, String errorMsg) {
                Log.e(Tag, String.format("getOutputsByPmmrIndex failed:%s", errorMsg));
                if (callback != null){
                    callback.onCall(false, startHeight);
                }
            }
        });
    }

    public static void getTokenOutputsByPmmrIndex(final long startHeight, final WalletCallback callback){
        if (startHeight == 0) {
            tokenOutputArr.clear();
        }

        RetrofitUtils.getNodeApiUrl().getTokenOutputs(startHeight)
                .compose(RxHelper.<NodeOutputs>io2main())
                .subscribe(new CommonObserver<NodeOutputs>() {
                    @Override
                    public void onSuccess(NodeOutputs result) {
                        for (NodeOutputs.NodeOutput item :result.outputs){
                            VcashTokenOutput output = (VcashTokenOutput)VcashWallet.getInstance().identifyUtxoOutput(item);
                            if (output != null){
                                tokenOutputArr.add(output);
                            }
                        }
                        if (result.highest_index > result.last_retrieved_index){
                            NodeApi.getTokenOutputsByPmmrIndex(result.last_retrieved_index, callback);
                            double percent = (double)result.last_retrieved_index / (double)result.highest_index;
                            if (callback != null){
                                callback.onCall(true, percent);
                            }
                        }
                        else{
                            if (callback != null){
                                callback.onCall(true, tokenOutputArr);
                            }
                            tokenOutputArr.clear();
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Log.e(Tag, String.format("getTokenOutputsByPmmrIndex failed:%s", errorMsg));
                        if (callback != null){
                            callback.onCall(false, startHeight);
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

    public static void getKernel(final String excess, final WalletCallback callback) {
        ArrayList<JsonElement> params = new ArrayList<>();
        params.add(new JsonPrimitive(excess));
        params.add(null);
        params.add(null);
        NodeJsonRpcRq rq = new NodeJsonRpcRq("get_kernel", params);

        final Gson rq_gson = new Gson();
        String req_str = rq_gson.toJson(rq);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), req_str);
        final Handler handler=new Handler();
        try{
            final String full_url = String.format("%s/v2/foreign", RetrofitUtils.getNodeBaseUrl());
            Request req = new Request.Builder().url(full_url).post(body).build();
            okClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(Tag, String.format("getKernel failed:%s", e.getMessage()));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onCall(false, null);
                            }
                        }
                    });
                }

                @Override
                public void onResponse(final Call call, Response response) {
                    try {
                        String json = response.body().string();
                        Gson res_gson = new Gson();
                        NodeJsonRpcRes res = res_gson.fromJson(json, NodeJsonRpcRes.class);
                        Object kernelObj = res.result.get("Ok");
                        if (res.error != null || kernelObj == null) {
                            Log.e(Tag, String.format("getKernel failed:%s", res.error));
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onCall(false, null);
                                    }
                                }
                            });
                        } else {
                            Log.w(Tag, String.format("getKernel suc:%s", excess));
//                            Gson kernel_gson = new GsonBuilder().registerTypeAdapter(VcashTransaction.TxKernel.class, new VcashTransaction.TxKernel.TxKernelTypeAdapter()).serializeNulls().create();
//                            final VcashTransaction.TxKernel kernel =  kernel_gson.fromJson(kernelStr, VcashTransaction.TxKernel.class);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onCall(true, null);
                                    }
                                }
                            });

                        }

                    }catch (Exception e){
                        Log.e(Tag, String.format("getKernel failed:%s", e.getMessage()));
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCall(false, null);
                                }
                            }
                        });
                    }

                }
            });
        } catch (Exception e){
            Log.e(Tag, String.format("getKernel failed:%s", e.getMessage()));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onCall(false, null);
                    }
                }
            });
        }
    }

    public static void getTokenKernel(final String tokenExcess, final WalletCallback callback) {
        ArrayList<JsonElement> params = new ArrayList<>();
        params.add(new JsonPrimitive(tokenExcess));
        params.add(null);
        params.add(null);
        NodeJsonRpcRq rq = new NodeJsonRpcRq("get_token_kernel", params);

        final Gson rq_gson = new Gson();
        String req_str = rq_gson.toJson(rq);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), req_str);
        final Handler handler=new Handler();
        try{
            final String full_url = String.format("%s/v2/foreign", RetrofitUtils.getNodeBaseUrl());
            Request req = new Request.Builder().url(full_url).post(body).build();
            okClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(Tag, String.format("getTokenKernel failed:%s", e.getMessage()));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onCall(false, null);
                            }
                        }
                    });
                }

                @Override
                public void onResponse(final Call call, Response response) {
                    try {
                        String json = response.body().string();
                        Gson res_gson = new Gson();
                        NodeJsonRpcRes res = res_gson.fromJson(json, NodeJsonRpcRes.class);
                        Object kernelObj = res.result.get("Ok");
                        if (res.error != null || kernelObj == null) {
                            Log.e(Tag, String.format("getTokenKernel failed:%s", res.error));
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onCall(false, null);
                                    }
                                }
                            });
                        } else {
                            Log.w(Tag, String.format("getTokenKernel suc:%s", tokenExcess));
//                            Gson kernel_gson = new GsonBuilder().registerTypeAdapter(VcashTransaction.TokenTxKernel.class, new VcashTransaction.TokenTxKernel.TokenTxKernelTypeAdapter()).serializeNulls().create();
//                            final VcashTransaction.TokenTxKernel kernel =  kernel_gson.fromJson(kernelStr, VcashTransaction.TokenTxKernel.class);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onCall(true, null);
                                    }
                                }
                            });

                        }

                    }catch (Exception e){
                        Log.e(Tag, String.format("getTokenKernel failed:%s", e.getMessage()));
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onCall(false, null);
                                }
                            }
                        });
                    }

                }
            });
        } catch (Exception e){
            Log.e(Tag, String.format("getTokenKernel failed:%s", e.getMessage()));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onCall(false, null);
                    }
                }
            });
        }
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
