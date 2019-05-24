package com.vcashorg.vcashwallet;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.mylhyl.zxing.scanner.encode.QREncode;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;

import butterknife.BindView;
import butterknife.OnClick;

public class VcashReceiveActivity extends ToolBarActivity {

    @BindView(R.id.iv_qrcode)
    ImageView mQrcode;
    @BindView(R.id.tv_wallet_id)
    TextView mWalletId;

    @Override
    protected void initToolBar() {
        setToolBarTitle("Receive Vcash");
    }

    @Override
    public void initView() {

        Bitmap bitmap = new QREncode.Builder(this)
                .setColor(UIUtils.getColor(R.color.black))//二维码颜色
                .setContents(mWalletId.getText().toString())//二维码内容
                .build().encodeAsBitmap();
        mQrcode.setImageBitmap(bitmap);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vcash_receive;
    }

    @OnClick(R.id.iv_copy)
    public void onCopyClick(){
        UIUtils.copyText(this,mWalletId.getText().toString());
    }
}
