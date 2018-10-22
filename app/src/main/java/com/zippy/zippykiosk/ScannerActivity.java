package com.zippy.zippykiosk;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Surface;
import android.view.View;

import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.HitBuilders;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import com.zippy.zippykiosk.rest.Business;
import com.zippy.zippykiosk.rest.CheckinResult;
import com.zippy.zippykiosk.rest.RetrofitUtils;
import com.zippy.zippykiosk.rest.ZippyApi;

import org.parceler.Parcels;

import java.util.EnumSet;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by KB on 1/04/15.
 * Copyright 2015 Zippy.com.au.
 */
public class ScannerActivity extends BaseActivity {
    private final Handler mHandler = new Handler();
    private CompoundBarcodeView mBarcodeView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        int orientation = getScreenOrientation();
        if(orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            setContentView(R.layout.activity_scanner_cam_right);
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_scanner_cam_left);
        }


        mBarcodeView = (CompoundBarcodeView) findViewById(R.id.barcode_scanner);

        CameraSettings settings = new CameraSettings();
        settings.setRequestedCameraId(1);
        settings.setContinuousFocusEnabled(true);
        findViewById(R.id.zxing_viewfinder_view).setVisibility(View.GONE);
        findViewById(R.id.zxing_status_view).setVisibility(View.GONE);

        mBarcodeView.getBarcodeView().setCameraSettings(settings);
        mBarcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(EnumSet.of(BarcodeFormat.QR_CODE), null, null, false));
        mBarcodeView.decodeSingle(callback);

        startScanHereAnimation();


        Button cancelButton = (Button)findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void startScanHereAnimation() {
        int orientation = getScreenOrientation();
        View scanHereView = findViewById(R.id.scanHere);
        ObjectAnimator anim1;
        ObjectAnimator anim2;
        if(orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            anim1 = ObjectAnimator.ofFloat(scanHereView, "translationX", -100f).setDuration(500);
            anim2 = ObjectAnimator.ofFloat(scanHereView, "translationX", 0f).setDuration(1500);
        }else {
            anim1 = ObjectAnimator.ofFloat(scanHereView, "x", 100f).setDuration(500);
            anim2 = ObjectAnimator.ofFloat(scanHereView, "x", 0f).setDuration(1500);
        }
        anim2.setInterpolator(AnimationUtils.loadInterpolator(this,android.R.interpolator.bounce));
        anim1.setStartDelay(2000);
        AnimatorSet animScanHere = new AnimatorSet();
        animScanHere.playSequentially(anim1, anim2);
        animScanHere.start();
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {

            handleResult(result.getText());
            //if (result.getText() != null) {
            //    mBarcodeView.setStatusText(result.getText());
            //}
            //Added preview of scanned barcode
            //ImageView imageView = (ImageView) findViewById(R.id.barcodePreview);
            //imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    private int getScreenOrientation() {
        Display display = getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();

        Point size = new Point();
        display.getSize(size);

        int screenOrientation;

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            // if rotation is 0 or 180 and width is greater than height, we have
            // a tablet
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_0) {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a phone
                if (rotation == Surface.ROTATION_0) {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }
        } else {
            // if rotation is 90 or 270 and width is greater than height, we have a phone
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_90) {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a tablet
                if (rotation == Surface.ROTATION_90) {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                } else {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
            }
        }
        return(screenOrientation);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
        mBarcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
        mBarcodeView.pause();
    }


    @Override
    protected void onDestroy() {
        mBarcodeView.getBarcodeView().stopDecoding();
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private final Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    private void handleResult(final String result) {
        mBarcodeView.pause();
        mHandler.removeCallbacksAndMessages(null);

        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.beep);
            mp.start();

        } catch (Exception e) {
            Log.e(e);
        }

        final MaterialDialog dialogProgress = new MaterialDialog.Builder(this)
                .title("") // Set title so we can hide/show if error
                .customView(R.layout.progress_layout, false)
                .build();

        dialogProgress.getTitleView().setVisibility(View.GONE);

        // Set custom size of dialog
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        WindowManager.LayoutParams lp = dialogProgress.getWindow().getAttributes();
        int minPixelWidth = Utils.convertDIPToPixels(this, 600);
        if (minPixelWidth < size.x) {
            lp.width = minPixelWidth;
            dialogProgress.getWindow().setAttributes(lp);
        }

        View v = dialogProgress.getCustomView();
        assert v != null;

        ((TextView)v.findViewById(R.id.content)).setText("Processing. Please wait...");

        //Here's the magic.. Set the dialog to not focusable (makes navigation ignore us adding the window)
        dialogProgress.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialogProgress.show(); //Show the dialog!
        dialogProgress.getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility());
        dialogProgress.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        final Business business = Business.load(getApplication());

        // Check if zippy supported QR code, zippy url, zippy QR code, Rewardle, etc
        final ZippyQRCode qrCode = ZippyQRCode.createFromZippyQRCodeUrl(result);
        if(qrCode==null) {
            KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("Checkin Failure. Invalid QR code " + result)
                    .setFatal(false)
                    .build());
            CrashlyticsCore.getInstance().logException(new Exception("Checkin Failure. Invalid QR code. " + result));
            showError(dialogProgress, "Error", "Invalid QR code");
        }else if(business==null) {
            showError(dialogProgress, "Error", "Unexpected error. Business details missing");
            CrashlyticsCore.getInstance().logException(new Exception("Checkin Failure. Business details missing"));
        }else {
            dialogProgress.setCancelable(false);

            ZippyApi.getInstance().checkin(business.config.valueConversionRate!=null, business.uri, qrCode.slug, new Callback<CheckinResult>() {
                @Override
                public void success(CheckinResult checkinResult, Response response) {
                        if(!dialogProgress.isCancelled())

                        {
                            dialogProgress.dismiss();
                            Intent intent = new Intent();
                            intent.putExtra("CheckinResult", Parcels.wrap(checkinResult));
                            intent.setClass(ScannerActivity.this, RewardsActivity.class);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }

                        else

                        {
                            // Shouldn't get here since we set dialog cancelable to false
                            mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                        }

                }

                    @Override
                public void failure(RetrofitError error) {
                    // TODO: Comms error. Should be have a retry here?
                    KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                            .setDescription(RetrofitUtils.getErrorDescription("Checkin Failure.", error))
                            .setFatal(false)
                            .build());
                    CrashlyticsCore.getInstance().logException(new Exception("Checkin Failure.", error));

                    showError(dialogProgress, "Error", RetrofitUtils.getUserErrorDescription(error));
                }
            });
        }
    }
    private void showError(final MaterialDialog dialogProgress, String title, String message) {
        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);

        dialogProgress.setCancelable(true);
        dialogProgress.getTitleView().setVisibility(View.VISIBLE);
        dialogProgress.setTitle(title);

        View customView = dialogProgress.getCustomView();
        assert customView != null;
        ((TextView)customView.findViewById(R.id.content)).setText(message);
        customView.findViewById(R.id.progressBar).setVisibility(View.GONE);
        dialogProgress.setActionButton(DialogAction.POSITIVE, android.R.string.ok);

        dialogProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBarcodeView.resume();
                mBarcodeView.decodeSingle(callback);
                startScanHereAnimation();
                //finish();
                //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

    }

}
