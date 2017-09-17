package com.yyxx.wechatfp.network.updateCheck;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.yyxx.wechatfp.Lang;
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
            Toast.makeText(context, Lang.getString(Lang.TOAST_CHECKING_UPDATE), Toast.LENGTH_LONG).show();
        }
        new GithubUpdateChecker(new UpdateResultListener() {
            @Override
            public void onNoUpdate() {
                if (!quite) {
                    Toast.makeText(context, Lang.getString(Lang.TOAST_NO_UPDATE), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNetErr() {
                if (!quite) {
                    Toast.makeText(context, Lang.getString(Lang.TOAST_CHECK_UPDATE_FAIL_NET_ERR), Toast.LENGTH_LONG).show();
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
                AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(Lang.getString(Lang.FOUND_NEW_VERSION) + version);
                builder.setMessage(content);
                builder.setNeutralButton(Lang.getString(Lang.SKIP_THIS_VERSION), (dialogInterface, i) -> Config.from(context).setSkipVersion(version));
                builder.setNegativeButton(Lang.getString(Lang.CANCEL), (dialogInterface, i) -> {

                });
                builder.setPositiveButton(Lang.getString(Lang.GOTO_UPDATE_PAGE), (dialogInterface, i) -> UrlUtil.openUrl(context, pageUrl));

                builder.show();
            }
        }).doUpdateCheck();
    }

    private static boolean isSkipVersion(Context context, String targetVersion) {
        Config config = Config.from(context);
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
