package com.vcashorg.vcashwallet.net;

import org.json.JSONException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;

import retrofit2.HttpException;

public class RxExceptionUtil {

    public static String exceptionHandler(Throwable e){
        String errorMsg = "Unknown error";
        if (e instanceof UnknownHostException) {
            errorMsg = "Network unavailable";
        } else if (e instanceof SocketTimeoutException) {
            errorMsg = "Request network timeout";
        } else if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            errorMsg = convertStatusCode(httpException);
        } else if (e instanceof ParseException || e instanceof JSONException) {
            errorMsg = "Data parsing error";
        }
        return errorMsg;
    }

    private static String convertStatusCode(HttpException httpException) {
        String msg;
        if (httpException.code() >= 500 && httpException.code() < 600) {
            msg = "Server processing request error";
        } else if (httpException.code() >= 400 && httpException.code() < 500) {
            msg = "Server was unable to process the request";
        } else if (httpException.code() >= 300 && httpException.code() < 400) {
            msg = "Requests are redirected to other pages";
        } else {
            msg = httpException.message();
        }
        return msg;
    }
}
