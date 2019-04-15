package com.zippy.zippykiosk;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by KB on 2/04/15.
 * Copyright 2015 Zippy.com.au.
 */
public abstract class BaseActivity extends Activity {
    private CustomViewGroup mStatusHideView; //
    private CustomViewGroup mNavHideView;
    private final PasscodeChecker mPasscodeChecker = new PasscodeChecker();
    private final Handler mHandler = new Handler();
    protected boolean passwordShutdown = false;
    protected boolean enablePasswordShutdown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | // // Show our window instead of regular lock screen
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final View decorView = getWindow().getDecorView();
            setFullScreenNoNavBar(decorView);

            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        setFullScreenNoNavBar(getWindow().getDecorView());
                    }
                }
            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
        } else {
            init();
        }
    }

    private void init() {
        // Set view over statusbar and navigationbar that intercepts onTouch events so use can't swipe for notification
        // or tap back, Home or recent soft keys. However, this doesn't work for navbar when keypad is up.
        WindowManager manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
        final WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            localLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            localLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        localLayoutParams.gravity = Gravity.TOP;


        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | // this is to enable the notification to receive touch events
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN; // Draws over status bar


        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        localLayoutParams.height = getStatusBarHeight();
        localLayoutParams.format = PixelFormat.TRANSPARENT;

        mStatusHideView = new CustomViewGroup(this);
        manager.addView(mStatusHideView, localLayoutParams);

        localLayoutParams.height = getNavBarHeight();
        localLayoutParams.gravity = Gravity.BOTTOM;
        mNavHideView = new CustomViewGroup(this);
        mNavHideView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d("mNavHideView, onTouch ACTION_DOWN");
                    if(enablePasswordShutdown) {
                        // Look for hidden unlock code
                        if (mPasscodeChecker.addHit()) {
                            AppPrefs.getSharedPreferences(BaseActivity.this).edit()
                                    .putBoolean(AppPrefs.PREF_KIOSK_MODE, false)
                                    .commit();

                            passwordShutdown = true;
                            Intent intent = new Intent(BaseActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }
                    }
                }
                return true; // Return true to consume the event, otherwise false
            }
        });
        manager.addView(mNavHideView, localLayoutParams);

        mChecker.run();

        ((KioskApp) getApplicationContext()).registerKioskModeScreenOffReceiver();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            init();

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // NOTE! removeCallbacks(null) doesn't cancel runnables, but removeCallbacksAndMessages(null) or removeCallbacks(mChecker) does.
        mHandler.removeCallbacksAndMessages(null);
        ((KioskApp)getApplicationContext()).unregisterKioskModeScreenOffReceiver();
        WindowManager manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
        if(mStatusHideView!=null) manager.removeView(mStatusHideView);
        if(mNavHideView!=null) manager.removeView(mNavHideView);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Set APP_UPDATE_INSTALLING to false if activity resumed as user may have canceled the install
        // and also sets this flag after install.
        AppPrefs.getSharedPreferences(this).edit().putBoolean(AppPrefs.APP_UPDATE_INSTALLING, false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // For long press home button( recent app activity or google now) or recent app button
        // http://stackoverflow.com/questions/14233304/develop-app-run-in-kiosk-mode-in-android
        // Requires <uses-permission android:name="android.permission.REORDER_TASKS" />
        SharedPreferences prefs = AppPrefs.getSharedPreferences(this);
        if(prefs.getBoolean(AppPrefs.PREF_KIOSK_MODE,false)) {
            if(!prefs.getBoolean(AppPrefs.APP_UPDATE_INSTALLING, false)) {
                ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.moveTaskToFront(getTaskId(), 0);
                Log.i("onPause. moveTaskToFront KioskActivity");
            }else {
                Log.i("onPause. moveTaskToFront disabled, APP_UPDATE_INSTALLING flag set");
            }

        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        // Deactivate the volume buttons
        final List blockedKeys = new ArrayList<>(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP));

        Log.i("Key " + event.getKeyCode());
        if (blockedKeys.contains(event.getKeyCode())) {
            // Test option to hide/show signup button
            //if(event.getKeyCode()==KeyEvent.KEYCODE_VOLUME_DOWN) {
            //mWebView.loadUrl("(function() { document.getElementById('signupHere').style.display = 'none' })()");
            //    mWebView.loadUrl("javascript:deviceConnected(true);");
            //}else {
            //    mWebView.loadUrl("javascript:deviceConnected(false);");
            //}
            // Test to clear the cache
            //mWebView.clearCache(true);

            //if(!Utils.isCharging(this)) {

            //    AppPrefs.getSharedPreferences(KioskActivity.this).edit()
            //            .putBoolean(AppPrefs.PREF_KIOSK_MODE,false)
            //            .commit();
            //   startActivity(new Intent(KioskActivity.this,MainActivity.class));
            //    finish();
            //}
            return true;
        }else if(event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            // We get this if if long press on the power button
            // Allow long press to bring up the dialog to power off the device

            //Intent i = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
            //i.putExtra("android.intent.extra.KEY_CONFIRM", true);
            //startActivity(i);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }



    private final Runnable mChecker = new Runnable() {
        @Override
        public void run() {
            setFullScreenNoNavBar(getWindow().getDecorView());
            mHandler.postDelayed(mChecker, 1000);
        }
    };


    @Override
    public void onBackPressed() {
        //Disable the back button
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // If device is plugged into a charger, we want to close any system dialogs that may pop up.
        // However if unplugged we want to allow user to long press and power off the device
        if(!hasFocus && Utils.isCharging(this)) {
            // Close every kind of system dialog.
            // This immediately closes any system dialog such as;
            // Long power press popup to shutdown/restart
            // Swipe down from top navigation drawer or settings
            // And any other system dialogs that could pop up.
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }

    private int getNavBarHeight() {
        Resources resources = getResources();

        int result = 0;
        int resourceId = resources.getIdentifier("navigation_bar_height_landscape","dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        if(result==0) {
            result = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, resources.getDisplayMetrics());
        }
        return result;
    }

    private int getStatusBarHeight() {
        Resources resources = getResources();
        int result = 0;
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }


    private void setFullScreenNoNavBar(View v) {
         int newUiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; // hide nav bar

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
        }
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        v.setSystemUiVisibility(newUiOptions);
    }

/*
    BroadcastReceiver mBatteryLevelReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            if(Utils.isCharging(intent) != mCharging) {
                // Charging state has changed
                mCharging = !mCharging;

                if(mCharging) {
                    // Block power off button
                    ((KioskApp) getApplicationContext()).registerKioskModeScreenOffReceiver();
                }else {
                    // Enable power off button
                    ((KioskApp) getApplicationContext()).unregisterKioskModeScreenOffReceiver();
                }

            }
        }
    };
*/

}
