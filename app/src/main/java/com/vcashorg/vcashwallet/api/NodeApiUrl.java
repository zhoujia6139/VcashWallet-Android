package com.vcashorg.vcashwallet.api;

import com.google.gson.JsonObject;
import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshOutput;
import com.vcashorg.vcashwallet.api.bean.NodeRefreshTokenOutput;

import java.util.ArrayList;
import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NodeApiUrl {

    @GET("/v1/txhashset/outputs?max=800")
    Observable<NodeOutputs> getOutputs(@Query("start_index") long start_index);

    @GET("/v1/txhashset/tokenoutputs?max=800")
    Observable<NodeOutputs> getTokenOutputs(@Query("start_index") long start_index);

    @GET("/v1/chain/outputs/byids")
    Observable<ArrayList<NodeRefreshOutput>> getOutputsByCommitArr(@Query("id") String ids);

    @GET("/v1/chain/tokenoutputs/byids")
    Observable<ArrayList<NodeRefreshTokenOutput>> getTokenOutputsByCommitArr(@Query("token_type") String token_type, @Query("id") String ids);

    @GET("/v1/chain")
    Observable<NodeChainInfo> getChainHeight();

    @POST("/v1/pool/push_tx?fluff")
    Observable<ResponseBody> postTx(@Body JsonObject body);
}
