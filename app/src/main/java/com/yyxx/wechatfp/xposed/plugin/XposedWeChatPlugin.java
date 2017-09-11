package com.yyxx.wechatfp.xposed.plugin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.Constant;
import com.yyxx.wechatfp.network.updateCheck.UpdateFactory;
import com.yyxx.wechatfp.util.Config;
import com.yyxx.wechatfp.util.Task;
import com.yyxx.wechatfp.util.log.L;
import com.yyxx.wechatfp.view.SettingsView;
import com.yyxx.wechatfp.xposed.ObfuscationHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Jason on 2017/9/8.
 */

public class XposedWeChatPlugin {

    private static Activity mWalletPayUIActivity;
    private static EditText mInputEditText;
    private RelativeLayout mPasswordLayout;
    private ImageView mFingerprintImageView;
    private RelativeLayout mFingerPrintLayout;
    private TextView mPayTitleTextView, mPasswordTextView;
    private static FingerprintIdentify mFingerprintIdentify;
    private static boolean mNeedFingerprint;
    private Activity mCurrentActivity;

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {

            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constant.PACKAGE_NAME_WECHAT, 0);
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;
            boolean isVersionSupported = ObfuscationHelper.init(versionCode, versionName, lpparam);
            if (!isVersionSupported) {
                Toast.makeText(context, "当前版本:" + versionName + "." + versionCode + "不支持", Toast.LENGTH_LONG).show();
                L.d("当前版本:" + versionName + "." + versionCode + "不支持");
                return;
            }

            XposedHelpers.findAndHookMethod(ObfuscationHelper.MM_Classes.PayUI, "onCreate", Bundle.class, new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    L.d("PayUI onCreate");
                    mWalletPayUIActivity = (Activity) param.thisObject;
                    if (new Config(context).isOn()) {
                        mNeedFingerprint = true;
                    } else {
                        mNeedFingerprint = false;
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    boolean firstStartUp = mCurrentActivity == null;
                    mCurrentActivity = (Activity) param.thisObject;
                    if (firstStartUp) {
                        Task.onMain(6000L, new Runnable() {
                            @Override
                            public void run() {
                                UpdateFactory.doUpdateCheck(mCurrentActivity);
                            }
                        });
                    }
                    L.d("Activity onResume =", mCurrentActivity);
                }
            });

            XposedHelpers.findAndHookMethod(ObfuscationHelper.MM_Classes.FetchUI, "onCreate", Bundle.class, new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    L.d("FetchUI onCreate");
                    mWalletPayUIActivity = (Activity) param.thisObject;
                    if (new Config(context).isOn()) {
                        mNeedFingerprint = true;
                    } else {
                        mNeedFingerprint = false;
                    }
                }
            });

            XposedHelpers.findAndHookConstructor(ObfuscationHelper.MM_Classes.Payview, Context.class, new XC_MethodHook() {
                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    L.d("Payview Constructor");

                    if (mNeedFingerprint && mWalletPayUIActivity != null) {
                        initFingerPrintLock();
                        mPasswordLayout = (RelativeLayout) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PaypwdView);
                        mInputEditText = (EditText) XposedHelpers.getObjectField(mPasswordLayout, ObfuscationHelper.MM_Fields.PaypwdEditText);
                        L.d("密码输入框:" + mInputEditText.getClass().getName());
                        mInputEditText.setVisibility(View.GONE);
                        mPayTitleTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PayTitle);
                        mPayTitleTextView.setText(ObfuscationHelper.MM_Res.Finger_title);
                        final View mKeyboard = (View) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PayInputView);
                        L.d("密码键盘:" + mKeyboard.getClass().getName());
                        mKeyboard.setVisibility(View.GONE);
                        mFingerPrintLayout = new RelativeLayout(mWalletPayUIActivity);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        mFingerPrintLayout.setLayoutParams(layoutParams);
                        mFingerprintImageView = new ImageView(mWalletPayUIActivity);
                        mFingerprintImageView.setImageResource(ObfuscationHelper.MM_Res.Finger_icon);
                        mFingerPrintLayout.addView(mFingerprintImageView);
                        mPasswordLayout.addView(mFingerPrintLayout);
                        mFingerprintImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mPasswordLayout.removeView(mFingerPrintLayout);
                                mInputEditText.setVisibility(View.VISIBLE);
                                mKeyboard.setVisibility(View.VISIBLE);
                                mFingerprintIdentify.cancelIdentify();
                                mPayTitleTextView.setText(ObfuscationHelper.MM_Res.Passwd_title);
                            }
                        });
                        mPasswordTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.Passwd_Text);
                        mPasswordTextView.setVisibility(View.VISIBLE);
                        mPasswordTextView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mPasswordLayout.removeView(mFingerPrintLayout);
                                mInputEditText.setVisibility(View.VISIBLE);
                                mKeyboard.setVisibility(View.VISIBLE);
                                mFingerprintIdentify.cancelIdentify();
                                mPayTitleTextView.setText(ObfuscationHelper.MM_Res.Passwd_title);
                            }
                        });
                    } else {

                    }
                }
            });

            XposedHelpers.findAndHookMethod(ObfuscationHelper.MM_Classes.Payview, "dismiss", new XC_MethodHook() {
                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    L.d("Payview dismiss");
                    if (mWalletPayUIActivity != null) {
                        mFingerprintIdentify.cancelIdentify();
                        mWalletPayUIActivity = null;
                        mNeedFingerprint = false;
                    }
                }
            });

            final Class<?> className = ObfuscationHelper.MM_Classes.PreferenceAdapter;
            XposedHelpers.findAndHookMethod(className, "getView", int.class, View.class, ViewGroup.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.getResult();
                    if (view == null) {
                        return;
                    }
                    int position = (int) param.args[0];
                    BaseAdapter baseAdapter = (BaseAdapter) param.thisObject;
                    Object item = baseAdapter.getItem(position);
                    if(String.valueOf(item).contains(BuildConfig.APP_SETTINGS_NAME)) {
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View view) {
                                view.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Activity activity = mCurrentActivity;
                                            if (activity == null || activity.isDestroyed()) {
                                                return;
                                            }
                                            SettingsView settingsView = new SettingsView(activity);
                                            settingsView.showInDialog();
                                        } catch (Exception | Error e) {
                                            L.e(e);
                                        }
                                    }
                                });

                            }
                        });
                        L.d(item);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(className, "notifyDataSetChanged", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                    Field vpQField = className.getDeclaredField(ObfuscationHelper.MM_Fields.PreferenceAdapter_vpQ);
                    vpQField.setAccessible(true);
                    HashMap<String, Object> vpQ = (HashMap<String, Object>) vpQField.get(param.thisObject);

                    if (!vpQ.toString().contains("通用")) {
                        return;
                    }
                    Field vpPField = className.getDeclaredField(ObfuscationHelper.MM_Fields.PreferenceAdapter_vpP);
                    vpPField.setAccessible(true);
                    LinkedList<String> vpP = (LinkedList<String>) vpPField.get(param.thisObject);

                    String key = BuildConfig.APPLICATION_ID;
                    if (vpP.contains(key)) {
                        return;
                    }

                    Class<?> preferenceClz = XposedHelpers.findClass("com.tencent.mm.ui.base.preference.Preference", lpparam.classLoader);
                    Constructor<?> preferenceCon = preferenceClz.getConstructor(Context.class);
                    preferenceCon.setAccessible(true);
                    Object preference = preferenceCon.newInstance(context);

                    Method setTitleMethod = preferenceClz.getDeclaredMethod("setTitle", CharSequence.class);
                    setTitleMethod.setAccessible(true);
                    setTitleMethod.invoke(preference, BuildConfig.APP_SETTINGS_NAME);

                    Method setSummaryMethod = preferenceClz.getDeclaredMethod("setSummary", CharSequence.class);
                    setSummaryMethod.setAccessible(true);
                    setSummaryMethod.invoke(preference, BuildConfig.VERSION_NAME);

                    vpP.add(0, key);
                    vpQ.put(key, preference);
                }

                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }

    public static void initFingerPrintLock() {
        mFingerprintIdentify = new FingerprintIdentify(mWalletPayUIActivity);
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(3, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    Toast.makeText(mWalletPayUIActivity, "指纹识别成功", Toast.LENGTH_SHORT).show();
                    L.e("指纹识别成功");
                    onSuccessUnlock(mWalletPayUIActivity);
                }

                @Override
                public void onNotMatch(int availableTimes) {
                    // 指纹不匹配，并返回可用剩余次数并自动继续验证
                    L.e("指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次");
                    Toast.makeText(mWalletPayUIActivity, "指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailed(boolean isDeviceLocked) {
                    // 错误次数达到上限或者API报错停止了验证，自动结束指纹识别
                    // isDeviceLocked 表示指纹硬件是否被暂时锁定
                    L.e("多次尝试错误，请确认指纹");
                    Toast.makeText(mWalletPayUIActivity, "多次尝试错误，请确认指纹", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.e("系统限制，重启后必须验证密码后才能使用指纹验证");
                    Toast.makeText(mWalletPayUIActivity, "系统限制，重启后必须验证密码后才能使用指纹验证", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            L.e("系统指纹功能未启用");
            Toast.makeText(mWalletPayUIActivity, "系统指纹功能未启用", Toast.LENGTH_SHORT).show();
        }
    }

    private static void onSuccessUnlock(Context context) {
        Config config = new Config(context);
        String pwd = config.getPassword();
        if (TextUtils.isEmpty(pwd)) {
            Toast.makeText(mWalletPayUIActivity, "未设定支付密码，请前往設置->指紋設置中设定微信的支付密码", Toast.LENGTH_SHORT).show();
            return;
        }
        mInputEditText.setText(pwd);
    }
}
