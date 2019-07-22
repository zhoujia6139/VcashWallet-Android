package com.vcashorg.vcashwallet;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.CancellationSignal;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.nestia.biometriclib.BiometricPromptManager;
import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.payload.PayloadUtil;
import com.vcashorg.vcashwallet.utils.AESUtil;
import com.vcashorg.vcashwallet.utils.CharSequenceX;
import com.vcashorg.vcashwallet.utils.DecryptionException;
import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.TimeOutUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.utils.ValidateUtil;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.UnsupportedEncodingException;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class VcashValidateActivity extends BaseActivity {

    public static final String PARAM_MODE = "mode";

    public static final int MODE_LAUNCHER_VALIDATE = 0;
    public static final int MODE_TIMEOUT_VALIDATE = 1;

    private int mode = MODE_TIMEOUT_VALIDATE;

    @BindView(R.id.til_psw)
    TextInputLayout mTilPsw;

    @BindView(R.id.et_validate)
    EditText mEtValidate;

    @BindView(R.id.open_wallet)
    FrameLayout mOpenWallet;

    @BindView(R.id.tv_recover)
    TextView mTvRecover;

    @BindView(R.id.tv_fingerprint)
    TextView mTvFinger;

    BiometricPromptManager manager;

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vcash_validate;
    }

    @Override
    public void initParams() {
        mode = getIntent().getIntExtra(PARAM_MODE, MODE_TIMEOUT_VALIDATE);
        if(mode == MODE_LAUNCHER_VALIDATE){
            mTvRecover.setVisibility(View.VISIBLE);
            mTvFinger.setVisibility(View.GONE);
        }else {
            try {
                manager = BiometricPromptManager.from(this);
                if(manager.isBiometricPromptEnable() && manager.isBiometricSettingEnable()){
                    mTvRecover.setVisibility(View.GONE);
                    mTvFinger.setVisibility(View.VISIBLE);
                }else {
                    mTvRecover.setVisibility(View.INVISIBLE);
                    mTvFinger.setVisibility(View.GONE);
                }
            }catch (Exception e){
                mTvRecover.setVisibility(View.INVISIBLE);
                mTvFinger.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public void initView() {
        mEtValidate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTilPsw.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    mOpenWallet.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_orange_light_round_rect));
                } else {
                    mOpenWallet.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_orange));
                }
            }
        });

        showFingerprintDialog();
    }

    @Override
    public void initData() {
        TimeOutUtil.getInstance().updateLastTime();
    }

    @OnClick(R.id.open_wallet)
    public void onOpenWalletClick() {
        if (!mEtValidate.getText().toString().trim().equals("")) {
            if (mode == MODE_LAUNCHER_VALIDATE) {
                validateLauncher();
            } else if (mode == MODE_TIMEOUT_VALIDATE) {
                validateTimeOut();
            }
        }
    }

    @OnClick(R.id.tv_fingerprint)
    public void onFingerprintClick(){
        showFingerprintDialog();
    }

    @Override
    public void onBackPressed() {
        if (mode == MODE_LAUNCHER_VALIDATE) {
            super.onBackPressed();
        }
    }

    private void validateTimeOut() {
        if (ValidateUtil.validate(mEtValidate.getText().toString())) {
            TimeOutUtil.getInstance().updateLastTime();
            finish();
        } else {
            errorNotify();
        }
    }

    private void validateLauncher() {
        List<String> mneonicList = ValidateUtil.validate2(mEtValidate.getText().toString());
        if (mneonicList != null) {
            validate(mneonicList);
        } else {
            errorNotify();
        }
    }

    private void errorNotify() {
        mTilPsw.setErrorEnabled(true);
        mTilPsw.setError(UIUtils.getString(R.string.psw_incorrect));
    }

    private void showFingerprintDialog(){
        try {
            if(mode == MODE_TIMEOUT_VALIDATE && manager.isBiometricPromptEnable() && manager.isBiometricSettingEnable()){

                manager.authenticate(new BiometricPromptManager.OnBiometricIdentifyCallback() {
                    @Override
                    public void onUsePassword() {

                    }

                    @Override
                    public void onSucceeded() {
                        TimeOutUtil.getInstance().updateLastTime();
                        finish();
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
            }
        }catch (Exception e){

        }

    }

    private void validate(final List<String> words) {

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(UIUtils.getString(R.string.loading_wallet));
        progress.show();

        Observable.create(new ObservableOnSubscribe() {

            @Override
            public void subscribe(ObservableEmitter emitter) {

                boolean result = WalletApi.createWallet(words,null);
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
                        UIUtils.showToastCenter("Create Wallet Error");
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        nv(WalletMainActivity.class);
                        finish();
                    }
                });
    }

    @OnClick(R.id.tv_recover)
    public void onRecoverClick() {
        AndPermission.with(this)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        new AlertDialog.Builder(VcashValidateActivity.this)
                                .setTitle(R.string.warning)
                                .setMessage(R.string.recover_content)
                                .setPositiveButton(R.string.generate, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(VcashValidateActivity.this, MnemonicRestoreActivity.class);
                                        nv(intent);
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        UIUtils.showToastCenter(R.string.storage_permission);
                    }
                })
                .start();
    }
}
