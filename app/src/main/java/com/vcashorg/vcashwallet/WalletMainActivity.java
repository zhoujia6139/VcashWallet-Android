package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.api.ServerTxManager;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.bean.WalletTxEntity;
import com.vcashorg.vcashwallet.utils.DateUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletNoParamCallBack;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.PopUtil;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class WalletMainActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {

    public static final int REQUEST_CODE_SERVER_TX = 101;
    public static final int REQUEST_CODE_TX_LOG = 102;
    public static final int REQUEST_CODE_TX_SEND = 103;
    public static final int REQUEST_CODE_TX_RECEIVE = 104;

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

    private List<WalletTxEntity> mData = new ArrayList<>();

    private PopUtil popUtil;


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

        adapter = new VcashTxAdapter(mData);

        adapter.addHeaderView(headerView);
        adapter.addFooterView(footerView);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                WalletTxEntity entity = (WalletTxEntity) adapter.getData().get(position);
                if(entity.getItemType() == WalletTxEntity.TYPE_SERVER_TX){
                    ServerTransaction serverTx = entity.getServerTxEntity();
                    ServerTxManager.getInstance().addBlackList(serverTx);
                    Intent intent = new Intent(WalletMainActivity.this, TxDetailsActivity.class);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE, TxDetailsActivity.TYPE_TX_SERVER);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, serverTx);
                    nv2(intent, REQUEST_CODE_SERVER_TX);
                }else {
                    VcashTxLog vcashTxLog = entity.getTxLogEntity();
                    Intent intent = new Intent(WalletMainActivity.this, TxDetailsActivity.class);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE, TxDetailsActivity.TYPE_TX_LOG);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, vcashTxLog);
                    nv2(intent, REQUEST_CODE_TX_LOG);
                }
                if (popUtil != null && popUtil.isShowing()){
                    popUtil.dismiss();
                }
            }
        });

        adapter.setEnableLoadMore(false);
        mRvTx.setAdapter(adapter);

        mSrTx.setOnRefreshListener(this);
        walletDrawer = new WalletDrawer(this);

        ServerTxManager.getInstance().addNewTxCallBack(new ServerTxManager.ServerTxCallBack() {
            @Override
            public void onChecked() {
                showNewTxPop();
                refreshData();
            }

            @Override
            public void onForceRefresh() {
                refreshData();
            }
        });
    }

    @Override
    public void initData() {
        String userid = WalletApi.getWalletUserId();
        Log.i("yjq", "userid: " + userid);

        mTvHeight.setText("Height:" + WalletApi.getCurChainHeight());

        //HEIGHT
        WalletApi.addChainHeightListener(new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                mTvHeight.setText("Height:" + WalletApi.getCurChainHeight());
            }
        });

        WalletApi.addTxDataListener(new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                refreshData();
            }
        });

        int mode = getIntent().getIntExtra(PasswordActivity.PARAM_MODE, 1);
        if (mode == PasswordActivity.MODE_RESTORE) {
            showProgressDialog("Recovering");
            WalletApi.checkWalletUtxo(new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    if (yesOrNo) {
                        UIUtils.showToastCenter("Recover Success");
                        refreshData();
                    } else {
                        UIUtils.showToastCenter("Recover Failed");
                    }
                    dismissProgressDialog();
                }
            });
        } else {
            mSrTx.setRefreshing(true);
            refreshData();
        }
    }

    public void showNewTxPop() {
        if (popUtil != null && popUtil.isShowing()) return;
        final ServerTransaction recentTx = ServerTxManager.getInstance().getRecentTx();
        if (recentTx != null) {
            popUtil = PopUtil.get(WalletMainActivity.this,recentTx).setConfirmListener(new PopUtil.PopOnCall() {
                @Override
                public void onConfirm() {
                    Intent intent = new Intent(WalletMainActivity.this, TxDetailsActivity.class);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE, TxDetailsActivity.TYPE_TX_SERVER);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, recentTx);
                    nv2(intent, REQUEST_CODE_SERVER_TX);
                }
            });
            popUtil.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ServerTxManager.getInstance().startWork();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ServerTxManager.getInstance().stopWork();
    }

    private void initHeaderView() {
        headerView = LayoutInflater.from(this).inflate(R.layout.layout_vcash_tx_header, null);
        mTvBalance = headerView.findViewById(R.id.tv_balance);
        mTvAvailable = headerView.findViewById(R.id.tv_available);
        mTvUnconfirmed = headerView.findViewById(R.id.tv_unconfirmed);
    }

    private void initFooterView() {
        footerView = LayoutInflater.from(this).inflate(R.layout.layout_tx_empty_footer, null);
    }

    private void refreshData() {
        WalletApi.updateOutputStatusWithComplete(new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {

                //refreshlist
                List<VcashTxLog> txLogs = deleteDbTxLog(WalletApi.getTransationArr());
                List<ServerTransaction> serverTxs = ServerTxManager.getInstance().getSeverTxList();
                Collections.reverse(txLogs);

                mData.clear();

                for (int i = 0; i < serverTxs.size(); i++) {
                    WalletTxEntity entity = new WalletTxEntity();
                    entity.setItemType(WalletTxEntity.TYPE_SERVER_TX);
                    entity.setServerTxEntity(serverTxs.get(i));
                    mData.add(entity);
                }

                for (int i = 0; i < txLogs.size(); i++) {
                    WalletTxEntity entity = new WalletTxEntity();
                    entity.setItemType(WalletTxEntity.TYPE_TX_LOG);
                    entity.setTxLogEntity(txLogs.get(i));
                    mData.add(entity);
                }

                if (mData != null && mData.size() != 0) {
                    adapter.removeFooterView(footerView);
                }
                adapter.setNewData(mData);
                mSrTx.setRefreshing(false);

                //refreshbalance
                WalletApi.WalletBalanceInfo balanceInfo = WalletApi.getWalletBalanceInfo();
                mTvBalance.setText(WalletApi.nanoToVcashString(balanceInfo.total));
                mTvAvailable.setText(WalletApi.nanoToVcashString(balanceInfo.spendable) + " V");
                mTvUnconfirmed.setText(WalletApi.nanoToVcashString(balanceInfo.unconfirmed) + " V");

                //refreshheight
                mTvHeight.setText("Height:" + WalletApi.getCurChainHeight());
            }
        });

    }

    private List<VcashTxLog> deleteDbTxLog(ArrayList<VcashTxLog> txLogs) {
        List<VcashTxLog> list = new ArrayList<>();
        if(txLogs != null){
            for (VcashTxLog txLog : txLogs) {
                if (!ServerTxManager.getInstance().inServerTxList(txLog.tx_slate_id)) {
                    list.add(txLog);
                }
            }
        }
        return list;
    }

    @Override
    public void onRefresh() {

        ServerTxManager.getInstance().fetchTxStatus(true);
    }

    class VcashTxAdapter extends BaseMultiItemQuickAdapter<WalletTxEntity, BaseViewHolder> {

        /**
         * Same as QuickAdapter#QuickAdapter(Context,int) but with
         * some initialization data.
         *
         * @param data A new list is created out of this one to avoid mutable list
         */
        public VcashTxAdapter(List<WalletTxEntity> data) {
            super(data);
            addItemType(WalletTxEntity.TYPE_SERVER_TX, R.layout.item_vcash_tx);
            addItemType(WalletTxEntity.TYPE_TX_LOG, R.layout.item_vcash_tx);
        }

        @Override
        protected void convert(BaseViewHolder helper, WalletTxEntity item) {
            switch (item.getItemType()) {
                case WalletTxEntity.TYPE_SERVER_TX:
                    ServerTransaction serverTx = item.getServerTxEntity();
                    helper.setImageResource(R.id.iv_tx, serverTx.isSend ? R.drawable.ic_tx_up : R.drawable.ic_tx_down);
                    helper.setText(R.id.tv_tx_id, TextUtils.isEmpty(serverTx.tx_id) ? "" : serverTx.tx_id);
                    helper.setText(R.id.tv_tx_amount, WalletApi.nanoToVcashString(serverTx.slateObj.amount));
                    helper.setText(R.id.tv_tx_state, "waiting for processing");
                    helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.red));
                    TextView txState1 = helper.getView(R.id.tv_tx_state);
                    txState1.setCompoundDrawablesWithIntrinsicBounds(
                            UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                    helper.setText(R.id.tv_tx_time, "Now");
                    break;
                case WalletTxEntity.TYPE_TX_LOG:
                    VcashTxLog txLog = item.getTxLogEntity();

                    String txId = txLog.tx_slate_id;
                    if (!TextUtils.isEmpty(txId)) {
                        helper.setText(R.id.tv_tx_id, txId);
                    } else {
                        helper.setText(R.id.tv_tx_id, "UnReachable");
                    }

                    VcashTxLog.TxLogEntryType txType = txLog.tx_type;
                    switch (txType) {
                        case ConfirmedCoinbase:
                            helper.setText(R.id.tv_tx_id, "CoinBase");
                            helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_down);
                            break;
                        case TxReceived:
                        case TxReceivedCancelled:
                            helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_down);
                            break;
                        case TxSent:
                        case TxSentCancelled:
                            helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_up);
                            break;
                    }


                    long amount = txLog.amount_credited - txLog.amount_debited;
                    helper.setText(R.id.tv_tx_amount, WalletApi.nanoToVcash(amount) + "");
                    helper.setText(R.id.tv_tx_time, DateUtil.formatDateTimeStamp(txLog.create_time));

                    VcashTxLog.TxLogConfirmType confirmState = txLog.confirm_state;
                    TextView txState = helper.getView(R.id.tv_tx_state);
                    helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.A2));
                    helper.setText(R.id.tv_tx_time, DateUtil.formatDateTimeStamp(txLog.create_time));
                    switch (confirmState) {
                        case DefaultState:
                        case LoalConfirmed://waiting confirm
                            if (txType == VcashTxLog.TxLogEntryType.TxSentCancelled || txType == VcashTxLog.TxLogEntryType.TxReceivedCancelled) {
                                helper.setText(R.id.tv_tx_state, "Canceled");
                                txState.setCompoundDrawablesWithIntrinsicBounds(
                                        UIUtils.getResource().getDrawable(R.drawable.ic_tx_canceled), null, null, null);
                            } else {
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
                    break;
            }

            if (helper.getAdapterPosition() == getData().size()) {
                helper.setBackgroundRes(R.id.rl_tx_bg, R.drawable.selector_shadow_2);
            } else {
                helper.setBackgroundRes(R.id.rl_tx_bg, R.drawable.selector_shadow);
            }
        }
    }

    @OnClick(R.id.iv_open_menu)
    public void onOpenMenuClick() {
        walletDrawer.openDrawer();
    }

    @OnClick(R.id.send)
    public void onVcashSendClick() {
        nv2(VcashSendActivity.class, REQUEST_CODE_TX_SEND);
    }

    @OnClick(R.id.receive)
    public void onVcashReceiveClick() {
        nv2(VcashReceiveActivity.class, REQUEST_CODE_TX_RECEIVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        refreshData();
        showNewTxPop();
    }
}
