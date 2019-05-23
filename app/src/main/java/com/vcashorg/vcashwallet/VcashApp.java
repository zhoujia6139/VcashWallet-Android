package com.vcashorg.vcashwallet;

import android.app.Application;
import android.content.Context;

import com.vcashorg.vcashwallet.wallet.WalletApi;

public class VcashApp extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();

        WalletApi.setWalletContext(getApplicationContext());
        WalletApi.createWallet(null, null);
    }


    public static Context getContext(){
        return mContext;
    }
}
