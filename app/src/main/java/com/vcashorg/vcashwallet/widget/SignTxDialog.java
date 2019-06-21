package com.vcashorg.vcashwallet.widget;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.utils.DateUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WalletApi;

public class SignTxDialog extends DialogFragment {

    public static final String KEY = "vcash_slate";

    private TextView mTvTxId;
    private TextView mTvTxAmount;
    private TextView mTvTxFee;
    private TextView mTvTxTime;

    private TextView mBtnSign;

    private OnSignClickListener mListener;

    public static SignTxDialog newInstance(Bundle args){
        SignTxDialog fragment = new SignTxDialog();
        if(args != null){
            fragment.setArguments(args);
        }
        return fragment;
    }

    public SignTxDialog setOnSignClickListener(OnSignClickListener listener){
        mListener = listener;
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.TxDialog);
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            dialog.getWindow().setLayout((int) (dm.widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_tx_file_dialog, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTvTxId = view.findViewById(R.id.tv_tx_id);
        mTvTxAmount = view.findViewById(R.id.tv_tx_amount);
        mTvTxFee = view.findViewById(R.id.tv_tx_fee);
        mTvTxTime = view.findViewById(R.id.tv_tx_time);
        mBtnSign = view.findViewById(R.id.btn_sign);

        view.findViewById(R.id.ll_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignTxDialog.this.dismiss();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        if (getArguments() != null) {
//            VcashSlate vcashSlate = (VcashSlate) getArguments().getSerializable(KEY);
//            if (vcashSlate != null) {
//                mTvTxId.setText(vcashSlate.txLog.tx_id);
//                mTvTxAmount.setText(WalletApi.nanoToVcashWithUnit(vcashSlate.amount));
//                mTvTxFee.setText(WalletApi.nanoToVcashWithUnit(vcashSlate.fee));
//                mTvTxTime.setText(DateUtil.formatDateTimeStamp(vcashSlate.txLog.create_time));
//            }
//        }

        mBtnSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onSignClick();
                }
                SignTxDialog.this.dismiss();
            }
        });
    }

    public interface OnSignClickListener{
        void onSignClick();
    }
}
