/*
 * created by wulin on 18-8-15 下午3:04.
 * Copyright (c) 2018 Blockin. All Rights Reserved.
 */

package com.vcashorg.vcashwallet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SPUtil {

    public static final String FP = "fingerprint";
    public static final String GUID_PRE = "guid_pre";
    public static final String ACCESS_HASH = "accessHash";
    public static final String USER_ID = "user_id";
    public static final String TIME_OUT = "time_out";
    public static final String FIRST_CREATE_WALLET = "first_create_wallet";

    private static Context context = null;
    private static SPUtil instance = null;

    private SPUtil() {
    }

    public static SPUtil getInstance(Context ctx) {
        context = ctx;
        if (instance == null) {
            instance = new SPUtil();
        }
        return instance;
    }

    public String getValue(String name, String defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(name, defaultValue);
    }

    public boolean setValue(String name, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(name, value);
        return editor.commit();
    }

    public int getValue(String name, int defaultValue){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(name, defaultValue);
    }

    public boolean setValue(String name, int value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(name, value);
        return editor.commit();
    }

    public boolean setValue(String name, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(name, value);
        return editor.commit();
    }

    public boolean getValue(String name, boolean defaultValue){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(name, defaultValue);
    }
}
