package com.vcashorg.vcashwallet.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.vcashorg.vcashwallet.LockScreenActivity;
import com.vcashorg.vcashwallet.PasswordChangeActivity;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.WalletMainActivity;
import com.vcashorg.vcashwallet.base.BaseFragment;
import com.vcashorg.vcashwallet.utils.TimeOutUtil;

import butterknife.BindView;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class SettingFragment extends BaseFragment {

    @BindView(R.id.tv_timeout)
    TextView tvTimeOut;


    @Override
    protected int provideContentViewId() {
        return R.layout.fragment_setting;
    }

    @Override
    public void initView(View rootView) {
        tvTimeOut.setText(TimeOutUtil.getInstance().getTimeOutString());
    }

    @Override
    protected void loadData() {

    }

    @OnClick(R.id.rl_lock_screen)
    public void onLockScreenClick(){
        Intent intent = new Intent(getActivity(), LockScreenActivity.class);
        startActivityForResult(intent,1000);
    }

    @OnClick(R.id.tv_change_psw)
    public void onChangePswClick(){
        nv(PasswordChangeActivity.class);
    }

    @OnClick(R.id.iv_open_menu)
    public void onMenuClick(){
        ((WalletMainActivity)mActivity).openDrawer();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000 && resultCode == RESULT_OK){
            tvTimeOut.setText(TimeOutUtil.getInstance().getTimeOutString());
        }
    }
}
