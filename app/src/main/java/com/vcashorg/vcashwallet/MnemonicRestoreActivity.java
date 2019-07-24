package com.vcashorg.vcashwallet;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vcashorg.vcashwallet.adapter.MnemonicFilterAdapter;
import com.vcashorg.vcashwallet.base.ToolBarActivity;
import com.vcashorg.vcashwallet.bean.MnemonicData;
import com.vcashorg.vcashwallet.net.RxHelper;
import com.vcashorg.vcashwallet.utils.ACTVHeightUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.MnemonicHelper;
import com.vcashorg.vcashwallet.wallet.WalletApi;
import com.vcashorg.vcashwallet.widget.GridLineItemDecoration;
import com.vcashorg.vcashwallet.widget.WordAutoCompleteTextView;
import com.vcashorg.vcashwallet.widget.keyboard.GlobalLayoutListener;
import com.vcashorg.vcashwallet.widget.keyboard.OnKeyboardChangedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class MnemonicRestoreActivity extends ToolBarActivity {

    private List<String> allPhraseWords;
    private List<MnemonicData> restoreMnemonicData = new ArrayList<>();

    @BindView(R.id.rv_mneonic)
    RecyclerView mRvRestore;

    @BindView(R.id.btn_next)
    Button btnNext;

    @BindView(R.id.scrollView)
    NestedScrollView mScrollView;

    MnemonicRestoreAdapter adapter;

    @Override
    protected void initToolBar() {
        setToolBarTitle(UIUtils.getString(R.string.restore_seed_phrase));
    }

    @Override
    public void initView() {
        allPhraseWords = WalletApi.getAllPhraseWords();

        adapter = new MnemonicRestoreAdapter(R.layout.item_mnemonic_restore, restoreMnemonicData);
        adapter.bindToRecyclerView(mRvRestore);

        mRvRestore.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false));
        mRvRestore.addItemDecoration(new GridLineItemDecoration(UIUtils.dip2Px(1), UIUtils.dip2Px(1), UIUtils.getColor(R.color.grey_4)));

        mRvRestore.setHasFixedSize(true);
        mRvRestore.setNestedScrollingEnabled(false);

        mRvRestore.setAdapter(adapter);
    }

    @Override
    public void initData() {
        for (int i = 1; i <= 24; i++) {
            MnemonicData data = new MnemonicData();
            data.num = i;
            restoreMnemonicData.add(data);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_mnemonic_restore;
    }

    @Override
    protected void onResume() {
        super.onResume();
        showWordsDialog();
    }


    class MnemonicRestoreAdapter extends BaseQuickAdapter<MnemonicData, BaseViewHolder> {

        public MnemonicRestoreAdapter(int layoutResId, @Nullable List<MnemonicData> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(final BaseViewHolder helper, final MnemonicData item) {
            final WordAutoCompleteTextView etWord = helper.getView(R.id.et_word);
            helper.setText(R.id.tv_num, String.valueOf(helper.getAdapterPosition() + 1));
            if (!TextUtils.isEmpty(item.data)) {
                helper.setText(R.id.et_word, item.data);
                if (item.state == MnemonicData.STATE_CHECK_TRUE) {
                    helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_green);
                    helper.setTextColor(R.id.et_word, UIUtils.getColor(R.color.black));
                } else if (item.state == MnemonicData.STATE_CHECK_FALSE) {
                    helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_red);
                    helper.setTextColor(R.id.et_word, UIUtils.getColor(R.color.red));
                }
            }

            etWord.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        parse = false;
                    }
                }
            });

            etWord.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!parse) {
                        String str = s.toString();
                        if (str.indexOf("/r") >= 0 || str.indexOf("\n") >= 0) {
                            EditText nextEdt = (EditText) adapter.getViewByPosition(helper.getAdapterPosition() + 1, R.id.et_word);
                            etWord.setText(str.replace("/r", "").replace("\n", ""));
                            if (nextEdt != null) {
                                nextEdt.requestFocus();
                                nextEdt.setSelection(nextEdt.getText().length());
                            }
                        }

                        if (!str.replace("/r", "").replace("\n", "").equals("")) {
                            if (allPhraseWords.contains(s.toString())) {
                                item.state = MnemonicData.STATE_CHECK_TRUE;
                                item.data = s.toString();
                                helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_green);
                                helper.setTextColor(R.id.et_word, UIUtils.getColor(R.color.black));
                            } else {
                                item.state = MnemonicData.STATE_CHECK_FALSE;
                                item.data = "";
                                helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_red);
                                helper.setTextColor(R.id.et_word, UIUtils.getColor(R.color.red));
                            }
                        } else {
                            item.state = MnemonicData.STATE_UNCHECK;
                            item.data = "";
                            helper.setBackgroundRes(R.id.fl_bg, R.drawable.bg_circle_grey);
                        }
                    }

                    btnState();
                }
            });

            etWord.setThreshold(1);
            etWord.setDropDownWidth(350);
            etWord.setOnShowWindowListener(new WordAutoCompleteTextView.OnShowWindowListener() {
                @Override
                public boolean beforeShow() {
                    ACTVHeightUtil.setDropDownHeight(etWord, 4);
                    return true;
                }
            });
           // ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.pop_item, allPhraseWords);
            MnemonicFilterAdapter adapter = new MnemonicFilterAdapter(allPhraseWords);
            etWord.setAdapter(adapter);
        }
    }

    @OnClick(R.id.btn_next)
    public void onNextClick() {
        if (validate()) {
            validateMnemonic(buildMnemonicList());
        }
    }

    private void validateMnemonic(final List<String> words) {

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(UIUtils.getString(R.string.creating_wallet));
        progress.show();

        Observable.create(new ObservableOnSubscribe() {

            @Override
            public void subscribe(ObservableEmitter emitter) {

                boolean result = WalletApi.createWallet(words, null);
                if (result) {
                    emitter.onComplete();
                } else {
                    emitter.onError(null);
                }

            }
        }).compose(RxHelper.io2main())
                .subscribe(new Observer() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Object o) {
                    }

                    @Override
                    public void onError(Throwable e) {
                        UIUtils.showToastCenter(R.string.create_wallet_failed);
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        Intent intent = new Intent(MnemonicRestoreActivity.this, PasswordActivity.class);
                        intent.putStringArrayListExtra(PasswordActivity.PARAM_MNEMONIC_LIST, buildMnemonicList());
                        intent.putExtra(PasswordActivity.PARAM_MODE, PasswordActivity.MODE_RESTORE);
                        nv(intent);
                    }
                });
    }

    private void btnState() {
        for (MnemonicData data : restoreMnemonicData) {
            if (data.state == MnemonicData.STATE_UNCHECK || data.state == MnemonicData.STATE_CHECK_FALSE) {
                btnNext.setBackgroundResource(R.drawable.bg_orange_light_round_rect);
                return;
            }
        }
        btnNext.setBackgroundResource(R.drawable.selector_orange);
    }

    private boolean validate() {
        for (MnemonicData data : restoreMnemonicData) {
            if (data.state == MnemonicData.STATE_UNCHECK) {
                return false;
            } else if (data.state == MnemonicData.STATE_CHECK_FALSE) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<String> buildMnemonicList() {
        ArrayList<String> result = new ArrayList<>();
        for (MnemonicData data : restoreMnemonicData) {
            result.add(data.data);
        }
        return result;
    }

    private boolean parse;

    private void showWordsDialog() {
        if (wordsInBoard() != null) {
            new AlertDialog.Builder(this)
                    .setMessage("Paste 24 words from the clipboard?")
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            List<String> words = wordsInBoard();
                            if (words != null) {
                                for (int i = 0; i < restoreMnemonicData.size(); i++) {
                                    MnemonicData data = restoreMnemonicData.get(i);
                                    String word = words.get(i);
                                    data.data = word;
                                    data.state = allPhraseWords.contains(word) ? MnemonicData.STATE_CHECK_TRUE : MnemonicData.STATE_CHECK_FALSE;
                                }
                                parse = true;
                                adapter.notifyDataSetChanged();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

    }


    private List<String> wordsInBoard() {
        String board = UIUtils.getClipboardText(this).trim();
        if (board.contains(" ")) {
            String trim = board.replaceAll("\\s{1,}", " ");
            String[] w1 = trim.split(" ");
            if (w1.length == 24) {
                return Arrays.asList(w1);
            }
        }

        if (board.contains("-")) {
            String[] w2 = board.split("-");
            if (w2.length == 24) {
                return Arrays.asList(w2);
            }
        }

        if (board.contains("\n")) {
            String[] w3 = board.split("\n");
            if (w3.length == 24) {
                return Arrays.asList(w3);
            }
        }

        return null;
    }
}
