package com.vcashorg.vcashwallet;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.modal.Demo;
import com.vcashorg.vcashwallet.net.CommonObserver;
import com.vcashorg.vcashwallet.net.RequestUtils;
import com.vcashorg.vcashwallet.net.Response;
import com.vcashorg.vcashwallet.net.RetrofitUtils;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import java.util.List;

public class MainActivity extends BaseActivity {

    @Override
    public void initView() {
        WalletApi.setWalletContext(getApplicationContext());
        WalletApi.createWallet(null, null);
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

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
