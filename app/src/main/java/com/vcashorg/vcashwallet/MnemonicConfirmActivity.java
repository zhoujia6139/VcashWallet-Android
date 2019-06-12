package com.vcashorg.vcashwallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

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
import butterknife.OnClick;

public class MnemonicConfirmActivity extends ToolBarActivity {

    public static final String PARAM_MNEMONIC_LIST = "mnemonic_list";

    @BindView(R.id.rv_confirm)
    RecyclerView mRvConfirm;

    @BindView(R.id.rv_ensure)
    RecyclerView mRvEnsure;

    @BindView(R.id.btn_check)
    FrameLayout mBtnCheck;

    ArrayList<String> mnemonicList;
    List<MnemonicData> confirmDataList;
    List<MnemonicData> ensureDataList;

    MnemonicConfirmAdapter confirmAdapter;
    MnemonicEnsureAdapter ensureAdapter;

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

        initEnsureList();
    }

    private void initEnsureList() {
        ensureDataList = new ArrayList<>();
        for (int i = 0; i < confirmDataList.size(); i++) {
            MnemonicData mnemonicData = new MnemonicData();
            mnemonicData.data = "";
            mnemonicData.num = confirmDataList.get(i).num;
            ensureDataList.add(mnemonicData);
        }
        Collections.shuffle(ensureDataList);
        ensureDataList.get(0).state = MnemonicData.STATE_CHECK_NEXT;
    }

    @Override
    public void initView() {

        mRvConfirm.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false));

        confirmAdapter = new MnemonicConfirmAdapter(R.layout.item_center_check_txt, confirmDataList);
        confirmAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                MnemonicData item = (MnemonicData) adapter.getData().get(position);
                if (item.state == MnemonicData.STATE_CHECK_TRUE) {
                    return;
                }
                for (int i = 0; i < ensureDataList.size(); i++) {
                    MnemonicData data = ensureDataList.get(i);
                    if (data.state == MnemonicData.STATE_CHECK_NEXT) {
                        data.state = MnemonicData.STATE_CHECK_TRUE;
                        data.data = item.data;
                        item.state = MnemonicData.STATE_CHECK_TRUE;
                        if (i != ensureDataList.size() - 1) {
                            ensureDataList.get(i + 1).state = MnemonicData.STATE_CHECK_NEXT;
                        }
                        confirmAdapter.notifyDataSetChanged();
                        ensureAdapter.notifyDataSetChanged();
                        break;
                    }
                }

                btnState();
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

                if (item.state == MnemonicData.STATE_CHECK_TRUE) {

                    for (MnemonicData mnemonicData : confirmDataList) {
                        if (mnemonicData.data.equals(item.data)) {
                            mnemonicData.state = MnemonicData.STATE_UNCHECK;
                            confirmAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                    if (position == ensureDataList.size() - 1) {
                        item.data = "";
                        item.state = MnemonicData.STATE_CHECK_NEXT;
                    } else {
                        int pos = getCheckNextPos();
                        if (pos != -1) {
                            for (int i = position; i < pos; i++) {
                                MnemonicData data = ensureDataList.get(i);
                                data.data = ensureDataList.get(i + 1).data;
                                data.state = ensureDataList.get(i + 1).state;
                            }
                            for (int i = pos; i < ensureDataList.size(); i++) {
                                ensureDataList.get(i).data = "";
                                ensureDataList.get(i).state = MnemonicData.STATE_UNCHECK;
                            }
                        } else {
                            for (int i = position; i < ensureDataList.size() - 1; i++) {
                                MnemonicData data = ensureDataList.get(i);
                                data.data = ensureDataList.get(i + 1).data;
                                data.state = ensureDataList.get(i + 1).state;
                            }
                            ensureDataList.get(ensureDataList.size() - 1).state = MnemonicData.STATE_CHECK_NEXT;
                            ensureDataList.get(ensureDataList.size() - 1).data = "";
                        }
                    }
                    ensureAdapter.notifyDataSetChanged();
                }

                btnState();
            }
        });

        mRvEnsure.setAdapter(ensureAdapter);
    }

    private int getCheckNextPos() {
        for (int i = 0; i < ensureDataList.size(); i++) {
            if (ensureDataList.get(i).state == MnemonicData.STATE_CHECK_NEXT) {
                return i;
            }
        }
        return -1;
    }

    class MnemonicEnsureAdapter extends BaseQuickAdapter<MnemonicData, BaseViewHolder> {

        public MnemonicEnsureAdapter(int layoutResId, @Nullable List<MnemonicData> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, MnemonicData item) {
            helper.setText(R.id.tv_num, item.num + "");
            helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_grey);
            if (item.state == MnemonicData.STATE_UNCHECK) {
                helper.setText(R.id.tv_word, "");
            } else if (item.state == MnemonicData.STATE_CHECK_NEXT) {
                helper.setText(R.id.tv_word, "|");
            } else {
                helper.setText(R.id.tv_word, item.data);
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
            if (item.state == MnemonicData.STATE_CHECK_TRUE) {
                helper.setBackgroundRes(R.id.tv_word, R.drawable.bg_grey_round_rect);
            } else {
                helper.setBackgroundRes(R.id.tv_word, R.drawable.bg_white_grey10_border_round_rect);
            }
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
            for (MnemonicData item : confirmDataList) {
                if (item.num == data.num) {
                    if (!item.data.equals(data.data)) {
                        UIUtils.showToastCenter("The words are inconsistent with the seed phrase.please try again.");
                        for (MnemonicData mnemonicData : confirmDataList){
                            mnemonicData.state = MnemonicData.STATE_UNCHECK;
                        }
                        for (MnemonicData mnemonicData : ensureDataList){
                            mnemonicData.state = MnemonicData.STATE_UNCHECK;
                            mnemonicData.data = "";
                        }
                        ensureDataList.get(0).state = MnemonicData.STATE_CHECK_NEXT;
                        ensureAdapter.notifyDataSetChanged();
                        confirmAdapter.notifyDataSetChanged();
                        return false;
                    } else {
                        break;
                    }
                }
            }
        }

        return true;
    }

    private boolean btnState() {
        for (MnemonicData data : ensureDataList) {
            if (data.state != MnemonicData.STATE_CHECK_TRUE) {
                mBtnCheck.setBackgroundResource(R.drawable.bg_orange_light_round_rect);
                return false;
            }
        }
        mBtnCheck.setBackgroundResource(R.drawable.selector_orange);
        return true;
    }


    @OnClick(R.id.btn_check)
    public void onCheckClick() {
        if (btnState() && validate()) {
            UIUtils.showToastCenter("Mnemonic Validate Success");
            Intent intent = new Intent(MnemonicConfirmActivity.this, PasswordActivity.class);
            intent.putExtra(PasswordActivity.PARAM_MNEMONIC_LIST, mnemonicList);
            intent.putExtra(PasswordActivity.PARAM_MODE, PasswordActivity.MODE_CREATE);
            nv(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Return to seed phrase")
                .setMessage("Your current seed will become obsolete and the new seed will be generated")
                .setPositiveButton("Generate", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        nv(MnemonicCreateActivity.class);
                        finish();
                    }
                })
                .setNegativeButton("Cancel",null)
                .show();
        //super.onBackPressed();
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
//			random range 0-list.size()-1
            int target = random.nextInt(list.size());
            backList.add(list.get(target));
            list.remove(target);
        }
        return backList;
    }

}
