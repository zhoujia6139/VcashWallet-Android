package com.vcashorg.vcashwallet.widget;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.vcashorg.vcashwallet.AddressAddActivity;
import com.vcashorg.vcashwallet.AddressBookActivity;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.utils.UIUtils;

public class AddressBotDialog extends BottomDialogView{
    private String id;

    public AddressBotDialog(@NonNull Context context,String id) {
        super(context, true);
        layoutId  = R.layout.layout_address_bot;
        this.id = id;
    }

    @Override
    protected void initView() {
        TextView tvCopy = findViewById(R.id.tv_copy);
        TextView tvSave = findViewById(R.id.tv_save);
        TextView tvCancel = findViewById(R.id.tv_cancel);

        tvCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtils.copyText(UIUtils.getContext(),id);
            }
        });

        tvSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AddressAddActivity.class);
                context.startActivity(intent);
            }
        });

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}
