package com.vcashorg.vcashwallet.update;

import com.google.gson.JsonObject;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface UpdateApi {

    @GET
    Observable<JsonObject> getUpdateConfig(@Url String url);

}
