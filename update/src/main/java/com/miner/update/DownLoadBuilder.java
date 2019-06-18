package com.miner.update;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.miner.update.callback.OnForceUpdateListener;
import com.miner.update.ui.UpdateActivity;
import com.miner.update.utils.FileUtils;
import com.miner.update.utils.UpdateUtils;

import java.io.Serializable;

/**
 * Author:YJQ
 * Time:2018/4/27  16:35
 * Description:下载构造器
 */
public class DownLoadBuilder implements Serializable{

    /**
     * 版本号
     */
    private int versionCode;
    /**
     * 版本名
     */
    private String versionName;
    /**
     * 下载的apk的保存地址(似乎用不到)
     */
    private String downloadAPKPath = FileUtils.getDownloadApkCachePath();
    /**
     * 下载地址
     */
    private String downloadUrl;
    /**
     * 下载策略
     */
    private int downloadType;
    /**
     * 标题
     */
    private String title;
    /**
     * 描述
     */
    private String content;
    /**
     * 在下载器中的id
     */
    private long downId;

    /**
     * 强制更新取消的监听
     */
    private OnForceUpdateListener listener;

    public DownLoadBuilder setVersionCode(int versionCode){
        this.versionCode = versionCode;
        return this;
    }

    public DownLoadBuilder setVersionName(String versionName){
        this.versionName = versionName;
        return this;
    }

    public DownLoadBuilder setTitle(String title){
        this.title = title;
        return this;
    }

    public DownLoadBuilder setContent(String content){
        this.content = content;
        return this;
    }

    public DownLoadBuilder setDownloadAPKPath(String downloadAPKPath) {
        this.downloadAPKPath = downloadAPKPath;
        return this;
    }

    public DownLoadBuilder setDownLoadType(int type){
        this.downloadType = type;
        return this;
    }


    public DownLoadBuilder setDownloadUrl(@NonNull String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }

    public DownLoadBuilder setDownLoadId(long id){
        this.downId = id;
        return this;
    }

    public DownLoadBuilder setListener(OnForceUpdateListener listener){
        this.listener = listener;
        return this;
    }

    public String getDownloadAPKPath() {
        return downloadAPKPath;
    }

    public String getDownLoadUrl(){
        return downloadUrl;
    }

    public String getContent() {
        return content;
    }

    public String getTitle() {
        return title;
    }

    public int getDownloadType() {
        return downloadType;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public long getDownId() {
        return downId;
    }



    public OnForceUpdateListener getListener(){
        return listener;
    }

    public void execute(Activity context,int requestCode){
        if(this.downloadType != UpdateUtils.TYPE_NO_UPDATE){
            Intent intent = new Intent(context, UpdateActivity.class);
            intent.putExtra(UpdateActivity.PARAM_DATA,this);
            context.startActivityForResult(intent,requestCode);
            context.overridePendingTransition(0, 0);
        }
    }

    public void execute(Activity context){
        if(this.downloadType != UpdateUtils.TYPE_NO_UPDATE){
            Intent intent = new Intent(context, UpdateActivity.class);
            intent.putExtra(UpdateActivity.PARAM_DATA,this);
            context.startActivity(intent);
            context.overridePendingTransition(0, 0);
        }
    }
}