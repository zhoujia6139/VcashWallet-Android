package com.vcashorg.vcashwallet;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.vcashorg.vcashwallet.utils.SPUtil;
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

public class PasswordActivity extends ToolBarActivity {

    public static final String PARAM_MNEMONIC_LIST = "mnemonic_list";
    public static final String PARAM_MODE = "mode";

    public static final int MODE_CHANGE_PSW = 0;
    public static final int MODE_CREATE = 1;
    public static final int MODE_RESTORE = 2;

    private int mode = MODE_CREATE;

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
        mode = getIntent().getIntExtra(PARAM_MODE, MODE_CREATE);
        if(mode == MODE_CHANGE_PSW){
            setToolBarTitle("Change wallet password");
            btnStart.setText("Save new password");
        }
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

                btnState();
            }
        });

        et_psw_confirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                til_psw_confirm.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

                btnState();
            }
        });
    }

    private void btnState() {
        if (!et_psw.getText().toString().trim().equals("") && !et_psw_confirm.getText().toString().trim().equals("")) {
            btnStart.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_orange));
        } else {
            btnStart.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_grey_round_rect));
        }
    }

    private boolean validatePassword() {
        String psw1 = til_psw.getEditText().getText().toString();
        String psw2 = til_psw_confirm.getEditText().getText().toString();

        if (TextUtils.isEmpty(psw1) || TextUtils.isEmpty(psw2)) {
            return false;
        }

        if (!psw1.equals(psw2)) {
            til_psw_confirm.setErrorEnabled(true);
            til_psw_confirm.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    @OnClick(R.id.btn_start)
    public void onBtnStartClick() {
        if (validatePassword()) {
            create(et_psw.getText().toString());
        }
    }

    private void create(final String psw) {

        final ProgressDialog progress = new ProgressDialog(PasswordActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage("Create wallet...");
        progress.show();

        Observable.create(new ObservableOnSubscribe() {

            @Override
            public void subscribe(ObservableEmitter emitter) {
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
                    try {
                        String encrypt = AESUtil.encrypt(json, new CharSequenceX(psw), AESUtil.DefaultPBKDF2Iterations);
                        Log.i("yjq","Encrypt: " + encrypt);
                        boolean save = PayloadUtil.getInstance(PasswordActivity.this).saveMnemonicToSDCard(encrypt);
                        if(save){
                            SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.FIRST_CREATE_WALLET,true);
                            emitter.onComplete();
                        }else {
                            emitter.onError(null);
                        }
                    }catch (Exception e){
                        emitter.onError(null);
                    }
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
                        Intent intent = new Intent(PasswordActivity.this,WalletMainActivity.class);
                        intent.putExtra(PARAM_MODE,mode);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        nv(intent);
                        finish();
                    }
                });
    }

}
