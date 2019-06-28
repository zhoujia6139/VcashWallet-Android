package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.SignTxDialog;

import butterknife.BindView;
import butterknife.OnClick;

public class ReceiveTxFileActivity extends ToolBarActivity {

    @BindView(R.id.et_tx_content)
    EditText mEtContent;

    @BindView(R.id.btn_read_tx)
    TextView mBtnReadTx;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.receive_transaction_file));
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_receive_tx_file;
    }

    @Override
    public void initView() {
        mEtContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().trim().equals("")){
                    mBtnReadTx.setBackground(UIUtils.getResource().getDrawable(R.drawable.bg_orange_light_round_rect));
                }else {
                    mBtnReadTx.setBackground(UIUtils.getResource().getDrawable(R.drawable.selector_orange));
                }
            }
        });
    }

    @OnClick(R.id.btn_read_tx)
    public void onReadTxClick(){
        if(mEtContent.getText().toString().trim().equals(""))return;
        WalletApi.isValidSlateConent(mEtContent.getText().toString(), new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if(yesOrNo){
                    final VcashSlate vcashSlate = (VcashSlate) data;
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(SignTxDialog.KEY,vcashSlate);
                    SignTxDialog.newInstance(bundle).setOnSignClickListener(new SignTxDialog.OnSignClickListener() {
                        @Override
                        public void onSignClick() {
                            WalletApi.receiveTransactionBySlate(vcashSlate, new WalletCallback() {
                                @Override
                                public void onCall(boolean yesOrNo, Object data) {
                                    if(yesOrNo){
                                        Intent intent = new Intent(ReceiveTxFileActivity.this,ReceiveTxFileCopyActivity.class);
                                        intent.putExtra(ReceiveTxFileCopyActivity.PARAM_CONTENT,(String) data);
                                        nv(intent);
                                        finish();
                                    }else {
                                        if(data instanceof String){
                                            UIUtils.showToastCenter((String) data);
                                        }
                                    }
                                }
                            });
                        }
                    }).show(getSupportFragmentManager(),"dialog");
                }else {
                    if(data instanceof String){
                        UIUtils.showToastCenter((String) data);
                    }
                }
            }
        });
    }
}
