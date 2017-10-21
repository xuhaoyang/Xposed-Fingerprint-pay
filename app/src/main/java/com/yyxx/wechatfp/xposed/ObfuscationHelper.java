package com.yyxx.wechatfp.xposed;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ObfuscationHelper {

    public static class MM_Classes {

        public static Class<?> PayUI, FetchUI, PayView, WalletBaseUI, PreferenceAdapter;

        private static void init(int idx, LoadPackageParam lpparam) throws Throwable {
            //classes3
            PayUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.pay.ui." + new String[]{
                    "WalletPayUI", //6.5.8
                    "WalletPayUI", //6.5.10-1080
                    "WalletPayUI", //6.5.13-1081
                    "WalletPayUI", //6.5.13-1100
                    "WalletPayUI", //6.5.16-1101
                    "WalletPayUI", //6.5.16-1120
            }[idx], lpparam.classLoader);
            //classes2
            PayView = XposedHelpers.findClass("com.tencent.mm.plugin.wallet_core.ui." + new String[]{
                    "l",  //6.5.8
                    "l",  //6.5.10-1080
                    "l",  //6.5.13-1081
                    "l",  //6.5.13-1100
                    "l",  //6.5.16-1101
                    "l",  //6.5.16-1120
            }[idx], lpparam.classLoader);
            //classes2
            FetchUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.balance.ui." + new String[]{
                    "WalletBalanceFetchPwdInputUI",  //6.5.8
                    "WalletBalanceFetchPwdInputUI",  //6.5.10-1080
                    "WalletBalanceFetchPwdInputUI",  //6.5.13-1081
                    "WalletBalanceFetchPwdInputUI",  //6.5.13-1100
                    "WalletBalanceFetchPwdInputUI",  //6.5.16-1101
                    "WalletBalanceFetchPwdInputUI",  //6.5.16-1120
            }[idx], lpparam.classLoader);
            //classes2
            WalletBaseUI = XposedHelpers.findClass("com.tencent.mm.wallet_core.ui." + new String[]{
                    "WalletBaseUI",  //6.5.8
                    "WalletBaseUI",  //6.5.10-1080
                    "WalletBaseUI",  //6.5.13-1081
                    "WalletBaseUI",  //6.5.13-1100
                    "WalletBaseUI",  //6.5.16-1101
                    "WalletBaseUI",  //6.5.16-1120
            }[idx], lpparam.classLoader);
            //classes
            PreferenceAdapter = XposedHelpers.findClass("com.tencent.mm.ui.base.preference." + new String[]{
                    "h",  //6.5.8
                    "h",  //6.5.10-1080
                    "h",  //6.5.13-1081
                    "h",  //6.5.13-1100
                    "h",  //6.5.16-1101
                    "h",  //6.5.16-1120
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
            //classes2 PayView(com.tencent.mm.plugin.wallet_core.ui.l) => public EditHintPasswdView rWo;
            PaypwdView = new String[]{
                    "qVO",  //6.5.8
                    "ryk",  //6.5.10-1080
                    "rNe",  //6.5.13-1081
                    "rLB",  //6.5.13-1100
                    "rWi",  //6.5.16-1101
                    "rWo",  //6.5.16-1120
            }[idx];
            //EditHintPasswdView =>  private TenpaySecureEditText xhU;
            PaypwdEditText = new String[]{
                    "vyO",  //6.5.8
                    "wjm",  //6.5.10-1080
                    "wFP",  //6.5.13-1081
                    "wDJ",  //6.5.13-1100
                    "xhN",  //6.5.16-1101
                    "xhU",  //6.5.16-1120
            }[idx];
            //classes2 PayView(com.tencent.mm.plugin.wallet_core.ui.l) => protected MyKeyboardWindow mKeyboard;\n protected View nzg;
            PayInputView = new String[]{
                    "mOL",  //6.5.8
                    "nnG",  //6.5.10-1080
                    "npM",  //6.5.13-1081
                    "nol",  //6.5.13-1100
                    "nyY",  //6.5.16-1101
                    "nzg",  //6.5.16-1120
            }[idx];
            //classes2 PayView(com.tencent.mm.plugin.wallet_core.ui.l) => public TextView rWj;\n public TextView rWk;\n public FavourLayout rWl;
            PayTitle = new String[]{
                    "qVK",  //6.5.8
                    "ryg",  //6.5.10-1080
                    "rMZ",  //6.5.13-1081
                    "rLw",  //6.5.13-1100
                    "rWd",  //6.5.16-1101
                    "rWj",  //6.5.16-1120
            }[idx];
            //classes2 PayView(com.tencent.mm.plugin.wallet_core.ui.l) => public Bankcard rWC; \n public TextView rWD;
            Passwd_Text = new String[]{
                    "qVK",  //6.5.8
                    "ryz",  //6.5.10-1080
                    "rNt",  //6.5.13-1081
                    "rLQ",  //6.5.13-1100
                    "rWx",  //6.5.16-1101
                    "rWD",  //6.5.16-1120
            }[idx];
            //classes SettingPreferenceAdapter(com.tencent.mm.ui.base.preference.h) => private final HashMap<String, Preference> vOF;
            PreferenceAdapter_vpQ = new String[]{
                    "uoo",  //6.5.8
                    "uYA",  //6.5.10-1080
                    "vrF",  //6.5.13-1081
                    "vpQ",  //6.5.13-1100
                    "vOy",  //6.5.16-1101
                    "vOF",  //6.5.16-1120
            }[idx];
            //classes SettingPreferenceAdapter(com.tencent.mm.ui.base.preference.h) => private final LinkedList<String> vOE;
            PreferenceAdapter_vpP = new String[]{
                    "uon",  //6.5.8
                    "uYz",  //6.5.10-1080
                    "vrE",  //6.5.13-1081
                    "vpP",  //6.5.13-1100
                    "vOx",  //6.5.16-1120
                    "vOE",  //6.5.16-1120
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
        return true;
    }

    public static int isSupportedVersion(int versionCode, String versionName) {
        if (versionName.contains("6.5.8")) {
            return 0;
        }
        if (versionName.contains("6.5.10") && versionCode == 1080) {
            return 1;
        }
        if (versionName.contains("6.5.13") && versionCode == 1081) {//Play版微信
            return 2;
        }
        if (versionName.contains("6.5.13") && versionCode == 1100) {
            return 3;
        }
        if (versionName.contains("6.5.16") && versionCode == 1101) {//Play
            return 4;
        }
        if (versionName.contains("6.5.16") && versionCode == 1120) {
            return 5;
        }
        return -1;
    }
}
