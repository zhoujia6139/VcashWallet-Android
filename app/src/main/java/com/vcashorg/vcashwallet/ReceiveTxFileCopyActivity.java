package com.vcashorg.vcashwallet;

import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.utils.VCashUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import butterknife.BindView;
import butterknife.OnClick;

public class ReceiveTxFileCopyActivity extends ToolBarActivity {

    public static final String PARAM_CONTENT = "content";
    public static final String PARAM_TX_ID = "tx_id";
    public static final String PARAM_TX_AMOUNT = "tx_amount";
    public static final String PARAM_TX_FEE = "tx_fee";
    public static final String PARAM_FROM = "from";
    public static final String PARAM_TOKEN = "token";

    @BindView(R.id.tv_content)
    TextView mTvContent;

    @BindView(R.id.tv_tx_id)
    TextView mTvTxId;
    @BindView(R.id.tv_tx_amount)
    TextView mTvTxAmount;
    @BindView(R.id.tv_tx_fee)
    TextView mTvTxFee;

    protected boolean fromDialog;

    private String tokenType = "VCash";

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.receive_transaction_file));
        setTitleSize(15);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_receive_tx_file_copy;
    }

    @Override
    public void initParams() {
        String content = getIntent().getStringExtra(PARAM_CONTENT);
        mTvContent.setText(content);
        mTvContent.setMovementMethod(ScrollingMovementMethod.getInstance());

        String token = getIntent().getStringExtra(PARAM_TOKEN);
        if(!TextUtils.isEmpty(token)){
            tokenType = token;
        }
        mTvTxId.setText(getIntent().getStringExtra(PARAM_TX_ID));
        mTvTxAmount.setText(WalletApi.nanoToVcash(getIntent().getLongExtra(PARAM_TX_AMOUNT,0)) + " " + VCashUtil.VCashUnit(tokenType));
        mTvTxFee.setText(WalletApi.nanoToVcashWithUnit(getIntent().getLongExtra(PARAM_TX_FEE,0)));

        fromDialog = getIntent().getBooleanExtra(PARAM_FROM,false);
        if(fromDialog){
            TextView tvRight = getSubTitle();
            tvRight.setText(R.string.done);
            TextPaint paint = tvRight.getPaint();
            paint.setFakeBoldText(true);
            tvRight.setTextColor(UIUtils.getColor(R.color.orange));
            tvRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nv(WalletMainActivity.class);
                }
            });
        }
    }


    @OnClick(R.id.tv_copy)
    public void onCopyClick(){
        UIUtils.copyText(this,mTvContent.getText().toString());
    }

    @Override
    protected boolean isShowBacking() {
        return !fromDialog;
    }
}
