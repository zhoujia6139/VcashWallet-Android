package com.vcashorg.vcashwallet;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vcashorg.vcashwallet.api.ServerTxManager;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.AddressFileUtil;
import com.vcashorg.vcashwallet.utils.DateUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.utils.VCashUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.AbstractVcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.AddressBotDialog;

import butterknife.BindView;
import butterknife.OnClick;

import static com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog.TxLogConfirmType.NetConfirmed;

public class TxDetailsActivity extends ToolBarActivity {

    public static final int TYPE_TX_SERVER = 0;
    public static final int TYPE_TX_LOG = 1;

    public static final String PARAM_TX_TYPE = "tx_type";
    public static final String PARAM_TX_DATA = "tx_data";
    public static final String PARAM_TX_SENDER = "tx_sender";
    public static final String PARAM_TX_ISTOKEN = "is_token";

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
    @BindView(R.id.tv_tx_confirm)
    TextView mTxConfirmNum;

    @BindView(R.id.iv_status)
    ImageView mIvStatus;
    @BindView(R.id.tv_status)
    TextView mTvStatus;

    @BindView(R.id.tv_token_name)
    TextView mTvTokenName;
    @BindView(R.id.layout_token)
    View mLayoutToken;
    @BindView(R.id.tv_token)
    TextView mTvToken;

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
    VcashTokenTxLog tokenTxLog;
    ServerTransaction serverTx;

    protected boolean sender;
    protected boolean isToken = false;

    private String tokenType = "";

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
            if(serverTx != null && serverTx.slateObj != null){
                tokenType = serverTx.slateObj.token_type;
            }
            configDataFromServerTransaction();
        } else {
            isToken = intent.getBooleanExtra(PARAM_TX_ISTOKEN,false);
            if(isToken){
                tokenTxLog = (VcashTokenTxLog) intent.getSerializableExtra(PARAM_TX_DATA);
                if(tokenType != null){
                    tokenType = tokenTxLog.token_type;
                }
                configDataFromVcashTxLog(tokenTxLog);
            }else {
                vcashTxLog = (VcashTxLog) intent.getSerializableExtra(PARAM_TX_DATA);
                configDataFromVcashTxLog(vcashTxLog);
            }
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
    public void initView() {

        if(!TextUtils.isEmpty(tokenType) && !VCashUtil.isVCash(tokenType)){
            mLayoutToken.setVisibility(View.VISIBLE);
            mTvTokenName.setText(WalletApi.getTokenInfo(tokenType).Name);
            mTvToken.setText(WalletApi.getTokenInfo(tokenType).Name);
        }

        addressBookRemark();
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
        if(serverTx.isSend){
            if(!mTvRecipient.getText().toString().equals(UIUtils.getString(R.string.unReachable))){
                mTvRecipient.setTextColor(UIUtils.getColor(R.color.blue));
            }
        }else {
            if(!mTvSender.getText().toString().equals(UIUtils.getString(R.string.unReachable))){
                mTvSender.setTextColor(UIUtils.getColor(R.color.blue));
            }
        }
    }

    public void configDataFromVcashTxLog(AbstractVcashTxLog abstractVcashTxLog) {
        if(abstractVcashTxLog == null) return;
        switch (abstractVcashTxLog.confirm_state) {
            case DefaultState:
                if (abstractVcashTxLog.tx_type == AbstractVcashTxLog.TxLogEntryType.TxSent) {
                    mIvStatus.setImageResource(R.drawable.ic_tx_ongoing_big);
                    mTvStatus.setText(R.string.tx_status_wait_receiver_sign);
                    mTvSign.setText(R.string.verify_signature);
                    mFlCancel.setVisibility(View.VISIBLE);
                    if (abstractVcashTxLog.server_status == ServerTxStatus.TxDefaultStatus) {
                        mFlSign.setVisibility(View.GONE);
                    } else if (abstractVcashTxLog.server_status == ServerTxStatus.TxReceiverd) {
                        serverTx = ServerTxManager.getInstance().getServerTxByTxId(abstractVcashTxLog.tx_slate_id);
                        mFlSign.setVisibility(serverTx != null ? View.VISIBLE : View.GONE);
                        if(serverTx != null){
                            mTvStatus.setText(serverTx.isSend ? R.string.tx_status_wait_receiver_sign : R.string.tx_status_wait_your_sign);
                        }
                    }
                } else if (abstractVcashTxLog.tx_type == VcashTxLog.TxLogEntryType.TxReceived) {
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

        mTvTxId.setText(UIUtils.isEmpty(abstractVcashTxLog.tx_slate_id)? UIUtils.getString(R.string.unReachable) : abstractVcashTxLog.tx_slate_id);
        mTxFee.setText(WalletApi.nanoToVcashString(abstractVcashTxLog.fee));
        mTxTime.setText(DateUtil.formatDateTimeStamp(abstractVcashTxLog.create_time));
        if(abstractVcashTxLog.confirm_height == 0){
            mTxConfirmNum.setText("0");
        }else {
            mTxConfirmNum.setText((WalletApi.getCurChainHeight() - abstractVcashTxLog.confirm_height) + "");
        }
        if(isToken){
            configInfoFromTxTokenType(tokenTxLog);
        }else {
            configInfoFromTxType(vcashTxLog);
        }
        if(abstractVcashTxLog.tx_type == AbstractVcashTxLog.TxLogEntryType.TxSent){
            if(!mTvRecipient.getText().toString().equals(UIUtils.getString(R.string.unReachable))){
                mTvRecipient.setTextColor(UIUtils.getColor(R.color.blue));
            }
        }else {
            if(!mTvSender.getText().toString().equals(UIUtils.getString(R.string.unReachable))){
                mTvSender.setTextColor(UIUtils.getColor(R.color.blue));
            }
        }
        if(abstractVcashTxLog.tx_type == VcashTxLog.TxLogEntryType.TxReceived
                && UIUtils.isEmpty(abstractVcashTxLog.parter_id)
                && !UIUtils.isEmpty(abstractVcashTxLog.signed_slate_msg)){
            mFlSign.setVisibility(View.GONE);
            if(abstractVcashTxLog.confirm_state == NetConfirmed){
                mFlCancel.setVisibility(View.GONE);
            }else {
                mFlCancel.setVisibility(View.VISIBLE);
            }
            mTvCancel.setText(R.string.delete_transaction);
            mTvCancel.setCompoundDrawablesWithIntrinsicBounds(UIUtils.getResource().getDrawable(R.drawable.ic_delete), null, null, null);
            mLLFile.setVisibility(View.VISIBLE);
            mTvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
            mTvContent.setText(abstractVcashTxLog.signed_slate_msg);
        }
    }

    private void configInfoFromTxType(VcashTxLog vcashTxLog) {
        if(vcashTxLog == null) return;
        VcashTxLog.TxLogEntryType txType = vcashTxLog.tx_type;
        switch (txType) {
            case ConfirmedCoinbaseOrTokenIssue:
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

    private void configInfoFromTxTokenType(VcashTokenTxLog tokenTxLog) {
        if(tokenTxLog == null) return;
        VcashTxLog.TxLogEntryType txType = tokenTxLog.tx_type;
        switch (txType) {
            case ConfirmedCoinbaseOrTokenIssue:
                mTvTxId.setText(R.string.tokenissue);
                mTvSender.setText(R.string.tokenissue);
                mTvRecipient.setText(WalletApi.getWalletUserId());
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(tokenTxLog.token_amount_credited - tokenTxLog.token_amount_debited)));
                break;
            case TxSent:
                mTvSender.setText(WalletApi.getWalletUserId());
                mTvRecipient.setText(UIUtils.isEmpty(tokenTxLog.parter_id) ? UIUtils.getString(R.string.unReachable) : tokenTxLog.parter_id);
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(tokenTxLog.token_amount_credited - tokenTxLog.token_amount_debited) - tokenTxLog.fee));
                break;
            case TxReceived:
                mTvSender.setText(UIUtils.isEmpty(tokenTxLog.parter_id) ? UIUtils.getString(R.string.unReachable) : tokenTxLog.parter_id);
                mTvRecipient.setText(WalletApi.getWalletUserId());
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(tokenTxLog.token_amount_credited - tokenTxLog.token_amount_debited)));
                break;
            case TxReceivedCancelled:
                mIvStatus.setImageResource(R.drawable.ic_tx_canceled_big);
                mTvStatus.setText(R.string.tx_status_cancel);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.VISIBLE);
                mTvCancel.setText(R.string.delete_transaction);
                mTvCancel.setCompoundDrawablesWithIntrinsicBounds(UIUtils.getResource().getDrawable(R.drawable.ic_delete), null, null, null);
                mTvSender.setText(UIUtils.isEmpty(tokenTxLog.parter_id) ? UIUtils.getString(R.string.unReachable) : tokenTxLog.parter_id);
                mTvRecipient.setText(WalletApi.getWalletUserId());
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(tokenTxLog.token_amount_credited - tokenTxLog.token_amount_debited)));
                break;
            case TxSentCancelled:
                mIvStatus.setImageResource(R.drawable.ic_tx_canceled_big);
                mTvStatus.setText(R.string.tx_status_cancel);
                mFlSign.setVisibility(View.GONE);
                mFlCancel.setVisibility(View.VISIBLE);
                mTvCancel.setText(R.string.delete_transaction);
                mTvCancel.setCompoundDrawablesWithIntrinsicBounds(UIUtils.getResource().getDrawable(R.drawable.ic_delete), null, null, null);
                mTvSender.setText(WalletApi.getWalletUserId());
                mTvRecipient.setText(UIUtils.isEmpty(tokenTxLog.parter_id) ? UIUtils.getString(R.string.unReachable) : tokenTxLog.parter_id);
                mTxAmount.setText(WalletApi.nanoToVcashString(Math.abs(tokenTxLog.token_amount_credited - tokenTxLog.token_amount_debited)));
                break;
        }
    }

    private void addressBookRemark(){
        if(mTvSender.getText().toString().trim().equals(WalletApi.getWalletUserId())){
            buildSpanRemark(mTvSender.getText().toString(),"me",mTvSender);
        }else {
            String senderRemark = AddressFileUtil.findRemarkByAddress(this,mTvSender.getText().toString().trim());
            if(senderRemark != null){
                buildSpanRemark(mTvSender.getText().toString(),senderRemark,mTvSender);
            }
        }

        if(mTvRecipient.getText().toString().trim().equals(WalletApi.getWalletUserId())){
            buildSpanRemark(mTvRecipient.getText().toString(),"me",mTvRecipient);
        }else {
            String receiveRemark = AddressFileUtil.findRemarkByAddress(this,mTvRecipient.getText().toString().trim());
            if(receiveRemark != null){
                buildSpanRemark(mTvRecipient.getText().toString(),receiveRemark,mTvRecipient);
            }
        }
    }

    private void buildSpanRemark(String id,String remark,TextView textView){
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(UIUtils.getColor(R.color.colorPrimary));
        String value = id + "(" + remark + ")";
        SpannableString ssText = new SpannableString(value);
        int index = value.indexOf("(");
        if(index != -1){
            ssText.setSpan(foregroundColorSpan, index, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(ssText);
    }

    @OnClick(R.id.fl_btn_sign)
    public void onSignClick() {
        boolean isSend = false;
        if (serverTx != null) {
            isSend = serverTx.isSend;
        }
        if (vcashTxLog != null) {
            isSend = (vcashTxLog.tx_type == AbstractVcashTxLog.TxLogEntryType.TxSent);
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
                case TxReceived:
                    if(UIUtils.isEmpty(vcashTxLog.parter_id)
                        && !UIUtils.isEmpty(vcashTxLog.signed_slate_msg)){
                        new AlertDialog.Builder(this)
                                .setTitle("Are you sure delete the transaction?")
                                .setMessage("The transaction will not be shown on your phone after being deleted.")
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteFileTx(vcashTxLog.tx_slate_id);
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
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


    @OnClick(R.id.sender_id)
    public void onSenderIdClick(){
        boolean isSend = false;
        if (serverTx != null) {
            isSend = serverTx.isSend;
        }
        if (vcashTxLog != null) {
            isSend = (vcashTxLog.tx_type == AbstractVcashTxLog.TxLogEntryType.TxSent);
        }
        if(!isSend && !mTvSender.getText().toString().equals(UIUtils.getString(R.string.unReachable))){
            new AddressBotDialog(this,mTvSender.getText().toString().trim().split("\\(")[0].trim(),1).show();
        }
    }

    @OnClick(R.id.receiver_id)
    public void onReceiverClick(){
        boolean isSend = false;
        if (serverTx != null) {
            isSend = serverTx.isSend;
        }
        if (vcashTxLog != null) {
            isSend = (vcashTxLog.tx_type == AbstractVcashTxLog.TxLogEntryType.TxSent);
        }
        if(isSend && !mTvRecipient.getText().toString().equals(UIUtils.getString(R.string.unReachable))){
            new AddressBotDialog(this,mTvRecipient.getText().toString().trim().split("\\(")[0].trim(),1).show();
        }
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

    private void deleteFileTx(String tx_id){
        if(WalletApi.cancelTransaction(tx_id)){
            boolean result = WalletApi.deleteTxByTxid(tx_id);
            if(result){
                UIUtils.showToastCenter(R.string.delete_success);
                finish();
            }else {
                UIUtils.showToastCenter(R.string.delete_failed);
            }
        } else {
            UIUtils.showToastCenter(R.string.delete_failed);
        }
    }
}
