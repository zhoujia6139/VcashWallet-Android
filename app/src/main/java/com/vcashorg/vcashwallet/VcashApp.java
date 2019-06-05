package com.vcashorg.vcashwallet;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;

import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.TimeOutUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;

public class VcashApp extends Application {

    private static Context mContext;

    private ArrayMap<String,String> passwordFilter = new ArrayMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();

        WalletApi.setWalletContext(getApplicationContext());

        registerActivityLifecycleCallbacks(lifecycleCallbacks);
        addFilter();


//        WalletApi.createWallet(null, null);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                WalletApi.createSendTransaction("acf39ed33ddb35196b0a", WalletApi.vcashToNano(1), 0, new WalletCallback() {
//                    @Override
//                    public void onCall(boolean yesOrNo, Object data) {
//                        if (yesOrNo){
//                            WalletApi.sendTransaction((VcashSlate) data, "acf39ed33ddb35196b0a", new WalletCallback() {
//                                @Override
//                                public void onCall(boolean yesOrNo, Object data) {
//
//                                }
//                            });
//                        }
//
//                    }
//                });
//            }
//        }, 10*1000);

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
            if(mFinalCount == 1 && canShowPassword(activity.getClass())){
                if(TimeOutUtil.getInstance().isTimeOut()
                        && SPUtil.getInstance(UIUtils.getContext()).getValue(SPUtil.FIRST_CREATE_WALLET,false)){
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

    public void addToPasswordFilter(Class<? extends Activity> clazz){
        String key = clazz.getName();
        String activityName = clazz.getSimpleName();
        if(!passwordFilter.containsKey(key)){
            passwordFilter.put(key,activityName);
        }
    }

    public void removeFromPasswordFilter(Class<? extends Activity> clazz){
        String key = clazz.getName();
        if(passwordFilter.containsKey(key)){
            passwordFilter.remove(key);
        }
    }

    private void addFilter(){
        addToPasswordFilter(LauncherActivity.class);
        addToPasswordFilter(MnemonicConfirmActivity.class);
        addToPasswordFilter(MnemonicCreateActivity.class);
        addToPasswordFilter(MnemonicRestoreActivity.class);
        addToPasswordFilter(PasswordActivity.class);
        addToPasswordFilter(VcashStartActivity.class);
        addToPasswordFilter(WalletCreateActivity.class);
        addToPasswordFilter(VcashValidateActivity.class);
    }

    private boolean canShowPassword(Class<? extends Activity> clazz){
        String key = clazz.getName();
        return !passwordFilter.containsKey(key);
    }
}
