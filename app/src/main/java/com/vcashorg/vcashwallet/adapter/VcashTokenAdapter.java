package com.vcashorg.vcashwallet.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.utils.VCashUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenInfo;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import java.util.List;

public class VcashTokenAdapter extends BaseQuickAdapter<VcashTokenInfo, BaseViewHolder> {

    public VcashTokenAdapter(int layoutResId, @Nullable List<VcashTokenInfo> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, VcashTokenInfo item) {
        helper.setText(R.id.tv_token_name,item.Name);
        helper.setText(R.id.tv_token_full_name,item.FullName);
        ImageView ivToken = helper.getView(R.id.iv_token);
        if(!TextUtils.isEmpty(item.IconData)){
            byte[] decodedString = Base64.decode(item.IconData.split(",")[1], Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivToken.setImageBitmap(decodedByte);
        }else {
            if(VCashUtil.isVCash(item.TokenType)){
                ivToken.setImageResource(R.drawable.ic_vcash);
            }else {
                ivToken.setImageResource(R.drawable.ic_vcash_placeholder);
            }
        }

        WalletApi.WalletBalanceInfo balance = item.Balance;
        if(balance != null){
            helper.setText(R.id.tv_token_balance,WalletApi.nanoToVcashString(balance.total));
        }
    }
}
