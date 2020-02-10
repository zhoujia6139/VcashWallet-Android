package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.DateUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.AbstractVcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.LinerLineItemDecoration;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;

public class ReceiveTxFileRecordActivity extends ToolBarActivity {

    @BindView(R.id.rv_record)
    RecyclerView mRvRecord;


    @Override
    protected void initToolBar() {
        setToolBarTitle("Transaction file signed record");
        setTitleSize(16);
    }

    @Override
    public void initView() {
        mRvRecord.setLayoutManager(new LinearLayoutManager(this));
        mRvRecord.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.VERTICAL,R.drawable.rv_divider));

        ArrayList<AbstractVcashTxLog> txArr = WalletApi.getFileReceiveTxArr();
        Collections.reverse(txArr);
        RecordAdapter adapter = new RecordAdapter(R.layout.item_vcash_tx_simple,txArr);
        adapter.setEmptyView(LayoutInflater.from(this).inflate(R.layout.layout_tx_empty,null));

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {

                AbstractVcashTxLog abstractVcashTxLog = (AbstractVcashTxLog) adapter.getData().get(position);
                if(abstractVcashTxLog instanceof VcashTxLog){
                    VcashTxLog vcashTxLog = (VcashTxLog) abstractVcashTxLog;
                    Intent intent = new Intent(ReceiveTxFileRecordActivity.this,ReceiveTxFileCopyActivity.class);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_CONTENT,vcashTxLog.signed_slate_msg);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_ID,vcashTxLog.tx_slate_id);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_AMOUNT,vcashTxLog.amount_credited - vcashTxLog.amount_debited);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_FEE,vcashTxLog.fee);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_FROM,false);
                    nv(intent);
                }else if(abstractVcashTxLog instanceof VcashTokenTxLog){
                    VcashTokenTxLog tokenTxLog = (VcashTokenTxLog) abstractVcashTxLog;
                    Intent intent = new Intent(ReceiveTxFileRecordActivity.this,ReceiveTxFileCopyActivity.class);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_CONTENT,tokenTxLog.signed_slate_msg);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_ID,tokenTxLog.tx_slate_id);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_AMOUNT,tokenTxLog.token_amount_credited - tokenTxLog.token_amount_debited);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_FEE,tokenTxLog.fee);
                    intent.putExtra(ReceiveTxFileCopyActivity.PARAM_FROM,false);
                    nv(intent);
                }
            }
        });

        mRvRecord.setAdapter(adapter);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_tx_file_record;
    }

    class RecordAdapter extends BaseQuickAdapter<AbstractVcashTxLog, BaseViewHolder>{

        public RecordAdapter(int layoutResId, @Nullable List<AbstractVcashTxLog> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, AbstractVcashTxLog item) {
            String txId = item.tx_slate_id;
            if (TextUtils.isEmpty(txId) || txId.equals("null")) {
                helper.setText(R.id.tv_tx_id, R.string.unReachable);
            } else {
                helper.setText(R.id.tv_tx_id, txId);
            }

            long amount = 0;
            if (item instanceof VcashTxLog) {
                VcashTxLog log = (VcashTxLog)item;
                amount = log.amount_credited - log.amount_debited;
            } else if (item instanceof VcashTokenTxLog) {
                VcashTokenTxLog log = (VcashTokenTxLog)item;
                amount = log.token_amount_credited - log.token_amount_debited;
            }

            VcashTxLog.TxLogEntryType txType = item.tx_type;
            switch (txType) {
                case ConfirmedCoinbaseOrTokenIssue:
                    helper.setText(R.id.tv_tx_id, R.string.coinbase);
                case TxReceived:
                case TxReceivedCancelled:
                    helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_down);
                    helper.setText(R.id.tv_tx_amount,  "+" + WalletApi.nanoToVcashString(amount));
                    break;
                case TxSent:
                case TxSentCancelled:
                    helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_up);
                    helper.setText(R.id.tv_tx_amount,  WalletApi.nanoToVcashString(amount));
                    break;
            }

            helper.setText(R.id.tv_tx_time, DateUtil.formatDateTimeStamp(item.create_time));

            VcashTxLog.TxLogConfirmType confirmState = item.confirm_state;

            TextView txState = helper.getView(R.id.tv_tx_state);
            helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.A2));
            helper.setText(R.id.tv_tx_time, DateUtil.formatDateTimeSimple(item.create_time));
            switch (confirmState) {
                case DefaultState:
                    if(txType == VcashTxLog.TxLogEntryType.TxSent){
                        helper.setText(R.id.tv_tx_state, "recipient processing now");
                    }else if(txType == VcashTxLog.TxLogEntryType.TxReceived){
                        helper.setText(R.id.tv_tx_state, "sender processing now");
                    }
                    helper.setTextColor(R.id.tv_tx_state,UIUtils.getColor(R.color.red));
                    txState.setCompoundDrawablesWithIntrinsicBounds(
                            UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                    break;
                case LoalConfirmed://waiting confirm
                    helper.setText(R.id.tv_tx_state, "waiting for confirmation");
                    txState.setCompoundDrawablesWithIntrinsicBounds(
                            UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                    helper.setTextColor(R.id.tv_tx_state,UIUtils.getColor(R.color.red));
                    break;
                case NetConfirmed:
                    helper.setText(R.id.tv_tx_state,R.string.confirmed);
                    txState.setCompoundDrawablesWithIntrinsicBounds(
                            UIUtils.getResource().getDrawable(R.drawable.ic_tx_confirmed), null, null, null);
                    helper.setTextColor(R.id.tv_tx_state,UIUtils.getColor(R.color.A2));
                    break;
            }

            if (txType == VcashTxLog.TxLogEntryType.TxSentCancelled || txType == VcashTxLog.TxLogEntryType.TxReceivedCancelled) {
                helper.setText(R.id.tv_tx_state, R.string.canceled);
                txState.setCompoundDrawablesWithIntrinsicBounds(
                        UIUtils.getResource().getDrawable(R.drawable.ic_tx_canceled), null, null, null);
                helper.setTextColor(R.id.tv_tx_state,UIUtils.getColor(R.color.A2));
            }
        }
    }

}
