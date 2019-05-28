package com.vcashorg.vcashwallet;

import android.app.ProgressDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.payload.PayloadUtil;
import com.vcashorg.vcashwallet.utils.AESUtil;
import com.vcashorg.vcashwallet.utils.CharSequenceX;
import com.vcashorg.vcashwallet.utils.DecryptionException;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WalletApi;

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

    @BindView(R.id.et_validate)
    EditText mEtValidate;

    @BindView(R.id.open_wallet)
    FrameLayout mOpenWallet;

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vcash_validate;
    }

    @Override
    public void initView() {
        mEtValidate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")){
                    mOpenWallet.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_grey_round_rect));
                }else {
                    mOpenWallet.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_home_create));
                }
            }
        });
    }

    @OnClick(R.id.open_wallet)
    public void onOpenWalletClick(){
        if(!mEtValidate.getText().toString().trim().equals("")){
            if(PayloadUtil.getInstance(this).ifMnemonicFileExist()){
                String words = PayloadUtil.getInstance(this).readMnemonicFromSDCard();
                String psw = mEtValidate.getText().toString();
                try {
                    String json = AESUtil.decrypt(words,new CharSequenceX(psw),AESUtil.DefaultPBKDF2Iterations);
                    Log.e("yjq","JSON: " + json);
                    List<String> mneonicList = new Gson().fromJson(json, new TypeToken<List<String>>() {}.getType());
                    Log.e("yjq","mneonicList: " + mneonicList.toString());
                    validate(mneonicList,psw);
                } catch (UnsupportedEncodingException e) {
                    Log.e("yjq","UnsupportedEncodingException");
                    e.printStackTrace();
                } catch (InvalidCipherTextException e) {
                    Log.e("yjq","InvalidCipherTextException");
                    UIUtils.showToast("Incorrect Password");
                    e.printStackTrace();
                } catch (DecryptionException e) {
                    Log.e("yjq","DecryptionException");
                    e.printStackTrace();
                }
            }
        }
    }

    private void validate(final List<String> words, final String psw) {

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage("Validate Wallet...");
        progress.show();

        Observable.create(new ObservableOnSubscribe() {

            @Override
            public void subscribe(ObservableEmitter emitter) {

                boolean result = WalletApi.createWallet(words,psw);
                if(result){
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
                        UIUtils.showToast("Validate Wallet Error");
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                    }

                    @Override
                    public void onComplete() {
                        UIUtils.showToast("Validate Wallet Success");
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        nv(WalletMainActivity.class);
                        finish();
                    }
                });
    }
}
