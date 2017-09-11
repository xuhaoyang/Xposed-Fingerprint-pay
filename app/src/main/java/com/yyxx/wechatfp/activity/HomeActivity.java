package com.yyxx.wechatfp.activity;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.R;
import com.yyxx.wechatfp.adapter.PreferenceAdapter;
import com.yyxx.wechatfp.network.updateCheck.UpdateFactory;
import com.yyxx.wechatfp.util.Task;
import com.yyxx.wechatfp.util.Umeng;
import com.yyxx.wechatfp.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

import static com.yyxx.wechatfp.Constant.HELP_URL_ALIPAY;
import static com.yyxx.wechatfp.Constant.HELP_URL_WECHAT;
import static com.yyxx.wechatfp.Constant.PROJECT_URL;

public class HomeActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static final String SETTINGS_NAME_HELP_WECHAT = "微信指纹";
    public static final String SETTINGS_NAME_HELP_ALIPAY = "支付宝指纹";
    private static final String SETTINGS_NAME_CHECKUPDATE = "檢查更新";
    private static final String SETTINGS_NAME_WEBSIDE = "項目主頁";

    private PreferenceAdapter mListAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Umeng.init(this);
        setContentView(R.layout.home);

        ListView listView = (ListView) findViewById(R.id.list);
        List<PreferenceAdapter.Data> list = new ArrayList<>();
        list.add(new PreferenceAdapter.Data(SETTINGS_NAME_HELP_WECHAT, "查看使用教程"));
        list.add(new PreferenceAdapter.Data(SETTINGS_NAME_HELP_ALIPAY, "查看使用教程"));
        list.add(new PreferenceAdapter.Data(SETTINGS_NAME_CHECKUPDATE, "點擊檢查软件更新"));
        list.add(new PreferenceAdapter.Data(SETTINGS_NAME_WEBSIDE, "訪問項目主頁"));
        list.add(new PreferenceAdapter.Data("当前版本", BuildConfig.VERSION_NAME));
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
        if (SETTINGS_NAME_HELP_WECHAT.equals(data.title)) {
            WebActivity.openUrl(this, HELP_URL_WECHAT);
        } else if (SETTINGS_NAME_HELP_ALIPAY.equals(data.title)) {
            WebActivity.openUrl(this, HELP_URL_ALIPAY);
        } else if (SETTINGS_NAME_CHECKUPDATE.equals(data.title)) {
            UpdateFactory.doUpdateCheck(this, false, true);
        } else if (SETTINGS_NAME_WEBSIDE.equals(data.title)) {
            UrlUtil.openUrl(this, PROJECT_URL);
            Toast.makeText(this, "如果您擁有Github賬戶, 別忘了給我的項目+個Star噢", Toast.LENGTH_LONG).show();
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

