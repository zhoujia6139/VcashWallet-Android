package com.vcashorg.vcashwallet.base;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;

/**
 * BaseFragment
 * Created By YJQ
 */
public abstract class BaseFragment extends Fragment {

    protected Activity mActivity;
    private View rootView;

    BroadcastReceiver receiver;

    public void register(String action) {
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null) {
                        onReceiver(intent);
                    }

                }
            };
        }
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, new IntentFilter(action));
    }

    public void unregister() {
        if (receiver != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
        }
    }

    protected void onReceiver(Intent intent) {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(provideContentViewId(),container,false);
            ButterKnife.bind(this, rootView);

            initView(rootView);
            initData();
            initListener();
        } else {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
        }
        return rootView;
    }

    public View getStateViewRoot() {
        return rootView;
    }


    /**
     * 初始化一些view
     * @param rootView
     */
    public void initView(View rootView) {
    }

    /**
     * 初始化数据
     */
    public void initData() {

    }

    /**
     * 设置listener的操作
     */
    public void initListener() {

    }

    //得到当前界面的布局文件id(由子类实现)
    protected abstract int provideContentViewId();

    //加载数据
    protected abstract void loadData();


    public void nv(Class targetActivity) {
        startActivity(new Intent(mActivity, targetActivity));
    }

    public void nv(Intent intent) {
        startActivity(intent);
    }

    public void nv2(Class targetActivity, int requestCode) {
        startActivityForResult(new Intent(mActivity, targetActivity),requestCode);
    }
    public void nv2(Intent intent, int requestCode) {
        startActivityForResult(intent,requestCode);
    }

    ProgressDialog progressDialog;

    public void showProgressDialog(String text) {
        progressDialog = new ProgressDialog(mActivity);
        progressDialog.setTitle("");
        progressDialog.setMessage(text);
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if(progressDialog != null){
            progressDialog.cancel();
        }
    }
}
