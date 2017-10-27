package com.yyxx.wechatfp.xposed.plugin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import com.yyxx.wechatfp.Lang;
import com.yyxx.wechatfp.network.updateCheck.UpdateFactory;
import com.yyxx.wechatfp.util.Config;
import com.yyxx.wechatfp.util.ImageUtil;
import com.yyxx.wechatfp.util.Task;
import com.yyxx.wechatfp.util.Umeng;
import com.yyxx.wechatfp.util.ViewUtil;
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

    private EditText mInputEditText;
    private RelativeLayout mPasswordLayout;
    private ImageView mFingerprintImageView;
    private RelativeLayout mFingerPrintLayout;
    private TextView mPayTitleTextView, mPasswordTextView;
    private FingerprintIdentify mFingerprintIdentify;
    private Activity mCurrentActivity;
    private boolean mMockCurrentUser = false;

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            Umeng.init(context);
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constant.PACKAGE_NAME_WECHAT, 0);
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;
            boolean isVersionSupported = ObfuscationHelper.init(versionCode, versionName, lpparam);
            if (!isVersionSupported) {
                Toast.makeText(context, "当前版本:" + versionName + "." + versionCode + "不支持", Toast.LENGTH_LONG).show();
                L.d("当前版本:" + versionName + "." + versionCode + "不支持");
                return;
            }
            //for multi user
            if (!isCurrentUserOwner(context)) {
                XposedHelpers.findAndHookMethod(UserHandle.class, "myUserId", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mMockCurrentUser) {
                            param.setResult(0);
                        }
                    }
                });
            }
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    boolean firstStartUp = mCurrentActivity == null;
                    mCurrentActivity = (Activity) param.thisObject;
                    L.d("Activity onResume =", mCurrentActivity);
                    if (firstStartUp) {
                        Task.onMain(6000L, () -> UpdateFactory.doUpdateCheck(mCurrentActivity));
                    }
                }
            });

            XposedHelpers.findAndHookConstructor(ObfuscationHelper.MM_Classes.PayView, Context.class, new XC_MethodHook() {
                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    L.d("PayView Constructor");
                    if (Config.from(context).isOn()) {
                        mPasswordLayout = (RelativeLayout) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PaypwdView);
                        Context context = mPasswordLayout.getContext();

                        mInputEditText = (EditText) XposedHelpers.getObjectField(mPasswordLayout, ObfuscationHelper.MM_Fields.PaypwdEditText);
                        L.d("密码输入框:" + mInputEditText.getClass().getName());
                        mPayTitleTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PayTitle);
                        final View mKeyboard = (View) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PayInputView);
                        L.d("密码键盘:" + mKeyboard.getClass().getName());
                        mFingerPrintLayout = new RelativeLayout(context);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        mFingerPrintLayout.setLayoutParams(layoutParams);
                        mFingerprintImageView = new ImageView(context);

                        try {
                            final Bitmap bitmap = ImageUtil.base64ToBitmap(Constant.ICON_FINGER_PRINT_WECHAT_BASE64);
                            mFingerprintImageView.setImageBitmap(bitmap);
                            mFingerprintImageView.getViewTreeObserver().addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
                                @Override
                                public void onWindowAttached() {

                                }

                                @Override
                                public void onWindowDetached() {
                                    mFingerprintImageView.getViewTreeObserver().removeOnWindowAttachListener(this);
                                    try {
                                        bitmap.recycle();
                                    } catch (Exception e) {
                                    }
                                }
                            });
                        } catch (OutOfMemoryError e) {
                            L.d(e);
                        }
                        mFingerPrintLayout.addView(mFingerprintImageView);

                        TextView switchFpPwdTextView = (TextView) ViewUtil.findViewByText(mPasswordLayout.getRootView(),
                                Lang.getString(Lang.WECHAT_PAYVIEW_FINGERPRINT_SWITCH_TEXT),
                                Lang.getString(Lang.WECHAT_PAYVIEW_PASSWORD_SWITCH_TEXT));

                        final Runnable switchToFingerprintRunnable = ()-> {
                            mInputEditText.setVisibility(View.GONE);
                            mKeyboard.setVisibility(View.GONE);
                            mPasswordLayout.addView(mFingerPrintLayout);
                            initFingerPrintLock(context);
                            mPayTitleTextView.setText(Lang.getString(Lang.WECHAT_PAYVIEW_FINGERPRINT_TITLE));
                            if (switchFpPwdTextView != null) {
                                switchFpPwdTextView.setText(Lang.getString(Lang.WECHAT_PAYVIEW_PASSWORD_SWITCH_TEXT));
                            }
                        };

                        final Runnable switchToPasswordRunnable = ()-> {
                            mPasswordLayout.removeView(mFingerPrintLayout);
                            mInputEditText.setVisibility(View.VISIBLE);
                            mKeyboard.setVisibility(View.VISIBLE);
                            mFingerprintIdentify.cancelIdentify();
                            mMockCurrentUser = false;
                            mPayTitleTextView.setText(Lang.getString(Lang.WECHAT_PAYVIEW_PASSWORD_TITLE));
                            if (switchFpPwdTextView != null) {
                                switchFpPwdTextView.setText(Lang.getString(Lang.WECHAT_PAYVIEW_FINGERPRINT_SWITCH_TEXT));
                            }
                        };

                        if (switchFpPwdTextView != null) {
                            Task.onMain(()-> switchFpPwdTextView.setVisibility(View.VISIBLE));
                            switchFpPwdTextView.setOnTouchListener((view, motionEvent) -> {
                                try {
                                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                                        if (mInputEditText.getVisibility() == View.GONE) {
                                            switchToPasswordRunnable.run();
                                        } else {
                                            switchToFingerprintRunnable.run();
                                        }
                                    }
                                } catch (Exception e) {
                                    L.e(e);
                                }
                                return true;
                            });
                        }

                        mFingerprintImageView.setOnClickListener(view -> switchToPasswordRunnable.run());
                        mPasswordTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.Passwd_Text);
                        mPasswordTextView.setVisibility(View.VISIBLE);
                        mPasswordTextView.setOnClickListener(view -> switchToPasswordRunnable.run());
                        switchToFingerprintRunnable.run();
                    } else {

                    }
                }
            });

            XposedHelpers.findAndHookMethod(ObfuscationHelper.MM_Classes.PayView, "dismiss", new XC_MethodHook() {
                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    L.d("PayView dismiss");
                    if (Config.from(context).isOn()) {
                        mFingerprintIdentify.cancelIdentify();
                        mMockCurrentUser = false;
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
                    if(String.valueOf(item).contains(Lang.getString(Lang.APP_SETTINGS_NAME))) {
                        view.setOnClickListener(view1 -> view1.post(() -> {
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
                        }));
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

                    if (!vpQ.toString().contains(Lang.getString(Lang.WECHAT_GENERAL))) {
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
                    setTitleMethod.invoke(preference, Lang.getString(Lang.APP_SETTINGS_NAME));

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

    public synchronized void initFingerPrintLock(Context context) {
        mMockCurrentUser = true;
        mFingerprintIdentify = new FingerprintIdentify(context, exception -> L.e("fingerprint", exception));
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(3, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    Toast.makeText(context, Lang.getString(Lang.TOAST_FINGERPRINT_MATCH), Toast.LENGTH_SHORT).show();
                    L.d("指纹识别成功");
                    onSuccessUnlock(context);
                    mMockCurrentUser = false;
                }

                @Override
                public void onNotMatch(int availableTimes) {
                    // 指纹不匹配，并返回可用剩余次数并自动继续验证
                    L.d("指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次");
                    Toast.makeText(context, Lang.getString(Lang.TOAST_FINGERPRINT_NOT_MATCH), Toast.LENGTH_SHORT).show();
                    mMockCurrentUser = false;
                }

                @Override
                public void onFailed(boolean isDeviceLocked) {
                    // 错误次数达到上限或者API报错停止了验证，自动结束指纹识别
                    // isDeviceLocked 表示指纹硬件是否被暂时锁定
                    L.d("多次尝试错误，请确认指纹");
                    Toast.makeText(context, Lang.getString(Lang.TOAST_FINGERPRINT_RETRY_ENDED), Toast.LENGTH_SHORT).show();
                    mMockCurrentUser = false;
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.d("系统限制，重启后必须验证密码后才能使用指纹验证");
                    Toast.makeText(context, Lang.getString(Lang.TOAST_FINGERPRINT_UNLOCK_REBOOT), Toast.LENGTH_SHORT).show();
                    mMockCurrentUser = false;
                }
            });
        } else {
            L.d("系统指纹功能未启用");
            Toast.makeText(context, Lang.getString(Lang.TOAST_FINGERPRINT_NOT_ENABLE), Toast.LENGTH_SHORT).show();
            mMockCurrentUser = false;
        }
    }

    private void onSuccessUnlock(Context context) {
        Config config = Config.from(context);
        String pwd = config.getPassword();
        if (TextUtils.isEmpty(pwd)) {
            Toast.makeText(context, Lang.getString(Lang.TOAST_PASSWORD_NOT_SET_WECHAT), Toast.LENGTH_SHORT).show();
            return;
        }
        mInputEditText.setText(pwd);
    }


    public boolean isCurrentUserOwner(Context context) {
        try {
            Method getUserHandle = UserManager.class.getMethod("getUserHandle");
            int userHandle = (Integer) getUserHandle.invoke(context.getSystemService(Context.USER_SERVICE));
            return userHandle == 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
