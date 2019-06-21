package com.vcashorg.vcashwallet;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
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


    @OnClick(R.id.btn_read_tx)
    public void onReadTxClick(){

        VcashSlate slate = new VcashSlate();

        Bundle bundle = new Bundle();
        bundle.putSerializable(SignTxDialog.KEY,slate);

        SignTxDialog.newInstance(bundle).setOnSignClickListener(new SignTxDialog.OnSignClickListener() {
            @Override
            public void onSignClick() {
                nv(ReceiveTxFileCopyActivity.class);
            }
        }).show(getSupportFragmentManager(),"dialog");
    }
}
