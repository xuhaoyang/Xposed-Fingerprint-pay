package com.yyxx.wechatfp.util.log.handler;

import com.yyxx.wechatfp.util.Umeng;
import com.yyxx.wechatfp.util.log.inf.ILog;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by Jason on 2017/9/10.
 */

public class XposedLog implements ILog {

    @Override
    public void debug(String tag, String msg) {
        XposedBridge.log(tag + " " + msg);
    }

    @Override
    public void error(String tag, String msg) {
        XposedBridge.log(tag + " " + msg);
        Umeng.reportError(tag + " " + msg);
    }
}
