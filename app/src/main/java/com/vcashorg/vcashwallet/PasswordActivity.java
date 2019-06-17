package com.vcashorg.vcashwallet;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import com.vcashorg.vcashwallet.utils.DecryptionException;
import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.MnemonicHelper;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import java.io.UnsupportedEncodingException;
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
        setToolBarTitle(UIUtils.getString(R.string.password));
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_password_create;
    }

    @Override
    public void initParams() {
        words = getIntent().getStringArrayListExtra(PARAM_MNEMONIC_LIST);
        mode = getIntent().getIntExtra(PARAM_MODE, MODE_CREATE);
        if (mode == MODE_CHANGE_PSW) {
            setToolBarTitle(UIUtils.getString(R.string.change_password));
            btnStart.setText(R.string.save_new_password);
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
            btnStart.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_orange_light_round_rect));
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
            til_psw_confirm.setError(UIUtils.getString(R.string.password_dont_match));
            return false;
        }

        return true;
    }

    @OnClick(R.id.btn_start)
    public void onBtnStartClick() {
        if (validatePassword()) {
            if (mode == MODE_CREATE || mode == MODE_CHANGE_PSW) {
                create(et_psw.getText().toString());
            } else {
                recover(et_psw.getText().toString());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mode == MODE_CREATE) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.back_seed_phrase)
                    .setMessage(R.string.back_seed_phrase_message)
                    .setPositiveButton(R.string.generate, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            nv(MnemonicCreateActivity.class);
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private void create(final String psw) {

        final ProgressDialog progress = new ProgressDialog(PasswordActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(UIUtils.getString(R.string.creating_wallet));
        progress.show();

        Observable.create(new ObservableOnSubscribe() {

            @Override
            public void subscribe(ObservableEmitter emitter) {

                boolean result = WalletApi.createWallet(words, psw);
                if (result) {
                    String json = new Gson().toJson(words);
                    // Log.i("yjq","JSON: " + json);
                    try {
                        String encrypt = AESUtil.encrypt(json, new CharSequenceX(psw), AESUtil.DefaultPBKDF2Iterations);
                        //  Log.i("yjq","Encrypt: " + encrypt);
                        boolean save = PayloadUtil.getInstance(PasswordActivity.this).saveMnemonicToSDCard(encrypt);
                        if (save) {
                            SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.FIRST_CREATE_WALLET, true);
                            emitter.onComplete();
                        } else {
                            emitter.onError(null);
                        }
                    } catch (Exception e) {
                        emitter.onError(null);
                    }
                } else {
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
                        UIUtils.showToastCenter(R.string.create_wallet_failed);
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                    }

                    @Override
                    public void onComplete() {
                        UIUtils.showToastCenter(R.string.create_wallet_success);
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        Intent intent = new Intent(PasswordActivity.this, WalletMainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        nv(intent);
                        finish();
                    }
                });
    }

    private void recover(final String psw) {
        final ProgressDialog progress = new ProgressDialog(PasswordActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(UIUtils.getString(R.string.restoring_wallet));
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setProgress(100);
        progress.setIndeterminate(false);
        progress.setProgressNumberFormat("");
        progress.show();

        WalletApi.clearWallet();
        SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.FIRST_CREATE_WALLET,false);

        Observable.create(new ObservableOnSubscribe() {

            @Override
            public void subscribe(ObservableEmitter emitter) {

                boolean result = WalletApi.createWallet(words, psw);
                if (result) {
                    emitter.onComplete();
                } else {
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
                        UIUtils.showToastCenter(R.string.restore_fail);
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                    }

                    @Override
                    public void onComplete() {
                        WalletApi.checkWalletUtxo(new WalletCallback() {
                            @Override
                            public void onCall(boolean yesOrNo, Object data) {
                               // Log.i("yjq","回调返回时间 : " + System.currentTimeMillis());

                                if (yesOrNo) {
                                    if(data instanceof Double){
                                        double percent = (double) data;
                                        progress.setProgress((int)(percent * 100));
                                    }else {
                                        try {
                                            String json = new Gson().toJson(words);
                                            String encrypt = AESUtil.encrypt(json, new CharSequenceX(psw), AESUtil.DefaultPBKDF2Iterations);
                                            boolean save = PayloadUtil.getInstance(PasswordActivity.this).saveMnemonicToSDCard(encrypt);
                                            if (save) {
                                                progress.setProgress(100);
                                                if (progress.isShowing()) {
                                                    progress.dismiss();
                                                }
                                                SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.FIRST_CREATE_WALLET, true);
                                                UIUtils.showToastCenter(R.string.restore_success);
                                                Intent intent = new Intent(PasswordActivity.this, WalletMainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                nv(intent);
                                                finish();
                                            } else {
                                                if (progress.isShowing()) {
                                                    progress.dismiss();
                                                }
                                                UIUtils.showToastCenter(R.string.restore_fail);
                                            }
                                        } catch (DecryptionException e) {
                                            UIUtils.showToastCenter(R.string.restore_fail);
                                            e.printStackTrace();
                                        } catch (UnsupportedEncodingException e) {
                                            UIUtils.showToastCenter(R.string.restore_fail);
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    if (progress.isShowing()) {
                                        progress.dismiss();
                                    }
                                    if (data instanceof String) {
                                        UIUtils.showToastCenter((String) data);
                                    } else {
                                        UIUtils.showToastCenter(R.string.restore_fail);
                                    }
                                }
                            }
                        });
                    }
                });
    }

}
