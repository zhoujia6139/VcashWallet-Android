package com.vcashorg.vcashwallet.widget;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.vcashorg.vcashwallet.AddressAddActivity;
import com.vcashorg.vcashwallet.AddressBookActivity;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.utils.AddressFileUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;

public class AddressBotDialog extends BottomDialogView{
    private String id;
    private int type;

    private AddressHandleCallBack callBack;

    public AddressBotDialog(@NonNull Context context,String id,int type) {
        super(context, true);
        layoutId  = R.layout.layout_address_bot;
        this.type = type;
        this.id = id;
    }

    @Override
    protected void initView() {
        TextView tvCopy = findViewById(R.id.tv_copy);
        TextView tvSave = findViewById(R.id.tv_save);
        TextView tvCancel = findViewById(R.id.tv_cancel);
        TextView tvEdit = findViewById(R.id.tv_edit);
        TextView tvDelete = findViewById(R.id.tv_delete);

        tvCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtils.copyText(UIUtils.getContext(),id);
                dismiss();
            }
        });

        if(type == 0){
            tvSave.setVisibility(View.GONE);
            tvEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(callBack != null){
                        callBack.onAddressEdit();
                    }
                    dismiss();
                }
            });
            tvDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AddressFileUtil.deleteAddress(context,id);
                    if(callBack != null){
                        callBack.onAddressDelete();
                    }
                    dismiss();
                }
            });
        }else {
            tvEdit.setVisibility(View.GONE);
            tvDelete.setVisibility(View.GONE);
            tvSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, AddressAddActivity.class);
                    intent.putExtra(AddressAddActivity.PARAM_TYPE,"add");
                    intent.putExtra("id",id);
                    context.startActivity(intent);
                    dismiss();
                }
            });
        }

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public void setCallBack(AddressHandleCallBack callBack){
        this.callBack = callBack;
    }

    public interface AddressHandleCallBack{

        void onAddressDelete();

        void onAddressEdit();
    }
}
