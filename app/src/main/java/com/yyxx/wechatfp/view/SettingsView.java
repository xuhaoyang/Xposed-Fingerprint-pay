package com.yyxx.wechatfp.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.Constant;
import com.yyxx.wechatfp.Lang;
import com.yyxx.wechatfp.adapter.PreferenceAdapter;
import com.yyxx.wechatfp.network.updateCheck.UpdateFactory;
import com.yyxx.wechatfp.util.Config;
import com.yyxx.wechatfp.util.DpUtil;
import com.yyxx.wechatfp.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

import static com.yyxx.wechatfp.util.StyleUtil.TEXT_SIZE_BIG;
import static com.yyxx.wechatfp.view.PasswordInputView.DEFAULT_HIDDEN_PASS;

/**
 * Created by Jason on 2017/9/9.
 */

public class SettingsView extends DialogFrameLayout implements AdapterView.OnItemClickListener {

    private List<PreferenceAdapter.Data> mSettingsDataList = new ArrayList<>();
    private PreferenceAdapter mListAdapter;
    private ListView mListView;

    public SettingsView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SettingsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SettingsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LinearLayout rootVerticalLayout = new LinearLayout(context);
        rootVerticalLayout.setOrientation(LinearLayout.VERTICAL);

        View lineView = new View(context);
        lineView.setBackgroundColor(Color.TRANSPARENT);

        TextView settingsTitle = new TextView(context);
        settingsTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_BIG);
        settingsTitle.setText(Lang.getString(Lang.APP_SETTINGS_NAME) + " " + BuildConfig.VERSION_NAME);
        settingsTitle.setTextColor(Color.WHITE);
        settingsTitle.setTypeface(null, Typeface.BOLD);
        settingsTitle.setBackgroundColor(0xFF1AAEE5);
        int defHPadding = DpUtil.dip2px(context, 15);
        int defVPadding = DpUtil.dip2px(context, 12);
        settingsTitle.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);

        mListView = new ListView(context);
        mListView.setDividerHeight(0);
        mListView.setOnItemClickListener(this);
        mListView.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);
        mListView.setDivider(new ColorDrawable(Color.TRANSPARENT));

        if (context.getPackageName().equals(Constant.PACKAGE_NAME_WECHAT)) {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_SWITCH), Lang.getString(Lang.SETTINGS_SUB_TITLE_SWITCH_WECHAT), true, Config.from(context).isOn()));
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_PASSWORD), Lang.getString(Lang.SETTINGS_SUB_TITLE_PASSWORD_WECHAT)));
        } else if (context.getPackageName().equals(Constant.PACKAGE_NAME_QQ)) {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_SWITCH), Lang.getString(Lang.SETTINGS_SUB_TITLE_SWITCH_QQ), true, Config.from(context).isOn()));
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_PASSWORD), Lang.getString(Lang.SETTINGS_SUB_TITLE_PASSWORD_QQ)));
        } else {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_SWITCH), Lang.getString(Lang.SETTINGS_SUB_TITLE_SWITCH_ALIPAY), true, Config.from(context).isOn()));
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_PASSWORD), Lang.getString(Lang.SETTINGS_SUB_TITLE_PASSWORD_ALIPAY)));
        }
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_DONATE), Lang.getString(Lang.SETTINGS_SUB_TITLE_DONATE)));
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_CHECKUPDATE), Lang.getString(Lang.SETTINGS_SUB_TITLE_CHECKUPDATE)));
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_WEBSIDE), Constant.PROJECT_URL));
        mListAdapter = new PreferenceAdapter(mSettingsDataList);

        rootVerticalLayout.addView(settingsTitle);
        rootVerticalLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtil.dip2px(context, 2)));
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
        final Config config = Config.from(context);
        if (Lang.getString(Lang.SETTINGS_TITLE_SWITCH).equals(data.title)) {
            if (!data.selectionState && !config.getLicenseAgree()) {
                new LicenseView(context)
                    .withOnNegativeButtonClickListener((dialog, which) -> {
                        config.setOn(false);
                        config.setLicenseAgree(false);
                        dialog.dismiss();
                        data.selectionState = false;
                        mListAdapter.notifyDataSetChanged();
                    }).withOnPositiveButtonClickListener((dialog, which) -> {
                        config.setLicenseAgree(true);
                        if (checkPasswordAndNofify(context)) {
                            config.setOn(true);
                            data.selectionState = true;
                        }
                        dialog.dismiss();
                        mListAdapter.notifyDataSetChanged();
                    }).showInDialog();
            } else {
                if (data.selectionState || checkPasswordAndNofify(context)) {
                    config.setOn(false);
                    data.selectionState = !data.selectionState;
                    mListAdapter.notifyDataSetChanged();
                }
            }
        } else if (Lang.getString(Lang.SETTINGS_TITLE_PASSWORD).equals(data.title)) {
            PasswordInputView passwordInputView = new PasswordInputView(context);
            if (!TextUtils.isEmpty(config.getPassword())) {
                passwordInputView.setDefaultText(DEFAULT_HIDDEN_PASS);
            }
            passwordInputView.withOnDismissListener(v -> {
                PasswordInputView inputView = (PasswordInputView) v;
                String inputText = inputView.getInput();
                if (TextUtils.isEmpty(inputText)) {
                    config.setPassword("");
                    return;
                }
                if (DEFAULT_HIDDEN_PASS.equals(inputText)) {
                    return;
                }
                config.setPassword(inputText);
            }).showInDialog();
        } else if (Lang.getString(Lang.SETTINGS_TITLE_CHECKUPDATE).equals(data.title)) {
            UpdateFactory.doUpdateCheck(context, false, true);
        } else if (Lang.getString(Lang.SETTINGS_TITLE_DONATE).equals(data.title)) {
            new DonateView(context).showInDialog();
        } else if (Lang.getString(Lang.SETTINGS_TITLE_WEBSIDE).equals(data.title)) {
            UrlUtil.openUrl(context, Constant.PROJECT_URL);
            Toast.makeText(context, Lang.getString(Lang.TOAST_GIVE_ME_STAR), Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkPasswordAndNofify(Context context) {
        String pwd = Config.from(context).getPassword();
        if (TextUtils.isEmpty(pwd)) {
            if (context.getPackageName().equals(Constant.PACKAGE_NAME_WECHAT)) {
                Toast.makeText(context, Lang.getString(Lang.TOAST_PASSWORD_NOT_SET_WECHAT), Toast.LENGTH_SHORT).show();
            } else if (context.getPackageName().equals(Constant.PACKAGE_NAME_ALIPAY)) {
                Toast.makeText(context, Lang.getString(Lang.TOAST_PASSWORD_NOT_SET_ALIPAY), Toast.LENGTH_SHORT).show();
            } else if (context.getPackageName().equals(Constant.PACKAGE_NAME_TAOBAO)) {
                Toast.makeText(context, Lang.getString(Lang.TOAST_PASSWORD_NOT_SET_TAOBAO), Toast.LENGTH_SHORT).show();
            } else if (context.getPackageName().equals(Constant.PACKAGE_NAME_QQ)) {
                Toast.makeText(context, Lang.getString(Lang.TOAST_PASSWORD_NOT_SET_QQ), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }
}
