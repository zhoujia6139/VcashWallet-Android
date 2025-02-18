package com.vcashorg.vcashwallet.fragment;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.vcashorg.vcashwallet.AddressAddActivity;
import com.vcashorg.vcashwallet.AddressBookActivity;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.WalletMainActivity;
import com.vcashorg.vcashwallet.adapter.AddressBookAdapter;
import com.vcashorg.vcashwallet.base.BaseFragment;
import com.vcashorg.vcashwallet.bean.Address;
import com.vcashorg.vcashwallet.utils.AddressFileUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.widget.AddressBotDialog;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class AddressBookFragment extends BaseFragment {

    private static final int REQUEST_CODE_ADD = 101;
    private static final int REQUEST_CODE_EDIT = 102;

    @BindView(R.id.rv_address)
    RecyclerView mRvAddress;

    private AddressBookAdapter addressAdapter;

    @Override
    protected int provideContentViewId() {
        return R.layout.fragment_address_book;
    }

    @Override
    public void initView(View rootView) {
        AddressFileUtil.init(mActivity);

        mRvAddress.setLayoutManager(new LinearLayoutManager(mActivity));
        RecyclerViewDivider divider = new RecyclerViewDivider(mActivity, LinearLayoutManager.VERTICAL, R.drawable.rv_divider);
        mRvAddress.addItemDecoration(divider);
        List<Address> addressList = AddressFileUtil.readAddressList(mActivity);

        addressAdapter = new AddressBookAdapter(R.layout.item_address_book,addressList);
        addressAdapter.setEmptyView(LayoutInflater.from(mActivity).inflate(R.layout.layout_address_empty,null));

        addressAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                final Address address = (Address) adapter.getData().get(position);
                AddressBotDialog dialog = new AddressBotDialog(mActivity,address.userId,0);
                dialog.setCallBack(new AddressBotDialog.AddressHandleCallBack() {
                    @Override
                    public void onAddressDelete() {
                        addressAdapter.setNewData(AddressFileUtil.readAddressList(mActivity));
                    }

                    @Override
                    public void onAddressEdit() {
                        Intent intent = new Intent(mActivity,AddressAddActivity.class);
                        intent.putExtra(AddressAddActivity.PARAM_TYPE,"edit");
                        intent.putExtra("id",address.userId);
                        intent.putExtra("remark",address.remark);
                        nv2(intent,REQUEST_CODE_EDIT);
                    }
                });
                dialog.show();
            }
        });

        mRvAddress.setAdapter(addressAdapter);
    }

    @Override
    protected void loadData() {

    }

    @OnClick(R.id.iv_add_address)
    public void onAddClick(){
        Intent intent = new Intent(mActivity,AddressAddActivity.class);
        intent.putExtra(AddressAddActivity.PARAM_TYPE,"add");
        nv2(intent,REQUEST_CODE_ADD);
    }

    @OnClick(R.id.iv_open_menu)
    public void onMenuClick(){
        ((WalletMainActivity)mActivity).openDrawer();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD || requestCode == REQUEST_CODE_EDIT){
            addressAdapter.setNewData(AddressFileUtil.readAddressList(mActivity));
        }
    }
}
