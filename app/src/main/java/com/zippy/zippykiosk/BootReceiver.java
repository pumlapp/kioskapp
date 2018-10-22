package com.zippy.zippykiosk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * Created by KB on 7/03/15.
 * Copyright 2015 Zippy.com.au.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (AppPrefs.getSharedPreferences(context).getBoolean(AppPrefs.PREF_KIOSK_MODE,false)) {
            // If Kiosk mode is enable start KioskActivity after booting the device
            /*
            Intent kioskIntent = new Intent(context, KioskActivity.class);
            Log.d("bootstart");
            kioskIntent.putExtra("bootstart",true); // Note: passing a parameter in the intent did not seem to work.
            kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(kioskIntent);
            */
        }
    }
}

