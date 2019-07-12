package com.vcashorg.vcashwallet.adapter;

import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.bean.Address;

import java.util.List;

public class AddressBookAdapter extends BaseQuickAdapter<Address, BaseViewHolder> {
    public AddressBookAdapter(int layoutResId, @Nullable List<Address> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Address item) {
        helper.setText(R.id.tv_remark,item.remark);
        helper.setText(R.id.tv_user_id,item.userId);
    }
}
