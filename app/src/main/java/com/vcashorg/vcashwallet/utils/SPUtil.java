package com.vcashorg.vcashwallet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SPUtil {

    public static final String FP = "fingerprint";
    public static final String GUID_PRE = "guid_pre";
    public static final String ACCESS_HASH = "accessHash";
    public static final String USER_ID = "user_id";
    public static final String TIME_OUT = "time_out";
    public static final String FIRST_CREATE_WALLET = "first_create_wallet";
    public static final String RECOVER_WALLET_FAILED = "recover_wallet_failed";
    public static final String TOKEN_ADDED_TYPE = "token_added";
    public static final String TOKEN_ALL = "token_all";

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

    public boolean setStringListValue(String name, Set<String> set){
        List<String> list = new ArrayList<String>(set);
        Gson gson = new Gson();
        String data = gson.toJson(list);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(name, data);
        return editor.commit();
    }

    public Set<String> getStringListValue(String name){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String data = prefs.getString(name, "");
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> list = gson.fromJson(data, listType);
        if(list != null){
            return new HashSet<>(list);
        }
        return new HashSet<>();
    }

    /**
     * 用于保存集合
     *
     * @param key key
     * @param map map数据
     * @return 保存结果
     */
    public <K, V> boolean putHashMapData(String key, Map<K, V> map) {
        boolean result;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        try {
            Gson gson = new Gson();
            String json = gson.toJson(map);
            editor.putString(key, json);
            result = true;
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        }
        editor.apply();
        return result;
    }

    /**
     * 用于保存集合
     *
     * @param key key
     * @return HashMap
     */
    public <V> HashMap<String, V> getHashMapData(String key, Class<V> clsV) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String json = prefs.getString(key, "");
        HashMap<String, V> map = new HashMap<>();
        Gson gson = new Gson();
        JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entrySet = obj.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String entryKey = entry.getKey();
            JsonObject value = (JsonObject) entry.getValue();
            map.put(entryKey, gson.fromJson(value, clsV));
        }
        return map;
    }


}
