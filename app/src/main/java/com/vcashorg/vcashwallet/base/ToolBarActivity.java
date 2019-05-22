package com.vcashorg.vcashwallet.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;


import com.vcashorg.vcashwallet.R;

import butterknife.BindView;

public abstract class ToolBarActivity extends BaseActivity{

    @BindView(R.id.tv_title)
    TextView mTvTitle;
    @BindView(R.id.tv_right)
    TextView mTvRight;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showBackArrow();
        initToolBar();
    }

    protected abstract void initToolBar();

    private void setBackIcon(){
        if (null != getToolbar() && isShowBacking()) {
//            getToolbar().setNavigationIcon(android.R.drawable.ar);
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }
    }

    /**
     * @return TextView in center
     */
    public TextView getToolbarTitle() {
        return mTvTitle;
    }

    /**
     * @return TextView on the right
     */
    public TextView getSubTitle() {
        return mTvRight;
    }

    /**
     * set Title
     * @param title
     */
    public void setToolBarTitle(CharSequence title) {
        if (mTvTitle != null) {
            mTvTitle.setText(title);
        } else {
            getToolbar().setTitle(title);
            setSupportActionBar(getToolbar());
        }
    }

    /**
     * the toolbar of this Activity
     * @return support.v7.widget.Toolbar.
     */
    public Toolbar getToolbar() {
        return mToolbar;
    }

    protected void showBackArrow(){
        if(isShowBacking()){
            setSupportActionBar(mToolbar);
            ActionBar actionBar = getSupportActionBar();

            if(actionBar != null){
                actionBar.setTitle("");
                actionBar.setDisplayHomeAsUpEnabled(true);
                getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
            }

        }
    }

    protected void setRightDrawable(int drawable){
        if(mTvRight != null){
            Drawable leftDrawable = getResources().getDrawable(drawable);
            leftDrawable.setBounds(0, 0, leftDrawable.getMinimumWidth(), leftDrawable.getMinimumHeight());
            mTvRight.setCompoundDrawables(leftDrawable, null, null, null);
        }
    }

    /**
     * is show back icon,default is none。
     * you can override the function in subclass and return to true show the back icon
     * @return
     */
    protected boolean isShowBacking() {
        return true;
    }
}
