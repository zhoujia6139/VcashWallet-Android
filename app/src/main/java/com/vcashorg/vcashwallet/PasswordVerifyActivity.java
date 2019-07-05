package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.utils.ValidateUtil;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

public class PasswordVerifyActivity extends ToolBarActivity {

    public static final int TYPE_CHANGE_PSW = 0;
    public static final int TYPE_RESTORE_PHRASE = 1;

    public static final String PARAM_TYPE = "type";


    @BindView(R.id.til_psw)
    TextInputLayout textInputLayout;
    @BindView(R.id.et_psw)
    EditText editText;
    @BindView(R.id.btn_next)
    Button btnNext;

    private int type;


    @Override
    protected void initToolBar() {
       // setToolBarTitle(UIUtils.getString(R.string.change_password));
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_psw_verify;
    }

    @Override
    public void initParams() {
        type = getIntent().getIntExtra(PARAM_TYPE,TYPE_RESTORE_PHRASE);
        if(type == TYPE_CHANGE_PSW){
            setToolBarTitle(UIUtils.getString(R.string.change_password));
        }else {
            setToolBarTitle(UIUtils.getString(R.string.verify_psw));
        }
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
                    btnNext.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_orange));
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
                if(type == TYPE_CHANGE_PSW){
                    Intent intent = new Intent(PasswordVerifyActivity.this,PasswordActivity.class);
                    intent.putExtra(PasswordActivity.PARAM_MODE,PasswordActivity.MODE_CHANGE_PSW);
                    intent.putStringArrayListExtra(PasswordActivity.PARAM_MNEMONIC_LIST,words);
                    startActivity(intent);
                }else {
                    Intent intent = new Intent(PasswordVerifyActivity.this,RecoverPhraseActivity.class);
                    intent.putStringArrayListExtra(RecoverPhraseActivity.PARAM_PHRASE,words);
                    startActivity(intent);
                    finish();
                }
            }else {
                textInputLayout.setError(UIUtils.getString(R.string.old_psw_incorrect));
                textInputLayout.setErrorEnabled(true);
            }
        }
    }
}
