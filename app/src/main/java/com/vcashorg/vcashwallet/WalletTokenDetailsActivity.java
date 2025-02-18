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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.api.ServerTxManager;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.base.BaseActivity;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.bean.WalletTxEntity;
import com.vcashorg.vcashwallet.utils.Args;
import com.vcashorg.vcashwallet.utils.DateUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.utils.VCashUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.AbstractVcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenTxLog;
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

public class WalletTokenDetailsActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {

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
    @BindView(R.id.tv_name)
    TextView mTvName;

    VcashTxAdapter adapter;

    View headerView;
    View footerView;

    //header
    TextView mTvBalance;
    TextView mTvAvaliable;
    TextView mTvPending;

    private List<WalletTxEntity> mData = new ArrayList<>();

    private PopUtil popUtil;

    private String tokenType;

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_token_details;
    }


    @Override
    public void initParams() {
        tokenType = getIntent().getStringExtra(Args.TOKEN_TYPE);
        if (!VCashUtil.isVCash(tokenType)) {
            mTvName.setText(WalletApi.getTokenInfo(tokenType).Name);
        }else {
            mTvName.setText("VCash");
        }
    }

    @Override
    public void initView() {

        initHeaderView();
        initFooterView();

        mRvTx.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewDivider divider = new RecyclerViewDivider(this, LinearLayoutManager.VERTICAL, R.drawable.rv_divider);
        mRvTx.addItemDecoration(divider);

        adapter = new VcashTxAdapter(mData);

        adapter.addHeaderView(headerView);
        adapter.addFooterView(footerView);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                WalletTxEntity entity = (WalletTxEntity) adapter.getData().get(position);
                if (entity.getItemType() == WalletTxEntity.TYPE_TX_ONGOING || entity.getItemType() == WalletTxEntity.TYPE_TX_COMPLETE)
                    return;
                if (entity.getItemType() == WalletTxEntity.TYPE_SERVER_TX) {
                    ServerTransaction serverTx = entity.getServerTxEntity();
                    ServerTxManager.getInstance().addBlackList(serverTx);
                    Intent intent = new Intent(WalletTokenDetailsActivity.this, TxDetailsActivity.class);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE, TxDetailsActivity.TYPE_TX_SERVER);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, serverTx);
                    nv2(intent, REQUEST_CODE_SERVER_TX);
                } else if(entity.getItemType() == WalletTxEntity.TYPE_TX_LOG){
                    VcashTxLog vcashTxLog = entity.getTxLogEntity();
                    Intent intent = new Intent(WalletTokenDetailsActivity.this, TxDetailsActivity.class);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE, TxDetailsActivity.TYPE_TX_LOG);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, vcashTxLog);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_ISTOKEN,false);
                    nv2(intent, REQUEST_CODE_TX_LOG);
                }else if(entity.getItemType() == WalletTxEntity.TYPE_TOKEN_TX_LOG){
                    VcashTokenTxLog tokenTxLog = entity.getTokenTxLogEntity();
                    Intent intent = new Intent(WalletTokenDetailsActivity.this, TxDetailsActivity.class);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE, TxDetailsActivity.TYPE_TX_LOG);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, tokenTxLog);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_ISTOKEN,true);
                    nv2(intent, REQUEST_CODE_TX_LOG);
                }
                if (popUtil != null && popUtil.isShowing()) {
                    popUtil.dismiss();
                }
            }
        });

        adapter.setEnableLoadMore(false);
        mRvTx.setAdapter(adapter);

        mSrTx.setOnRefreshListener(this);
    }

    @Override
    public void initData() {

        mTvHeight.setText(UIUtils.getString(R.string.height) + WalletApi.getCurChainHeight());

        //HEIGHT
        WalletApi.addChainHeightListener(new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                mTvHeight.setText(UIUtils.getString(R.string.height) + WalletApi.getCurChainHeight());
            }
        });

        WalletApi.addTxDataListener(new WalletNoParamCallBack() {
            @Override
            public void onCall() {
                refreshData();
            }
        });

        setNewData();
        mSrTx.setRefreshing(true);
        refreshData();
    }

    public void showNewTxPop() {
        if (popUtil != null && popUtil.isShowing()) return;
        final ServerTransaction recentTx = ServerTxManager.getInstance().getRecentTx();
        if (recentTx != null) {
            popUtil = PopUtil.get(this, recentTx).setConfirmListener(new PopUtil.PopOnCall() {
                @Override
                public void onConfirm() {
                    Intent intent = new Intent(WalletTokenDetailsActivity.this, TxDetailsActivity.class);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE, TxDetailsActivity.TYPE_TX_SERVER);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, recentTx);
                    nv2(intent, REQUEST_CODE_SERVER_TX);
                }
            });
            ServerTxManager.getInstance().addBlackList(recentTx);
            popUtil.show();
        }
    }

    private void initHeaderView() {
        headerView = LayoutInflater.from(this).inflate(R.layout.layout_vcash_tx_header, null);
        mTvBalance = headerView.findViewById(R.id.tv_balance);
        mTvAvaliable = headerView.findViewById(R.id.tv_available);
        mTvPending = headerView.findViewById(R.id.tv_pending);
    }

    private void initFooterView() {
        footerView = LayoutInflater.from(this).inflate(R.layout.layout_tx_empty_footer, null);
    }

    private void refreshData() {
        WalletApi.updateTxStatus();
        if (!VCashUtil.isVCash(tokenType)) {
            WalletApi.updateTokenOutputStatusWithComplete(new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    setNewData();
                }
            });
        }else {
            WalletApi.updateOutputStatusWithComplete(new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    setNewData();
                }
            });
        }
    }

    private void setNewData() {
        //refreshlist
        List<ServerTransaction> serverTxs = ServerTxManager.getInstance().getSeverTxList();

        mData.clear();

        List<WalletTxEntity> onGoingList = new ArrayList<>();
        List<WalletTxEntity> completeList = new ArrayList<>();

        for (int i = 0; i < serverTxs.size(); i++) {
            WalletTxEntity entity = new WalletTxEntity();
            entity.setItemType(WalletTxEntity.TYPE_SERVER_TX);
            entity.setServerTxEntity(serverTxs.get(i));
            onGoingList.add(entity);
        }

        if (VCashUtil.isVCash(tokenType)) {
            List<VcashTxLog> txLogs = deleteDbTxLog(WalletApi.getTransationArr());
            Collections.reverse(txLogs);
            for (int i = 0; i < txLogs.size(); i++) {
                WalletTxEntity entity = new WalletTxEntity();
                entity.setItemType(WalletTxEntity.TYPE_TX_LOG);
                entity.setTxLogEntity(txLogs.get(i));
                if (txLogs.get(i).confirm_state == VcashTxLog.TxLogConfirmType.NetConfirmed
                        || txLogs.get(i).tx_type == VcashTxLog.TxLogEntryType.TxSentCancelled
                        || txLogs.get(i).tx_type == VcashTxLog.TxLogEntryType.TxReceivedCancelled) {
                    completeList.add(entity);
                } else {
                    onGoingList.add(entity);
                }
            }
        } else {
            List<VcashTokenTxLog> tokenTxLogs = deleteDbTokenTxLog(WalletApi.getTokenTransationArr(tokenType));
            Collections.reverse(tokenTxLogs);
            for (int i = 0; i < tokenTxLogs.size(); i++) {
                WalletTxEntity entity = new WalletTxEntity();
                entity.setItemType(WalletTxEntity.TYPE_TOKEN_TX_LOG);
                entity.setTokenTxLogEntity(tokenTxLogs.get(i));
                if (tokenTxLogs.get(i).confirm_state == VcashTxLog.TxLogConfirmType.NetConfirmed
                        || tokenTxLogs.get(i).tx_type == VcashTxLog.TxLogEntryType.TxSentCancelled
                        || tokenTxLogs.get(i).tx_type == VcashTxLog.TxLogEntryType.TxReceivedCancelled) {
                    completeList.add(entity);
                } else {
                    onGoingList.add(entity);
                }
            }
        }

        if (onGoingList.size() != 0) {
            WalletTxEntity entity = new WalletTxEntity();
            entity.setItemType(WalletTxEntity.TYPE_TX_ONGOING);
            onGoingList.add(0, entity);
        }

        if (completeList.size() != 0) {
            WalletTxEntity entity = new WalletTxEntity();
            entity.setItemType(WalletTxEntity.TYPE_TX_COMPLETE);
            completeList.add(0, entity);
        }

        mData.addAll(onGoingList);
        mData.addAll(completeList);

        if (mData != null && mData.size() != 0) {
            adapter.removeFooterView(footerView);
        }else {
            WalletTxEntity entity = new WalletTxEntity();
            entity.setItemType(WalletTxEntity.TYPE_TX_COMPLETE);
            mData.add(entity);
        }

        adapter.setNewData(mData);
        mSrTx.setRefreshing(false);

        //refreshbalance
        if (VCashUtil.isVCash(tokenType)) {
            WalletApi.WalletBalanceInfo balanceInfo = WalletApi.getWalletBalanceInfo();
            mTvBalance.setText(WalletApi.nanoToVcashString(balanceInfo.total));
            mTvAvaliable.setText(WalletApi.nanoToVcashString(balanceInfo.spendable));
            mTvPending.setText(WalletApi.nanoToVcashString(balanceInfo.unconfirmed));
        } else {
            WalletApi.WalletBalanceInfo walletTokenBalanceInfo = WalletApi.getWalletTokenBalanceInfo(tokenType);
            mTvBalance.setText(WalletApi.nanoToVcashString(walletTokenBalanceInfo.total));
            mTvAvaliable.setText(WalletApi.nanoToVcashString(walletTokenBalanceInfo.spendable));
            mTvPending.setText(WalletApi.nanoToVcashString(walletTokenBalanceInfo.unconfirmed));
        }

        //refreshheight
        mTvHeight.setText(UIUtils.getString(R.string.height) + WalletApi.getCurChainHeight());
    }

    private List<VcashTxLog> deleteDbTxLog(ArrayList<VcashTxLog> txLogs) {
        List<VcashTxLog> list = new ArrayList<>();
        if (txLogs != null) {
            for (VcashTxLog txLog : txLogs) {
                if (!ServerTxManager.getInstance().inServerTxList(txLog.tx_slate_id)) {
                    list.add(txLog);
                }
            }
        }
        return list;
    }

    private List<VcashTokenTxLog> deleteDbTokenTxLog(ArrayList<VcashTokenTxLog> txLogs) {
        List<VcashTokenTxLog> list = new ArrayList<>();
        if (txLogs != null) {
            for (VcashTokenTxLog txLog : txLogs) {
                if (!ServerTxManager.getInstance().inServerTxList(txLog.tx_slate_id)) {
                    list.add(txLog);
                }
            }
        }
        return list;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i("ppp","TokenDetails onResume");

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
        ServerTxManager.getInstance().startWork();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i("ppp","TokenDetails onPause");

        ServerTxManager.getInstance().stopWork();
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
            addItemType(WalletTxEntity.TYPE_TOKEN_TX_LOG,R.layout.item_vcash_tx);
            addItemType(WalletTxEntity.TYPE_TX_ONGOING, R.layout.layout_vcash_tx_title);
            addItemType(WalletTxEntity.TYPE_TX_COMPLETE, R.layout.layout_vcash_tx_title);
        }

        @Override
        protected void convert(BaseViewHolder helper, WalletTxEntity item) {
            switch (item.getItemType()) {
                case WalletTxEntity.TYPE_SERVER_TX:
                    ServerTransaction serverTx = item.getServerTxEntity();

                    Glide.with(mContext).load(serverTx.isSend ? R.drawable.gif_send : R.drawable.gif_receive).into((ImageView) helper.getView(R.id.iv_tx));
                    // helper.setImageResource(R.id.iv_tx, serverTx.isSend ? R.drawable.ic_tx_up : R.drawable.ic_tx_down);
                    helper.setText(R.id.tv_tx_id, TextUtils.isEmpty(serverTx.tx_id) ? "" : serverTx.tx_id);
                    helper.setText(R.id.tv_tx_amount, WalletApi.nanoToVcashString(serverTx.slateObj.amount));
                    helper.setText(R.id.tv_tx_state, R.string.wait_for_process);
                    helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.red));
                    TextView txState1 = helper.getView(R.id.tv_tx_state);
                    txState1.setCompoundDrawablesWithIntrinsicBounds(
                            UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                    helper.setText(R.id.tv_tx_time, R.string.now);
                    helper.setBackgroundRes(R.id.rl_tx_bg, R.color.orange_light2);
                    break;
                case WalletTxEntity.TYPE_TX_LOG:
                    VcashTxLog txLog = item.getTxLogEntity();

                    String txId = txLog.tx_slate_id;
                    if (TextUtils.isEmpty(txId) || txId.equals("null")) {
                        helper.setText(R.id.tv_tx_id, R.string.unReachable);
                    } else {
                        helper.setText(R.id.tv_tx_id, txId);
                    }

                    long amount = txLog.amount_credited - txLog.amount_debited;

                    VcashTxLog.TxLogEntryType txType = txLog.tx_type;
                    VcashTxLog.TxLogConfirmType confirmState = txLog.confirm_state;

                    helper.setBackgroundRes(R.id.rl_tx_bg, R.drawable.selector_white_grey);

                    txLogTypeState(txType, confirmState, false, amount, helper);

                    TextView txState = helper.getView(R.id.tv_tx_state);
                    helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.A2));
                    helper.setText(R.id.tv_tx_time, DateUtil.formatDateTimeSimple(txLog.create_time));
                    switch (confirmState) {
                        case DefaultState:
                            if (txType == VcashTxLog.TxLogEntryType.TxSent) {
                                helper.setText(R.id.tv_tx_state, "recipient processing now");
                            } else if (txType == VcashTxLog.TxLogEntryType.TxReceived) {
                                helper.setText(R.id.tv_tx_state, "sender processing now");
                            }
                            helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.red));
                            txState.setCompoundDrawablesWithIntrinsicBounds(
                                    UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                            break;
                        case LoalConfirmed://waiting confirm
                            helper.setText(R.id.tv_tx_state, "waiting for confirmation");
                            txState.setCompoundDrawablesWithIntrinsicBounds(
                                    UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                            helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.red));
                            break;
                        case NetConfirmed:
                            helper.setText(R.id.tv_tx_state, R.string.confirmed);
                            txState.setCompoundDrawablesWithIntrinsicBounds(
                                    UIUtils.getResource().getDrawable(R.drawable.ic_tx_confirmed), null, null, null);
                            helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.A2));
                            break;
                    }

                    if (txType == VcashTxLog.TxLogEntryType.TxSentCancelled || txType == VcashTxLog.TxLogEntryType.TxReceivedCancelled) {
                        helper.setText(R.id.tv_tx_state, R.string.canceled);
                        txState.setCompoundDrawablesWithIntrinsicBounds(
                                UIUtils.getResource().getDrawable(R.drawable.ic_tx_canceled), null, null, null);
                        helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.A2));
                    }

                    break;
                case WalletTxEntity.TYPE_TOKEN_TX_LOG:
                    VcashTokenTxLog tokenTxLogEntity = item.getTokenTxLogEntity();

                    String tokenTxId = tokenTxLogEntity.tx_slate_id;
                    if (TextUtils.isEmpty(tokenTxId) || tokenTxId.equals("null")) {
                        helper.setText(R.id.tv_tx_id, R.string.unReachable);
                    } else {
                        helper.setText(R.id.tv_tx_id, tokenTxId);
                    }

                    long tokenAmount = tokenTxLogEntity.token_amount_credited - tokenTxLogEntity.token_amount_debited;

                    VcashTxLog.TxLogEntryType tokenTxType = tokenTxLogEntity.tx_type;
                    VcashTxLog.TxLogConfirmType tokenConfirmState = tokenTxLogEntity.confirm_state;

                    helper.setBackgroundRes(R.id.rl_tx_bg, R.drawable.selector_white_grey);

                    txLogTypeState(tokenTxType, tokenConfirmState, true, tokenAmount, helper);

                    TextView tokenTxState = helper.getView(R.id.tv_tx_state);
                    helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.A2));
                    helper.setText(R.id.tv_tx_time, DateUtil.formatDateTimeSimple(tokenTxLogEntity.create_time));
                    switch (tokenConfirmState) {
                        case DefaultState:
                            if (tokenTxType == VcashTxLog.TxLogEntryType.TxSent) {
                                helper.setText(R.id.tv_tx_state, "recipient processing now");
                            } else if (tokenTxType == VcashTxLog.TxLogEntryType.TxReceived) {
                                helper.setText(R.id.tv_tx_state, "sender processing now");
                            }
                            helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.red));
                            tokenTxState.setCompoundDrawablesWithIntrinsicBounds(
                                    UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                            break;
                        case LoalConfirmed://waiting confirm
                            helper.setText(R.id.tv_tx_state, "waiting for confirmation");
                            tokenTxState.setCompoundDrawablesWithIntrinsicBounds(
                                    UIUtils.getResource().getDrawable(R.drawable.ic_tx_ongoing), null, null, null);
                            helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.red));
                            break;
                        case NetConfirmed:
                            helper.setText(R.id.tv_tx_state, R.string.confirmed);
                            tokenTxState.setCompoundDrawablesWithIntrinsicBounds(
                                    UIUtils.getResource().getDrawable(R.drawable.ic_tx_confirmed), null, null, null);
                            helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.A2));
                            break;
                    }

                    if (tokenTxType == VcashTxLog.TxLogEntryType.TxSentCancelled || tokenTxType == VcashTxLog.TxLogEntryType.TxReceivedCancelled) {
                        helper.setText(R.id.tv_tx_state, R.string.canceled);
                        tokenTxState.setCompoundDrawablesWithIntrinsicBounds(
                                UIUtils.getResource().getDrawable(R.drawable.ic_tx_canceled), null, null, null);
                        helper.setTextColor(R.id.tv_tx_state, UIUtils.getColor(R.color.A2));
                    }
                    break;
                case WalletTxEntity.TYPE_TX_ONGOING:
                    helper.setText(R.id.tv_title, R.string.ongoing_tx);
                    break;
                case WalletTxEntity.TYPE_TX_COMPLETE:
                    helper.setText(R.id.tv_title, R.string.complete_tx);
                    break;
            }

        }

        private void txLogTypeState(AbstractVcashTxLog.TxLogEntryType txLogEntryType, VcashTxLog.TxLogConfirmType confirmState, boolean isToken, long amount, BaseViewHolder helper) {
            switch (txLogEntryType) {
                case ConfirmedCoinbaseOrTokenIssue:
                    if (isToken) {
                        helper.setText(R.id.tv_tx_id, R.string.tokenissue);
                    } else {
                        helper.setText(R.id.tv_tx_id, R.string.coinbase);
                    }
                case TxReceived:
                    if (confirmState == VcashTxLog.TxLogConfirmType.NetConfirmed) {
                        helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_down);
                    } else {
                        helper.setBackgroundRes(R.id.rl_tx_bg, R.color.orange_light2);
                        Glide.with(mContext).load(R.drawable.gif_receive).into((ImageView) helper.getView(R.id.iv_tx));
                    }
                    helper.setText(R.id.tv_tx_amount, "+" + WalletApi.nanoToVcashString(amount));
                    break;
                case TxReceivedCancelled:
                    helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_down);
                    helper.setText(R.id.tv_tx_amount, "+" + WalletApi.nanoToVcashString(amount));
                    break;
                case TxSent:
                    if (confirmState == VcashTxLog.TxLogConfirmType.NetConfirmed) {
                        helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_up);
                    } else {
                        helper.setBackgroundRes(R.id.rl_tx_bg, R.color.orange_light2);
                        Glide.with(mContext).load(R.drawable.gif_send).into((ImageView) helper.getView(R.id.iv_tx));
                    }
                    helper.setText(R.id.tv_tx_amount, WalletApi.nanoToVcashString(amount));
                    break;
                case TxSentCancelled:
                    helper.setImageResource(R.id.iv_tx, R.drawable.ic_tx_up);
                    helper.setText(R.id.tv_tx_amount, WalletApi.nanoToVcashString(amount));
                    break;
            }
        }

    }

    @OnClick(R.id.iv_back)
    public void onBackClick() {
        onBackPressed();
    }

    @OnClick(R.id.send)
    public void onVcashSendClick() {
        Intent intent = new Intent(this,VcashSendActivity.class);
        intent.putExtra(Args.TOKEN_TYPE,tokenType);
        startActivityForResult(intent,REQUEST_CODE_TX_SEND);
    }

    @OnClick(R.id.receive)
    public void onVcashReceiveClick() {
        nv2(VcashReceiveActivity.class, REQUEST_CODE_TX_RECEIVE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        refreshData();
        showNewTxPop();
    }
}
