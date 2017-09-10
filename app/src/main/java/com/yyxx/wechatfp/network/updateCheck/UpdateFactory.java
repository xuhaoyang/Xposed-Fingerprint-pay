package com.yyxx.wechatfp.network.updateCheck;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.Toast;

import com.yyxx.wechatfp.network.inf.UpdateResultListener;
import com.yyxx.wechatfp.network.updateCheck.github.GithubUpdateChecker;
import com.yyxx.wechatfp.util.Config;
import com.yyxx.wechatfp.util.UrlUtil;
import com.yyxx.wechatfp.util.log.L;

/**
 * Created by Jason on 2017/9/10.
 */

public class UpdateFactory {

    public static void doUpdateCheck(final Context context) {
        doUpdateCheck(context, true, false);
    }

    public static void doUpdateCheck(final Context context, final boolean quite, final boolean dontSkip) {
        if (!quite) {
            Toast.makeText(context, "正在檢查更新", Toast.LENGTH_LONG).show();

        }
        new GithubUpdateChecker(new UpdateResultListener() {
            @Override
            public void onNoUpdate() {
                if (!quite) {
                    Toast.makeText(context, "暫無更新", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNetErr() {
                if (!quite) {
                    Toast.makeText(context, "網絡錯誤, 檢查更新失敗", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onHasUpdate(final String version, String content, final String pageUrl, String downloadUrl) {
                if (!dontSkip) {
                    if (isSkipVersion(context, version)) {
                        L.d("已跳過版本: " + version);
                        return;
                    }
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle("發現新版本 " + version);
                builder.setMessage(content);
                builder.setNeutralButton("跳過這個版本", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new Config(context).setSkipVersion(version);
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                builder.setPositiveButton("前往更新頁", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        UrlUtil.openUrl(context, pageUrl);
                    }
                });

                builder.show();
            }
        }).doUpdateCheck();
    }

    private static boolean isSkipVersion(Context context, String targetVersion) {
        Config config = new Config(context);
        String skipVersion = config.getSkipVersion();
        if (TextUtils.isEmpty(skipVersion)) {
            return false;
        }
        if (String.valueOf(targetVersion).equals(skipVersion)) {
            return true;
        }
        return false;
    }
}
