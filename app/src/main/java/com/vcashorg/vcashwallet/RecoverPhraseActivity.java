package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.utils.ValidateUtil;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.GridLineItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class RecoverPhraseActivity extends ToolBarActivity {

    public static final String PARAM_PHRASE = "phrase";

    @BindView(R.id.rv_mneonic)
    RecyclerView mRv;
    @BindView(R.id.tv_copy_mneonic)
    TextView mTvCopy;

    private ArrayList<String> mnemonicListData;


    @Override
    protected int provideContentViewId() {
        return R.layout.activity_mneonic_create;
    }

    @Override
    public void initParams() {
        mnemonicListData = getIntent().getStringArrayListExtra(PARAM_PHRASE);

         getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public void initView() {
        mRv.setLayoutManager(new GridLayoutManager(this,3,GridLayoutManager.VERTICAL, false));
        mRv.addItemDecoration(new GridLineItemDecoration(UIUtils.dip2Px(1),UIUtils.dip2Px(1),UIUtils.getColor(R.color.grey_4)));
        mRv.setHasFixedSize(true);
        mRv.setNestedScrollingEnabled(false);

        MnemonicAdapter adapter = new MnemonicAdapter(R.layout.item_mnemonic,mnemonicListData);

        mRv.setAdapter(adapter);

        mTvCopy.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.backup_phrase));
    }

    class MnemonicAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

        public MnemonicAdapter(int layoutResId, @Nullable List<String> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, String item) {
            helper.setText(R.id.tv_num,(helper.getLayoutPosition()+1) + "");
            helper.setText(R.id.tv_word,item);
        }

    }

    @OnClick(R.id.tv_copy_mneonic)
    public void onCopyClick(){
        if(mnemonicListData != null){
            StringBuilder sb = new StringBuilder();
            for (String word : mnemonicListData){
                sb.append(word);
                sb.append(" ");
            }
            UIUtils.copyText(this,sb.toString().trim());
        }
    }

    @OnClick(R.id.btn_next)
    public void onNextClick(){
        Intent intent = new Intent(this,MnemonicConfirmActivity.class);
        intent.putStringArrayListExtra(MnemonicConfirmActivity.PARAM_MNEMONIC_LIST,mnemonicListData);
        intent.putExtra(MnemonicConfirmActivity.PARAM_TYPE,MnemonicConfirmActivity.TYPE_RECOVER_PHRASE);
        nv(intent);
        finish();
    }
}
