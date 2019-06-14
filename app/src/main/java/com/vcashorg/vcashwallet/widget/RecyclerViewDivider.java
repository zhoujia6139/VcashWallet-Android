package com.vcashorg.vcashwallet.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.vcashorg.vcashwallet.utils.UIUtils;

public class RecyclerViewDivider extends RecyclerView.ItemDecoration{

    private Paint mPaint;
    //分割线
    private Drawable mDivider;
    //分割线高度，默认是1px
    private int mDividerHeight = 1;
    //列表的方向：LinearLayoutManager.VERTICAL或LinearLayoutManager.HORIZONTAL
    private int mOrientation;

    private int mMarginLeft = 0;
    private int mMarginRight = 0;

    private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

    private boolean hideFirst = false;
    private boolean hideLast = false;

    /**
     * 自定义分割线
     *
     * @param context
     * @param orientation 列表方向
     * @param drawableId  分割线图片
     */
    public RecyclerViewDivider(Context context, int orientation, int drawableId) {
        if (orientation != LinearLayoutManager.VERTICAL && orientation != LinearLayoutManager.HORIZONTAL) {
            throw new IllegalArgumentException("Illegal Argument");
        }
        mOrientation = orientation;

        mDivider = ContextCompat.getDrawable(context, drawableId);
        mDividerHeight = mDivider.getIntrinsicHeight();
    }

    /**
     * 自定义分割线
     *
     * @param context
     * @param orientation 列表方向
     * @param drawableId  分割线图片
     */
    public RecyclerViewDivider(Context context, int orientation, int drawableId, int margin) {
        if (orientation != LinearLayoutManager.VERTICAL && orientation != LinearLayoutManager.HORIZONTAL) {
            throw new IllegalArgumentException("Illegal Argument");
        }
        mOrientation = orientation;
        mMarginLeft = margin;
        mDivider = ContextCompat.getDrawable(context, drawableId);
        mDividerHeight = mDivider.getIntrinsicHeight();
    }

    /**
     * 自定义分割线
     *
     * @param context
     * @param orientation 列表方向
     * @param drawableId  分割线图片
     */
    public RecyclerViewDivider(Context context, int orientation, int drawableId, boolean hideFirst) {
        if (orientation != LinearLayoutManager.VERTICAL && orientation != LinearLayoutManager.HORIZONTAL) {
            throw new IllegalArgumentException("Illegal Argument");
        }
        mOrientation = orientation;
        this.hideFirst = hideFirst;
        mDivider = ContextCompat.getDrawable(context, drawableId);
        mDividerHeight = mDivider.getIntrinsicHeight();
    }

    public void hideFirstDecoration(){
        hideFirst = true;
    }

    public void hideLastDecoration(){
        hideLast = true;
    }

    public void setMarginLeft(int marginLeft){
        mMarginLeft = marginLeft;
    }

    public void setMarginRight(int marginRight){
        mMarginRight = marginRight;
    }



    //获取分割线尺寸
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

        if (mOrientation == LinearLayoutManager.VERTICAL) {
            outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
        } else {
            outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
        }

        outRect.set(0, 0, 0, mDividerHeight);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        if(mOrientation== LinearLayoutManager.VERTICAL){
            drawVerticalLine(c,parent);
        }else{
            drawHorizontalLine(c,parent);
        }
    }

    //为横方向item, 画分割线
    private void drawHorizontalLine(Canvas canvas, RecyclerView parent) {
        final int top = parent.getPaddingTop();
        final int bottom = parent.getMeasuredHeight() - parent.getPaddingBottom();
        final int childSize = parent.getChildCount();
        for (int i = 0; i < childSize; i++) {
            final View child = parent.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int left = child.getRight() + layoutParams.rightMargin;
            final int right = left + mDividerHeight;
            if (mDivider != null) {
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(canvas);
            }
            if (mPaint != null) {
                canvas.drawRect(left, top, right, bottom, mPaint);
            }
        }
    }

    //为竖方向item, 画分割线
    private void drawVerticalLine(Canvas canvas, RecyclerView parent) {
        final int left = parent.getPaddingLeft() + UIUtils.dip2Px(mMarginLeft);
        final int right = parent.getMeasuredWidth() - parent.getPaddingRight() - UIUtils.dip2Px(mMarginRight);
        final int childSize = parent.getChildCount();
        for (int i = 0; i < childSize; i++) {
            if(hideFirst && i == 0){
                continue;
            }
            if(hideLast && i == childSize - 1){
                continue;
            }
            final View child = parent.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int top = child.getBottom() + layoutParams.bottomMargin;
            final int bottom = top + mDividerHeight;
            if (mDivider != null) {
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(canvas);
            }
            if (mPaint != null) {
                canvas.drawRect(left, top, right, bottom, mPaint);
            }
        }
    }
}
