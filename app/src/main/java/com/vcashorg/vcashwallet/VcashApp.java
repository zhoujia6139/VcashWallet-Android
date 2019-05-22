package com.vcashorg.vcashwallet;

import android.app.Application;
import android.content.Context;

public class VcashApp extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
    }


    public static Context getContext(){
        return mContext;
    }
}
