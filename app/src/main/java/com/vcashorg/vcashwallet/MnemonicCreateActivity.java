package com.vcashorg.vcashwallet;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.WindowManager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.GridLineItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class MnemonicCreateActivity extends ToolBarActivity {

    @BindView(R.id.rv_mneonic)
    RecyclerView mRv;

    private ArrayList<String> mnemonicListData;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.create_new_wallet));
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_mneonic_create;
    }

    @Override
    public void initParams() {
        //No screenshots
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public void initView() {
        mRv.setLayoutManager(new GridLayoutManager(this,3,GridLayoutManager.VERTICAL, false));
        mRv.addItemDecoration(new GridLineItemDecoration(UIUtils.dip2Px(1),UIUtils.dip2Px(1),UIUtils.getColor(R.color.grey_4)));
        mRv.setHasFixedSize(true);
        mRv.setNestedScrollingEnabled(false);

        mnemonicListData = (ArrayList<String>) WalletApi.generateMnemonicPassphrase();

        MnemonicAdapter adapter = new MnemonicAdapter(R.layout.item_mnemonic,mnemonicListData);

        mRv.setAdapter(adapter);
    }



    class MnemonicAdapter extends BaseQuickAdapter<String, BaseViewHolder>{

        public MnemonicAdapter(int layoutResId, @Nullable List<String> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, String item) {
            helper.setText(R.id.tv_num,(helper.getLayoutPosition()+1) + "");
            helper.setText(R.id.tv_word,item);
        }

    }

    @OnClick(R.id.btn_next)
    public void onNextClick(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.save_seed_phrase)
                .setMessage(R.string.save_seed_phrase_content)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MnemonicCreateActivity.this,MnemonicConfirmActivity.class);
                        intent.putStringArrayListExtra(MnemonicConfirmActivity.PARAM_MNEMONIC_LIST,mnemonicListData);
                        nv(intent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel,null)
                .show();
    }
}
