package com.yyxx.wechatfp.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.yyxx.wechatfp.util.Task;
import com.yyxx.wechatfp.util.Umeng;
import com.yyxx.wechatfp.util.UrlUtil;
import com.yyxx.wechatfp.util.log.L;



/**
 * Created by Jason on 2017/9/10.
 */

public class WebActivity extends AppCompatActivity {

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

        try {
            WebView webView = new WebView(this);
            if (!TextUtils.isEmpty(url)) {
                webView.loadUrl(url);
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (TextUtils.isEmpty(url)) {
                            return super.shouldOverrideUrlLoading(view, url);
                        }
                        String lurl = url.toLowerCase();
                        if (lurl.startsWith("http://") || lurl.startsWith("https://")) {
                            if (lurl.endsWith(".apk") || lurl.endsWith(".zip") || lurl.endsWith(".tar.gz") || lurl.contains("pan.baidu.com/s/")) {
                                UrlUtil.openUrl(WebActivity.this, url);
                                return true;
                            }
                            view.loadUrl(url);
                            return true;
                        }
                        UrlUtil.openUrl(WebActivity.this, url);
                        return true;
                    }
                });
            }
            setContentView(webView);
        } catch (RuntimeException e) {
            //FIX OPPO R9s cant load webview //java.lang.UnsatisfiedLinkError: dlopen failed: "/data/app/com.google.android.webview-1/lib/arm/libwebviewchromium.so" is 32-bit instead of 64-bit
            UrlUtil.openUrl(getApplicationContext(), url);
            Task.onMain(100, this::onBackPressed);
        } catch (Exception e) {
            //FIX OPPO R9s cant load webview //java.lang.UnsatisfiedLinkError: dlopen failed: "/data/app/com.google.android.webview-1/lib/arm/libwebviewchromium.so" is 32-bit instead of 64-bit
            UrlUtil.openUrl(getApplicationContext(), url);
            Task.onMain(100, this::onBackPressed);
            L.e(e);
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
