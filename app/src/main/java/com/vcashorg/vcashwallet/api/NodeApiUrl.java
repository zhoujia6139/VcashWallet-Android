package com.vcashorg.vcashwallet.api;

import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;

import java.util.ArrayList;
import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.FieldMap;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NodeApiUrl {
    public static final String BaseUrl = "http://47.75.163.56:13513";

    @GET("/v1/txhashset/outputs?max=500")
    Observable<NodeOutputs> getOutputs(@Query("start_index") long start_index);

    @GET("/v1/chain/outputs/byids?")
    Observable<ArrayList<NodeRefreshOutput>> getOutputsByCommitArr(@Query("id") String ids);

    @GET("/v1/chain")
    Observable<NodeChainInfo> getChainHeight();

    @POST("/v1/pool/push?fluff")
    Observable postTx(@FieldMap Map<String, String>map);
}
