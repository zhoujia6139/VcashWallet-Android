package com.vcashorg.vcashwallet;

import android.widget.ImageView;
import android.widget.TextView;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;

import butterknife.BindView;

public class TxDetailsActivity extends ToolBarActivity {

    @BindView(R.id.tv_tx_id)
    TextView mTvTxId;
    @BindView(R.id.tv_sender)
    TextView mTvSender;
    @BindView(R.id.tv_recipient)
    TextView mTvRecipient;
    @BindView(R.id.tv_tx_amount)
    TextView mTxAmount;
    @BindView(R.id.tv_tx_fee)
    TextView mTxFee;
    @BindView(R.id.tv_tx_time)
    TextView mTxTime;

    @BindView(R.id.iv_status)
    ImageView mIvStatus;
    @BindView(R.id.tv_status)
    TextView mTvStatus;

    VcashTxLog vcashTxLog;


    @Override
    protected void initToolBar() {
        setToolBarTitle("Transaction Details");
        setToolBarBgColor(R.color.grey_4);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_tx_details;
    }
}
