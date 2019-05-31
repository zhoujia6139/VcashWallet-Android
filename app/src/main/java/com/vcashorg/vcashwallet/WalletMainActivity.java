package com.vcashorg.vcashwallet;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.bean.VcashTx;
import com.vcashorg.vcashwallet.utils.DateUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.PopUtil;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class WalletMainActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener{

    @BindView(R.id.rv_tx)
    RecyclerView mRvTx;
    @BindView(R.id.sr_tx)
    SwipeRefreshLayout mSrTx;
    @BindView(R.id.tv_height)
    TextView mTvHeight;

    WalletDrawer walletDrawer;
    VcashTxAdapter adapter;

    View headerView;
    View footerView;

    //header
    TextView mTvBalance;
    TextView mTvAvailable;
    TextView mTvUnconfirmed;

    private List<VcashTxLog> mDatas = new ArrayList<>();


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

        adapter.setEnableLoadMore(false);
        mRvTx.setAdapter(adapter);

        mSrTx.setOnRefreshListener(this);
        walletDrawer = new WalletDrawer(this);

        mTvHeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopUtil.get(WalletMainActivity.this).setConfirmListener(new PopUtil.PopOnCall() {
                    @Override
                    public void onConfirm() {

                    }
                }).show();
            }
        });

    }

    @Override
    public void initData() {
        String userid = WalletApi.getWalletUserId();
        Log.i("yjq","userid: " + userid);
        refreshData();
    }

    private void initHeaderView() {
        headerView = LayoutInflater.from(this).inflate(R.layout.layout_vcash_tx_header, null);
        mTvBalance = headerView.findViewById(R.id.tv_balance);
        mTvAvailable = headerView.findViewById(R.id.tv_available);
        mTvUnconfirmed = headerView.findViewById(R.id.tv_unconfirmed);
    }

    private void initFooterView(){
        footerView =  LayoutInflater.from(this).inflate(R.layout.layout_tx_empty_footer, null);;
    }

    private void refreshData(){
        WalletApi.updateOutputStatusWithComplete(new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                mDatas = WalletApi.getTransationArr();
                if(mDatas== null || mDatas.size() == 0){
                    adapter.getFooterLayout().setVisibility(View.VISIBLE);
                }else {
                    adapter.getFooterLayout().setVisibility(View.GONE);
                }
                adapter.setNewData(mDatas);
            }
        });

        WalletApi.WalletBalanceInfo balanceInfo = WalletApi.getWalletBalanceInfo();
        mTvBalance.setText(WalletApi.nanoToVcash(balanceInfo.locked) + "");
        mTvAvailable.setText(WalletApi.nanoToVcash(balanceInfo.spendable) + "");
        mTvUnconfirmed.setText(WalletApi.nanoToVcash(balanceInfo.unconfirmed) + "");

        mTvHeight.setText("Height:" + WalletApi.getCurChainHeight());
    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    class VcashTxAdapter extends BaseQuickAdapter<VcashTxLog, BaseViewHolder> {

        public VcashTxAdapter(int layoutResId, @Nullable List<VcashTxLog> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, VcashTxLog item) {
            VcashTxLog.TxLogEntryType txType = item.tx_type;
            switch (txType){
                case ConfirmedCoinbase:
                case TxReceived:
                case TxReceivedCancelled:
                    helper.setImageResource(R.id.iv_tx,R.drawable.ic_tx_down);
                    break;
                case TxSent:
                case TxSentCancelled:
                    helper.setImageResource(R.id.iv_tx,R.drawable.ic_tx_up);
                    break;
            }

            String txId = item.tx_slate_id;
            if(!TextUtils.isEmpty(txId)){
                helper.setText(R.id.tv_tx_id,txId);
            }else {
                helper.setText(R.id.tv_tx_id,"");
            }

            long amount = item.amount_credited - item.amount_debited;
            helper.setText(R.id.tv_tx_amount,WalletApi.nanoToVcash(amount) + "");
            helper.setText(R.id.tv_tx_time, DateUtil.formatDateTimeStamp(item.create_time));

            VcashTxLog.TxLogConfirmType confirmState = item.confirm_state;
            TextView txState = helper.getView(R.id.tv_tx_state);
            switch (confirmState){
                case DefaultState:
                case LoalConfirmed://waiting confirm
                    if(txType == VcashTxLog.TxLogEntryType.TxSentCancelled || txType == VcashTxLog.TxLogEntryType.TxReceivedCancelled){
                        helper.setText(R.id.tv_tx_state, "Canceled");
                        txState.setCompoundDrawablesWithIntrinsicBounds(
                                UIUtils.getResource().getDrawable(R.drawable.ic_tx_canceled), null, null, null);
                    }else {
                        helper.setText(R.id.tv_tx_state, "Ongoing");
                        txState.setCompoundDrawablesWithIntrinsicBounds(
                                UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                    }
                    break;
                case NetConfirmed:
                    helper.setText(R.id.tv_tx_state, "Confirmed");
                    txState.setCompoundDrawablesWithIntrinsicBounds(
                            UIUtils.getResource().getDrawable(R.drawable.ic_tx_confirmed), null, null, null);
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
