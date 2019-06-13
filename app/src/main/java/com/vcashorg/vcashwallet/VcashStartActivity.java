package com.vcashorg.vcashwallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;

import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.payload.PayloadUtil;
import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.List;

import butterknife.OnClick;

public class VcashStartActivity extends BaseActivity {
    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vcash_start;
    }

    @Override
    public void initView() {

    }

    @OnClick(R.id.create_wallet)
    public void onCreateWalletClick(){
        AndPermission.with(this)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        nv(WalletCreateActivity.class);
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        UIUtils.showToastCenter("Need Storage Permission");
                    }
                })
                .start();
    }


    @OnClick(R.id.restore_wallet)
    public void onRestoreWalletClick(){
        AndPermission.with(this)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        new AlertDialog.Builder(VcashStartActivity.this)
                                .setTitle("Warning")
                                .setMessage("The recovered wallet will cover the original wallet,please be cautious")
                                .setPositiveButton("Generate", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        WalletApi.clearWallet();
                                        SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.FIRST_CREATE_WALLET,false);
                                        nv(MnemonicRestoreActivity.class);
                                    }
                                })
                                .setNegativeButton("Cancel",null)
                                .show();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        UIUtils.showToastCenter("Need Storage Permission");
                    }
                })
                .start();
    }
}
