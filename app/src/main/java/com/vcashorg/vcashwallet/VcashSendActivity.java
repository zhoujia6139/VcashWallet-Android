package com.vcashorg.vcashwallet;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.mylhyl.zxing.scanner.common.Scanner;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
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
        setToolBarTitle("Send Vcash");
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

    private void btnState() {
        if (!mEtAmount.getText().toString().trim().equals("")
                && !mEtAddress.getText().toString().trim().equals("")) {
            mBtnSend.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_green_round_rect));
            mBtnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIUtils.showToast("Click Send");
                }
            });
        } else {
            mBtnSend.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_grey_round_rect));
            mBtnSend.setOnClickListener(null);
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
