package com.miner.update.ui;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.miner.update.DownLoadBuilder;
import com.miner.update.DownloadApkManager;
import com.miner.update.R;
import com.miner.update.bean.DownLoadBean;
import com.miner.update.callback.OnForceUpdateListener;
import com.miner.update.utils.AppUtils;
import com.miner.update.utils.UpdateUtils;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.SettingService;

import java.util.List;

/**
 * Author:YJQ
 * Time:2018/4/28  10:39
 * Description: 处理升级逻辑的Activity
 */
public class UpdateActivity extends AppCompatActivity {
    public static final String PARAM_DATA = "data";
    private static final int REQUEST_CODE_PERMISSION_SD = 101;

    private DownLoadBuilder mBuilder;
    private DownloadApkManager mManager;

    private CommonProgressDialog pBar;
    private Thread mThread;
    private boolean live;
    private OnForceUpdateListener mListener;

    private boolean update = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if(intent == null){
            return;
        }
        mBuilder = (DownLoadBuilder) intent.getSerializableExtra(PARAM_DATA);
        if(mBuilder == null){
            return;
        }

        setContentView(R.layout.activity_update);

        //初mManager始化管理者
        mManager = new DownloadApkManager();
        mListener = mBuilder.getListener();

        showVersionDialog();
    }


    /**
     * onNewIntent初始化
     */
    private void init() {
        if(mBuilder.getDownId() == 0){
            return;
        }

        //判断下载状态
        DownLoadBean downInfo = mManager.query(this, mBuilder.getDownId());
        //没有下载记录
        if (downInfo == null) {
            //正常提示
            return;
        }
        switch (downInfo.status) {
            //下载失败
            case DownloadManager.STATUS_FAILED:
                //暂停
            case DownloadManager.STATUS_PAUSED:
                //挂起
            case DownloadManager.STATUS_PENDING:
                //正常提示
                break;
            //下载中
            case DownloadManager.STATUS_RUNNING:
                onDownloading(false);
                break;
            //下载成功
            case DownloadManager.STATUS_SUCCESSFUL:
                install(downInfo.localUrl);
                break;
            default:

                break;
        }
    }

    /**
     * 展示新版本提示信息
     */
    private void showVersionDialog() {
        String content;
        if(mBuilder.getDownloadType() == UpdateUtils.TYPE_FORCE_UPDATE){
            content = getString(R.string.versionchecklib_lowversion_update);
        }else {
            content = getString(R.string.versionchecklib_newversion_update);
        }
        AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.versionchecklib_check_new_version))
                .setMessage(content)
                .setPositiveButton(getString(R.string.versionchecklib_update), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        update = true;

                        dialog.dismiss();

                        requestPermission();
                    }
                })
                .setNegativeButton(getString(R.string.versionchecklib_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(mBuilder.getDownloadType() == UpdateUtils.TYPE_FORCE_UPDATE){
                            setResult(RESULT_OK);
                        }
                        finish();
                    }
                })
                .create();
        if(mBuilder.getDownloadType() == UpdateUtils.TYPE_FORCE_UPDATE){
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(!update){
                    finish();
                }
            }
        });


        dialog.show();
    }

    /**
     * 展示下载进度的progressBar并开始下载
     */
    private void showDownloadpBar(){
        pBar = new CommonProgressDialog(UpdateActivity.this);
        pBar.setCanceledOnTouchOutside(false);
        pBar.setTitle(getString(R.string.versionchecklib_downloading));
        pBar.setCustomTitle(LayoutInflater.from(UpdateActivity.this).inflate(R.layout.title_dialog, null));
        pBar.setMessage(getString(R.string.versionchecklib_downloading));
        pBar.setIndeterminate(true);
        pBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pBar.setCancelable(true);
        //放到后台去下载
        long id = mManager.startDownload(UpdateActivity.this, mBuilder.getDownLoadUrl(), mBuilder.getTitle(), mBuilder.getContent());
        mBuilder.setDownLoadId(id);

        onDownloading(true);
        if(!pBar.isShowing()){
            pBar.show();
        }

        pBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(mBuilder.getDownloadType() == UpdateUtils.TYPE_FORCE_UPDATE){
                    setResult(RESULT_OK);
                }
                
                finish();
            }
        });
    }

    /**
     * 展示下载错误提示
     */
    private void showErrorDialog(){
        new android.app.AlertDialog.Builder(this)
                .setMessage(getString(R.string.versionchecklib_download_fail_retry))
                .setPositiveButton(getString(R.string.versionchecklib_retry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showDownloadpBar();
                    }
                })
                .setNeutralButton(getString(R.string.versionchecklib_download_net_download), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(mBuilder.getDownLoadUrl());
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);

                        dialog.dismiss();
                        if(mBuilder.getDownloadType() == UpdateUtils.TYPE_FORCE_UPDATE){
                            if(mListener != null){
                                mListener.onForceCancel();
                            }
                            setResult(RESULT_OK);
                        }
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.versionchecklib_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(mBuilder.getDownloadType() == UpdateUtils.TYPE_FORCE_UPDATE){
                            if(mListener != null){
                                mListener.onForceCancel();
                            }
                            setResult(RESULT_OK);
                        }
                        finish();
                    }
                })
                .show();
    }

    /**
     * 申请sd卡权限
     */
    private void requestPermission(){
        AndPermission.with(this)
                .permission(Permission.Group.STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        //获取权限成功，展示进度并下载
                        showDownloadpBar();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        if(AndPermission.hasAlwaysDeniedPermission(UpdateActivity.this,permissions)){
                            final SettingService settingService = AndPermission.permissionSetting(UpdateActivity.this);
                            new android.app.AlertDialog.Builder(UpdateActivity.this)
                                    .setMessage(getString(R.string.versionchecklib_write_permission_deny))
                                    .setPositiveButton(getString(R.string.versionchecklib_permission), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            settingService.execute();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.versionchecklib_cancel), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            settingService.cancel();
                                            if(mBuilder.getDownloadType() == UpdateUtils.TYPE_FORCE_UPDATE){
                                                if(mListener != null){
                                                    mListener.onForceCancel();
                                                }
                                                setResult(RESULT_OK);
                                            }
                                            finish();

                                        }
                                    })
                                    .create();

                        }
                    }
                })
                .start();
    }


    /**
     * 下载中
     */
    private void onDownloading(boolean retry) {
        if (retry || mThread == null) {
            mThread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    live = true;
                    while (live) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        final DownLoadBean downBean = mManager.query(UpdateActivity.this, mBuilder.getDownId());
                        if (downBean != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pBar.setIndeterminate(false);
                                    pBar.setMax(100);
                                    pBar.setProgress(downBean.getProgress());
                                    if (downBean.status == DownloadManager.STATUS_SUCCESSFUL) {
                                        pBar.dismiss();
                                        if(mBuilder.getDownloadType() == UpdateUtils.TYPE_FORCE_UPDATE){
                                            setResult(RESULT_OK);
                                        }
                                        finish();
                                        live = false;
                                    }else if(downBean.status == DownloadManager.STATUS_FAILED){
                                        live = false;
                                        pBar.dismiss();
                                        mManager.removeTask(UpdateActivity.this, mBuilder.getDownId());
                                        showErrorDialog();
                                    }
                                }
                            });
                        }
                    }
                }
            };
            mThread.start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent != null){
            DownLoadBuilder builder = (DownLoadBuilder) intent.getSerializableExtra(PARAM_DATA);
            if(builder != null){
                mBuilder = builder;
                init();
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        live = false;
    }

    /**
     * 安装
     * @param localUrl
     */
    private void install(String localUrl){
        AppUtils.installApp(getApplicationContext(), localUrl.replaceAll("file://", ""), "com.vcashorg.vcashwallet.fileprovider");
    }

}
