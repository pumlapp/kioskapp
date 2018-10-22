package com.zippy.zippykiosk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Created by KB on 8/03/15.
 * Copyright 2015 Zippy.com.au.
 */
public class ScreenOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
            // is Kiosk Mode active?
            if(AppPrefs.getSharedPreferences(context).getBoolean(AppPrefs.PREF_KIOSK_MODE,false)) {
                wakeUpDevice(context);
            }
        }
    }

    private void wakeUpDevice(Context context) {
        PowerManager.WakeLock wakeLock = ((KioskApp)context.getApplicationContext()).getWakeLock(); // get WakeLock reference
        if (wakeLock.isHeld()) {
            wakeLock.release(); // release old wake lock
        }
        wakeLock.acquire(); // create a new wake lock...
        wakeLock.release(); // ... and release again
    }
}