package com.yyxx.wechatfp.util;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.yyxx.wechatfp.util.log.L;

/**
 * Created by Jason on 2017/11/1.
 */

public class Tools {

    public static void doUnSupportVersionUpload(Context context, String message) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;

            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("UnSupport: versionName:").append(versionName)
                    .append(" versionCode:").append(versionCode)
                    .append(" viewInfos:").append(message);

            L.e(stringBuffer);
        } catch (Exception e) {
            L.e(e);
        }
    }
}
