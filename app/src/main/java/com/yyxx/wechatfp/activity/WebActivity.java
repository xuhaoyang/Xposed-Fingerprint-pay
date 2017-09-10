package com.yyxx.wechatfp.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.webkit.WebView;

import com.yyxx.wechatfp.util.log.L;


/**
 * Created by Jason on 2017/9/10.
 */

public class WebActivity extends AppCompatActivity {

    public static final String TAG = WebActivity.class.getName();

    public static void openUrl(Context context, String url) {
        try {
            Intent intent = new Intent(context, WebActivity.class);
            intent.putExtra("url", url);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            L.e(e);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

        WebView webView = new WebView(this);
        if (!TextUtils.isEmpty(url)) {
            webView.loadUrl(url);
        }
        setContentView(webView);
    }
}
