package com.vcashorg.vcashwallet;

import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;

import butterknife.BindView;
import butterknife.OnClick;

public class PasswordCreateActivity extends ToolBarActivity {

    @BindView(R.id.til_psw)
    TextInputLayout til_psw;
    @BindView(R.id.et_psw)
    TextInputEditText et_psw;
    @BindView(R.id.til_psw_confirm)
    TextInputLayout til_psw_confirm;
    @BindView(R.id.et_psw_confirm)
    TextInputEditText et_psw_confirm;

    @BindView(R.id.btn_start)
    Button btnStart;

    @Override
    protected void initToolBar() {
        setToolBarTitle("Password");
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_password_create;
    }

    @Override
    public void initView() {
        et_psw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().equals("")){
                    til_psw.setErrorEnabled(false);
                }
                btnState();
            }
        });

        et_psw_confirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().equals("")){
                    til_psw_confirm.setErrorEnabled(false);
                }
                btnState();
            }
        });
    }

    private void btnState(){
        if(!et_psw.getText().toString().trim().equals("") && !et_psw_confirm.getText().toString().trim().equals("")){
            btnStart.setEnabled(true);
            btnStart.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_home_create));
        }else {
            btnStart.setEnabled(false);
            btnStart.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_grey_round_rect));
        }
    }

    /**
     * 显示错误提示，并获取焦点
     * @param textInputLayout
     * @param error
     */
    private void showError(TextInputLayout textInputLayout,String error){
        textInputLayout.setError(error);
        textInputLayout.getEditText().setFocusable(true);
        textInputLayout.getEditText().setFocusableInTouchMode(true);
        textInputLayout.getEditText().requestFocus();
    }

    private boolean validatePassword() {
        String psw1 = til_psw.getEditText().getText().toString();
        String psw2 = til_psw_confirm.getEditText().getText().toString();

        if (TextUtils.isEmpty(psw1)) {
            showError(til_psw,"Password cant be empty");
            return false;
        }

        if (TextUtils.isEmpty(psw2)) {
            showError(til_psw_confirm,"Confirm password cant be empty");
            return false;
        }

        if (!psw1.equals(psw2)) {
            UIUtils.showToast("Password not same");
            return false;
        }

        return true;
    }

    @OnClick(R.id.btn_start)
    public void onBtnStartClick(){
        if(validatePassword()){
            nv(WalletMainActivity.class);
        }
    }
}
