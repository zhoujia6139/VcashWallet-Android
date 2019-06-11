package com.vcashorg.vcashwallet.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.api.ServerTxManager;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;

public class PopUtil extends PopupWindow {

    private Activity activity;
    private View mPopWindow;
    private TextView tvConfirm, tvCancel;
    private ServerTransaction ServerTx;

    public interface PopOnCall {

        void onConfirm();
    }

    public PopUtil(Activity activity, ServerTransaction serverTx) {
        this.activity = activity;
        this.ServerTx = serverTx;
        mPopWindow = LayoutInflater.from(activity).inflate(R.layout.layout_top_pop, null);
        tvConfirm = mPopWindow.findViewById(R.id.tv_confirm);
        tvCancel = mPopWindow.findViewById(R.id.tv_cancel);
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerTxManager.getInstance().addBlackList(ServerTx);
                if(isShowing()){
                    dismiss();
                }
            }
        });
        setmPopWindow();
    }

    private void setmPopWindow() {
        // 把View添加到PopWindow中
        this.setContentView(mPopWindow);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(dip2px(activity, 124));
        //  设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(false);
        //   设置背景透明
        this.setBackgroundDrawable(new ColorDrawable(0x00000000));
    }

    public static PopUtil get(Activity activity,ServerTransaction serverTx) {
        PopUtil popUtil = new PopUtil(activity,serverTx);
        popUtil.setAnimationStyle(R.style.pop_anim_style);
        return popUtil;
    }

    public PopUtil setConfirmListener(final PopOnCall popOnCall){
        if(popOnCall != null){
            tvConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popOnCall.onConfirm();
                    ServerTxManager.getInstance().addBlackList(ServerTx);
                    if(isShowing()){
                        dismiss();
                    }
                }
            });
        }
        return this;
    }

    public void show(){
        showAtLocation(activity.getWindow().getDecorView(),
                Gravity.TOP, 0, 0);
    }



    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
