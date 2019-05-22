package com.vcashorg.vcashwallet.api;

import com.vcashorg.vcashwallet.modal.Demo;
import com.vcashorg.vcashwallet.net.Response;

import io.reactivex.Observable;
import retrofit2.http.GET;

public interface ApiUrl {

    @GET("values")
    Observable<Demo> demo();

}
