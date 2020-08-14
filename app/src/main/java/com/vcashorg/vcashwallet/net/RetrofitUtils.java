package com.vcashorg.vcashwallet.net;

import android.support.annotation.NonNull;

import com.vcashorg.vcashwallet.api.NodeApiUrl;
import com.vcashorg.vcashwallet.api.ServerApiUrl;
import com.vcashorg.vcashwallet.utils.AppUtil;

import java.util.concurrent.TimeUnit;

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
                    mNodeApiUrl = RetrofitUtils.getNodeRetrofit();
                }
            }
        }
        return mNodeApiUrl;
    }

    public static ServerApiUrl getServerApiUrl(){
        if(mServerApiUrl == null){
            synchronized (RetrofitUtils.class){
                if(mServerApiUrl == null){
                    mServerApiUrl = RetrofitUtils.getServerRetrofit();
                }
            }
        }
        return mServerApiUrl;
    }


    private RetrofitUtils(){};

    public static NodeApiUrl getNodeRetrofit(){
        return initRetrofit(initOkHttp(), getNodeBaseUrl()).create(NodeApiUrl.class);
    }

    public static ServerApiUrl getServerRetrofit(){
        return initRetrofit(initOkHttp(), getServerBaseUrl()).create(ServerApiUrl.class);
    }

    public static String getNodeBaseUrl(){
        if (AppUtil.isInTestNet){
            //return "http://172.20.10.2:13513";
            return "http://192.168.31.213:13513";
        }
        else{
            return "https://api-node.vcashwallet.app";
        }
    }

    private static String getServerBaseUrl(){
        if (AppUtil.isInTestNet){
            return "http://192.168.31.213:13500";
            //return "https://api.vcashwallet.app";
            //return "http://172.20.10.2:13500";
            //return "http://192.168.31.213:13500";
        }
        else{
            return "https://api.vcashwallet.app";
        }
    }


    @NonNull
    private static Retrofit initRetrofit(OkHttpClient client, String baseUrl){
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @NonNull
    private static OkHttpClient initOkHttp(){
        return new OkHttpClient().newBuilder()
                .addInterceptor(new LogInterceptor())
                .retryOnConnectionFailure(true)
                .connectTimeout(20,TimeUnit.SECONDS)
                .readTimeout(20,TimeUnit.SECONDS)
                .writeTimeout(15,TimeUnit.SECONDS)
                .build();
    }
}
