package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.TimeOutUtil;

import butterknife.BindView;
import butterknife.OnClick;

public class SettingActivity extends ToolBarActivity {

    @BindView(R.id.tv_timeout)
    TextView tvTimeOut;

    @Override
    protected void initToolBar() {
        setToolBarTitle("Settings");
        setToolBarBgColor(R.color.white);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_setting;
    }

    @Override
    public void initView() {
        tvTimeOut.setText(TimeOutUtil.getInstance().getTimeOutString());
    }

    @OnClick(R.id.rl_lock_screen)
    public void onLockScreenClick(){
        Intent intent = new Intent(this,LockScreenActivity.class);
        startActivityForResult(intent,1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 1000 && resultCode == RESULT_OK){
            tvTimeOut.setText(TimeOutUtil.getInstance().getTimeOutString());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
