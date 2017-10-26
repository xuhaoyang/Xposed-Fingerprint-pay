package com.yyxx.wechatfp.activity;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.Lang;
import com.yyxx.wechatfp.R;
import com.yyxx.wechatfp.adapter.PreferenceAdapter;
import com.yyxx.wechatfp.network.updateCheck.UpdateFactory;
import com.yyxx.wechatfp.util.Task;
import com.yyxx.wechatfp.util.Umeng;
import com.yyxx.wechatfp.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

import static com.yyxx.wechatfp.Constant.HELP_URL_ALIPAY;
import static com.yyxx.wechatfp.Constant.HELP_URL_TAOBAO;
import static com.yyxx.wechatfp.Constant.HELP_URL_WECHAT;
import static com.yyxx.wechatfp.Constant.PROJECT_URL;

public class HomeActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {


    private PreferenceAdapter mListAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Umeng.init(this);
        setContentView(R.layout.home);

        ListView listView = (ListView) findViewById(R.id.list);
        List<PreferenceAdapter.Data> list = new ArrayList<>();
        list.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_HELP_WECHAT), Lang.getString(Lang.SETTINGS_SUB_TITLE_HELP_WECHAT)));
        list.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_HELP_ALIPAY), Lang.getString(Lang.SETTINGS_SUB_TITLE_HELP_ALIPAY)));
        list.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_HELP_TAOBAO), Lang.getString(Lang.SETTINGS_SUB_TITLE_HELP_TAOBAO)));
        list.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_CHECKUPDATE), Lang.getString(Lang.SETTINGS_SUB_TITLE_CHECKUPDATE)));
        list.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_WEBSIDE), PROJECT_URL));
        list.add(new PreferenceAdapter.Data(Lang.getString(Lang.SETTINGS_TITLE_VERSION), BuildConfig.VERSION_NAME));
        mListAdapter = new PreferenceAdapter(list);
        listView.setAdapter(mListAdapter);
        listView.setOnItemClickListener(this);
        Task.onMain(1000L, () -> UpdateFactory.doUpdateCheck(HomeActivity.this));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        PreferenceAdapter.Data data = mListAdapter.getItem(i);
        if (data == null || TextUtils.isEmpty(data.title)) {
            return;
        }
        if (Lang.getString(Lang.SETTINGS_TITLE_HELP_WECHAT).equals(data.title)) {
            WebActivity.openUrl(this, HELP_URL_WECHAT);
        } else if (Lang.getString(Lang.SETTINGS_TITLE_HELP_ALIPAY).equals(data.title)) {
            WebActivity.openUrl(this, HELP_URL_ALIPAY);
        } else if (Lang.getString(Lang.SETTINGS_TITLE_HELP_TAOBAO).equals(data.title)) {
            WebActivity.openUrl(this, HELP_URL_TAOBAO);
        } else if (Lang.getString(Lang.SETTINGS_TITLE_CHECKUPDATE).equals(data.title)) {
            UpdateFactory.doUpdateCheck(this, false, true);
        } else if (Lang.getString(Lang.SETTINGS_TITLE_WEBSIDE).equals(data.title)) {
            UrlUtil.openUrl(this, PROJECT_URL);
            Toast.makeText(this, Lang.getString(Lang.TOAST_GIVE_ME_STAR), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Umeng.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Umeng.onPause(this);
    }
}

