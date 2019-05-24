package com.vcashorg.vcashwallet;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
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
import com.vcashorg.vcashwallet.widget.LinerLineItemDecoration;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class WalletMainActivity extends BaseActivity {

    @BindView(R.id.rv_tx)
    RecyclerView mRvTx;

    WalletDrawer walletDrawer;

    View headerView;


    @Override
    protected int provideContentViewId() {
        return R.layout.activity_drawer;
    }

    @Override
    public void initView() {
        initHeaderView();

        mRvTx.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewDivider divider = new RecyclerViewDivider(this, LinearLayoutManager.VERTICAL, R.drawable.rv_divider);
        divider.hideFirstDecoration();
        divider.hideLastDecoration();
        divider.setMarginLeft(12);
        divider.setMarginRight(12);
        mRvTx.addItemDecoration(divider);

        List<VcashTx> data = randomData();

        VcashTxAdapter adapter = new VcashTxAdapter(R.layout.item_vcash_tx, data);

        adapter.addHeaderView(headerView);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                nv(TxDetailsActivity.class);
            }
        });

        mRvTx.setAdapter(adapter);

        walletDrawer = new WalletDrawer(this);
    }

    private void initHeaderView() {
        headerView = LayoutInflater.from(this).inflate(R.layout.layout_vcash_tx_header, null);
    }

    private List<VcashTx> randomData() {
        List<VcashTx> data = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            VcashTx vcashTx = new VcashTx();
            vcashTx.type = i % 3;
            data.add(vcashTx);
        }

        return data;
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
