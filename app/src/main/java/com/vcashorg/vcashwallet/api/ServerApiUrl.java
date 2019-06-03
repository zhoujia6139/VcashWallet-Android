package com.vcashorg.vcashwallet.api;

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
    Observable<ArrayList<ServerTransaction>> checkStatus(@Path ("user_id") String user_id);

    @POST("/sendvcash")
    Observable<ResponseBody> sendTransaction(@Body String tx);

    @POST("/receivevcash")
    Observable receiveTransaction(@Body String tx);

    @POST("/finalizevcash")
    Observable filanizeTransaction(@Body String tx);

    @POST("/finalizevcash")
    Observable cancelTransaction(@Body String tx);

    @POST("/finalizevcash")
    Observable closeTransaction(@Body String tx);
}
