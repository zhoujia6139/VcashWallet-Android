package com.vcashorg.vcashwallet;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.vcashorg.vcashwallet.adapter.VcashTokenAdapter;
import com.vcashorg.vcashwallet.adapter.VcashTokenAddAdapter;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletNoParamCallBack;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import butterknife.BindView;

public class VcashTokenAddActivity extends ToolBarActivity {

    @BindView(R.id.rv_token_add)
    RecyclerView mRvTokenAdd;

    VcashTokenAddAdapter addAdapter;

    @Override
    protected void initToolBar() {
        setToolBarTitle("Token List");
    }

    @Override
    public void initView() {
        mRvTokenAdd.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewDivider divider = new RecyclerViewDivider(this, LinearLayoutManager.VERTICAL, R.drawable.rv_divider);
        mRvTokenAdd.addItemDecoration(divider);

        addAdapter = new VcashTokenAddAdapter(R.layout.item_vcash_token_add,null);
        mRvTokenAdd.setAdapter(addAdapter);
    }

    @Override
    public void initData() {
        Set addedTokens = WalletApi.getAllTokens();
        if(addedTokens.size() == 0){
            showProgressDialog(UIUtils.getString(R.string.wait));
            WalletApi.updateTokenInfos(new WalletNoParamCallBack() {
                @Override
                public void onCall() {
                    if(!isFinishing()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dismissProgressDialog();
                                addAdapter.setNewData(generateAllToken());
                            }
                        });
                    }
                }
            });
        }else {
            addAdapter.setNewData(generateAllToken());
        }
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vcash_token_add;
    }


    private List<VcashTokenInfo> generateAllToken(){
        List<VcashTokenInfo> tokenInfos = new ArrayList<>();

        Set allTokens = WalletApi.getAllTokens();
        Iterator iterator = allTokens.iterator();

        while (iterator.hasNext()) {
            String token = (String) iterator.next();
            VcashTokenInfo tokenInfo = WalletApi.getTokenInfo(token);

            if(tokenInfo != null){
                tokenInfos.add(tokenInfo);
            }
        }

        return tokenInfos;
    }
}
