package com.vcashorg.vcashwallet.fragment;

import android.content.Intent;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.TxDetailsActivity;
import com.vcashorg.vcashwallet.VcashTokenAddActivity;
import com.vcashorg.vcashwallet.WalletMainActivity;
import com.vcashorg.vcashwallet.WalletTokenDetailsActivity;
import com.vcashorg.vcashwallet.adapter.VcashTokenAdapter;
import com.vcashorg.vcashwallet.api.ServerTxManager;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.base.BaseFragment;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.utils.Args;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.PopUtil;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;

public class TokenListFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener{

    public static final int REQUEST_CODE_ADD_TOKEN = 100;
    public static final int REQUEST_CODE_DETAILS = 101;
    public static final int REQUEST_CODE_SERVER_TX = 102;

    @BindView(R.id.sr_token)
    SwipeRefreshLayout mSrToken;
    @BindView(R.id.rv_token)
    RecyclerView mRvToken;

    VcashTokenAdapter adapter;

    boolean vcRefreshed;
    boolean tokenRefreshed;
    private long lastFetch = 0;

    private PopUtil popUtil;

    @Override
    protected int provideContentViewId() {
        return R.layout.fragment_vcash_token;
    }

    @Override
    public void initView(View rootView) {
        mRvToken.setLayoutManager(new LinearLayoutManager(mActivity));
        RecyclerViewDivider divider = new RecyclerViewDivider(mActivity, LinearLayoutManager.VERTICAL, R.drawable.rv_divider);
        mRvToken.addItemDecoration(divider);

        adapter = new VcashTokenAdapter(R.layout.item_vcash_token,null);
        mRvToken.setAdapter(adapter);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                VcashTokenInfo tokenInfo = (VcashTokenInfo) adapter.getData().get(position);

                Intent intent = new Intent(mActivity,WalletTokenDetailsActivity.class);
                intent.putExtra(Args.TOKEN_TYPE,tokenInfo.TokenId);
                startActivityForResult(intent,REQUEST_CODE_DETAILS);
            }
        });

        mSrToken.setOnRefreshListener(this);

        WalletApi.initTokenInfos();
    }

    @Override
    public void initData() {
        refreshData();
        onRefresh();
    }

    @Override
    protected void loadData() {

    }

    private List<VcashTokenInfo> generateTokenList(){
        List<VcashTokenInfo> tokenInfos = new ArrayList<>();

        VcashTokenInfo vCashToken = WalletApi.getTokenInfo("VCash");
        if(vCashToken != null){
            tokenInfos.add(vCashToken);
        }

        Set balancedToken = WalletApi.getBalancedToken();

        Set addedTokens = WalletApi.getAddedTokens();


        Set totalTokens = new LinkedHashSet();
        totalTokens.addAll(balancedToken);
        totalTokens.addAll(addedTokens);

        Iterator iterator = totalTokens.iterator();

        while (iterator.hasNext()) {
            String token = (String) iterator.next();
            VcashTokenInfo tokenInfo = WalletApi.getTokenInfo(token);

            if(tokenInfo != null){
                tokenInfos.add(tokenInfo);
            }
        }

        return tokenInfos;
    }

    private void showLoading(){
        mSrToken.setRefreshing(true);
    }

    private void hideLoading(){
        mSrToken.setRefreshing(false);
    }

    @OnClick(R.id.iv_menu)
    public void onOpenMenuClick() {
        ((WalletMainActivity)mActivity).openDrawer();
    }

    @OnClick(R.id.tv_add_token)
    public void onAddTokenClick(){
        Intent intent = new Intent(mActivity, VcashTokenAddActivity.class);
        startActivityForResult(intent,REQUEST_CODE_ADD_TOKEN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_TOKEN || requestCode == REQUEST_CODE_DETAILS){
            refreshData();
        }else if(requestCode == REQUEST_CODE_SERVER_TX){
            refreshData();
            showNewTxPop();
        }
    }

    @Override
    public void onRefresh() {
        showLoading();
        ServerTxManager.getInstance().fetchTxStatus(true);
        refreshWalletStatus(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("ppp","TokenListFragment onResume");
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
        Log.i("ppp","TokenListFragment onPause");

        ServerTxManager.getInstance().stopWork();
    }

    private void refreshWalletStatus(final boolean force) {
        if (force || (AppUtil.getCurrentTimeSecs() - lastFetch) >= 60) {
            vcRefreshed = false;
            tokenRefreshed = false;
            WalletApi.updateOutputStatusWithComplete(new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    vcRefreshed = true;
                    checkRefreshEnd();
                }
            });
            WalletApi.updateTokenOutputStatusWithComplete(new WalletCallback() {
                @Override
                public void onCall(boolean yesOrNo, Object data) {
                    tokenRefreshed = true;
                    checkRefreshEnd();
                }
            });
        }
    }

    private void checkRefreshEnd() {
        if (vcRefreshed && tokenRefreshed) {
            hideLoading();
            refreshData();
            lastFetch = AppUtil.getCurrentTimeSecs();
        }
    }

    private void refreshData(){
        adapter.setNewData(generateTokenList());
    }

    public void showNewTxPop() {
        if (popUtil != null && popUtil.isShowing()) return;
        final ServerTransaction recentTx = ServerTxManager.getInstance().getRecentTx();
        if (recentTx != null) {
            popUtil = PopUtil.get(mActivity, recentTx).setConfirmListener(new PopUtil.PopOnCall() {
                @Override
                public void onConfirm() {
                    Intent intent = new Intent(mActivity, TxDetailsActivity.class);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_TYPE, TxDetailsActivity.TYPE_TX_SERVER);
                    intent.putExtra(TxDetailsActivity.PARAM_TX_DATA, recentTx);
                    nv2(intent, REQUEST_CODE_SERVER_TX);
                }
            });
            ServerTxManager.getInstance().addBlackList(recentTx);
            popUtil.show();
        }
    }

}
