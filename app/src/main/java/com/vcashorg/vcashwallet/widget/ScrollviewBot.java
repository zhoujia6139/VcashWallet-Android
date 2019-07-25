package com.vcashorg.vcashwallet.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class ScrollviewBot extends ScrollView {

    private int _calCount;
    boolean intercept = true;

    public ScrollviewBot(Context context) {
        super(context);
    }

    public ScrollviewBot(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollviewBot(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (intercept) {
            return super.onInterceptTouchEvent(ev);
        } else {
            return false;
        }

    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        View view = this.getChildAt(0);
        if (this.getHeight() + this.getScrollY() == view.getHeight()) {
            _calCount++;
            if (_calCount == 1) {
                intercept = false;
            }
        } else {
            intercept = true;
            _calCount = 0;
        }
    }
}
