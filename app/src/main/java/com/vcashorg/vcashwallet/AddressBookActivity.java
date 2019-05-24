package com.vcashorg.vcashwallet;

import com.vcashorg.vcashwallet.base.ToolBarActivity;

public class AddressBookActivity extends ToolBarActivity {

    @Override
    protected void initToolBar() {
        setToolBarTitle("Addresses book");
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_address_book;
    }
}
