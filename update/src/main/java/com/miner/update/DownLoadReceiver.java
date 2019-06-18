package com.miner.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.miner.update.bean.DownLoadBean;
import com.miner.update.ui.UpdateActivity;
import com.miner.update.utils.AppUtils;

/**
 * Created by yjq
 * 2018/5/15.
 * 监听下载结束
 */

public class DownLoadReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
        if(id != 0 && id != -1){
            DownloadApkManager manager = new DownloadApkManager();
            DownLoadBean downBean = manager.query(context, id);
            if(downBean != null && !TextUtils.isEmpty(downBean.localUrl)){
                AppUtils.installApp(context, downBean.localUrl.replaceAll("file://", ""), "com.vcashorg.vcashwallet.fileprovider");
            }
        }
    }
}
