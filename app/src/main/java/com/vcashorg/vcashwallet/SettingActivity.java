package com.vcashorg.vcashwallet;

import com.vcashorg.vcashwallet.base.ToolBarActivity;

public class SettingActivity extends ToolBarActivity {

    @Override
    protected void initToolBar() {
        setToolBarTitle("Settings");
        setToolBarBgColor(R.color.white);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_setting;
    }
}
