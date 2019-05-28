package com.vcashorg.vcashwallet;


import android.util.Log;

import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import java.util.List;

public class MainActivity extends BaseActivity {

    @Override
    public void initView() {
        WalletApi.setWalletContext(getApplicationContext());
        WalletApi.createWallet(null, null);
        Log.d("----------------", WalletApi.getWalletUserId());
    }

    @Override
    public void initData() {
        super.initData();

    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_main;
    }

}
