package com.vcashorg.vcashwallet;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.bean.MnemonicData;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.MnemonicHelper;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.GridLineItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class MnemonicRestoreActivity extends ToolBarActivity {

    private List<String> allPhraseWords;
    private List<MnemonicData> restoreMnemonicData = new ArrayList<>();

    @BindView(R.id.rv_mneonic)
    RecyclerView mRvRestore;

    @BindView(R.id.btn_next)
    Button btnNext;

    MnemonicRestoreAdapter adapter;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.restore_seed_phrase));
    }

    @Override
    public void initView() {
        allPhraseWords = WalletApi.getAllPhraseWords();

        adapter = new MnemonicRestoreAdapter(R.layout.item_mnemonic_restore,restoreMnemonicData);
        adapter.bindToRecyclerView(mRvRestore);

        mRvRestore.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false));
        mRvRestore.addItemDecoration(new GridLineItemDecoration(UIUtils.dip2Px(1), UIUtils.dip2Px(1), UIUtils.getColor(R.color.grey_4)));

        mRvRestore.setHasFixedSize(true);
        mRvRestore.setNestedScrollingEnabled(false);

        mRvRestore.setAdapter(adapter);
    }

    @Override
    public void initData() {
        for (int i=1 ;i<=24;i++){
            MnemonicData data = new MnemonicData();
            data.num = i;
            restoreMnemonicData.add(data);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_mnemonic_restore;
    }

    class MnemonicRestoreAdapter extends BaseQuickAdapter<MnemonicData, BaseViewHolder>{

        public MnemonicRestoreAdapter(int layoutResId, @Nullable List<MnemonicData> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(final BaseViewHolder helper, final MnemonicData item) {
            final EditText etWord = helper.getView(R.id.et_word);
            helper.setText(R.id.tv_num,String.valueOf(helper.getAdapterPosition() + 1));

            etWord.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    String str = s.toString();
                    if (str.indexOf("/r") >= 0 || str.indexOf("\n") >= 0){
                        EditText nextEdt = (EditText) adapter.getViewByPosition(helper.getAdapterPosition() + 1,R.id.et_word);
                        etWord.setText(str.replace("/r", "").replace("\n", ""));
                        if (nextEdt != null) {
                            nextEdt.requestFocus();
                            nextEdt.setSelection(nextEdt.getText().length());
                        }
                    }

                    if(!str.replace("/r", "").replace("\n", "").equals("")){
                        if(allPhraseWords.contains(s.toString())){
                            item.state = MnemonicData.STATE_CHECK_TRUE;
                            item.data = s.toString();
                            helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_green);
                            helper.setTextColor(R.id.et_word, UIUtils.getColor(R.color.green));
                        }else {
                            item.state = MnemonicData.STATE_CHECK_FALSE;
                            item.data = "";
                            helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_red);
                            helper.setTextColor(R.id.et_word, UIUtils.getColor(R.color.red));
                        }
                    }else {
                        item.state = MnemonicData.STATE_UNCHECK;
                        item.data = "";
                        helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_grey);
                    }

                    btnState();

                }
            });
        }
    }

    @OnClick(R.id.btn_next)
    public void onNextClick(){
        if(validate()){
            validateMnemonic(buildMnemonicList());
        }
    }

    private void validateMnemonic(final List<String> words) {

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(UIUtils.getString(R.string.creating_wallet));
        progress.show();

        Observable.create(new ObservableOnSubscribe() {

            @Override
            public void subscribe(ObservableEmitter emitter) {

                boolean result = WalletApi.createWallet(words, null);
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
                        UIUtils.showToastCenter(R.string.create_wallet_failed);
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        Intent intent = new Intent(MnemonicRestoreActivity.this, PasswordActivity.class);
                        intent.putStringArrayListExtra(PasswordActivity.PARAM_MNEMONIC_LIST,buildMnemonicList());
                        intent.putExtra(PasswordActivity.PARAM_MODE, PasswordActivity.MODE_RESTORE);
                        nv(intent);
                    }
                });
    }


    private void btnState(){
        for (MnemonicData data : restoreMnemonicData){
            if(data.state == MnemonicData.STATE_UNCHECK || data.state == MnemonicData.STATE_CHECK_FALSE){
                btnNext.setBackgroundResource(R.drawable.bg_orange_light_round_rect);
                return ;
            }
        }
        btnNext.setBackgroundResource(R.drawable.selector_orange);
    }

    private boolean validate(){
//        for (MnemonicData data : restoreMnemonicData){
//            if(data.state == MnemonicData.STATE_UNCHECK){
//                return false;
//            }else if(data.state == MnemonicData.STATE_CHECK_FALSE){
//                return false;
//            }
//        }
        return true;
    }

    private ArrayList<String> buildMnemonicList(){
        ArrayList<String> result = new ArrayList<>();
        for (MnemonicData data : restoreMnemonicData){
            result.add(data.data);
        }
        return result;
    }
}
