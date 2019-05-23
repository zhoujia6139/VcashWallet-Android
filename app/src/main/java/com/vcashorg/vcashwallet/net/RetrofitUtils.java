package com.vcashorg.vcashwallet.net;

import android.support.annotation.NonNull;

import com.vcashorg.vcashwallet.api.ApiUrl;
import com.vcashorg.vcashwallet.utils.Constants;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitUtils {
    private static final String TAG = "RetrofitUtils";

    private static ApiUrl mApiUrl;


    public static ApiUrl getApiUrl(){
        if(mApiUrl == null){
            synchronized (RetrofitUtils.class){
                if(mApiUrl == null){
                    mApiUrl = new RetrofitUtils().getRetrofit();
                }
            }
        }
        return mApiUrl;
    }


    private RetrofitUtils(){};

    public ApiUrl getRetrofit(){
        return initRetrofit(initOkHttp()).create(ApiUrl.class);
    }


    @NonNull
    private Retrofit initRetrofit(OkHttpClient client){
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(Constants.BASE_URL)
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
