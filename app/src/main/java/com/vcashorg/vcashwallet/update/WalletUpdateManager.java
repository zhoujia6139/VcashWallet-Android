package com.vcashorg.vcashwallet.update;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.miner.update.utils.UpdateUtils;
import com.vcashorg.vcashwallet.net.CommonObserver;
import com.vcashorg.vcashwallet.net.RetrofitUtils;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.utils.UIUtils;

public class WalletUpdateManager {

    public static final int UPDATE_REQUEST_CODE = 100;

    private static WalletUpdateManager mInstance;

    private WalletUpdateManager(){

    }

    public static WalletUpdateManager getInstance(){
        if(mInstance == null){
            mInstance = new WalletUpdateManager();
        }
        return mInstance;
    }


    public void fetchUpdateConfig(final Activity activity){
        RetrofitUtils.getUpdateRetrofit().getUpdateConfig("")
                .compose(RxHelper.<JsonObject>io2main())
                .subscribe(new CommonObserver<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject result) {
                        try {
                           // Log.i("yjq","Update Success");
                            JsonObject versionObject = result.getAsJsonObject("version");
                            JsonObject androidObject = versionObject.getAsJsonObject("android");
                            Version version = new Gson().fromJson(androidObject,Version.class);
                            checkUpdate(version,activity);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                       // Log.i("yjq","Update onFailure");
                    }
                });
    }

    private void checkUpdate(Version version,Activity activity){
        if (version != null) {
            if (!TextUtils.isEmpty(version.download_url)) {
                int type = UpdateUtils.getUpdateStrategy(UIUtils.getContext(), version.current_version, version.lowest_version);
                if (type == UpdateUtils.TYPE_FORCE_UPDATE) { //forceUpdate
                    com.miner.update.UpdateManager.getInstance()
                            .createDownLoadBuilder()
                            .setDownLoadType(type)
                            .setDownloadUrl(version.download_url)
                            .execute(activity, UPDATE_REQUEST_CODE);
                } else {
                    com.miner.update.UpdateManager.getInstance()
                            .createDownLoadBuilder()
                            .setDownLoadType(type)
                            .setDownloadUrl(version.download_url)
                            .execute(activity);
                }
            }
        }
    }

    public interface FetchUpdateConfigListener{
        void onSuccess();
    }

}
