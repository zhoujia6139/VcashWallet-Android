package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.payload.PayloadUtil;
import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(SPUtil.getInstance(UIUtils.getContext()).getValue(SPUtil.FIRST_CREATE_WALLET,false)
                && PayloadUtil.getInstance(this).ifMnemonicFileExist()){
            Intent intent = new Intent(this,VcashValidateActivity.class);
            intent.putExtra(VcashValidateActivity.PARAM_MODE,VcashValidateActivity.MODE_LAUNCHER_VALIDATE);
            startActivity(intent);
        }else {
            startActivity(new Intent(this,VcashStartActivity.class));
        }

        finish();
    }

}
