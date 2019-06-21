package com.vcashorg.vcashwallet;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.mylhyl.zxing.scanner.encode.QREncode;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import butterknife.BindView;
import butterknife.OnClick;

public class VcashReceiveActivity extends ToolBarActivity {

    @BindView(R.id.iv_qrcode)
    ImageView mQrcode;
    @BindView(R.id.tv_wallet_id)
    TextView mWalletId;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.receive_vcash));
    }

    @Override
    public void initView() {

        Bitmap bitmap = new QREncode.Builder(this)
                .setColor(UIUtils.getColor(R.color.black))
                .setContents(WalletApi.getWalletUserId())
                .build().encodeAsBitmap();
        mQrcode.setImageBitmap(bitmap);

        mWalletId.setText(WalletApi.getWalletUserId());
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vcash_receive;
    }

    @OnClick(R.id.iv_copy)
    public void onCopyClick() {
        UIUtils.copyText(this, mWalletId.getText().toString());
    }


    @OnClick(R.id.tv_receive_tx_file)
    public void onReceiveTxFileClick(){

        nv(ReceiveTxFileActivity.class);
    }
}
