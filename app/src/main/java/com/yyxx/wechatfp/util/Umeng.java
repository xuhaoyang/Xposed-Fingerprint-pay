package com.yyxx.wechatfp.util;

import android.content.Context;

import com.umeng.analytics.MobclickAgent;
import com.yyxx.wechatfp.util.log.L;

/**
 * Created by Jason on 2017/9/11.
 */

public class Umeng {

    private static Context sContext;

    public static void init(Context context) {
        sContext = context;
        try {
//            MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(context.getApplicationContext(), "59b68c81f5ade45de90004f7", context.getPackageName()));
        } catch (Exception e) {
            L.e(e);
        }
    }

    public static void reportError(String message) {
        Context context = sContext;
        if (context == null) {
            L.d("context not set");
            return;
        }
//        MobclickAgent.reportError(context, message);
    }

    public static void onResume(Context context) {
//        MobclickAgent.onResume(context);
    }

    public static void onPause(Context context) {
//        MobclickAgent.onPause(context);
    }
}
