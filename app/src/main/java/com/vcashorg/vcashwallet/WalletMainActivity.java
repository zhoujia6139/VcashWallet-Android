package com.vcashorg.vcashwallet;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.bean.VcashTx;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class WalletMainActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener,BaseQuickAdapter.RequestLoadMoreListener{

    @BindView(R.id.rv_tx)
    RecyclerView mRvTx;
    @BindView(R.id.sr_tx)
    SwipeRefreshLayout mSrTx;

    WalletDrawer walletDrawer;
    VcashTxAdapter adapter;

    View headerView;
    View footerView;

    private List<VcashTx> mDatas = new ArrayList<>();


    @Override
    protected int provideContentViewId() {
        return R.layout.activity_drawer;
    }

    @Override
    public void initView() {
        initHeaderView();
        initFooterView();

        mRvTx.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewDivider divider = new RecyclerViewDivider(this, LinearLayoutManager.VERTICAL, R.drawable.rv_divider);
        divider.hideFirstDecoration();
        divider.hideLastDecoration();
        divider.setMarginLeft(12);
        divider.setMarginRight(12);
        mRvTx.addItemDecoration(divider);

        adapter = new VcashTxAdapter(R.layout.item_vcash_tx, mDatas);

        adapter.addHeaderView(headerView);
        adapter.addFooterView(footerView);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                nv(TxDetailsActivity.class);
            }
        });

        adapter.setOnLoadMoreListener(this,mRvTx);
        adapter.setEnableLoadMore(false);
        mRvTx.setAdapter(adapter);

        mSrTx.setOnRefreshListener(this);
        walletDrawer = new WalletDrawer(this);
    }

    private void initHeaderView() {
        headerView = LayoutInflater.from(this).inflate(R.layout.layout_vcash_tx_header, null);
    }

    private void initFooterView(){
        footerView =  LayoutInflater.from(this).inflate(R.layout.layout_tx_empty_footer, null);;
    }

    private List<VcashTx> randomData() {
        List<VcashTx> data = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            VcashTx vcashTx = new VcashTx();
            vcashTx.type = i % 3;
            data.add(vcashTx);
        }

        return data;
    }

    private void loadData(boolean refresh){
        if(refresh){
            adapter.removeFooterView(footerView);
            adapter.setNewData(randomData());
        }else {
            adapter.addData(randomData());
            adapter.loadMoreEnd();
        }
        mSrTx.setRefreshing(false);
        adapter.setEnableLoadMore(true);
    }

    @Override
    public void onRefresh() {
        loadData(true);
    }

    @Override
    public void onLoadMoreRequested() {
        loadData(false);
    }

    class VcashTxAdapter extends BaseQuickAdapter<VcashTx, BaseViewHolder> {

        public VcashTxAdapter(int layoutResId, @Nullable List<VcashTx> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, VcashTx item) {
            TextView txState = helper.getView(R.id.tv_tx_state);
            switch (item.type) {
                case 0:
                    helper.setText(R.id.tv_tx_state, "Confirmed");
                    Drawable d1 = UIUtils.getResource().getDrawable(R.drawable.ic_tx_confirmed);
                    txState.setCompoundDrawablesWithIntrinsicBounds(d1, null, null, null);
                    break;
                case 1:
                    helper.setText(R.id.tv_tx_state, "Ongoing");
                    Drawable d2 = UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing);
                    txState.setCompoundDrawablesWithIntrinsicBounds(d2, null, null, null);
                    break;
                case 2:
                    helper.setText(R.id.tv_tx_state, "Canceled");
                    Drawable d3 = UIUtils.getResource().getDrawable(R.drawable.ic_tx_canceled);
                    txState.setCompoundDrawablesWithIntrinsicBounds(d3, null, null, null);
                    break;
            }

            if(helper.getAdapterPosition() == getData().size()){
                helper.setBackgroundRes(R.id.rl_tx_bg,R.drawable.selector_shadow_2);
            }else {
                helper.setBackgroundRes(R.id.rl_tx_bg,R.drawable.selector_shadow);
            }
        }
    }

    @OnClick(R.id.iv_open_menu)
    public void onOpenMenuClick() {
        walletDrawer.openDrawer();
    }

    @OnClick(R.id.send)
    public void onVcashSendClick() {
        nv(VcashSendActivity.class);
    }

    @OnClick(R.id.receive)
    public void onVcashReceiveClick() {
        nv(VcashReceiveActivity.class);
    }


}
