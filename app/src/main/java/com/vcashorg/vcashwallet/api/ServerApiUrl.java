package com.vcashorg.vcashwallet.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vcashorg.vcashwallet.api.bean.FinalizeTxInfo;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;

import java.util.ArrayList;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ServerApiUrl {
    public static final String BaseUrl = "http://47.75.163.56:13515";

    @GET("/statecheck/{user_id}")
    Observable<ArrayList<JsonElement>> checkStatus(@Path ("user_id") String user_id);

    @POST("/sendvcash")
    Observable<ResponseBody> sendTransaction(@Body JsonElement tx);

    @POST("/receivevcash")
    Observable<ResponseBody> receiveTransaction(@Body JsonElement tx);

    @POST("/finalizevcash")
    Observable<ResponseBody> filanizeTransaction(@Body JsonElement tx);

    @POST("/finalizevcash")
    Observable<ResponseBody> cancelTransaction(@Body JsonElement tx);

    @POST("/finalizevcash")
    Observable<ResponseBody> closeTransaction(@Body JsonElement tx);
}
