package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.bean.MnemonicData;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.GridLineItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class MnemonicRestoreActivity extends ToolBarActivity {

    private List<String> allPhraseWords;
    private List<MnemonicData> restoreMnemonicData = new ArrayList<>();

    @BindView(R.id.rv_mneonic)
    RecyclerView mRvRestore;

    MnemonicRestoreAdapter adapter;

    @Override
    protected void initToolBar() {
        setToolBarTitle("Restore from seed phrase");
    }

    @Override
    public void initView() {
        allPhraseWords = WalletApi.getAllPhraseWords();

        adapter = new MnemonicRestoreAdapter(R.layout.item_mnemonic_restore,restoreMnemonicData);

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
            EditText etWord = helper.getView(R.id.et_word);
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
                    if(!s.toString().equals("")){
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

                }
            });
        }
    }

    @OnClick(R.id.btn_next)
    public void onNextClick(){
        if(validate()){
            Intent intent = new Intent(this,PasswordCreateActivity.class);
            intent.putStringArrayListExtra(PasswordCreateActivity.PARAM_MNEMONIC_LIST,buildMnemonicList());
            nv(intent);
        }
    }

    private boolean validate(){
        for (MnemonicData data : restoreMnemonicData){
            if(data.state == MnemonicData.STATE_UNCHECK){
                UIUtils.showToast("Have Empty Word");
                return false;
            }else if(data.state == MnemonicData.STATE_CHECK_FALSE){
                UIUtils.showToast("Have Incorrect Word");
                return false;
            }
        }
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
