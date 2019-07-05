package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vcashorg.vcashwallet.api.ServerTxManager;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.DateUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import butterknife.BindView;
import butterknife.OnClick;

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogEntryType.TxSent;

public class
TxDetailsActivity extends ToolBarActivity {

    public static final int TYPE_TX_SERVER = 0;
    public static final int TYPE_TX_LOG = 1;

    public static final String PARAM_TX_TYPE = "tx_type";
    public static final String PARAM_TX_DATA = "tx_data";
    public static final String PARAM_TX_SENDER = "tx_sender";

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
    @BindView(R.id.ll_details_bot)
    LinearLayout mLLBot;
    @BindView(R.id.ll_tx_file)
    LinearLayout mLLFile;
    @BindView(R.id.tv_content)
    TextView mTvContent;

    VcashTxLog vcashTxLog;
    ServerTransaction serverTx;

    protected boolean sender;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.transaction_details));
    }

    @Override
    protected boolean isShowBacking() {
        return !sender;
    }

    @Override
    public void initParams() {
        Intent intent = getIntent();
        int type = intent.getIntExtra(PARAM_TX_TYPE, TYPE_TX_LOG);
        if (type == TYPE_TX_SERVER) {
            serverTx = (ServerTransaction) intent.getSerializableExtra(PARAM_TX_DATA);
            configDataFromServerTransaction();
        } else {
            vcashTxLog = (VcashTxLog) intent.getSerializableExtra(PARAM_TX_DATA);
            configDataFromVcashTxLog();
        }
        sender = intent.getBooleanExtra(PARAM_TX_SENDER, false);
        if (sender) {
            TextView tvRight = getSubTitle();
            tvRight.setText(R.string.done);
            tvRight.setTextColor(UIUtils.getColor(R.color.orange));
            tvRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_tx_details;
    }

    public void configDataFromServerTransaction() {
        if (serverTx == null) return;
        mIvStatus.setImageResource(R.drawable.ic_tx_ongoing_big);
        mTvSign.setText(serverTx.isSend ? R.string.verify_signature : R.string.receive_signature);
        mFlCancel.setVisibility(serverTx.isSend ? View.VISIBLE : View.GONE);
        switch (serverTx.status) {
            case TxDefaultStatus:
                mTvStatus.setText(serverTx.isSend ? R.string.tx_status_wait_sender_sign : R.string.tx_status_wait_your_sign);
                mFlSign.setVisibility(View.VISIBLE);
                break;
            case TxFinalized:
                mTvStatus.setText(R.string.tx_status_wait_confirm);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.GONE);
                break;
            case TxReceiverd:
                //The recipient has already signed, waiting for the sender to broadcast
                mTvStatus.setText(serverTx.isSend ? R.string.tx_status_wait_your_sign : R.string.tx_status_wait_receiver_sign);
                break;
            case TxCanceled:
                mIvStatus.setImageResource(R.drawable.ic_tx_canceled_big);
                mTvStatus.setText(R.string.tx_status_cancel);
                mFlSign.setVisibility(View.GONE);
                mTvCancel.setText(R.string.delete_transaction);
                mTvCancel.setCompoundDrawablesWithIntrinsicBounds(UIUtils.getResource().getDrawable(R.drawable.ic_delete), null, null, null);
        }
        mTvTxId.setText(serverTx.tx_id);
        mTvSender.setText(serverTx.sender_id);
        mTvRecipient.setText(serverTx.receiver_id);
        mTxAmount.setText(WalletApi.nanoToVcashString(serverTx.slateObj.amount));
        mTxFee.setText(WalletApi.nanoToVcashString(serverTx.slateObj.fee));
        mTxTime.setText(DateUtil.formatDateTimeStamp2(System.currentTimeMillis()));
    }


    public void configDataFromVcashTxLog() {
        if (vcashTxLog == null) return;
        switch (vcashTxLog.confirm_state) {
            case DefaultState:
                if (vcashTxLog.tx_type == TxSent) {
                    mIvStatus.setImageResource(R.drawable.ic_tx_ongoing_big);
                    mTvStatus.setText(R.string.tx_status_wait_receiver_sign);
                    mTvSign.setText(R.string.verify_signature);
                    mFlCancel.setVisibility(View.VISIBLE);
                    if (vcashTxLog.server_status == ServerTxStatus.TxDefaultStatus) {
                        mFlSign.setVisibility(View.GONE);
                    } else if (vcashTxLog.server_status == ServerTxStatus.TxReceiverd) {
                        serverTx = ServerTxManager.getInstance().getServerTxByTxId(vcashTxLog.tx_slate_id);
                        mFlSign.setVisibility(serverTx != null ? View.VISIBLE : View.GONE);
                        if(serverTx != null){
                            mTvStatus.setText(serverTx.isSend ? R.string.tx_status_wait_receiver_sign : R.string.tx_status_wait_your_sign);
                        }
                    }
                } else if (vcashTxLog.tx_type == VcashTxLog.TxLogEntryType.TxReceived) {
                    mIvStatus.setImageResource(R.drawable.ic_tx_ongoing_big);
                    mTvStatus.setText(R.string.tx_status_wait_sender_sign);
                    mFlSign.setVisibility(View.GONE);
                    mFlCancel.setVisibility(View.GONE);
                }
                break;
            case LoalConfirmed:
                mTvStatus.setText(R.string.tx_status_wait_confirm);
                mIvStatus.setImageResource(R.drawable.ic_tx_ongoing_big);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.GONE);
                break;
            case NetConfirmed:
                mTvStatus.setText(R.string.tx_status_completed);
                mIvStatus.setImageResource(R.drawable.ic_tx_confirmed_big);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.GONE);
                break;
        }

        mTvTxId.setText(UIUtils.isEmpty(vcashTxLog.tx_slate_id)? UIUtils.getString(R.string.unReachable) : vcashTxLog.tx_slate_id);
        mTxFee.setText(WalletApi.nanoToVcashString(vcashTxLog.fee));
        mTxTime.setText(DateUtil.formatDateTimeStamp(vcashTxLog.create_time));
        configInfoFromTxType(vcashTxLog.tx_type);
        if(vcashTxLog.tx_type == VcashTxLog.TxLogEntryType.TxReceived
                && UIUtils.isEmpty(vcashTxLog.parter_id)
                && !UIUtils.isEmpty(vcashTxLog.signed_slate_msg)){
            mLLBot.setVisibility(View.GONE);
            mLLFile.setVisibility(View.VISIBLE);
            mTvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
            mTvContent.setText(vcashTxLog.signed_slate_msg);
        }
    }


    public void configInfoFromTxType(VcashTxLog.TxLogEntryType txType) {
        switch (txType) {
            case ConfirmedCoinbase:
                mTvTxId.setText(R.string.coinbase);
                mTvSender.setText(R.string.coinbase);
                mTvRecipient.setText(R.string.coinbase);
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(vcashTxLog.amount_credited - vcashTxLog.amount_debited)));
                break;
            case TxSent:
                mTvSender.setText(WalletApi.getWalletUserId());
                mTvRecipient.setText(UIUtils.isEmpty(vcashTxLog.parter_id) ? UIUtils.getString(R.string.unReachable) : vcashTxLog.parter_id);
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(vcashTxLog.amount_credited - vcashTxLog.amount_debited) - vcashTxLog.fee));
                break;
            case TxReceived:
                mTvSender.setText(UIUtils.isEmpty(vcashTxLog.parter_id) ? UIUtils.getString(R.string.unReachable) : vcashTxLog.parter_id);
                mTvRecipient.setText(WalletApi.getWalletUserId());
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(vcashTxLog.amount_credited - vcashTxLog.amount_debited)));
                break;
            case TxReceivedCancelled:
                mIvStatus.setImageResource(R.drawable.ic_tx_canceled_big);
                mTvStatus.setText(R.string.tx_status_cancel);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.VISIBLE);
                mTvCancel.setText(R.string.delete_transaction);
                mTvCancel.setCompoundDrawablesWithIntrinsicBounds(UIUtils.getResource().getDrawable(R.drawable.ic_delete), null, null, null);
                mTvSender.setText(UIUtils.isEmpty(vcashTxLog.parter_id) ? UIUtils.getString(R.string.unReachable) : vcashTxLog.parter_id);
                mTvRecipient.setText(WalletApi.getWalletUserId());
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(vcashTxLog.amount_credited - vcashTxLog.amount_debited)));
                break;
            case TxSentCancelled:
                mIvStatus.setImageResource(R.drawable.ic_tx_canceled_big);
                mTvStatus.setText(R.string.tx_status_cancel);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.VISIBLE);
                mTvCancel.setText(R.string.delete_transaction);
                mTvCancel.setCompoundDrawablesWithIntrinsicBounds(UIUtils.getResource().getDrawable(R.drawable.ic_delete), null, null, null);
                mTvSender.setText(WalletApi.getWalletUserId());
                mTvRecipient.setText(UIUtils.isEmpty(vcashTxLog.parter_id) ? UIUtils.getString(R.string.unReachable) : vcashTxLog.parter_id);
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(vcashTxLog.amount_credited - vcashTxLog.amount_debited) - vcashTxLog.fee));
                break;
        }
    }


    @OnClick(R.id.fl_btn_sign)
    public void onSignClick() {
        boolean isSend = false;
        if (serverTx != null) {
            isSend = serverTx.isSend;
        }
        if (vcashTxLog != null) {
            isSend = (vcashTxLog.tx_type == TxSent);
        }
        if (serverTx != null) {
            showProgressDialog(R.string.wait);
            if (isSend) {
                WalletApi.finalizeServerTransaction(serverTx, new WalletCallback() {
                    @Override
                    public void onCall(boolean yesOrNo, Object data) {
                        dismissProgressDialog();
                        if (yesOrNo) {
                            UIUtils.showToastCenter(R.string.broadcast_success);
                            if (serverTx != null) {
                                ServerTxManager.getInstance().removeServerTx(serverTx.tx_id);
                            }
                            finish();
                        } else {
                            if (data instanceof String) {
                                UIUtils.showToastCenter((String) data);
                            } else {
                                UIUtils.showToastCenter(R.string.broadcast_failed);
                            }
                        }
                    }
                });
            } else {
                WalletApi.receiveTransaction(serverTx, new WalletCallback() {
                    @Override
                    public void onCall(boolean yesOrNo, Object data) {
                        dismissProgressDialog();
                        if (yesOrNo) {
                            UIUtils.showToastCenter(R.string.receive_success);
                            if (serverTx != null) {
                                ServerTxManager.getInstance().removeServerTx(serverTx.tx_id);
                            }
                            finish();
                        } else {
                            if (data instanceof String) {
                                UIUtils.showToastCenter((String) data);
                            } else {
                                UIUtils.showToastCenter(R.string.receive_failed);
                            }
                        }
                    }
                });
            }
        }

    }

    @OnClick(R.id.fl_btn_cancel)
    public void onCancelClick() {
        if (vcashTxLog != null) {
            switch (vcashTxLog.tx_type) {
                case TxSent:
                    if(vcashTxLog.confirm_state == VcashTxLog.TxLogConfirmType.DefaultState){
                        cancelTransaction(vcashTxLog.tx_slate_id);
                    }
                    break;
                case TxSentCancelled:
                case TxReceivedCancelled:
                    deleteTransaction();
                    break;
            }
        }
        if(serverTx != null){
            if(serverTx.status == ServerTxStatus.TxReceiverd){
                cancelTransaction(serverTx.tx_id);
            }
        }
    }

    @OnClick(R.id.tv_copy)
    public void onCopyClick(){
        UIUtils.copyText(this,mTvContent.getText().toString());
    }


    public void deleteTransaction() {
        boolean result = WalletApi.deleteTxByTxid(vcashTxLog.tx_slate_id);
        if (result) {
            UIUtils.showToastCenter(R.string.delete_success);
            finish();
        } else {
            UIUtils.showToastCenter(R.string.delete_failed);
        }
    }

    public void cancelTransaction(String tx_id){
        if(WalletApi.cancelTransaction(tx_id)){
            UIUtils.showToastCenter(R.string.cancel_success);
            if (serverTx != null) {
                ServerTxManager.getInstance().removeServerTx(serverTx.tx_id);
            }
            finish();
        } else {
            UIUtils.showToastCenter(R.string.cancel_failed);
        }
    }
}
