package com.vcashorg.vcashwallet;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.mylhyl.zxing.scanner.common.Scanner;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;
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

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.send_vcash));
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vcash_send;
    }

    @Override
    public void initView() {
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

    @OnClick(R.id.btn_send)
    public void onSendClick(){
        if(btnState() && validate()){
            showProgressDialog(R.string.wait);
            WalletApi.createSendTransaction(mEtAddress.getText().toString().trim(), WalletApi.vcashToNano(Double.parseDouble(mEtAmount.getText().toString().trim())), 0, new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    if(yesOrNo){
                        final VcashSlate slate = (VcashSlate) data;
                        WalletApi.sendTransaction(slate, mEtAddress.getText().toString(), new WalletCallback() {
                            @Override
                            public void onCall(boolean yesOrNo, Object data) {
                                dismissProgressDialog();
                                if(yesOrNo){
                                    UIUtils.showToastCenter(R.string.send_success);
                                    VcashTxLog vcashTxLog = WalletApi.getTxByTxid(slate.uuid);
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

    private boolean validate(){
        if(mEtAddress.getText().toString().trim().length() != 66){
            UIUtils.showToastCenter(R.string.send_address_length);
            return false;
        }else if(Double.parseDouble(mEtAmount.getText().toString().trim()) == 0){
            UIUtils.showToastCenter(R.string.send_cant_0);
            return false;
        }else if(mEtAddress.getText().toString().trim().equals(WalletApi.getWalletUserId())){
            UIUtils.showToastCenter(R.string.send_cant_me);
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BasicScannerActivity.REQUEST_CODE_SCANNER && resultCode == RESULT_OK) {
            if (data != null) {
                String result = data.getStringExtra(Scanner.Scan.RESULT);
                mEtAddress.setText(result);
            }
        }
    }
}
