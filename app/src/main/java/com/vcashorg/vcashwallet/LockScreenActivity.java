package com.vcashorg.vcashwallet;

import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckedTextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.TimeOutUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.widget.LinerLineItemDecoration;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class LockScreenActivity extends ToolBarActivity {

    @BindView(R.id.rv_lock_screen)
    RecyclerView mRv;

    @Override
    protected void initToolBar() {
        setToolBarTitle("Lock Screen");
        setToolBarBgColor(R.color.white);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_lock_screen;
    }

    @Override
    public void initView() {

        final List<LockScreenItem> data = new ArrayList<>();
        int type = TimeOutUtil.getInstance().getTimeOutType();

        data.add(new LockScreenItem(type == TimeOutUtil.TIME_OUT_NEVER,TimeOutUtil.TIME_OUT_NEVER,"Never"));
        data.add(new LockScreenItem(type == TimeOutUtil.TIME_OUT_30SEC,TimeOutUtil.TIME_OUT_30SEC,"After 30 seconds"));
        data.add(new LockScreenItem(type == TimeOutUtil.TIME_OUT_1MIN,TimeOutUtil.TIME_OUT_1MIN,"After 1 minute"));
        data.add(new LockScreenItem(type == TimeOutUtil.TIME_OUT_3MIN,TimeOutUtil.TIME_OUT_3MIN,"After 3 minute"));

        mRv.setLayoutManager(new LinearLayoutManager(this));
        mRv.addItemDecoration(new LinerLineItemDecoration(1, 1, UIUtils.getColor(R.color.grey_4)));
        LockScreenAdapter adapter = new LockScreenAdapter(R.layout.item_time_out, data);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                for (int i = 0; i < data.size(); i++) {
                    LockScreenItem item = data.get(i);
                    if (i == position) {
                        item.checked = true;
                        adapter.notifyDataSetChanged();
                        TimeOutUtil.getInstance().updateTimeOutType(item.type);
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        item.checked = false;
                    }
                }
            }
        });

        mRv.setAdapter(adapter);
    }

    class LockScreenAdapter extends BaseQuickAdapter<LockScreenItem, BaseViewHolder> {

        public LockScreenAdapter(int layoutResId, @Nullable List<LockScreenItem> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, LockScreenItem item) {
            CheckedTextView tv = helper.getView(R.id.tv_check);
            helper.setText(R.id.tv_check, item.value);
            if (item.checked) {
                tv.setChecked(true);
                tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_choosed, 0);
            } else {
                tv.setChecked(false);
                tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    }


    class LockScreenItem {
        boolean checked;

        int type;

        String value;

        public LockScreenItem(boolean checked, int type, String value) {
            this.checked = checked;
            this.type = type;
            this.value = value;
        }
    }
}
