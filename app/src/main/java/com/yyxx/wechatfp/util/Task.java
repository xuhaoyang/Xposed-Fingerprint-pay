package com.yyxx.wechatfp.util;

import android.os.Handler;
import android.os.Looper;

import com.yyxx.wechatfp.util.log.L;

/**
 * Created by Jason on 2017/9/10.
 */

public class Task {

    private static Handler sMainHandler = new Handler(Looper.getMainLooper());

    public static void onMain(long msec, final Runnable runnable) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    L.d(e);
                }
            }
        };
        sMainHandler.postDelayed(run, msec);
    }

    public static void onMain(final Runnable runnable) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    L.d(e);
                }
            }
        };
        sMainHandler.post(run);
    }
}
