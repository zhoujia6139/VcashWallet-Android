package com.vcashorg.vcashwallet.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenInfo;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import java.util.List;
import java.util.Set;

public class VcashTokenAddAdapter extends BaseQuickAdapter<VcashTokenInfo, BaseViewHolder> {

    public VcashTokenAddAdapter(int layoutResId, @Nullable List<VcashTokenInfo> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, final VcashTokenInfo item) {
        helper.setText(R.id.tv_token_name,item.Name);
        helper.setText(R.id.tv_token_full_name,item.FullName);
        ImageView ivToken = helper.getView(R.id.iv_token);
        if(!TextUtils.isEmpty(item.IconData)){
            byte[] decodedString = Base64.decode(item.IconData.split(",")[1], Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivToken.setImageBitmap(decodedByte);
        }else {
            ivToken.setImageResource(R.drawable.ic_vcash_placeholder);
        }

        Set<String> addedTokens = WalletApi.getAddedTokens();

        SwitchCompat switchCompat = helper.getView(R.id.token_switcher);

        if(addedTokens.contains(item.TokenId)){
            switchCompat.setChecked(true);
        }else {
            switchCompat.setChecked(false);
        }

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    WalletApi.addAddedToken(item.TokenId);
                }else {
                    WalletApi.deleteAddedToken(item.TokenId);
                }
            }
        });
    }
}
