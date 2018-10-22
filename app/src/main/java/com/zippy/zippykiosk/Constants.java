package com.zippy.zippykiosk;

/**
 * Created by KB on 26/05/15.
 * Copyright 2015 Zippy.com.au.
 */
public class Constants {
    public static final int INACTIVITY_TIMEOUT = 60*1000;
    //public static final boolean ENABLE_STETHO = false; //BuildConfig.DEBUG
    public static final boolean ENABLE_CRASHLYTICS = true;
    public static final boolean ENABLE_GOOGLE_ANALYTICS = !BuildConfig.DEBUG;
    public static final String GOOGLE_ANALYTICS_TRACKING_ID = BuildConfig.DEBUG ? "UA-9384256-10" : "UA-9384256-9"; // Production: "UA-9384256-9", Dev: "UA-9384256-10"
}
