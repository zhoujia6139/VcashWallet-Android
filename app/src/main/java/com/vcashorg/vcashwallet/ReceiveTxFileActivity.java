package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.View;
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
        setTitleSize(15);
        setSubTitleSize(15);
        TextView tvRight = getSubTitle();
        tvRight.setText("Records");
        TextPaint paint = tvRight.getPaint();
        paint.setFakeBoldText(true);
        tvRight.setTextColor(UIUtils.getColor(R.color.orange));
        tvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nv(ReceiveTxFileRecordActivity.class);
            }
        });
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
        WalletApi.isValidSlateConentForReceive(mEtContent.getText().toString(), new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                if(yesOrNo){
                    final VcashSlate vcashSlate = (VcashSlate) data;
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(SignTxDialog.KEY,vcashSlate);
                    bundle.putString(SignTxDialog.TOKEN,vcashSlate.token_type);
                    SignTxDialog.newInstance(bundle).setOnSignClickListener(new SignTxDialog.OnSignClickListener() {
                        @Override
                        public void onSignClick() {
                            WalletApi.receiveTransactionBySlate(vcashSlate, new WalletCallback() {
                                @Override
                                public void onCall(boolean yesOrNo, Object data) {
                                    if(yesOrNo){
                                        Intent intent = new Intent(ReceiveTxFileActivity.this,ReceiveTxFileCopyActivity.class);
                                        intent.putExtra(ReceiveTxFileCopyActivity.PARAM_CONTENT,(String) data);
                                        intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_ID,vcashSlate.uuid);
                                        intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_AMOUNT,vcashSlate.amount);
                                        intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TX_FEE,vcashSlate.fee);
                                        intent.putExtra(ReceiveTxFileCopyActivity.PARAM_FROM,true);
                                        intent.putExtra(ReceiveTxFileCopyActivity.PARAM_TOKEN,vcashSlate.token_type);
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
