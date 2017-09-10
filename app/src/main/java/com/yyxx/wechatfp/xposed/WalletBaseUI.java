package com.yyxx.wechatfp.xposed;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;

import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.util.log.L;
import com.yyxx.wechatfp.xposed.loader.XposedPluginLoader;
import com.yyxx.wechatfp.xposed.plugin.XposedPlugin;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static com.yyxx.wechatfp.Constant.PACKAGE_BANE_WECHAT;


public class WalletBaseUI implements IXposedHookZygoteInit, IXposedHookLoadPackage {


    public void initZygote(StartupParam startupParam) throws Throwable {
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(PACKAGE_BANE_WECHAT)) {
            L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    L.d("Application onCreate");
                    Context context = (Context) param.thisObject;
                    XposedPluginLoader.load(XposedPlugin.class, context, lpparam);
                }
            });
        }
    }
}
