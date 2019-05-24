package com.vcashorg.vcashwallet;

import com.vcashorg.vcashwallet.base.ToolBarActivity;

public class TxDetailsActivity extends ToolBarActivity {

    @Override
    protected void initToolBar() {
        setToolBarTitle("Transaction Details");
        setToolBarBgColor(R.color.grey_4);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_tx_details;
    }
}
