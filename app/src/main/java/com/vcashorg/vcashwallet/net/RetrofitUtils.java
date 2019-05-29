package com.vcashorg.vcashwallet.net;

import android.support.annotation.NonNull;

import com.vcashorg.vcashwallet.api.NodeApiUrl;
import com.vcashorg.vcashwallet.api.ServerApiUrl;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitUtils {
    private static final String TAG = "RetrofitUtils";

    private static NodeApiUrl mNodeApiUrl;
    private static ServerApiUrl mServerApiUrl;


    public static NodeApiUrl getNodeApiUrl(){
        if(mNodeApiUrl == null){
            synchronized (RetrofitUtils.class){
                if(mNodeApiUrl == null){
                    mNodeApiUrl = new RetrofitUtils().getNodeRetrofit();
                }
            }
        }
        return mNodeApiUrl;
    }

    public static ServerApiUrl getServerApiUrl(){
        if(mServerApiUrl == null){
            synchronized (RetrofitUtils.class){
                if(mServerApiUrl == null){
                    mServerApiUrl = new RetrofitUtils().getServerRetrofit();
                }
            }
        }
        return mServerApiUrl;
    }


    private RetrofitUtils(){};

    public NodeApiUrl getNodeRetrofit(){
        return initRetrofit(initOkHttp(), NodeApiUrl.BaseUrl).create(NodeApiUrl.class);
    }

    public ServerApiUrl getServerRetrofit(){
        return initRetrofit(initOkHttp(), ServerApiUrl.BaseUrl).create(ServerApiUrl.class);
    }


    @NonNull
    private Retrofit initRetrofit(OkHttpClient client, String baseUrl){
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @NonNull
    private OkHttpClient initOkHttp(){
        return new OkHttpClient().newBuilder()
                .addInterceptor(new LogInterceptor())
                .build();
    }
}
