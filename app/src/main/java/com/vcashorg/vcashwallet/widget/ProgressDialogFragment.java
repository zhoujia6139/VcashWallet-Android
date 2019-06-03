/*
 * Created by wulin on 18-4-25 下午6:34.
 * Copyright (c) 2018 Blockin. All Rights Reserved.
 * Last modified 18-4-25 下午6:34.
 */

package com.vcashorg.vcashwallet.widget;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {
    private String mText;
    private DialogInterface.OnCancelListener mOnCancelListener = null;
    private boolean mIsDismissOnPause = false;

    public ProgressDialogFragment() {
    }

    public ProgressDialogFragment setTitle(String text) {
        this.mText = text;
        return this;
    }

    public ProgressDialogFragment setOnCancelListener(DialogInterface.OnCancelListener listener) {
        this.mOnCancelListener = listener;
        return this;
    }

    public ProgressDialogFragment setDissmissOnPause(boolean dissmissOnPause) {
        this.mIsDismissOnPause = dissmissOnPause;
        return this;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(this.getActivity());
        dialog.setProgressStyle(android.R.style.Widget_Material_Light_ProgressBar_Small);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.setMessage(this.mText);
        dialog.setIndeterminate(true);
        if (this.mOnCancelListener != null) {
            this.setCancelable(true);
            dialog.setOnCancelListener(this.mOnCancelListener);
        } else {
            this.setCancelable(this.isCancelable());
        }

        return dialog;
    }

    public void onPause() {
        super.onPause();
        if (this.mIsDismissOnPause) {
            this.dismissAllowingStateLoss();
        }
    }
}
