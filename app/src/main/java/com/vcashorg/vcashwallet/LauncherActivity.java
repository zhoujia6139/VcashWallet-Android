package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.payload.PayloadUtil;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(PayloadUtil.getInstance(this).ifMnemonicFileExist()){
            startActivity(new Intent(this,VcashValidateActivity.class));
        }else {
            startActivity(new Intent(this,VcashStartActivity.class));
        }

        finish();
    }

}
