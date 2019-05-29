package com.vcashorg.vcashwallet;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.vcashorg.vcashwallet.utils.TimeOutUtil;
import com.vcashorg.vcashwallet.wallet.WalletApi;

public class VcashApp extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();

        WalletApi.setWalletContext(getApplicationContext());

        registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }


    public static Context getContext(){
        return mContext;
    }

    private int mFinalCount;

    private ActivityLifecycleCallbacks lifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            mFinalCount++;
            Log.e("VcashApp", "onActivityStarted: " + activity.getClass().getSimpleName() + ">>>" + mFinalCount);
            if(mFinalCount == 1){
                if(TimeOutUtil.getInstance().isTimeOut() && activity instanceof WalletMainActivity){
                    Intent intent = new Intent(activity, VcashValidateActivity.class);
                    intent.putExtra(VcashValidateActivity.PARAM_MODE,VcashValidateActivity.MODE_TIMEOUT_VALIDATE);
                    activity.startActivity(intent);
                }
            }

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            mFinalCount --;
            if(mFinalCount == 0){
                //front to back
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };
}
