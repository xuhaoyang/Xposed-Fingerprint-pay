package com.yyxx.wechatfp.xposed.plugin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.yyxx.wechatfp.util.DpUtil;
import com.yyxx.wechatfp.util.ImageUtil;
import com.yyxx.wechatfp.util.StyleUtil;
import com.yyxx.wechatfp.util.Task;
import com.yyxx.wechatfp.util.Umeng;
import com.yyxx.wechatfp.util.ViewUtil;
import com.yyxx.wechatfp.util.log.L;
import com.yyxx.wechatfp.view.SettingsView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Jason on 2017/9/8.
 */

public class XposedWeChatPlugin {

    private FingerprintIdentify mFingerprintIdentify;
    private Activity mCurrentActivity;
    private boolean mMockCurrentUser = false;

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            Umeng.init(context);
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
                    Activity activity = (Activity) param.thisObject;
                    L.d("Activity onResume =", activity);
                    mCurrentActivity = activity;
                    if (firstStartUp) {
                        Task.onMain(6000L, () -> UpdateFactory.doUpdateCheck(activity));
                    }
                    final String activityClzName = activity.getClass().getName();
                    if (activityClzName.contains("com.tencent.mm.plugin.setting.ui.setting.SettingsUI")) {
                        Task.onMain(100, () -> doSettingsMenuInject(activity));
                    }
                }
            });

            XposedHelpers.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!"com.tencent.mm.plugin.wallet_core.ui.l".equals(param.thisObject.getClass().getName())) {
                        return;
                    }
                    L.d("PayDialog Constructor", param.thisObject);
                    if (Config.from(context).isOn()) {
                        ViewGroup rootView = (ViewGroup) ((Dialog) param.thisObject).getWindow().getDecorView();
                        Context context = rootView.getContext();
                        PayDialog payDialogView = PayDialog.findFrom(rootView);
                        L.d(payDialogView);
                        if (payDialogView == null) {
                            notifyCurrentVersionUnSupport(context);
                            return;
                        }

                        ViewGroup passwordLayout = payDialogView.passwordLayout;
                        EditText mInputEditText = payDialogView.inputEditText;
                        View keyboardView = payDialogView.keyboardView;
                        TextView usePasswordText = payDialogView.usePasswordText;
                        TextView titleTextView = payDialogView.titleTextView;

                        RelativeLayout fingerPrintLayout = new RelativeLayout(context);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        fingerPrintLayout.setLayoutParams(layoutParams);
                        ImageView fingerprintImageView = new ImageView(context);

                        try {
                            final Bitmap bitmap = ImageUtil.base64ToBitmap(Constant.ICON_FINGER_PRINT_WECHAT_BASE64);
                            fingerprintImageView.setImageBitmap(bitmap);
                            fingerprintImageView.getViewTreeObserver().addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
                                @Override
                                public void onWindowAttached() {

                                }

                                @Override
                                public void onWindowDetached() {
                                    fingerprintImageView.getViewTreeObserver().removeOnWindowAttachListener(this);
                                    try {
                                        bitmap.recycle();
                                    } catch (Exception e) {
                                    }
                                }
                            });
                        } catch (OutOfMemoryError e) {
                            L.d(e);
                        }
                        fingerPrintLayout.addView(fingerprintImageView);


                        final Runnable switchToFingerprintRunnable = ()-> {
                            mInputEditText.setVisibility(View.GONE);
                            keyboardView.setVisibility(View.GONE);
                            passwordLayout.addView(fingerPrintLayout);

                            initFingerPrintLock(context, ()-> {
                                //SUCCESS UNLOCK
                                Config config = Config.from(context);
                                String pwd = config.getPassword();
                                if (TextUtils.isEmpty(pwd)) {
                                    Toast.makeText(context, Lang.getString(Lang.TOAST_PASSWORD_NOT_SET_WECHAT), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                mInputEditText.setText(pwd);
                            });
                            if (titleTextView != null) {
                                titleTextView.setText(Lang.getString(Lang.WECHAT_PAYVIEW_FINGERPRINT_TITLE));
                            }
                            if (usePasswordText != null) {
                                usePasswordText.setText(Lang.getString(Lang.WECHAT_PAYVIEW_PASSWORD_SWITCH_TEXT));
                            }
                        };

                        final Runnable switchToPasswordRunnable = ()-> {
                            passwordLayout.removeView(fingerPrintLayout);
                            mInputEditText.setVisibility(View.VISIBLE);
                            keyboardView.setVisibility(View.VISIBLE);
                            mFingerprintIdentify.cancelIdentify();
                            mMockCurrentUser = false;
                            if (titleTextView != null) {
                                titleTextView.setText(Lang.getString(Lang.WECHAT_PAYVIEW_PASSWORD_TITLE));
                            }
                            if (usePasswordText != null) {
                                usePasswordText.setText(Lang.getString(Lang.WECHAT_PAYVIEW_FINGERPRINT_SWITCH_TEXT));
                            }
                        };

                        if (usePasswordText != null) {
                            Task.onMain(()-> usePasswordText.setVisibility(View.VISIBLE));
                            usePasswordText.setOnTouchListener((view, motionEvent) -> {
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

                        fingerprintImageView.setOnClickListener(view -> switchToPasswordRunnable.run());
                        switchToFingerprintRunnable.run();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(Dialog.class, "dismiss", new XC_MethodHook() {
                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!"com.tencent.mm.plugin.wallet_core.ui.l".equals(param.thisObject.getClass().getName())) {
                        return;
                    }
                    L.d("PayDialog dismiss");
                    if (Config.from(context).isOn()) {
                        mFingerprintIdentify.cancelIdentify();
                        mMockCurrentUser = false;
                    }
                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }

    private void doSettingsMenuInject(final Activity activity) {
        ListView itemView = (ListView)ViewUtil.findViewByName(activity, "android", "list");
        if (ViewUtil.findViewByText(itemView, Lang.getString(Lang.APP_SETTINGS_NAME)) != null) {
            return;
        }

        LinearLayout settingsItemRootLLayout = new LinearLayout(activity);
        settingsItemRootLLayout.setOrientation(LinearLayout.VERTICAL);
        settingsItemRootLLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        settingsItemRootLLayout.setPadding(0, DpUtil.dip2px(activity, 20), 0, 0);

        LinearLayout settingsItemLinearLayout = new LinearLayout(activity);
        settingsItemLinearLayout.setOrientation(LinearLayout.VERTICAL);

        settingsItemLinearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(ViewUtil.genBackgroundDefaultDrawable(Color.WHITE));
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtil.dip2px(activity, 15);

        TextView itemNameText = new TextView(activity);
        itemNameText.setTextColor(0xFF353535);
        itemNameText.setText(Lang.getString(Lang.APP_SETTINGS_NAME));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(DpUtil.dip2px(activity, 14), 0, 0, 0);
        itemNameText.setTextSize(StyleUtil.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtil.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(0xFF999999);

        //try use WeChat style
        try {
            View generalView = ViewUtil.findViewByText(itemView, "通用", "一般", "General");
            L.d("generalView", generalView);
            if (generalView instanceof TextView) {
                TextView generalTextView = (TextView) generalView;
                float scale = itemNameText.getTextSize() / generalTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, generalTextView.getTextSize());

                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                View generalItemView = (View) generalView.getParent().getParent().getParent().getParent().getParent().getParent();
                if (generalItemView != null) {
                    Drawable background = generalItemView.getBackground();
                    if (background != null) {
                        Drawable.ConstantState constantState = background.getConstantState();
                        if (constantState != null) {
                            itemHlinearLayout.setBackground(constantState.newDrawable());
                        }
                    }
                }
                itemNameText.setTextColor(generalTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        settingsItemLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtil.dip2px(activity, 50)));

        settingsItemRootLLayout.addView(settingsItemLinearLayout);

        itemView.addHeaderView(settingsItemRootLLayout);

    }

    public synchronized void initFingerPrintLock(Context context, Runnable onSuccessUnlockRunnable) {
        mMockCurrentUser = true;
        mFingerprintIdentify = new FingerprintIdentify(context, exception -> L.e("fingerprint", exception));
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(3, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    Toast.makeText(context, Lang.getString(Lang.TOAST_FINGERPRINT_MATCH), Toast.LENGTH_SHORT).show();
                    L.d("指纹识别成功");
                    onSuccessUnlockRunnable.run();
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


    public boolean isCurrentUserOwner(Context context) {
        try {
            Method getUserHandle = UserManager.class.getMethod("getUserHandle");
            int userHandle = (Integer) getUserHandle.invoke(context.getSystemService(Context.USER_SERVICE));
            return userHandle == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private static class PayDialog {

        private ViewGroup passwordLayout;
        private EditText inputEditText;
        private View keyboardView;
        @Nullable
        private TextView usePasswordText;
        @Nullable
        private TextView titleTextView;



        @Nullable
        public static PayDialog findFrom(ViewGroup rootView) {
            try {
                PayDialog payDialog = new PayDialog();

                List<View> childViews = new ArrayList<>();
                ViewUtil.getChildViews(rootView, childViews);
                for (View view : childViews) {
                    if (view == null) {
                        continue;
                    }
                    if (view.getClass().getName().endsWith(".EditHintPasswdView")) {
                        L.d("mPasswordLayout:" + view);
                        if (view instanceof ViewGroup) {
                            payDialog.passwordLayout = (ViewGroup)view;
                        }
                    } else if (view.getClass().getName().endsWith(".TenpaySecureEditText")) {
                        L.d("密码输入框:" + view);
                        if (view instanceof EditText) {
                            payDialog.inputEditText = (EditText)view;
                        }
                    } else if (view.getClass().getName().endsWith(".MyKeyboardWindow")) {
                        L.d("密码键盘:" + view);
                        if (view.getParent() != null) {
                            payDialog.keyboardView = (View)view.getParent();
                        }
                    }
                }

                if (payDialog.passwordLayout == null) {
                    doUnSupportVersionUpload(rootView.getContext(), "[passwordLayout NOT FOUND]  " + viewsDesc(childViews));
                    return null;
                }

                if (payDialog.inputEditText == null) {
                    doUnSupportVersionUpload(rootView.getContext(), "[inputEditText NOT FOUND]  " + viewsDesc(childViews));
                    return null;
                }

                if (payDialog.keyboardView == null) {
                    doUnSupportVersionUpload(rootView.getContext(), "[keyboardView NOT FOUND]  " + viewsDesc(childViews));
                    return null;
                }

                payDialog.usePasswordText = (TextView)ViewUtil.findViewByText(rootView,
                        Lang.getString(Lang.WECHAT_PAYVIEW_FINGERPRINT_SWITCH_TEXT),
                        Lang.getString(Lang.WECHAT_PAYVIEW_PASSWORD_SWITCH_TEXT));
                if (payDialog.usePasswordText == null) {
                    doUnSupportVersionUpload(rootView.getContext(), "[usePasswordText NOT FOUND]  " + viewsDesc(childViews));
                }

                payDialog.titleTextView = (TextView)ViewUtil.findViewByText(rootView,
                        Lang.getString(Lang.WECHAT_PAYVIEW_FINGERPRINT_TITLE),
                        Lang.getString(Lang.WECHAT_PAYVIEW_PASSWORD_TITLE));

                return payDialog;
            } catch (Exception e) {
                L.e(e);
            }
            return null;
        }

        @Override
        public String toString() {
            return "PayDialog{" +
                    "passwordLayout=" + passwordLayout +
                    ", inputEditText=" + inputEditText +
                    ", keyboardView=" + keyboardView +
                    ", usePasswordText=" + usePasswordText +
                    ", titleTextView=" + titleTextView +
                    '}';
        }
    }

    private static String viewsDesc(List<View> childView) {
        StringBuffer stringBuffer = new StringBuffer();
        for (View view: childView) {
            stringBuffer.append(ViewUtil.getViewInfo(view)).append("\n");
        }
        return stringBuffer.toString();
    }

    private static void doUnSupportVersionUpload(Context context, String message) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constant.PACKAGE_NAME_WECHAT, 0);
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;

            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("WeChat un support: versionName:").append(versionName)
                    .append(" versionCode:").append(versionCode)
                    .append(" viewInfos:").append(message);

            L.e(stringBuffer);
        } catch (Exception e) {
            L.e(e);
        }
    }

    private static void notifyCurrentVersionUnSupport(final Context context) {
        Task.onMain(()->{
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constant.PACKAGE_NAME_WECHAT, 0);
                int versionCode = packageInfo.versionCode;
                String versionName = packageInfo.versionName;
                Toast.makeText(context, "当前版本:" + versionName + "." + versionCode + "不支持", Toast.LENGTH_LONG).show();
                L.d("当前版本:" + versionName + "." + versionCode + "不支持");
            } catch (Exception e) {
                L.e(e);
            }
        });
    }
}
