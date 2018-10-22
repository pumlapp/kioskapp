package com.zippy.zippykiosk;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shamanland.fonticon.FontIconTypefaceHolder;
import com.squareup.leakcanary.LeakCanary;
import com.zippy.zippykiosk.rest.Business;
import com.zippy.zippykiosk.rest.ZippyApi;

import io.fabric.sdk.android.Fabric;

/**
 * Created by KB on 7/03/15.
 * Copyright 2015 Zippy.com.au.
 */
public class KioskApp extends Application {
    private static PowerManager.WakeLock mWakeLock=null;
    private static ScreenOffReceiver mScreenOffReceiver = null;
    private static Context mContext;
    public static GoogleAnalytics analytics;
    public static Tracker tracker;



    @Override
    public void onCreate() {
        super.onCreate();
        KioskApp.mContext = getApplicationContext();

        if(!BuildConfig.DEBUG) {
            Log.disable();
        }

        LeakCanary.install(this);

        // Setup GoogleAnalytics
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(BuildConfig.DEBUG ? 300 : 1800); // Debug,5min Release,30mins

        //noinspection PointlessBooleanExpression
        analytics.setDryRun(!Constants.ENABLE_GOOGLE_ANALYTICS);

        tracker = analytics.newTracker(Constants.GOOGLE_ANALYTICS_TRACKING_ID); // (BuildConfig.DEBUG ? "UA-9384256-10": "UA-9384256-9")
        tracker.enableExceptionReporting(false);
        tracker.enableAutoActivityTracking(true);

        // Setup Crashlytics

        // http://blog.emaillenin.com/2015/01/ignoredisable-crashlytics-reports-during-app-development.html
        @SuppressWarnings("PointlessBooleanExpression")
        CrashlyticsCore crashlytics = new CrashlyticsCore.Builder().disabled(!Constants.ENABLE_CRASHLYTICS).build();

        // http://support.crashlytics.com/knowledgebase/articles/390061-why-don-t-i-see-answers-data-for-my-android-app
        final Fabric fabric = new Fabric.Builder(this)
                .kits(crashlytics)
                .debuggable(BuildConfig.DEBUG)
                .build();
        Fabric.with(fabric);

        // Seems that for Crashlytics we need to set the user identifier immediately after initializing Fabric,
        // otherwise it doesn't get reported with the crash.
        setAnalyticsUserIdentifier();
/*
        if (Constants.ENABLE_STETHO) {
            Stetho.initialize(
                    Stetho.newInitializerBuilder(this)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                            .build());
        }
*/
        // font-awesome icons
        FontIconTypefaceHolder.init(getAssets(), "icons.ttf");

        
        ZippyApi.initialize(this);
        if (AppPrefs.getSharedPreferences(this).getBoolean(AppPrefs.PREF_KIOSK_MODE,false)) {
            // If Kiosk mode is enable set bootstart flag to indicate that the app is starting
            // from re-boot or power off/on of the device
            AppPrefs.getSharedPreferences(this).edit().putBoolean("bootstart", true).commit();
        }

    }

    public static Context getAppContext() {
        return KioskApp.mContext;
    }

    public void registerKioskModeScreenOffReceiver() {
        // register screen off receiver
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        KioskApp.mScreenOffReceiver = new ScreenOffReceiver();
        registerReceiver(KioskApp.mScreenOffReceiver, filter);
    }

    public void unregisterKioskModeScreenOffReceiver() {
        if(KioskApp.mScreenOffReceiver!=null) {
            unregisterReceiver(KioskApp.mScreenOffReceiver);
            KioskApp.mScreenOffReceiver = null;
        }
    }

    public PowerManager.WakeLock getWakeLock() {
        if(KioskApp.mWakeLock == null) {
            // lazy loading: first call, create wakeLock via PowerManager.
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            KioskApp. mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "wakeup");
        }
        return KioskApp.mWakeLock;
    }

    public static void setAnalyticsUserIdentifier() {

        Business business = Business.load(getAppContext());
        if(business!=null) {
            // Set Identifier for Google Analytics
            //tracker.setClientId(String.valueOf(business.id));

            // You only need to set User ID on a tracker once. By setting it on the tracker, the ID will be sent with all subsequent hits.
            tracker.set("&uid", String.valueOf(business.id));

            // This should set the BusinessID custom dimension for all future hits. This allow us to track analytics for each business
            // https://support.google.com/analytics/answer/2709829
            tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, String.valueOf(business.id)).build());

            // In SDK V3 custom dimension were set like this
            //tracker.set(Fields.customDimension(1), String.valueOf(business.id));


            // Set Identifier for Crashlytics
            CrashlyticsCore.getInstance().setUserName("(" + String.valueOf(business.id) + ") " + (business.name !=null ? business.name : ""));
        }
        String devID = Utils.getAndroidDeviceID(KioskApp.getAppContext());
        CrashlyticsCore.getInstance().setUserIdentifier(devID);
    }
}
