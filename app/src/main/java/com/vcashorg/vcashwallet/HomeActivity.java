package com.vcashorg.vcashwallet;

import com.vcashorg.vcashwallet.base.BaseActivity;

import butterknife.OnClick;

public class HomeActivity extends BaseActivity {
    @Override
    protected int provideContentViewId() {
        return R.layout.activity_home;
    }


    @OnClick(R.id.create_wallet)
    public void onCreateWalletClick(){
        nv(WalletCreateActivity.class);
    }


    @OnClick(R.id.restore_wallet)
    public void onRestoreWalletClick(){
        nv(WalletMainActivity.class);
        //nv(MainActivity.class);
    }
}
