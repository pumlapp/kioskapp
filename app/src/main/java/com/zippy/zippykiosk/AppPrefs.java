package com.zippy.zippykiosk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by KB on 7/03/15.
 * Copyright 2015 Zippy.com.au.
 */
public class AppPrefs {
    public static final String PREF_KIOSK_MODE = "kiosk.mode";
    public static final String PREF_APP_VERSION = "app.version";
    public static final String APP_UPDATE_VERSION = "app.update.version";
    public static final String APP_UPDATE_VERSION_NAME = "app.update.version.name";
    public static final String APP_UPDATE_FILE = "app.update.file";
    public static final String APP_UPDATE_INSTALLING = "app.update.installing"; // temp flag used to allow app installer to run
    public static final String PREF_ACCOUNT_FORCE_EMAIL = "pref_account_force_email";
    public static final String PREF_ACCOUNT_FIELDS = "pref_account_fields";
    public static SharedPreferences getSharedPreferences(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

}
