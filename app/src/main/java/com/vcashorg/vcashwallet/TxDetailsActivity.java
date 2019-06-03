package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.db.EncryptedDBHelper;
import com.vcashorg.vcashwallet.utils.DateUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import butterknife.BindView;
import butterknife.OnClick;

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogEntryType.TxSent;

public class TxDetailsActivity extends ToolBarActivity {

    public static final int TYPE_TX_SERVER = 0;
    public static final int TYPE_TX_LOG = 1;

    public static final String PARAM_TX_TYPE = "tx_type";
    public static final String PARAM_TX_DATA = "tx_data";

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

    @BindView(R.id.fl_btn_sign)
    FrameLayout mFlSign;
    @BindView(R.id.tv_sign)
    TextView mTvSign;
    @BindView(R.id.fl_btn_cancel)
    FrameLayout mFlCancel;
    @BindView(R.id.tv_cancel)
    TextView mTvCancel;

    VcashTxLog vcashTxLog;
    ServerTransaction transaction;


    @Override
    protected void initToolBar() {
        setToolBarTitle("Transaction Details");
        setToolBarBgColor(R.color.grey_4);
    }

    @Override
    public void initParams() {
        Intent intent = getIntent();
        int type = intent.getIntExtra(PARAM_TX_TYPE,TYPE_TX_LOG);
        if(type == TYPE_TX_SERVER){
            transaction = (ServerTransaction) intent.getSerializableExtra(PARAM_TX_DATA);
            configDataFromServerTransaction();
        }else {
            vcashTxLog = (VcashTxLog) intent.getSerializableExtra(PARAM_TX_DATA);
            configDataFromVcashTxLog();
        }
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_tx_details;
    }

    public void configDataFromServerTransaction(){
        if(transaction == null)return;
        mIvStatus.setImageResource(R.drawable.ic_tx_ongoing);
        mTvSign.setText(transaction.isSend ? "Verify and Sign": "Receive and Sign");
        mFlCancel.setVisibility(transaction.isSend ? View.GONE : View.VISIBLE);
        switch (transaction.status){
            case TxDefaultStatus:
                mTvStatus.setText(transaction.isSend ?
                        "Tx Status: waiting for the sender to sign":"Tx Status: waiting for the recipient to sign");
                mFlSign.setVisibility(View.VISIBLE);
                break;
            case TxFinalized:
                mTvStatus.setText("Tx Status: waiting for confirming");
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.GONE);
                break;
            case TxReceiverd:
                //The recipient has already signed, waiting for the sender to broadcast
                mTvStatus.setText(transaction.isSend ?
                        "Tx Status: waiting for the sender to sign":"Tx Status: waiting for the recipient to sign");
                break;
            case TxCanceled:
                mIvStatus.setImageResource(R.drawable.ic_tx_canceled);
                mTvStatus.setText("Tx Status: transaction canceled");
                mFlSign.setVisibility(View.GONE);
                mTvCancel.setText("Delete the transaction");
        }
        mTvTxId.setText(transaction.tx_id);
        mTvSender.setText(transaction.sender_id);
        mTvRecipient.setText(transaction.receiver_id);
        mTxAmount.setText(WalletApi.nanoToVcashString(transaction.slateObj.amount));
        mTxFee.setText(WalletApi.nanoToVcashString(transaction.slateObj.fee));
    }


    public void configDataFromVcashTxLog(){
        if(vcashTxLog == null) return;
        switch (vcashTxLog.confirm_state){
            case DefaultState:
                if(vcashTxLog.tx_type == TxSent){
                    mIvStatus.setImageResource(R.drawable.ic_tx_ongoing);
                    mTvStatus.setText("Tx Status: waiting for the recipient to sign");
                    mTvSign.setText("Verify and Sign");
                    mFlCancel.setVisibility(View.VISIBLE);
                    if(vcashTxLog.server_status == ServerTxStatus.TxDefaultStatus){
                        mFlSign.setVisibility(View.GONE);
                    }else if(vcashTxLog.server_status == ServerTxStatus.TxReceiverd){
                        mFlSign.setVisibility(View.VISIBLE);
                    }
                }else if(vcashTxLog.tx_type == VcashTxLog.TxLogEntryType.TxReceived){
                    mIvStatus.setImageResource(R.drawable.ic_tx_ongoing);
                    mTvStatus.setText("Tx Status: waiting for the sender to sign");
                    mFlSign.setVisibility(View.GONE);
                    mFlCancel.setVisibility(View.GONE);
                }
                break;
            case LoalConfirmed:
                mTvStatus.setText("Tx Status: waiting for confirming");
                mIvStatus.setImageResource(R.drawable.ic_tx_ongoing);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.GONE);
                break;
            case NetConfirmed:
                mTvStatus.setText("Tx Status: transaction completed");
                mIvStatus.setImageResource(R.drawable.ic_tx_confirmed);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.GONE);
                break;
        }

        mTvTxId.setText(vcashTxLog.tx_slate_id == null ? "unreachable" : vcashTxLog.tx_slate_id);
        mTxFee.setText(WalletApi.nanoToVcashString(vcashTxLog.fee));
        mTxTime.setText(DateUtil.formatDateTimeStamp(vcashTxLog.create_time));
        configInfoFromTxType(vcashTxLog.tx_type);
    }


    public void configInfoFromTxType(VcashTxLog.TxLogEntryType txType){
        switch (txType){
            case ConfirmedCoinbase:
                mTvTxId.setText("coinbase");
                break;
            case TxSent:
                mTvSender.setText(WalletApi.getWalletUserId());
                mTvRecipient.setText(TextUtils.isEmpty(vcashTxLog.parter_id) ? "unreachable":vcashTxLog.parter_id);
                mTxAmount.setText(WalletApi.nanoToVcashString(vcashTxLog.amount_credited - vcashTxLog.amount_debited - vcashTxLog.fee));
                break;
            case TxReceived:
                mTvSender.setText(TextUtils.isEmpty(vcashTxLog.parter_id) ? "unreachable":vcashTxLog.parter_id);
                mTvRecipient.setText(WalletApi.getWalletUserId());
                mTxAmount.setText(WalletApi.nanoToVcashString(vcashTxLog.amount_credited - vcashTxLog.amount_debited));
                break;
            case TxReceivedCancelled:
                mIvStatus.setImageResource(R.drawable.ic_tx_canceled);
                mTvStatus.setText("Tx Status: transaction cancelled");
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.VISIBLE);
                mTvCancel.setText("Delete the transaction");
                mTvSender.setText(TextUtils.isEmpty(vcashTxLog.parter_id) ? "unreachable":vcashTxLog.parter_id);
                mTvRecipient.setText(WalletApi.getWalletUserId());
                mTxAmount.setText(WalletApi.nanoToVcashString(vcashTxLog.amount_credited - vcashTxLog.amount_debited));
                break;
            case TxSentCancelled:
                mIvStatus.setImageResource(R.drawable.ic_tx_canceled);
                mTvStatus.setText("Tx Status: transaction cancelled");
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.VISIBLE);
                mTvCancel.setText("Delete the transaction");
                mTvSender.setText(WalletApi.getWalletUserId());
                mTvRecipient.setText(TextUtils.isEmpty(vcashTxLog.parter_id) ? "unreachable":vcashTxLog.parter_id);
                mTxAmount.setText(WalletApi.nanoToVcashString(vcashTxLog.amount_credited - vcashTxLog.amount_debited - vcashTxLog.fee));
                break;
        }
    }


    @OnClick(R.id.fl_btn_sign)
    public void onSignClick(){
        boolean isSend = false;
        if(transaction != null){
            isSend = transaction.isSend;
        }
        if(vcashTxLog != null){
            isSend = (vcashTxLog.tx_type == TxSent);
        }
        if(transaction != null){
            if(isSend){
                WalletApi.finalizeTransaction(transaction, new WalletCallback() {
                    @Override
                    public void onCall(boolean yesOrNo, Object data) {
                        if(yesOrNo){
                            UIUtils.showToastCenter("Finalize Success");
                        }else {
                            UIUtils.showToastCenter("Finalize Failed");
                        }
                    }
                });
            }else {
                WalletApi.receiveTransaction(transaction, new WalletCallback() {
                    @Override
                    public void onCall(boolean yesOrNo, Object data) {
                        if(yesOrNo){
                            UIUtils.showToastCenter("Receive Success");
                        }else {
                            UIUtils.showToastCenter("Receive Failed");
                        }
                    }
                });
            }
        }

    }

    @OnClick(R.id.fl_btn_cancel)
    public void onCancelClick(){
        if(vcashTxLog != null){
            switch (vcashTxLog.tx_type){
                case TxSent:
                    if(vcashTxLog.confirm_state == VcashTxLog.TxLogConfirmType.DefaultState){
                        cancelTransaction(vcashTxLog);
                    }
                    break;
                case TxSentCancelled:
                case TxReceivedCancelled:
                    deleteTransaction();
                    break;
            }
        }
        if(transaction != null){
            if(transaction.status == ServerTxStatus.TxReceiverd){
                VcashTxLog vcashTxLog = WalletApi.getTxByTxid(transaction.tx_id);
                cancelTransaction(vcashTxLog);
            }
        }
    }


    public void deleteTransaction(){
        boolean result = WalletApi.deleteTxByTxid(vcashTxLog.tx_slate_id);
        if(result){
            UIUtils.showToastCenter("Delete Success");
        }else {
            UIUtils.showToastCenter("Delete Success");
        }
    }

    public void cancelTransaction(VcashTxLog vcashTxLog){
        if(WalletApi.cancelTransaction(vcashTxLog)){
            UIUtils.showToastCenter("Tx Cancel Success");
        }else {
            UIUtils.showToastCenter("Tx Cancel Failed");
        }
    }
}
