package com.vcashorg.vcashwallet;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.adapter.AddressBookAdapter;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.bean.Address;
import com.vcashorg.vcashwallet.utils.AddressFileUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.widget.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class AddressBookActivity extends ToolBarActivity {

    private static final int REQUEST_CODE_ADD = 101;

    @BindView(R.id.rv_address)
    RecyclerView mRvAddress;

    private AddressBookAdapter addressAdapter;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.address_book));
        setRightDrawable(R.drawable.ic_add);
        getSubTitle().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nv2(AddressAddActivity.class,REQUEST_CODE_ADD);
            }
        });
    }

    @Override
    public void initParams() {
        AddressFileUtil.init(this);
    }

    @Override
    public void initView() {
        mRvAddress.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewDivider divider = new RecyclerViewDivider(this, LinearLayoutManager.VERTICAL, R.drawable.rv_divider);
        mRvAddress.addItemDecoration(divider);
        List<Address> addressList = AddressFileUtil.readAddressList(this);

        addressAdapter = new AddressBookAdapter(R.layout.item_address_book,addressList);
        addressAdapter.setEmptyView(LayoutInflater.from(this).inflate(R.layout.layout_address_empty,null));

        addressAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Address address = (Address) adapter.getData().get(position);
                Intent intent = getIntent();
                intent.putExtra(Address.RESULT_ADDRESS, address.userId);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        mRvAddress.setAdapter(addressAdapter);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_address_book;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD){
            addressAdapter.setNewData(AddressFileUtil.readAddressList(this));
        }
    }
}
