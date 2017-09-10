package com.yyxx.wechatfp.network.updateCheck;

import com.yyxx.wechatfp.network.inf.IUpdateCheck;
import com.yyxx.wechatfp.network.inf.UpdateResultListener;
import com.yyxx.wechatfp.util.Task;

/**
 * Created by Jason on 2017/9/9.
 */

public abstract class BaseUpdateChecker implements IUpdateCheck, UpdateResultListener {

    private UpdateResultListener mResultListener;

    public BaseUpdateChecker(UpdateResultListener listener) {
        mResultListener = listener;
    }

    @Override
    public void onNoUpdate() {
        Task.onMain(new Runnable() {
            @Override
            public void run() {
                UpdateResultListener listener = mResultListener;
                if (listener == null) {
                    return;
                }
                listener.onNoUpdate();
            }
        });
    }

    @Override
    public void onNetErr() {
        Task.onMain(new Runnable() {
            @Override
            public void run() {
                UpdateResultListener listener = mResultListener;
                if (listener == null) {
                    return;
                }
                listener.onNetErr();
            }
        });
    }

    @Override
    public void onHasUpdate(final String version, final String content, final String pageUrl, final String downloadUrl) {
        Task.onMain(new Runnable() {
            @Override
            public void run() {
                UpdateResultListener listener = mResultListener;
                if (listener == null) {
                    return;
                }
                listener.onHasUpdate(version, content, pageUrl, downloadUrl);
            }
        });
    }
}
