package com.vcashorg.vcashwallet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.FrameLayout;

import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.fragment.SettingFragment;
import com.vcashorg.vcashwallet.fragment.WalletMainFragment;
import com.vcashorg.vcashwallet.update.WalletUpdateManager;
import com.vcashorg.vcashwallet.utils.UIUtils;

import butterknife.BindView;

public class WalletMainActivity extends BaseActivity {

    @BindView(R.id.fragment_container)
    FrameLayout mContainer;

    WalletDrawer walletDrawer;

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_drawer;
    }

    @Override
    public void initParams() {
        WalletMainFragment fragment = new WalletMainFragment();
        replaceFragment(fragment);
    }

    @Override
    public void initView() {
        walletDrawer = new WalletDrawer(this);

        walletDrawer.addOnDrawerItemSelectListener(new WalletDrawer.OnDrawerItemSelectListener() {
            @Override
            public void onDrawerItemSelected(String name) {
                if(name.equals(UIUtils.getString(R.string.setting))){
                    SettingFragment settingFragment = new SettingFragment();
                    replaceFragment(settingFragment);
                }else {
                    WalletMainFragment fragment = new WalletMainFragment();
                    replaceFragment(fragment);
                }
            }
        });
    }

    @Override
    public void initData() {
        WalletUpdateManager.getInstance().fetchUpdateConfig(this);
    }

    private void replaceFragment(Fragment fragment){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container,fragment);
        transaction.commit();
    }

    public void openDrawer(){
        walletDrawer.openDrawer();
    }
}
