package com.vcashorg.vcashwallet.base;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.WalletMainActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import butterknife.ButterKnife;

/**
 * BaseActivity
 * Created By YJQ
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static long mPreTime;
    private static Activity mCurrentActivity;// 对所有activity进行管理
    public static final List<Activity> mActivities = new LinkedList<Activity>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        synchronized (mActivities){
            mActivities.add(this);
        }


        setContentView(provideContentViewId());
        ButterKnife.bind(this);

        initParams();
        initView();
        initData();
        initListener();
    }


    //得到当前界面的布局文件id(由子类实现)
    protected abstract int provideContentViewId();

    public void initParams(){

    }

    public void initView() {
    }

    public void initData() {
    }

    public void initListener() {
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCurrentActivity = this;
    }

    @Override
    protected void onPause() {
        super.onPause();

        mCurrentActivity = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        synchronized (mActivities) {
            mActivities.remove(this);
        }
    }


    /**
     * 统一退出控制
     */
    @Override
    public void onBackPressed() {

        //如果是主页面
        if(this instanceof WalletMainActivity){
            if (System.currentTimeMillis() - mPreTime > 2000) {// 两次点击间隔大于2秒
                UIUtils.showToast(UIUtils.getString(R.string.exit));
                mPreTime = System.currentTimeMillis();
                return;
            }
            exitApp();
        }

        super.onBackPressed();// finish()
    }

    /**
     * 退出应用的方法
     */
    public static void exitApp() {

        ListIterator<Activity> iterator = mActivities.listIterator();

        while (iterator.hasNext()) {
            Activity next = iterator.next();
            next.finish();
        }
    }

    public void nv(Class targetActivity) {
        startActivity(new Intent(this, targetActivity));
    }

    public void nv(Intent intent) {
        startActivity(intent);
    }

    public void nv2(Class targetActivity, int requestCode) {
        startActivityForResult(new Intent(this, targetActivity),requestCode);
    }

    public void nv2(Intent intent, int requestCode){
        startActivityForResult(intent,requestCode);
    }


    public void showDialog(DialogFragment dialogFrag, String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialogFrag, tag);
        ft.commitAllowingStateLoss();
    }

    ProgressDialog progressDialog;

    public void showProgressDialog() {
        this.showProgressDialog("");
    }


    public void showProgressDialog(String text) {
//        ProgressDialogFragment dialogFragment = new ProgressDialogFragment();
//        dialogFragment.setTitle(text);
//        this.showDialog((DialogFragment) dialogFragment, "progress_dialog");
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("");
        progressDialog.setMessage(text);
        progressDialog.show();
    }

    public void showProgressDialog(int id) {
        this.showProgressDialog(getString(id));
    }

    public void dismissProgressDialog() {
//        this.dismissDialog("progress_dialog");
        if(progressDialog != null){
            progressDialog.cancel();
        }
    }

    public void dismissDialog(String tag) {
        DialogFragment dialogFrag = (DialogFragment) this.getSupportFragmentManager().findFragmentByTag(tag);
        if (dialogFrag != null) {
            try {
                dialogFrag.dismiss();
            } catch (IllegalStateException var4) {
                var4.printStackTrace();
            }
        }

    }

    protected boolean tintStatusBar(){
        return true;
    }
}
