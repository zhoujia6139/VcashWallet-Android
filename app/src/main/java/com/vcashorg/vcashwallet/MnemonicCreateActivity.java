package com.vcashorg.vcashwallet;

import com.vcashorg.vcashwallet.base.ToolBarActivity;

public class MnemonicCreateActivity extends ToolBarActivity {

    @Override
    protected void initToolBar() {
        setToolBarTitle("Create new wallet");
    }

    @Override
    protected int provideContentViewId() {
        return 0;
    }
}
