package com.vcashorg.vcashwallet;

import android.app.Activity;
import android.media.Image;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.vcashorg.vcashwallet.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class WalletDrawer {

    NavigationView navigationView;
    DrawerLayout drawer;

    Activity context;
    BaseAdapter adapter;

    List<MenuItem> itemList;

    public WalletDrawer(Activity activity) {
        context = activity;

        navigationView = activity.findViewById(R.id.nav_view);
        drawer = activity.findViewById(R.id.drawer_layout);

        createMenuItem();

        init();
    }

    private void init() {
        View v = context.getLayoutInflater().inflate(R.layout.layout_sliding_menu, null);

        final ListView lv = v.findViewById(R.id.lv_sliding_menu);

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return itemList.size();
            }

            @Override
            public MenuItem getItem(int position) {
                return itemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return itemList.get(position).hashCode();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_sliding_menu, null);
                MenuItem item = getItem(position);
                ImageView iv = convertView.findViewById(R.id.iv_menu);
                TextView tv = convertView.findViewById(R.id.tv_menu);
                LinearLayout bg = convertView.findViewById(R.id.ll_menu_bg);
                tv.setText(item.name);
                if (item.checked) {
                    tv.setTextColor(UIUtils.getColor(R.color.orange));
                    iv.setImageResource(item.checkId);
                    bg.setBackgroundColor(UIUtils.getColor(R.color.grey_4));
                } else {
                    tv.setTextColor(UIUtils.getColor(R.color.A10));
                    iv.setImageResource(item.uncheckId);
                    bg.setBackgroundColor(UIUtils.getColor(R.color.white));
                }
                return convertView;
            }

        };

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MenuItem item = itemList.get(position);
                if (!item.checked) {
                    for (int i = 0; i < itemList.size(); i++) {
                        itemList.get(i).checked = i == position;
                    }
                    item.checked = true;
                    adapter.notifyDataSetChanged();
                }
                drawer.closeDrawers();
            }
        });

        navigationView.removeAllViews();
        navigationView.addView(v);
    }

    public void openDrawer() {
        drawer.openDrawer(Gravity.START);
    }

    private class MenuItem {
        public boolean checked;

        public String name;

        public int checkId;

        public int uncheckId;
    }

    private void createMenuItem() {
        itemList = new ArrayList<>();
        //Vcash Wallet
        MenuItem item1 = new MenuItem();
        item1.checked = true;
        item1.name = "Vcash Wallet";
        item1.checkId = R.drawable.ic_menu_wallet_check;
        item1.uncheckId = R.drawable.ic_menu_wallet_uncheck;
        //Setting
        MenuItem item2 = new MenuItem();
        item2.name = "Setting";
        item2.checkId = R.drawable.ic_menu_setting_check;
        item2.uncheckId = R.drawable.ic_menu_setting_uncheck;
        //Address book
        MenuItem item3 = new MenuItem();
        item3.name = "Address book";
        item3.checkId = R.drawable.ic_menu_address_check;
        item3.uncheckId = R.drawable.ic_menu_address_uncheck;

        itemList.add(item1);
        itemList.add(item2);
        itemList.add(item3);
    }
}
