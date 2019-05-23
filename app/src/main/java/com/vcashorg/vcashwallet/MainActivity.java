package com.vcashorg.vcashwallet;


import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.bean.Demo;
import com.vcashorg.vcashwallet.net.CommonObserver;
import com.vcashorg.vcashwallet.net.RequestUtils;
import com.vcashorg.vcashwallet.utils.UIUtils;
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

        //http请求使用方式参考
        RequestUtils.getDemo(new CommonObserver<Demo>() {
            @Override
            public void onSuccess(Demo result) {
                List<Demo.DemoItem> demo = result.demo;
                UIUtils.showToast(demo.size() + "");
            }

            @Override
            public void onFailure(Throwable e, String errorMsg) {
                UIUtils.showToast(errorMsg);
            }
        });
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_main;
    }

}
