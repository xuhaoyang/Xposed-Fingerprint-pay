package com.yyxx.wechatfp.xposed;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ObfuscationHelper {

    private static int sVersionIdx = 0;

    public static class MM_Classes {
        public static Class<?> PayUI, FetchUI, Payview, WalletBaseUI, PreferenceAdapter;

        private static void init(int idx, LoadPackageParam lpparam) throws Throwable {
            PayUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.pay.ui." + new String[]{
                    "WalletPayUI", //6.5.8
                    "WalletPayUI", //6.5.10-1080
                    "WalletPayUI", //6.5.13-1100
            }[idx], lpparam.classLoader);
            Payview = XposedHelpers.findClass("com.tencent.mm.plugin.wallet_core.ui." + new String[]{
                    "l",  //6.5.8
                    "l",  //6.5.10-1080
                    "l",  //6.5.10-1061
                    "l",  //6.5.13-1100
            }[idx], lpparam.classLoader);
            FetchUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.balance.ui." + new String[]{
                    "WalletBalanceFetchPwdInputUI",  //6.5.8
                    "WalletBalanceFetchPwdInputUI",  //6.5.10-1080
                    "WalletBalanceFetchPwdInputUI",  //6.5.13-1100
            }[idx], lpparam.classLoader);
            WalletBaseUI = XposedHelpers.findClass("com.tencent.mm.wallet_core.ui." + new String[]{
                    "WalletBaseUI",  //6.5.8
                    "WalletBaseUI",  //6.5.10-1080
                    "WalletBaseUI",  //6.5.13-1100
            }[idx], lpparam.classLoader);
            PreferenceAdapter = XposedHelpers.findClass("com.tencent.mm.ui.base.preference." + new String[]{
                    "h",  //6.5.8
                    "h",  //6.5.10-1080
                    "h",  //6.5.13-1100
            }[idx], lpparam.classLoader);
        }
    }

    public static class MM_Fields {
        public static String PaypwdEditText;
        public static String PaypwdView;
        public static String PayInputView;
        public static String PayTitle;
        public static String Passwd_Text;
        public static String PreferenceAdapter_vpQ;
        public static String PreferenceAdapter_vpP;

        private static void init(int idx) throws Throwable {
            PaypwdView = new String[]{
                    "qVO",  //6.5.8
                    "ryk",  //6.5.10-1080
                    "rLB",  //6.5.13-1100
            }[idx];
            PaypwdEditText = new String[]{
                    "vyO",  //6.5.8
                    "wjm",  //6.5.10-1080
                    "wDJ",  //6.5.13-1100
            }[idx];
            PayInputView = new String[]{
                    "mOL",  //6.5.8
                    "nnG",  //6.5.10-1080
                    "nol",  //6.5.13-1100
            }[idx];
            PayTitle = new String[]{
                    "qVK",  //6.5.8
                    "ryg",  //6.5.10-1080
                    "rLw",  //6.5.13-1100
            }[idx];
            Passwd_Text = new String[]{
                    "qVK",  //6.5.8
                    "ryz",  //6.5.10-1080
                    "rLQ",  //6.5.13-1100
            }[idx];
            PreferenceAdapter_vpQ = new String[]{
                    "uoo",  //6.5.8
                    "uYA",  //6.5.10-1080
                    "vpQ",  //6.5.13-1100
            }[idx];
            PreferenceAdapter_vpP = new String[]{
                    "uon",  //6.5.8
                    "uYz",  //6.5.10-1080
                    "vpP",  //6.5.13-1100
            }[idx];
        }
    }

    public static class MM_Res {
        public static int Finger_icon;
        public static int Finger_title;
        public static int Passwd_title;

        private static void init(int idx) throws Throwable {
            Finger_icon = new int[]{
                    2130838280,  //6.5.8
                    2130838289,  //6.5.10-1080
                    2130838298,  //6.5.13-1100
            }[idx];
            Finger_title = new int[]{
                    2131236833,  //6.5.8
                    2131236918,  //6.5.10-1080
                    2131236964,  //6.5.13-1100
            }[idx];
            Passwd_title = new int[]{
                    2131236838,  //6.5.8
                    2131236923,  //6.5.10-1080
                    2131236969,  //6.5.13-1100
            }[idx];
        }
    }


    public static boolean init(int versioncode, String versionName, LoadPackageParam lpparam) throws Throwable {
        int versionIndex = isSupportedVersion(versioncode, versionName);
        if (versionIndex < 0) {
            return false;
        }
        MM_Classes.init(versionIndex, lpparam);
        MM_Fields.init(versionIndex);
        MM_Res.init(versionIndex);
        return true;
    }


    public static int isSupportedVersion(int versionCode, String versionName) {
        if (versionName.contains("6.5.8")) {
            sVersionIdx = 0;
            return 0;
        }
        if (versionName.contains("6.5.10") && versionCode == 1080) {
            sVersionIdx = 1;
            return 1;
        }
        if (versionName.contains("6.5.13") && versionCode == 1100) {
            sVersionIdx = 2;
            return 2;
        }
        return -1;
    }
}
