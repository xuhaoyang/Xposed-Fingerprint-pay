package com.yyxx.wechatfp.network.updateCheck.github;

import com.google.gson.Gson;
import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.Constant;
import com.yyxx.wechatfp.network.inf.UpdateResultListener;
import com.yyxx.wechatfp.network.updateCheck.BaseUpdateChecker;
import com.yyxx.wechatfp.network.updateCheck.github.bean.GithubLatestInfo;
import com.yyxx.wechatfp.util.DateUtil;
import com.yyxx.wechatfp.util.StringUtil;
import com.yyxx.wechatfp.util.log.L;

import java.io.IOException;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Jason on 2017/9/9.
 */

public class GithubUpdateChecker extends BaseUpdateChecker {

    public static OkHttpClient sHttpClient = new OkHttpClient();

    public GithubUpdateChecker(UpdateResultListener listener) {
        super(listener);
    }

    @Override
    public void doUpdateCheck() {
        Callback callback;
        callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onNetErr();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null && response.isSuccessful()) {
                    String replay = response.body().string();
                    response.close();
                    try {
                        GithubLatestInfo info = new Gson().fromJson(replay, GithubLatestInfo.class);
                        if (info != null) {
                            if (info.isDataComplete()) {
                                if (StringUtil.isAppNewVersion(BuildConfig.VERSION_NAME, info.version)) {
                                    String content = info.content;
                                    Date date = info.date;
                                    if (date != null) {
                                        content = content + "\n\n更新日期: " + DateUtil.toString(date);
                                    }
                                    onHasUpdate(info.version, content, info.contentUrl, info.getDownloadUrl());
                                } else {
                                    onNoUpdate();
                                }
                                return;
                            }
                        }
                    } catch (Exception e) {
                        L.d(e);
                    }
                }
                onNetErr();
            }
        };

        Request request = new Request.Builder()
                .url(Constant.UPDATE_URL_GITHUB)
                .build();
        sHttpClient.newCall(request).enqueue(callback);
    }

}
