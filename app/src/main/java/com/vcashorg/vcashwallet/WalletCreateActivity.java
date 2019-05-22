package com.vcashorg.vcashwallet;

import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.base.ToolBarActivity;

import butterknife.OnClick;

public class WalletCreateActivity extends ToolBarActivity {


    @Override
    protected int provideContentViewId() {
        return R.layout.activity_create_wallet;
    }

    @Override
    protected void initToolBar() {
        setToolBarTitle("Create new wallet");
    }

    @OnClick(R.id.btn_agree)
    public void onAgreeClick(){
        nv(MnemonicCreateActivity.class);
    }
}
