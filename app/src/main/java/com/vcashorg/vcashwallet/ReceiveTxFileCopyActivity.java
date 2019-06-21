package com.vcashorg.vcashwallet;

import android.view.View;
import android.widget.TextView;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;

import butterknife.BindView;
import butterknife.OnClick;

public class ReceiveTxFileCopyActivity extends ToolBarActivity {

    @BindView(R.id.tv_content)
    TextView mTvContent;
    @BindView(R.id.btn_read_tx)
    TextView mBtnReadTx;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.receive_transaction_file));
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

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_receive_tx_file_copy;
    }


    @OnClick(R.id.tv_copy)
    public void onCopyClick(){

    }


}
