package com.vcashorg.vcashwallet.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.vcashorg.vcashwallet.R;

public abstract class BottomDialogView extends Dialog {

    protected boolean iscancelable;//控制点击dialog外部是否dismiss
    protected int layoutId;
    protected View view;
    protected Context context;

    public BottomDialogView(@NonNull Context context, boolean isCancelable) {
        super(context, R.style.BottomDialogStyle);

        this.context = context;
        this.iscancelable = isCancelable;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layoutId);//这行一定要写在前面
        setCancelable(iscancelable);//点击外部不可dismiss
        Window window = this.getWindow();
        window.setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);

        initView();
    }

    protected abstract void initView();
}
