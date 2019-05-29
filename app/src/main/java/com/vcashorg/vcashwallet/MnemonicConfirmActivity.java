package com.vcashorg.vcashwallet;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.bean.MnemonicData;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.widget.GridLineItemDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import butterknife.BindView;

public class MnemonicConfirmActivity extends ToolBarActivity {

    public static final String PARAM_MNEMONIC_LIST = "mnemonic_list";

    @BindView(R.id.rv_confirm)
    RecyclerView mRvConfirm;

    @BindView(R.id.rv_ensure)
    RecyclerView mRvEnsure;

    ArrayList<String> mnemonicList;
    List<MnemonicData> confirmDataList;
    List<MnemonicData> ensureDataList;

    MnemonicConfirmAdapter confirmAdapter;
    MnemonicEnsureAdapter ensureAdapter;

    private String chooseData = "";

    @Override
    protected void initToolBar() {
        setToolBarTitle("Confirm seed phrase");
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_mneonic_confirm;
    }

    @Override
    public void initParams() {
        mnemonicList = getIntent().getStringArrayListExtra(PARAM_MNEMONIC_LIST);
        ArrayList<MnemonicData> mnemonicDataList = (ArrayList<MnemonicData>) buildMnemonicDataList(mnemonicList);
        confirmDataList = getSubStringByRandom(mnemonicDataList, 6);
        ensureDataList = new ArrayList<>(confirmDataList);
        Collections.shuffle(ensureDataList);
    }

    @Override
    public void initView() {

        mRvConfirm.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false));
        mRvConfirm.addItemDecoration(new GridLineItemDecoration(UIUtils.dip2Px(1), UIUtils.dip2Px(1), UIUtils.getColor(R.color.grey_4)));

        confirmAdapter = new MnemonicConfirmAdapter(R.layout.item_center_txt, confirmDataList);
        confirmAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                MnemonicData item = (MnemonicData) adapter.getData().get(position);
                chooseData = item.data;
                for (MnemonicData ensureData : ensureDataList) {
                    if (ensureData.state == MnemonicData.STATE_UNCHECK) {
                        if (chooseData.equals(ensureData.data)) {
                            ensureData.state = MnemonicData.STATE_CHECK_TRUE;
                        } else {
                            ensureData.state = MnemonicData.STATE_CHECK_FALSE;
                        }
                        ensureAdapter.notifyDataSetChanged();
                        break;
                    } else if (ensureData.state == MnemonicData.STATE_CHECK_FALSE) {
                        break;
                    }
                }
                if (validate()) {
                    UIUtils.showToast("助记词验证成功");
                    Intent intent = new Intent(MnemonicConfirmActivity.this, PasswordActivity.class);
                    intent.putExtra(PasswordActivity.PARAM_MNEMONIC_LIST,mnemonicList);
                    intent.putExtra(PasswordActivity.PARAM_MODE, PasswordActivity.MODE_CREATE);
                    nv(intent);
                }
            }
        });
        mRvConfirm.setAdapter(confirmAdapter);


        mRvEnsure.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false));
        mRvEnsure.addItemDecoration(new GridLineItemDecoration(UIUtils.dip2Px(1), UIUtils.dip2Px(1), UIUtils.getColor(R.color.grey_4)));
        ensureAdapter = new MnemonicEnsureAdapter(R.layout.item_mnemonic, ensureDataList);
        ensureAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                MnemonicData item = (MnemonicData) adapter.getData().get(position);
                if (item.state == MnemonicData.STATE_CHECK_FALSE) {
                    item.state = MnemonicData.STATE_UNCHECK;
                    ensureAdapter.notifyDataSetChanged();
                }
            }
        });

        mRvEnsure.setAdapter(ensureAdapter);
    }

    class MnemonicEnsureAdapter extends BaseQuickAdapter<MnemonicData, BaseViewHolder> {

        public MnemonicEnsureAdapter(int layoutResId, @Nullable List<MnemonicData> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, MnemonicData item) {
            helper.setText(R.id.tv_num, item.num + "");
            if (item.state == MnemonicData.STATE_UNCHECK) {
                helper.setText(R.id.tv_word, "");
                helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_grey);
            } else if (item.state == MnemonicData.STATE_CHECK_TRUE) {
                helper.setText(R.id.tv_word, item.data);
                helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_green);
            } else if (item.state == MnemonicData.STATE_CHECK_FALSE) {
                helper.setText(R.id.tv_word, chooseData);
                helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_red);
            }
        }
    }

    class MnemonicConfirmAdapter extends BaseQuickAdapter<MnemonicData, BaseViewHolder> {

        public MnemonicConfirmAdapter(int layoutResId, @Nullable List<MnemonicData> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, MnemonicData item) {
            helper.setText(R.id.tv_word, item.data);
        }
    }

    public List<MnemonicData> buildMnemonicDataList(List<String> list) {
        List<MnemonicData> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            MnemonicData item = new MnemonicData();
            item.num = i + 1;
            item.data = list.get(i);
            result.add(item);
        }
        return result;
    }

    private boolean validate() {
        for (MnemonicData data : ensureDataList) {
            if (data.state == MnemonicData.STATE_UNCHECK
                    || data.state == MnemonicData.STATE_CHECK_FALSE) {
                return false;
            }
        }
        return true;
    }

    /**
     * randomData
     *
     * @param list
     * @param count
     * @return
     */
    public List<MnemonicData> getSubStringByRandom(List<MnemonicData> list, int count) {
        List<MnemonicData> backList = new ArrayList<>();
        Random random = new Random();
        int backSum = 0;
        if (list.size() >= count) {
            backSum = count;
        } else {
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

}
