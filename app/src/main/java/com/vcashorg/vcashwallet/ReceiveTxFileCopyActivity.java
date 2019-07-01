package com.vcashorg.vcashwallet;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;

import butterknife.BindView;
import butterknife.OnClick;

public class ReceiveTxFileCopyActivity extends ToolBarActivity {

    public static final String PARAM_CONTENT = "content";

    @BindView(R.id.tv_content)
    TextView mTvContent;


    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.receive_transaction_file));
        setTitleSize(15);
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

    @Override
    public void initParams() {
        String content = getIntent().getStringExtra(PARAM_CONTENT);
        mTvContent.setText(content);
        mTvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
    }


    @OnClick(R.id.tv_copy)
    public void onCopyClick(){
        UIUtils.copyText(this,mTvContent.getText().toString());
    }


}
