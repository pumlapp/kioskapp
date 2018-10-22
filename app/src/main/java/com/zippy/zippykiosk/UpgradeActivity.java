package com.zippy.zippykiosk;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Window;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.analytics.HitBuilders;

import java.io.File;

/**
 * Created by kimb on 28/06/2015.
 */
public class UpgradeActivity extends Activity {
    MaterialDialog mDialog=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setLayout(0, 0);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        String versionName = AppPrefs.getSharedPreferences(this).getString(AppPrefs.APP_UPDATE_VERSION_NAME, "");
        mDialog= new MaterialDialog.Builder(UpgradeActivity.this)
            .title("Update Zippy")
            .content("Please select OK and follow the prompts to Install the new version of Zippy v" + versionName)
            .cancelable(false)
            .negativeText(android.R.string.cancel)
            .positiveText(android.R.string.ok)
            .onAny(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    if (which == DialogAction.POSITIVE) {
                        String path = AppPrefs.getSharedPreferences(UpgradeActivity.this).getString(AppPrefs.APP_UPDATE_FILE, "");
                        if (!TextUtils.isEmpty(path)) {
                            final File apkFile = new File(path);
                            if (apkFile.exists()) {
                                Intent newAPKIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                                newAPKIntent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                                newAPKIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(newAPKIntent);
                                finish();
                            }
                        }
                    } else {
                        KioskApp.tracker.send(new HitBuilders.EventBuilder()
                                .setCategory("App Kiosk Mode")
                                .setAction("Started")
                                .setLabel("Auto Started")
                                .build());

                        Intent intent = new Intent();
                        intent.setClass(UpgradeActivity.this, KioskActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            })
            .build();

        mDialog.show();
    }

    @Override
    protected void onDestroy() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        super.onDestroy();
    }

}
