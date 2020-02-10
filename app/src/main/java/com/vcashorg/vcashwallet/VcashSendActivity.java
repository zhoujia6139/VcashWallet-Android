package com.vcashorg.vcashwallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mylhyl.zxing.scanner.common.Scanner;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.bean.Address;
import com.vcashorg.vcashwallet.utils.Args;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.utils.VCashUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.AbstractVcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.VcashSendDialog;
import com.vcashorg.vcashwallet.widget.qrcode.BasicScannerActivity;
import com.vcashorg.vcashwallet.widget.qrcode.ScannerActivity;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class VcashSendActivity extends ToolBarActivity {

    @BindView(R.id.et_address)
    EditText mEtAddress;
    @BindView(R.id.et_amount)
    EditText mEtAmount;
    @BindView(R.id.btn_send)
    FrameLayout mBtnSend;
    @BindView(R.id.tv_available)
    TextView mTvAvailable;
    @BindView(R.id.line_id)
    View mLine1;
    @BindView(R.id.line_amount)
    View mLine2;
    @BindView(R.id.tv_vcash_name)
    TextView mTvVCashName;

    private String tokenType;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.send_vcash));
        if(!VCashUtil.isVCash(tokenType)){
            setToolBarTitle(UIUtils.getString(R.string.send) + " " + WalletApi.getTokenInfo(tokenType).Name);
        }
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vcash_send;
    }

    @Override
    public void initParams() {
        tokenType = getIntent().getStringExtra(Args.TOKEN_TYPE);
    }

    @Override
    public void initView() {
        mEtAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mLine1.setBackgroundColor(UIUtils.getColor(R.color.orange));
                mLine2.setBackgroundColor(UIUtils.getColor(R.color.grey_4));
            }
        });
        mEtAmount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mLine1.setBackgroundColor(UIUtils.getColor(R.color.grey_4));
                mLine2.setBackgroundColor(UIUtils.getColor(R.color.orange));
            }
        });
        mEtAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                btnState();
            }
        });
        mEtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                btnState();
            }
        });

        if(VCashUtil.isVCash(tokenType)){
            mTvAvailable.setText(UIUtils.getString(R.string.available) + ": " + WalletApi.nanoToVcashString(VCashUtil.VCashSpendable(tokenType)) + " V");
        }else {
            mTvAvailable.setText(UIUtils.getString(R.string.available) + ": " + WalletApi.nanoToVcashString(VCashUtil.VCashSpendable(tokenType)));
            mTvVCashName.setVisibility(View.GONE);
        }
    }

    private boolean btnState() {
        if (!mEtAmount.getText().toString().trim().equals("")
                && !mEtAddress.getText().toString().trim().equals("")) {
            mBtnSend.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_orange));
            return true;
        } else {
            mBtnSend.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_orange_light_round_rect));
            return false;
        }
    }

    @OnClick(R.id.iv_qrcode)
    public void onQrcodeClick() {
        AndPermission.with(this)
                .permission(Permission.Group.CAMERA)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        //权限已经被授予
                        ScannerActivity.gotoActivity(VcashSendActivity.this,
                                true, ScannerActivity.EXTRA_LASER_LINE_MODE_0, ScannerActivity.EXTRA_SCAN_MODE_0,
                                false, false, false);
                    }
                })
                .start();
    }

    @OnClick(R.id.tv_address_book)
    public void onAddressBookClick(){
        nv2(AddressBookActivity.class,100);
    }

    @OnClick(R.id.btn_send)
    public void onSendClick(){
        if(btnState() && validate() != -1){
            WalletApi.createSendTransaction(VCashUtil.isVCash(tokenType) ? null : tokenType, WalletApi.vcashToNano(Double.parseDouble(mEtAmount.getText().toString().trim())), new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    if(yesOrNo){
                        final VcashSlate slate = (VcashSlate) data;
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(VcashSendDialog.KEY,slate);
                        bundle.putString(VcashSendDialog.RECEIVER,mEtAddress.getText().toString());
                        bundle.putString(VcashSendDialog.TOKEN,tokenType);
                        VcashSendDialog.newInstance(bundle).setOnConfirmClickListener(new VcashSendDialog.OnConfirmClickListener() {
                            @Override
                            public void onConfirmClick() {
                                showProgressDialog(R.string.wait);
                                if(validate() == 0){
                                    WalletApi.sendTransactionForUser(slate, mEtAddress.getText().toString(), new WalletCallback() {
                                        @Override
                                        public void onCall(boolean yesOrNo, Object data) {
                                            dismissProgressDialog();
                                            if(yesOrNo){
                                                UIUtils.showToastCenter(R.string.send_success);
                                                AbstractVcashTxLog vcashTxLog = WalletApi.getTxByTxid(slate.uuid);
                                                Intent intent = new Intent(VcashSendActivity.this,TxDetailsActivity.class);
                                                intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE,TxDetailsActivity.TYPE_TX_LOG);
                                                intent.putExtra(TxDetailsActivity.PARAM_TX_DATA,vcashTxLog);
                                                intent.putExtra(TxDetailsActivity.PARAM_TX_SENDER,true);
                                                nv(intent);
                                                finish();
                                            }else {
                                                if(data instanceof String){
                                                    UIUtils.showToastCenter((String) data);
                                                }else {
                                                    UIUtils.showToastCenter(R.string.send_failed);
                                                }
                                            }
                                        }
                                    });
                                }else if(validate() == 1){
                                    WalletApi.sendTransactionForUrl(slate, mEtAddress.getText().toString(), new WalletCallback() {
                                        @Override
                                        public void onCall(boolean yesOrNo, Object data) {
                                            dismissProgressDialog();
                                            if(yesOrNo){
                                                UIUtils.showToastCenter(R.string.send_success);
                                                Intent intent = new Intent(VcashSendActivity.this,TxDetailsActivity.class);
                                                AbstractVcashTxLog abstractVcashTxLog = WalletApi.getTxByTxid(slate.uuid);
                                                if(VCashUtil.isVCash(tokenType)){
                                                    if(abstractVcashTxLog instanceof VcashTxLog){
                                                        VcashTxLog txLog = (VcashTxLog) abstractVcashTxLog;
                                                        txLog.confirm_state = VcashTxLog.TxLogConfirmType.LoalConfirmed;
                                                        intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, txLog);
                                                        intent.putExtra(TxDetailsActivity.PARAM_TX_ISTOKEN,false);
                                                    }
                                                }else {
                                                    if(abstractVcashTxLog instanceof VcashTokenTxLog){
                                                        VcashTokenTxLog tokenTxLog = (VcashTokenTxLog) abstractVcashTxLog;
                                                        tokenTxLog.confirm_state = VcashTxLog.TxLogConfirmType.LoalConfirmed;
                                                        intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, tokenTxLog);
                                                        intent.putExtra(TxDetailsActivity.PARAM_TX_ISTOKEN,true);
                                                    }
                                                }
                                                intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE,TxDetailsActivity.TYPE_TX_LOG);
                                                intent.putExtra(TxDetailsActivity.PARAM_TX_SENDER,true);
                                                nv(intent);
                                                finish();
                                            }else {
                                                if(data instanceof String){
                                                    UIUtils.showToastCenter((String) data);
                                                }else {
                                                    UIUtils.showToastCenter(R.string.send_failed);
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        }).show(getSupportFragmentManager(),"dialog");
                    }else {
                        dismissProgressDialog();
                        if(data instanceof String){
                            UIUtils.showToastCenter((String) data);
                        }else {
                            UIUtils.showToastCenter(R.string.send_failed);
                        }
                    }
                }
            });
        }
    }

    private int validate(){
        String address = mEtAddress.getText().toString().trim();

        if(Double.parseDouble(mEtAmount.getText().toString().trim()) == 0){
            UIUtils.showToastCenter(R.string.send_cant_0);
            return -1;
        }

        //send for url return 1
        if(address.startsWith("http") || address.startsWith("https")){
            return 1;
        }

        //send for user return 0;
        if(address.length() != 66){
            UIUtils.showToastCenter(R.string.send_address_length);
            return -1;
        }else if(address.equals(WalletApi.getWalletUserId())){
            UIUtils.showToastCenter(R.string.send_cant_me);
            return -1;
        }

        return 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BasicScannerActivity.REQUEST_CODE_SCANNER && resultCode == RESULT_OK) {
            if (data != null) {
                String result = data.getStringExtra(Scanner.Scan.RESULT);
                mEtAddress.setText(result);
            }
        }else if(requestCode == 100 && resultCode == RESULT_OK){
            if (data != null){
                String result = data.getStringExtra(Address.RESULT_ADDRESS);
                mEtAddress.setText(result);
            }
        }
    }


}
