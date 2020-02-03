package com.vcashorg.vcashwallet.fragment;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.VcashTokenAddActivity;
import com.vcashorg.vcashwallet.WalletMainActivity;
import com.vcashorg.vcashwallet.WalletTokenDetailsActivity;
import com.vcashorg.vcashwallet.adapter.VcashTokenAdapter;
import com.vcashorg.vcashwallet.base.BaseFragment;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenInfo;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;

public class TokenListFragment extends BaseFragment {

    @BindView(R.id.rv_token)
    RecyclerView mRvToken;

    VcashTokenAdapter adapter;


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
                nv(WalletTokenDetailsActivity.class);
            }
        });

        WalletApi.initTokenInfos();
    }

    @Override
    public void initData() {
        adapter.setNewData(generateTokenList());
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

    @OnClick(R.id.iv_menu)
    public void onOpenMenuClick() {
        ((WalletMainActivity)mActivity).openDrawer();
    }

    @OnClick(R.id.tv_add_token)
    public void onAddTokenClick(){
        Intent intent = new Intent(mActivity, VcashTokenAddActivity.class);
        startActivityForResult(intent,100);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        adapter.setNewData(generateTokenList());
    }
}
