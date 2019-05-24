package com.vcashorg.vcashwallet.widget.qrcode.adapter;


import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.vcashorg.vcashwallet.R;

import java.util.List;

/**
 * 照片浏览
 * Created by yjq
 * 2018/6/7.
 */
public class PickPictureAdapter extends CygAdapter<String> {

    public PickPictureAdapter(Context context, List<String> datas) {
        super(context, R.layout.activity_pick_picture_grid_item, datas);
    }

    @Override
    public void onBindData(CygViewHolder viewHolder, String item, int position) {
        ImageView imageView = viewHolder.findViewById(R.id.activity_pick_picture_grid_item_image);
        Glide.with(mContext).load(item).into(imageView);
    }
}
