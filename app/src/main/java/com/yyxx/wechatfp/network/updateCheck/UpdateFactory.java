package com.yyxx.wechatfp.network.updateCheck;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.yyxx.wechatfp.Lang;
import com.yyxx.wechatfp.network.inf.UpdateResultListener;
import com.yyxx.wechatfp.network.updateCheck.github.GithubUpdateChecker;
import com.yyxx.wechatfp.util.Config;
import com.yyxx.wechatfp.util.UrlUtil;
import com.yyxx.wechatfp.util.log.L;
import com.yyxx.wechatfp.view.UpdateInfoView;

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
        try {
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
                    UpdateInfoView updateInfoView = new UpdateInfoView(context);
                    updateInfoView.setTitle(Lang.getString(Lang.FOUND_NEW_VERSION) + version);
                    updateInfoView.setContent(content);
                    updateInfoView.withOnNeutralButtonClickListener((dialogInterface, i) -> {
                        Config.from(context).setSkipVersion(version);
                        dialogInterface.dismiss();
                    });
                    updateInfoView.withOnPositiveButtonClickListener((dialogInterface, i) -> UrlUtil.openUrl(context, pageUrl));
                    updateInfoView.showInDialog();
                }
            }).doUpdateCheck();
        } catch (Exception | Error e) {
            //for OPPO R11 Plus 6.0 NoSuchFieldError: No instance field mResultListener
            L.e(e);
        }
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
