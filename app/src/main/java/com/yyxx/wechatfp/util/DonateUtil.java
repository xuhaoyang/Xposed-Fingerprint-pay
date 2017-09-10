package com.yyxx.wechatfp.util;

import android.content.Context;
import android.content.Intent;

import com.yyxx.wechatfp.Constant;
import com.yyxx.wechatfp.util.log.L;

import java.net.URLEncoder;

/**
 * Created by Jason on 2017/9/10.
 */

public class DonateUtil {

    public static boolean openAlipayPayPage(Context context) {
        return openAlipayPayPage(context, Constant.DONATE_ID_ALIPAY);
    }

    public static boolean openAlipayPayPage(Context context, String qrcode) {
        try {
            qrcode = URLEncoder.encode(qrcode, "utf-8");
        } catch (Exception e) {
        }
        try {
            final String alipayqr = "alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + qrcode;
            openUri(context, alipayqr + "%3F_s%3Dweb-other&_t=" + System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            L.e(e);
        }
        return false;
    }

    public static void openWeChatPay(Context context) {
        try {
            Intent donateIntent = new Intent();
            donateIntent.setClassName(context, "com.tencent.mm.plugin.remittance.ui.RemittanceAdapterUI");
            donateIntent.putExtra("scene", 1);
            donateIntent.putExtra("pay_channel", 13);
            donateIntent.putExtra("receiver_name", Constant.DONATE_ID_WECHAT);
            context.startActivity(donateIntent);
        } catch (Exception e) {
            L.e(e);
        }
    }

    private static void openUri(Context context, String s) {
        try {
            Intent intent = Intent.parseUri(s, Intent.URI_INTENT_SCHEME);
            context.startActivity(intent);
        } catch (Exception e) {
            L.e(e);
        }
    }
}
