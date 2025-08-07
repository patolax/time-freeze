package com.time.freezer.base.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private static final String APP_SETTINGS = "com.time.freeze.settings";
    public static final String SCAN_DIRECTION = "SCAN_DIRECTION";
    public static final String SCAN_SHAPE = "SCAN_SHAPE";
    public static final String SCAN_COLOR = "SCAN_COLOR";
    public static final String SCAN_SPEED = "SCAN_SPEED";
    public static final String SCAN_SAVE_IMAGE = "SAVE_IMAGE";
    public static final String SCAN_FILTER_NAME = "SCAN_FILTER_NAME";
    public static final String SCAN_FILTER_IMAGE = "SCAN_FILTER_IMAGE";
    public static final String CAMERA_ID = "CAMERA_ID";
    public static final String SCAN_FILTER_FULLIMAGE = "SCAN_FILTER_FULLIMAGE";
    public static final String SHOW_HELP_MESSAGE = "HELP_MESSAGE";

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);
    }

    public static String getString(Context context, String key, String defaultValue) {
        return getSharedPreferences(context).getString(key , null);
    }

    public static void setString(Context context, String key, String newValue) {
        final SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(key , newValue);
        editor.apply();
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return getSharedPreferences(context).getInt(key, defaultValue);
    }

    public static void setInt(Context context, String key, int newValue) {
        final SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putInt(key , newValue);
        editor.apply();
    }
}
