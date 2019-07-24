package com.vcashorg.vcashwallet.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MnemonicFilterAdapter extends BaseAdapter implements Filterable{

    private List<String> mData;
    private List<String> mObjects;


    public MnemonicFilterAdapter(List<String> mData) {
        this.mData = mData;
        this.mObjects = mData;
    }

    @Override
    public int getCount() {
        return mObjects.size();
    }

    @Override
    public String getItem(int position) {
        return mObjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Context context = parent.getContext();

        final MnemonicFilterAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.pop_item, parent, false);
            holder = new MnemonicFilterAdapter.ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (MnemonicFilterAdapter.ViewHolder) convertView.getTag();
        }
        final String item = getItem(position);
        if(item == null) return convertView;

        holder.tvWord.setText(makeupBoldText(item));

        return convertView;
    }


    private String mKeyWord = "";

    private ForegroundColorSpan foregroundColorSpan;
    private RelativeSizeSpan sizeSpan = new RelativeSizeSpan(1.2f);
    private StyleSpan bordSpan = new StyleSpan(Typeface.BOLD);

    private CharSequence makeupBoldText(String word){
        if(!TextUtils.isEmpty(mKeyWord) && !TextUtils.isEmpty(word)){
            int index = word.indexOf(mKeyWord);
            if(index != -1){
                SpannableString ssText = new SpannableString(word);
                ssText.setSpan(sizeSpan, index, index + mKeyWord.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssText.setSpan(bordSpan, index, index + mKeyWord.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                return ssText;
            }else {
                return word;
            }
        }else {
            return word;
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults results = new FilterResults();

                if (mData == null) {
                    mData = new ArrayList<>(mObjects);
                }

                if (constraint == null || constraint.length() == 0) {
                    final ArrayList<String> list = new ArrayList<>(mData);
                    results.values = list;
                    results.count = list.size();
                } else {
                    final String prefixString = constraint.toString().toLowerCase();

                    final ArrayList<String> values = new ArrayList<>(mData);

                    final int count = values.size();
                    final ArrayList<String> newValues = new ArrayList<>();

                    for (int i = 0; i < count; i++) {
                        final String value = values.get(i);
                        final String valueText = value.toString().toLowerCase();

                        // First match against the whole, non-splitted value
                        if (valueText.startsWith(prefixString)) {
                            newValues.add(value);
                        } else {
                            final String[] words = valueText.split(" ");
                            for (String word : words) {
                                if (word.startsWith(prefixString)) {
                                    newValues.add(value);
                                    break;
                                }
                            }
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mObjects = (List<String>) results.values;
                mKeyWord = (String) constraint;
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    class ViewHolder {

        View item;
        @BindView(android.R.id.text1)
        TextView tvWord;

        ViewHolder(View item) {
            this.item = item;
            ButterKnife.bind(this, item);
        }
    }


}
