package com.yyxx.wechatfp.xposed.plugin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.util.Config;
import com.yyxx.wechatfp.util.DpUtil;
import com.yyxx.wechatfp.util.ImageUtil;
import com.yyxx.wechatfp.util.StyleUtil;
import com.yyxx.wechatfp.util.Task;
import com.yyxx.wechatfp.util.Umeng;
import com.yyxx.wechatfp.util.ViewUtil;
import com.yyxx.wechatfp.util.log.L;
import com.yyxx.wechatfp.view.SettingsView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.yyxx.wechatfp.Constant.ICON_FINGER_PRINT_BASE64;

/**
 * Created by Jason on 2017/9/8.
 */

public class XposedAlipayPlugin {

    private AlertDialog mFingerPrintAlertDialog;
    private boolean mPwdActivityDontShowFlag;

    private FingerprintIdentify mFingerprintIdentify;
    private Activity mCurrentActivity;

    private boolean mIsViewTreeObserverFirst;
    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            Umeng.init(context);
            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(lpparam.packageName, 0);
            final int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;
            L.d("app info: versionCode:" + versionCode + " versionName:" + versionName);

            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {

                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Activity activity = (Activity) param.thisObject;
                    final String activityClzName = activity.getClass().getName();
                    mCurrentActivity = activity;
                    if (activityClzName.contains(".UserSettingActivity")) {
                        Task.onMain(10, () -> doSettingsMenuInject(activity));
                    } else if (activityClzName.contains(".FlyBirdWindowActivity")) {
                        L.d("found");
                        final Config config = new Config(activity);
                        if (!config.isOn()) {
                            return;
                        }
                        mIsViewTreeObserverFirst = true;
                        activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                            if (mCurrentActivity == null) {
                                return;
                            }
                            if (activity.isDestroyed()) {
                                return;
                            }
                            if (mCurrentActivity != activity) {
                                return;
                            }
                            View key1View = ViewUtil.findViewByName(activity, "com.alipay.android.app", "simplePwdLayout");
                            if (key1View == null) {
                                return;
                            }
                            if (mIsViewTreeObserverFirst) {
                                mIsViewTreeObserverFirst = false;
                                showFingerPrintDialog(activity);
                            }
                        });

                    } else if (activityClzName.contains("PayPwdHalfActivity")) {
                        L.d("found");
                        final Config config = new Config(activity);
                        if (!config.isOn()) {
                            return;
                        }
                        activity.getWindow().getDecorView().setAlpha(0);
                        Task.onMain(1500, () -> {
                            final String modulePackageName = "com.alipay.android.phone.safepaybase";
                            View key1View = ViewUtil.findViewByName(activity, modulePackageName, "key_num_1");
                            if (key1View != null) {
                                showFingerPrintDialog(activity);
                                return;
                            }

                            //try again
                            Task.onMain(2000, () -> showFingerPrintDialog(activity));
                        });
                    }
                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }


    public void initFingerPrintLock(final Context context, final Runnable onSuccessUnlockCallback) {
        mFingerprintIdentify = new FingerprintIdentify(context);
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(3, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    Toast.makeText(context, "指纹识别成功", Toast.LENGTH_SHORT).show();
                    L.e("指纹识别成功");
                    onSuccessUnlockCallback.run();
                }

                @Override
                public void onNotMatch(int availableTimes) {
                    // 指纹不匹配，并返回可用剩余次数并自动继续验证
                    L.e("指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次");
                    Toast.makeText(context, "指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailed(boolean isDeviceLocked) {
                    // 错误次数达到上限或者API报错停止了验证，自动结束指纹识别
                    // isDeviceLocked 表示指纹硬件是否被暂时锁定
                    L.e("多次尝试错误，请使用密码输入");
                    Toast.makeText(context, "多次尝试错误，请使用密码输入", Toast.LENGTH_SHORT).show();
                    AlertDialog dialog = mFingerPrintAlertDialog;
                    if (dialog != null) {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.e("系统限制，重启后必须验证密码后才能使用指纹验证");
                    Toast.makeText(context, "系统限制，重启后必须验证密码后才能使用指纹验证", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            L.e("系统指纹功能未启用");
            Toast.makeText(context, "系统指纹功能未启用", Toast.LENGTH_SHORT).show();
        }
    }

    public void showFingerPrintDialog(final Activity activity) {
        final Context context = activity;
        try {
            activity.getWindow().getDecorView().setAlpha(0);
            mPwdActivityDontShowFlag = false;
            int defVMargin = DpUtil.dip2px(context, 30);
            final Bitmap bitmap = ImageUtil.base64ToBitmap(ICON_FINGER_PRINT_BASE64);
            LinearLayout rootVLinearLayout = new LinearLayout(context);
            rootVLinearLayout.setOrientation(LinearLayout.VERTICAL);
            rootVLinearLayout.setGravity(Gravity.CENTER_HORIZONTAL);

            ImageView imageView = new ImageView(context);
            imageView.setImageBitmap(bitmap);

            TextView textView = new TextView(context);
            StyleUtil.apply(textView);
            textView.setText("请验证已有指纹");

            View lineVView = new View(context);
            lineVView.setBackgroundColor(0xFFBBBBBB);

            LinearLayout buttonHLinearLayout = new LinearLayout(context);
            buttonHLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonHLinearLayout.setWeightSum(2);


            Button cancelBtn = new Button(context);
            cancelBtn.setBackground(ViewUtil.genBackgroundDefaultDrawable());
            cancelBtn.setText("取消");
            StyleUtil.apply(cancelBtn);
            cancelBtn.setOnClickListener(view -> {
                mPwdActivityDontShowFlag = true;
                AlertDialog dialog = mFingerPrintAlertDialog;
                if (dialog != null) {
                    dialog.dismiss();
                }
                activity.onBackPressed();
            });

            View lineHView = new View(context);
            lineHView.setBackgroundColor(0xFFBBBBBB);

            Button enterPassBtn = new Button(context);
            enterPassBtn.setBackground(ViewUtil.genBackgroundDefaultDrawable());
            enterPassBtn.setText("输入密码");
            StyleUtil.apply(enterPassBtn);
            enterPassBtn.setOnClickListener(view -> {
                AlertDialog dialog = mFingerPrintAlertDialog;
                if (dialog != null) {
                    dialog.dismiss();
                }
            });

            buttonHLinearLayout.addView(cancelBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
            buttonHLinearLayout.addView(lineHView, new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT));
            buttonHLinearLayout.addView(enterPassBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(DpUtil.dip2px(context, 60), DpUtil.dip2px(context, 60));
            params.topMargin = defVMargin;
            params.bottomMargin = defVMargin;
            rootVLinearLayout.addView(imageView, params);
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = defVMargin;
            rootVLinearLayout.addView(textView, params);
            rootVLinearLayout.addView(lineVView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            rootVLinearLayout.addView(buttonHLinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtil.dip2px(context, 60)));

            initFingerPrintLock(context, () -> {
                String pwd = new Config(activity).getPassword();
                if (TextUtils.isEmpty(pwd)) {
                    Toast.makeText(activity, "未设定支付密码，请前往設置->指紋設置中设定支付宝的支付密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                String modulePackageName = "com.alipay.android.phone.safepaybase";
                View keysView[] = new View[] {
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_1"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_2"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_3"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_4", "key_4"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_5"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_6"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_7"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_8"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_9"),
                        ViewUtil.findViewByName(activity, modulePackageName, "key_num_0"),
                };

                try {
                    inputPassword(pwd, keysView);
                } catch (NullPointerException e) {
                    Toast.makeText(context, "Oops.. 输入失败了. 请手动输入密码", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Oops.. 输入失败了. 请手动输入密码", Toast.LENGTH_LONG).show();
                    L.e(e);
                }
                AlertDialog dialog = mFingerPrintAlertDialog;
                if (dialog != null) {
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog_MinWidth)).setView(rootVLinearLayout).setOnDismissListener(dialogInterface -> {
                FingerprintIdentify fingerprintIdentify = mFingerprintIdentify;
                if (fingerprintIdentify != null) {
                    fingerprintIdentify.cancelIdentify();
                }
                if (!mPwdActivityDontShowFlag) {
                    activity.getWindow().getDecorView().setAlpha(1);
                }
                try {
                    bitmap.recycle();
                } catch (Exception e) {
                }
            }).setCancelable(false).create();
            mFingerPrintAlertDialog = dialog;
            dialog.show();

        } catch (OutOfMemoryError e) {
        }
    }

    private void doSettingsMenuInject(final Activity activity) {
        int logout_id = activity.getResources().getIdentifier("logout", "id", "com.alipay.android.phone.openplatform");

        View logutView = activity.findViewById(logout_id);
        LinearLayout linearLayout = (LinearLayout) logutView.getParent();
        linearLayout.setPadding(0, 0, 0, 0);
        List<ViewGroup.LayoutParams> childViewParamsList = new ArrayList<>();
        List<View> childViewList = new ArrayList<>();
        int childViewCount = linearLayout.getChildCount();
        for (int i = 0; i < childViewCount; i++) {
            View view = linearLayout.getChildAt(i);
            childViewList.add(view);
            childViewParamsList.add(view.getLayoutParams());
        }

        linearLayout.removeAllViews();

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFDFDFDF);

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(ViewUtil.genBackgroundDefaultDrawable(Color.WHITE));
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtil.dip2px(activity, 15);

        TextView itemNameText = new TextView(activity);
        StyleUtil.apply(itemNameText);
        itemNameText.setText(BuildConfig.APP_SETTINGS_NAME);
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(defHPadding, 0, 0, 0);
        itemNameText.setTextSize(StyleUtil.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtil.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(0xFF888888);

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View lineBottomView = new View(activity);
        lineBottomView.setBackgroundColor(0xFFDFDFDF);

        linearLayout.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        linearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtil.dip2px(activity, 50)));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineParams.bottomMargin = DpUtil.dip2px(activity, 20);
        linearLayout.addView(lineBottomView, lineParams);

        for (int i = 0; i < childViewCount; i++) {
            View view = childViewList.get(i);
            ViewGroup.LayoutParams params = childViewParamsList.get(i);
            linearLayout.addView(view, params);
        }
    }

    private void inputPassword(String password, View[] ks) {
        char[] chars = password.toCharArray();
        for (char c : chars) {
            View v;
            switch (c) {
                case '1':
                    v = ks[0];
                    break;
                case '2':
                    v = ks[1];
                    break;
                case '3':
                    v = ks[2];
                    break;
                case '4':
                    v = ks[3];
                    break;
                case '5':
                    v = ks[4];
                    break;
                case '6':
                    v = ks[5];
                    break;
                case '7':
                    v = ks[6];
                    break;
                case '8':
                    v = ks[7];
                    break;
                case '9':
                    v = ks[8];
                    break;
                case '0':
                    v = ks[9];
                    break;
                default:
                    continue;
            }
            ViewUtil.performActionClick(v);
        }
    }
}
