package com.vcashorg.vcashwallet.widget;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class LinerLineItemDecoration extends RecyclerView.ItemDecoration {

    private int leftRight;
    private int topBottom;

    protected Drawable mDivider;

    public LinerLineItemDecoration(int leftRight, int topBottom, int color) {
        this.leftRight = leftRight;
        this.topBottom = topBottom;
        if (color != 0) {
            mDivider = new ColorDrawable(color);
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
        //没有子view或者没有没有颜色直接return
        if (mDivider == null || layoutManager.getChildCount() == 0) {
            return;
        }
        int left;
        int right;
        int top;
        int bottom;
        final int childCount = parent.getChildCount();
        if (layoutManager.getOrientation() == GridLayoutManager.VERTICAL) {
            for (int i = 0; i < childCount - 1; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                //将有颜色的分割线处于中间位置
                final float center = (layoutManager.getTopDecorationHeight(child) + 1 - topBottom) / 2;
                //计算下边的
                left = layoutManager.getLeftDecorationWidth(child);
                right = parent.getWidth() - layoutManager.getLeftDecorationWidth(child);
                top = (int) (child.getBottom() + center);
                bottom = top + topBottom;
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        } else {
            for (int i = 0; i < childCount - 1; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                //将有颜色的分割线处于中间位置
                final float center = (layoutManager.getLeftDecorationWidth(child) + 1 - leftRight) / 2;
                //计算右边的
                left = (int) (child.getRight() + center);
                right = left + leftRight;
                top = layoutManager.getTopDecorationHeight(child);
                bottom = parent.getHeight() - layoutManager.getTopDecorationHeight(child);
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
        //竖直方向的
        if (layoutManager.getOrientation() == LinearLayoutManager.VERTICAL) {
            //最后一项需要 bottom
            if (parent.getChildAdapterPosition(view) == layoutManager.getItemCount() - 1) {
                outRect.bottom = topBottom;
            }
            outRect.top = topBottom;
            outRect.left = leftRight;
            outRect.right = leftRight;
        } else {
            //最后一项需要right
            if (parent.getChildAdapterPosition(view) == layoutManager.getItemCount() - 1) {
                outRect.right = leftRight;
            }
            outRect.top = topBottom;
            outRect.left = leftRight;
            outRect.bottom = topBottom;
        }
    }
}
