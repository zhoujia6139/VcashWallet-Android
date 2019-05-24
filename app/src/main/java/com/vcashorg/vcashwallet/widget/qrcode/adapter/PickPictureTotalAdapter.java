package com.vcashorg.vcashwallet.widget.qrcode.adapter;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.widget.qrcode.picture.Picture;

import java.util.List;

/**
 * Created by yjq
 * 2018/6/7.
 */
public class PickPictureTotalAdapter extends CygAdapter<Picture> {
    public PickPictureTotalAdapter(Context context, List<Picture> objects) {
        super(context, R.layout.activity_pick_picture_total_list_item, objects);
    }

    @Override
    public void onBindData(CygViewHolder viewHolder, Picture item, int position) {
        viewHolder.setText(R.id.pick_picture_total_list_item_group_title, item.getFolderName());
        viewHolder.setText(R.id.pick_picture_total_list_item_group_count
                , "(" + Integer.toString(item.getPictureCount()) + ")");
        ImageView imageView = viewHolder.findViewById(R.id.pick_picture_total_list_item_group_image);
        Glide.with(mContext).load(item.getTopPicturePath()).into(imageView);
    }
}
