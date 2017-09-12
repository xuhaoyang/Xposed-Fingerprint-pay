package com.yyxx.wechatfp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.yyxx.wechatfp.BuildConfig;

import java.util.WeakHashMap;

/**
 * Created by Jason on 2017/9/9.
 */

public class Config {


    private static WeakHashMap<Context, ObjectCache> sConfigCache = new WeakHashMap<>();

    public static Config from(Context context) {
        return new Config(context);
    }

    private ObjectCache mCache;

    private Config(Context context) {
        if (sConfigCache.containsKey(context)) {
            mCache = sConfigCache.get(context);
        }
        if (mCache == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".settings", Context.MODE_PRIVATE);
            String deviceId = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
            int passwordEncKey = deviceId.hashCode();
            mCache = new ObjectCache(sharedPreferences, passwordEncKey);
            sConfigCache.put(context, mCache);
        }
    }

    public boolean isOn() {
        return mCache.sharedPreferences.getBoolean("switch_on", false);
    }

    public void setOn(boolean on) {
        mCache.sharedPreferences.edit().putBoolean("switch_on", on).apply();
    }

    @Nullable
    public String getPassword() {
        String enc = mCache.sharedPreferences.getString("password", null);
        if (TextUtils.isEmpty(enc)) {
            return null;
        }
        return AESUtil.decrypt(enc, String.valueOf(mCache.passwordEncKey));
    }

    public void setPassword(String password) {
        String enc = AESUtil.encrypt(password, String.valueOf(mCache.passwordEncKey));
        mCache.sharedPreferences.edit().putString("password", enc).apply();
    }

    public void setSkipVersion(String version) {
        mCache.sharedPreferences.edit().putString("skip_version", version).apply();
    }

    @Nullable
    public String getSkipVersion() {
        return mCache.sharedPreferences.getString("skip_version", null);
    }

    private class ObjectCache {
        SharedPreferences sharedPreferences;
        int passwordEncKey;

        public ObjectCache(SharedPreferences sharedPreferences, int passwordEncKey) {
            this.sharedPreferences = sharedPreferences;
            this.passwordEncKey = passwordEncKey;
        }
    }
}
