package com.yyxx.wechatfp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Keep;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.yyxx.wechatfp.Utils.AESHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.yyxx.wechatfp.WalletBaseUI.WECHAT_PACKAGENAME;

/**
 * Created by Jason on 2017/9/8.
 */

public class XposedPlugin {

    private static Activity mWalletPayUIActivity;
    private static EditText mInputEditText;
    private RelativeLayout mPasswordLayout;
    private ImageView mFingerprintImageView;
    private RelativeLayout mFingerPrintLayout;
    private TextView mPayTitleTextView, mPasswordTextView;
    private static FingerprintIdentify mFingerprintIdentify;
    private static boolean mNeedFingerprint;

    @Keep
    public void main(final Context context, XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {

            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;
            boolean isVersionSupported = ObfuscationHelper.init(versionCode, versionName, lpparam);
            if (!isVersionSupported) {
                Toast.makeText(context, "当前版本:" + versionName + "." + versionCode + "不支持", Toast.LENGTH_LONG).show();
                XposedBridge.log("当前版本:" + versionName + "." + versionCode + "不支持");
                return;
            }

            XposedHelpers.findAndHookMethod(ObfuscationHelper.MM_Classes.PayUI, "onCreate", Bundle.class, new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("PayUI onCreate");
                    mWalletPayUIActivity = (Activity) param.thisObject;
                    mNeedFingerprint = true;
                }
            });

            XposedHelpers.findAndHookMethod(ObfuscationHelper.MM_Classes.FetchUI, "onCreate", Bundle.class, new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("FetchUI onCreate");
                    mWalletPayUIActivity = (Activity) param.thisObject;
                    mNeedFingerprint = true;
                }
            });

            XposedHelpers.findAndHookConstructor(ObfuscationHelper.MM_Classes.Payview, Context.class, new XC_MethodHook() {
                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Payview Constructor");
                    SharedPreferences xmodPrefs = XPreferenceProvider.getRemoteSharedPreference(context);
                    XposedBridge.log("设置数量" + String.valueOf(xmodPrefs.getAll().size()));
                    boolean fingerPrintEnabled = xmodPrefs.getBoolean("enable_fp", false);
                    XposedBridge.log("fingerPrintEnabled:" + fingerPrintEnabled);

                    if (fingerPrintEnabled && mNeedFingerprint && mWalletPayUIActivity != null) {
                        initFingerPrintLock();
                        mPasswordLayout = (RelativeLayout) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PaypwdView);
                        mInputEditText = (EditText) XposedHelpers.getObjectField(mPasswordLayout, ObfuscationHelper.MM_Fields.PaypwdEditText);
                        XposedBridge.log("密码输入框:" + mInputEditText.getClass().getName());
                        mInputEditText.setVisibility(View.GONE);
                        mPayTitleTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PayTitle);
                        mPayTitleTextView.setText(ObfuscationHelper.MM_Res.Finger_title);
                        final View mKeyboard = (View) XposedHelpers.getObjectField(param.thisObject, ObfuscationHelper.MM_Fields.PayInputView);
                        XposedBridge.log("密码键盘:" + mKeyboard.getClass().getName());
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
                                mPayTitleTextView.setText(ObfuscationHelper.MM_Res.passwd_title);
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
                                mPayTitleTextView.setText(ObfuscationHelper.MM_Res.passwd_title);
                            }
                        });
                    } else {

                    }
                }
            });

            XposedHelpers.findAndHookMethod(ObfuscationHelper.MM_Classes.Payview, "dismiss", new XC_MethodHook() {
                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Payview dismiss");
                    if (mWalletPayUIActivity != null) {
                        mFingerprintIdentify.cancelIdentify();
                        mWalletPayUIActivity = null;
                        mNeedFingerprint = false;
                    }
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
                    onSuccessUnlock(mWalletPayUIActivity);
                }

                @Override
                public void onNotMatch(int availableTimes) {
                    // 指纹不匹配，并返回可用剩余次数并自动继续验证
                    Toast.makeText(mWalletPayUIActivity, "指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailed(boolean isDeviceLocked) {
                    // 错误次数达到上限或者API报错停止了验证，自动结束指纹识别
                    // isDeviceLocked 表示指纹硬件是否被暂时锁定
                    Toast.makeText(mWalletPayUIActivity, "多次尝试错误，请确认指纹", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    Toast.makeText(mWalletPayUIActivity, "系统限制，重启后必须验证密码后才能使用指纹验证", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static void onSuccessUnlock(Context context) {
        String pwd;
        String ANDROID_ID = Settings.System.getString(mWalletPayUIActivity.getContentResolver(), Settings.System.ANDROID_ID);

        SharedPreferences xmodPrefs = XPreferenceProvider.getRemoteSharedPreference(context);
        XposedBridge.log("设置数量" + String.valueOf(xmodPrefs.getAll().size()));
        if (xmodPrefs.getAll().size() > 0) {
            pwd = xmodPrefs.getString("paypwd", "");
        } else {
            pwd = "";
        }
        if (pwd.length() > 0) {
            mInputEditText.setText(AESHelper.decrypt(pwd, ANDROID_ID));
        } else {
            Toast.makeText(mWalletPayUIActivity, "未设定支付密码，请在WechatFp中设定微信的支付密码", Toast.LENGTH_SHORT).show();
        }

    }
}
