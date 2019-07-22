package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mylhyl.zxing.scanner.common.Scanner;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.bean.Address;
import com.vcashorg.vcashwallet.utils.AddressFileUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.widget.qrcode.BasicScannerActivity;
import com.vcashorg.vcashwallet.widget.qrcode.ScannerActivity;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class AddressAddActivity extends ToolBarActivity {

    public static final String PARAM_TYPE = "type";

    @BindView(R.id.et_user_id)
    EditText mEtId;
    @BindView(R.id.et_remark)
    EditText mEtRemark;
    @BindView(R.id.btn_save)
    Button mBtnSave;

    private String type = "add";

    @Override
    protected void initToolBar() {
        setToolBarTitle("Address Book");
        setToolBarBgColor(R.color.white);
    }

    @Override
    public void initParams() {
        type = getIntent().getStringExtra(PARAM_TYPE);
        if(type.equals("edit")){
            String id = getIntent().getStringExtra("id");
            mEtId.setText(id);
            mEtId.setFocusable(false);
            mEtId.setFocusableInTouchMode(false);
        }else {
            String id = getIntent().getStringExtra("id");
            mEtId.setText(id);
        }
    }

    @Override
    public void initView() {
        mEtId.addTextChangedListener(new TextWatcher() {
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

        mEtRemark.addTextChangedListener(new TextWatcher() {
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

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_address_add;
    }

    private void btnState(){
        if(!mEtId.getText().toString().trim().equals("")
                && !mEtRemark.getText().toString().trim().equals("")){
            mBtnSave.setEnabled(true);
            mBtnSave.setBackgroundResource(R.drawable.selector_orange);
        }else {
            mBtnSave.setEnabled(false);
            mBtnSave.setBackgroundResource(R.drawable.bg_orange_light_round_rect);
        }
    }

    @OnClick(R.id.btn_save)
    public void onSaveClick(){
        if(!mEtId.getText().toString().trim().equals("")
                && !mEtRemark.getText().toString().trim().equals("")){
            if(type.equals("edit")){
                boolean result = AddressFileUtil.updateAddress(AddressAddActivity.this,mEtId.getText().toString(),mEtRemark.getText().toString());
                if(result){
                    UIUtils.showToastCenter("Save Success");
                    finish();
                }else {
                    UIUtils.showToastCenter("Save Failed");
                }
           }else {
                Address address = new Address();
                address.userId = mEtId.getText().toString().trim();
                address.remark = mEtRemark.getText().toString().trim();
                int result = AddressFileUtil.addAddress(AddressAddActivity.this,address);
                if(result == 0){
                    UIUtils.showToastCenter("Add Failed");
                }else if(result == 1){
                    UIUtils.showToastCenter("Add Success");
                    finish();
                }else if(result == 2){
                    UIUtils.showToastCenter("UserId Already Exist");
                }
            }

        }
    }

    @OnClick(R.id.iv_qrcode)
    public void onCodeClick(){
        AndPermission.with(this)
                .permission(Permission.Group.CAMERA)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        ScannerActivity.gotoActivity(AddressAddActivity.this,
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
                mEtId.setText(result);
            }
        }
    }
}
