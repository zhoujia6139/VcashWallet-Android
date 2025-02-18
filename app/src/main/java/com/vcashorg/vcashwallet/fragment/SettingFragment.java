package com.vcashorg.vcashwallet.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nestia.biometriclib.BiometricPromptManager;
import com.vcashorg.vcashwallet.LockScreenActivity;
import com.vcashorg.vcashwallet.PasswordVerifyActivity;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.WalletMainActivity;
import com.vcashorg.vcashwallet.base.BaseFragment;
import com.vcashorg.vcashwallet.utils.TimeOutUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;

import butterknife.BindView;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class SettingFragment extends BaseFragment {

    @BindView(R.id.tv_timeout)
    TextView tvTimeOut;

    @BindView(R.id.tv_version_name)
    TextView tvVersionName;

    @BindView(R.id.layout_touch_id)
    LinearLayout mLayoutTouch;

    @BindView(R.id.switcher)
    SwitchCompat mSwitcher;

    private BiometricPromptManager mManager;

    @Override
    protected int provideContentViewId() {
        return R.layout.fragment_setting;
    }

    @Override
    public void initView(View rootView) {
        tvTimeOut.setText(TimeOutUtil.getInstance().getTimeOutString());
        tvVersionName.setText(UIUtils.getString(R.string.app_version) + " " + UIUtils.getVersionName(mActivity));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            mManager = BiometricPromptManager.from(mActivity);

            if(mManager.isBiometricPromptEnable()){
                mLayoutTouch.setVisibility(View.VISIBLE);
                mSwitcher.setChecked(mManager.isBiometricSettingEnable());
            }else {
                mLayoutTouch.setVisibility(View.GONE);
            }
        }catch (Exception e){
            mLayoutTouch.setVisibility(View.GONE);
        }
    }

    @Override
    protected void loadData() {

    }

    @OnClick(R.id.trans_btn)
    public void onTransBtnClick(){
        if(!mSwitcher.isChecked()){
            mManager.authenticate(new BiometricPromptManager.OnBiometricIdentifyCallback() {
                @Override
                public void onUsePassword() {
                }

                @Override
                public void onSucceeded() {
                    mSwitcher.setChecked(true);
                    mManager.setBiometricSettingEnable(true);
                }

                @Override
                public void onFailed() {

                }

                @Override
                public void onError(int code, String reason) {
                }

                @Override
                public void onCancel() {
                }
            });
        }else {
            mSwitcher.setChecked(false);
            mManager.setBiometricSettingEnable(false);
        }
    }

    @OnClick(R.id.rl_lock_screen)
    public void onLockScreenClick(){
        Intent intent = new Intent(getActivity(), LockScreenActivity.class);
        startActivityForResult(intent,1000);
    }

    @OnClick(R.id.tv_change_psw)
    public void onChangePswClick(){
        Intent intent = new Intent(mActivity,PasswordVerifyActivity.class);
        intent.putExtra(PasswordVerifyActivity.PARAM_TYPE,PasswordVerifyActivity.TYPE_CHANGE_PSW);
        nv(intent);
    }

    @OnClick(R.id.tv_recover_phrase)
    public void onRecoverPhraseClick(){
        Intent intent = new Intent(mActivity,PasswordVerifyActivity.class);
        intent.putExtra(PasswordVerifyActivity.PARAM_TYPE,PasswordVerifyActivity.TYPE_RESTORE_PHRASE);
        nv(intent);
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
