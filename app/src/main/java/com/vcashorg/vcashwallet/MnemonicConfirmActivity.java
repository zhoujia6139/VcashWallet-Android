package com.vcashorg.vcashwallet;

import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.GridLineItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.OnClick;

public class MnemonicConfirmActivity extends ToolBarActivity {

    @BindView(R.id.rv_data)
    RecyclerView mRvData;

    @Override
    protected void initToolBar() {
        setToolBarTitle("Confirm seed phrase");
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_mneonic_confirm;
    }

    @Override
    public void initView() {

        mRvData.setLayoutManager(new GridLayoutManager(this,3,GridLayoutManager.VERTICAL, false));
        mRvData.addItemDecoration(new GridLineItemDecoration(UIUtils.dip2Px(1),UIUtils.dip2Px(1),UIUtils.getColor(R.color.grey_4)));

        List<String> words = WalletApi.generateMnemonicPassphrase();
        List<String> subWords = getSubStringByRandom(words,6);

        MnemonicDataAdapter adapter = new MnemonicDataAdapter(R.layout.item_center_txt,subWords);
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                UIUtils.showToast("Position: " + position);
            }
        });
        mRvData.setAdapter(adapter);

    }


    class MnemonicDataAdapter extends BaseQuickAdapter<String, BaseViewHolder>{

        public MnemonicDataAdapter(int layoutResId, @Nullable List<String> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, String item) {
            helper.setText(R.id.tv_word,item);
        }
    }


    public List<String> getSubStringByRandom(List<String> list, int count){
        List<String> backList = new ArrayList<>();
        Random random = new Random();
        int backSum = 0;
        if (list.size() >= count) {
            backSum = count;
        }else {
            backSum = list.size();
        }
        for (int i = 0; i < backSum; i++) {
//			随机数的范围为0-list.size()-1
            int target = random.nextInt(list.size());
            backList.add(list.get(target));
            list.remove(target);
        }
        return backList;
    }

    @OnClick(R.id.cv)
    public void textClick(){
        nv(PasswordCreateActivity.class);
    }

}
