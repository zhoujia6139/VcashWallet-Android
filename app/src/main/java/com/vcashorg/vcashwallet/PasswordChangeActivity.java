package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.utils.ValidateUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class PasswordChangeActivity extends ToolBarActivity {

    @BindView(R.id.til_psw)
    TextInputLayout textInputLayout;
    @BindView(R.id.et_psw)
    EditText editText;
    @BindView(R.id.btn_next)
    Button btnNext;


    @Override
    protected void initToolBar() {
        setToolBarTitle("Change Wallet Password");
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_change_psw;
    }

    @Override
    public void initView() {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textInputLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")){
                    btnNext.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_grey_round_rect));
                }else {
                    btnNext.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_home_create));
                }
            }
        });
    }

    @OnClick(R.id.btn_next)
    public void onNextClick(){
        if(!editText.getText().toString().equals("")){
            ArrayList<String> words = (ArrayList<String>) ValidateUtil.validate2(editText.getText().toString());
            if(words != null){
                textInputLayout.setErrorEnabled(false);
                Intent intent = new Intent(PasswordChangeActivity.this,PasswordActivity.class);
                intent.putExtra(PasswordActivity.PARAM_MODE,PasswordActivity.MODE_CHANGE_PSW);
                intent.putStringArrayListExtra(PasswordActivity.PARAM_MNEMONIC_LIST,words);
                startActivity(intent);
            }else {
                textInputLayout.setError("The old password you have entered is incorrect");
                textInputLayout.setErrorEnabled(true);
            }
        }
    }
}
