package com.yyxx.wechatfp.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.yyxx.wechatfp.Constant;
import com.yyxx.wechatfp.Lang;
import com.yyxx.wechatfp.adapter.PreferenceAdapter;
import com.yyxx.wechatfp.util.DonateUtil;
import com.yyxx.wechatfp.util.DpUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jason on 2017/9/9.
 */

public class DonateView extends DialogFrameLayout implements AdapterView.OnItemClickListener {

    private List<PreferenceAdapter.Data> mSettingsDataList = new ArrayList<>();
    private PreferenceAdapter mListAdapter;
    private ListView mListView;

    public DonateView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public DonateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DonateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LinearLayout rootVerticalLayout = new LinearLayout(context);
        rootVerticalLayout.setOrientation(LinearLayout.VERTICAL);

        int defHPadding = DpUtil.dip2px(context, 15);
        int defVPadding = DpUtil.dip2px(context, 12);

        mListView = new ListView(context);
        mListView.setDividerHeight(0);
        mListView.setOnItemClickListener(this);
        mListView.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);
        mListView.setDivider(new ColorDrawable(Color.TRANSPARENT));

        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_ALIPAY), Constant.AUTHOR_ALIPAY));
        if (Constant.PACKAGE_NAME_WECHAT.equals(context.getPackageName())) {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_WECHAT), Constant.AUTHOR_WECHAT));
        }
        mListAdapter = new PreferenceAdapter(mSettingsDataList);

        rootVerticalLayout.addView(mListView);

        this.addView(rootVerticalLayout);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mListView.setAdapter(mListAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        PreferenceAdapter.Data data = mListAdapter.getItem(position);
        final Context context = getContext();
        if (Lang.getString(Lang.SETTINGS_TITLE_ALIPAY).equals(data.title)) {
            if (!DonateUtil.openAlipayPayPage(context)) {
                Toast.makeText(context, Lang.getString(Lang.TOAST_GOTO_DONATE_PAGE_FAIL_ALIPAY), Toast.LENGTH_LONG).show();
            }
        } else if (Lang.getString(Lang.SETTINGS_TITLE_WECHAT).equals(data.title)) {
            if (!DonateUtil.openWeChatPay(context)) {
                Toast.makeText(context, Lang.getString(Lang.TOAST_GOTO_DONATE_PAGE_FAIL_WECHAT), Toast.LENGTH_LONG).show();
            }
        }
    }
}
