package com.vcashorg.vcashwallet;

import android.app.ProgressDialog;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;

import com.google.gson.Gson;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.payload.PayloadUtil;
import com.vcashorg.vcashwallet.utils.AESUtil;
import com.vcashorg.vcashwallet.utils.CharSequenceX;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class PasswordCreateActivity extends ToolBarActivity {

    public static final String PARAM_MNEMONIC_LIST = "mnemonic_list";

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

    ArrayList<String> words;

    @Override
    protected void initToolBar() {
        setToolBarTitle("Password");
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_password_create;
    }

    @Override
    public void initParams() {
        words = getIntent().getStringArrayListExtra(PARAM_MNEMONIC_LIST);
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
                if (!s.toString().equals("")) {
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
                if (!s.toString().equals("")) {
                    til_psw_confirm.setErrorEnabled(false);
                }
                btnState();
            }
        });
    }

    private void btnState() {
        if (!et_psw.getText().toString().trim().equals("") && !et_psw_confirm.getText().toString().trim().equals("")) {
            btnStart.setEnabled(true);
            btnStart.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_home_create));
        } else {
            btnStart.setEnabled(false);
            btnStart.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_grey_round_rect));
        }
    }

    /**
     * 显示错误提示，并获取焦点
     *
     * @param textInputLayout
     * @param error
     */
    private void showError(TextInputLayout textInputLayout, String error) {
        textInputLayout.setError(error);
        textInputLayout.getEditText().setFocusable(true);
        textInputLayout.getEditText().setFocusableInTouchMode(true);
        textInputLayout.getEditText().requestFocus();
    }

    private boolean validatePassword() {
        String psw1 = til_psw.getEditText().getText().toString();
        String psw2 = til_psw_confirm.getEditText().getText().toString();

        if (TextUtils.isEmpty(psw1)) {
            showError(til_psw, "Password cant be empty");
            return false;
        }

        if (TextUtils.isEmpty(psw2)) {
            showError(til_psw_confirm, "Confirm password cant be empty");
            return false;
        }

        if (!psw1.equals(psw2)) {
            UIUtils.showToast("Password not same");
            return false;
        }

        return true;
    }

    @OnClick(R.id.btn_start)
    public void onBtnStartClick() {
        if (validatePassword()) {
           // nv(WalletMainActivity.class);
            create(et_psw.getText().toString());
        }
    }

    private void create(final String psw) {

        final ProgressDialog progress = new ProgressDialog(PasswordCreateActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage("Create wallet...");
        progress.show();

        Observable.create(new ObservableOnSubscribe() {

            @Override
            public void subscribe(ObservableEmitter emitter) throws Exception {
//                String guid = AccessFactory.getInstance(PasswordCreateActivity.this).createGUID();
//                String hash = AccessFactory.getInstance(PasswordCreateActivity.this).getHash(guid, new CharSequenceX(psw), AESUtil.DefaultPBKDF2Iterations);
//                SPUtil.getInstance(PasswordCreateActivity.this).setValue(SPUtil.ACCESS_HASH, hash);

                boolean result = WalletApi.createWallet(words,psw);
                if(result){
//                    AccessFactory.getInstance(PasswordCreateActivity.this).setPIN(psw);
////                    PayloadUtil.getInstance(PasswordCreateActivity.this).saveWalletToJSON(words,
////                            new CharSequenceX(AccessFactory.getInstance(PasswordCreateActivity.this).getGUID() + psw));
                    String json = new Gson().toJson(words);
                    Log.i("yjq","JSON: " + json);
                    String encrypt = AESUtil.encrypt(json, new CharSequenceX(psw), AESUtil.DefaultPBKDF2Iterations);
                    Log.i("yjq","Encrypt: " + encrypt);
                    PayloadUtil.getInstance(PasswordCreateActivity.this).saveMnemonicToSDCard(encrypt);

                    emitter.onComplete();
                }else {
                    emitter.onError(null);
                }

            }
        }).compose(RxHelper.io2main())
                .subscribe(new Observer() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Object o) {
                    }

                    @Override
                    public void onError(Throwable e) {
                        UIUtils.showToast("Create Wallet Error");
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                    }

                    @Override
                    public void onComplete() {
                        UIUtils.showToast("Create Wallet Success");
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        nv(WalletMainActivity.class);
                        finish();
                    }
                });
    }
}
