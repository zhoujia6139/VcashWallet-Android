package com.vcashorg.vcashwallet.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;

public class WordAutoCompleteTextView extends AppCompatAutoCompleteTextView {

    public WordAutoCompleteTextView(Context context) {
        super(context);
    }

    public WordAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WordAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void showDropDown() {
        if (mListener != null) {
            if (mListener.beforeShow()) {
                WordAutoCompleteTextView.super.showDropDown();
            }
        }
    }


    private OnShowWindowListener mListener;

    public void setOnShowWindowListener(OnShowWindowListener l) {
        mListener = l;
    }

    public interface OnShowWindowListener {
        // void afterShow();
        boolean beforeShow();
    }
}
