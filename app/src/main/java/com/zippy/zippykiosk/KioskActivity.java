package com.zippy.zippykiosk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.HitBuilders;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.zippy.zippykiosk.rest.Business;

import com.zippy.zippykiosk.rest.LatestAppVersionResponse;
import com.zippy.zippykiosk.rest.RetrofitUtils;
import com.zippy.zippykiosk.rest.ZippyApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit.RetrofitError;
import retrofit.client.Response;

public class KioskActivity extends BaseActivity {
    private final static int APP_CHECK_PERIOD_SEC = 6*60*60; // Period to check if new version
    private final static int APP_DOWNLOAD_FAIL_CHECK_PERIOD_SEC = 2*60*60; // Retry period if download fails
    private Button mCheckinButton;
    private ListRecyclerAdapter mRewardsRecyclerAdapter;
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
    private int mUpdateBusinessRetries=0;
    private final IntentFilter mConnectivityIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    private boolean mIsNetworkConnected;
    private long  mNetworkConnectivityStateTime;
    private long mConnectivityStateCounter;

    private AsyncTask mDownloadTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.enablePasswordShutdown = true;

        setContentView(R.layout.activity_main);

        mCheckinButton = (Button)findViewById(R.id.checkin_button);

        findViewById(R.id.buttonHelp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(KioskActivity.this, AppHelpActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            }
        });
        findViewById(R.id.card_qr_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanActivity();

            }
        });
        findViewById(R.id.card_view_logo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanActivity();

            }
        });
        mCheckinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanActivity();

            }
        });

        mIsNetworkConnected = Utils.isNetworkConnected(this);
        mNetworkConnectivityStateTime = System.currentTimeMillis();
        mConnectivityStateCounter = 0;

        final Business business = Business.load(this);
        updateBusinessLogo(business);
        updateBusinessQRCode(business);

        mRewardsRecyclerAdapter = new ListRecyclerAdapter(null);
        RecyclerView rewardsListView = (RecyclerView) findViewById(R.id.lst_rewards);
        rewardsListView.setHasFixedSize(true);
        rewardsListView.setAdapter(mRewardsRecyclerAdapter);
        rewardsListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        rewardsListView.setLayoutManager(new LinearLayoutManager(this));

        // TODO: Scheduled heartbeat and business updates might need to consider activity lifecycle, because in low memory situation this activity could get destroyed and re-created when checking in

        // Schedule heartbeat timer to send updates to the server
        mScheduler.scheduleAtFixedRate(mDeviceHeartBeatTimer, 45, 60 * 60, TimeUnit.SECONDS);


        // When app starts, check and update business details
        mScheduler.schedule(mUpdateBusinessDetails, 20, TimeUnit.SECONDS);

        mScheduler.schedule(mCheckForAppUpdate, 30, TimeUnit.SECONDS);
        updateAppUpgradeMessage();
    }

    private void updateAppUpgradeMessage() {
        TextView step1View = (TextView)findViewById(R.id.lbl_step1);
        Button installButton = (Button)findViewById(R.id.btn_install);
        SharedPreferences prefs = AppPrefs.getSharedPreferences(this);
        // If new update is ready, then prompt user to install it
        int updateVersion = prefs.getInt(AppPrefs.APP_UPDATE_VERSION,-1);
        if(updateVersion > Utils.getAppVersion(this)) {
            String updateMessage =  "A new version of Zippy is available v" + prefs.getString(AppPrefs.APP_UPDATE_VERSION_NAME,"");
            step1View.setText(updateMessage);

            step1View.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_info_black_48dp, 0, 0, 0);
            step1View.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            findViewById(R.id.lbl_step2).setVisibility(View.GONE);
            findViewById(R.id.lbl_step3).setVisibility(View.GONE);

            installButton.setVisibility(View.VISIBLE);
            installButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences prefs = AppPrefs.getSharedPreferences(getApplicationContext());
                    prefs.edit().putBoolean(AppPrefs.APP_UPDATE_INSTALLING,true).commit(); // Temp flag, used so we don't close the install dialog
                    String apkFile = prefs.getString(AppPrefs.APP_UPDATE_FILE, "");
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(new File(apkFile)), "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });

        }else {
            step1View.setText(R.string.step1);
            installButton.setVisibility(View.GONE);
            step1View.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_looks_one_black_48dp, 0, 0, 0);
            step1View.setTextColor(getResources().getColor(android.R.color.black));
            findViewById(R.id.lbl_step2).setVisibility(View.VISIBLE);
            findViewById(R.id.lbl_step3).setVisibility(View.VISIBLE);
        }
    }

    private void startScanActivity() {
        Intent intent = new Intent();
        intent.setClass(KioskActivity.this, ScannerActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // ping the server with a noop to wake up the device mobile radio, so it should then be quicker to check-in
        noop();
    }

    private void updateBusinessLogo(@Nullable Business business) {
        final ImageView logo = (ImageView) findViewById(R.id.businessLogo);

        if(business==null || business.logo == null) {
            logo.setVisibility(View.GONE);
        }else {
            String logoFullUrl = ZippyApi.getInstance().addBaseUrlScheme(business.logo);
            Picasso.with(this).load(logoFullUrl)
                    .into(logo, new Callback.EmptyCallback() {
                        @Override
                        public void onSuccess() {
                            // After the image is loaded, find color of center/top pixel in the image
                            // and apply it to the background
                            final Bitmap bitmap = ((BitmapDrawable) logo.getDrawable()).getBitmap();
                            int color = bitmap.getPixel(bitmap.getWidth() / 2, 2);
                            ((CardView) findViewById(R.id.card_view_logo)).setCardBackgroundColor(color);
                            logo.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    private void updateBusinessQRCode(@Nullable Business business) {
        ImageView qrCodeImageView = (ImageView) findViewById(R.id.businessQRCode);
        if (business == null || business.qrCode == null || business.qrCode.length < 1 ||
                TextUtils.isEmpty(business.qrCode[0].slug) || business.config == null || business.config.displayQRCode == 0) {
            findViewById(R.id.card_qr_code).setVisibility(View.GONE);
        } else {
            // Display first QR code linked to the business, for scanning by mobile app
            // TODO: Should probably be done off the main thread
            Bitmap qrcodeBitmap = QRCodeEncode.encodeAsBitmap(business.qrCode[0].slug, 320, null);
            qrCodeImageView.setImageBitmap(qrcodeBitmap);
            findViewById(R.id.card_qr_code).setVisibility(View.VISIBLE);
        }
    }

    private void noop() {
        ZippyApi.getInstance().getApiService().noop(new retrofit.Callback<Response>() {
            @Override
            public void success(Response ignore, Response response) {
                // ignore
            }

            @Override
            public void failure(RetrofitError error) {
                // ignore
            }
        });
    }

    /**
     * Send device status/heart beat to the server
     */
    private final Runnable mDeviceHeartBeatTimer = new Runnable() {
        @Override
        public void run() {
            ZippyApi.getInstance().sendDeviceStatus(new retrofit.Callback<Response>() {
                @Override
                public void success(Response resp1, Response response) {
                }

                @Override
                public void failure(RetrofitError error) {
                    KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                            .setDescription(RetrofitUtils.getErrorDescription("Send Heartbeat Failure.", error))
                            .setFatal(false)
                            .build());
                    CrashlyticsCore.getInstance().logException(new Exception("Send Heartbeat Failure.", error));
                }
            });
        }
    };

    // Get the business details from the server and update UI if changed
    private final Runnable mUpdateBusinessDetails = new Runnable() {
        @Override
        public void run() {

            final Business currentBusiness = Business.load(KioskActivity.this);
            if(currentBusiness==null) return;

            ZippyApi.getInstance().getApiService().getBusiness(currentBusiness.id, new retrofit.Callback<Business>() {

                @Override
                public void success(Business newBusiness, Response response) {
                    //mScheduler.schedule(mUpdateBusinessDetails, 15, TimeUnit.SECONDS);

                    if (KioskActivity.this.isFinishing() || mScheduler == null || mScheduler.isShutdown()) {
                        Log.i("Get business details success, after activity destroyed.");
                        return;
                    }

                    // Check business details once a day
                    mUpdateBusinessRetries = 0;
                    mScheduler.schedule(mUpdateBusinessDetails, 24 * 60 * 60, TimeUnit.SECONDS);

                    // TODO: Implement a more generic method handling of changes in business details

                    // Check if any business details have changed and update UI with changes
                    if (!Business.logosEqual(currentBusiness, newBusiness)) {
                        updateBusinessLogo(newBusiness);
                        Log.i("Business logo changed. Updating business logo");
                    }

                    if (!Business.qrCodesEqual(currentBusiness, newBusiness) || !Business.displayQRCodesEqual(currentBusiness, newBusiness)) {
                        // Display first QR code that is linked to the business, for scanning by mobile app
                        updateBusinessQRCode(newBusiness);
                        Log.i("Business QR code or status changed. Updating business QR code");
                    }

                    if (!Business.rewardsEqual(currentBusiness, newBusiness)) {
                        mRewardsRecyclerAdapter.setList(DB.getRewardsList(newBusiness));
                        Log.i("Business rewards changed. Updating rewards");
                    }
                    Business.save(KioskActivity.this, newBusiness);

                }


                @Override
                public void failure(RetrofitError error) {
                    //Log.e("HeartBeat Exception.", error.getMessage(), error);
                    //mScheduler.schedule(mUpdateBusinessDetails, 15, TimeUnit.SECONDS);

                    if (KioskActivity.this.isFinishing() || mScheduler == null || mScheduler.isShutdown()) {
                        Log.i("Get business details failure, after activity destroyed.");
                        return;
                    }

                    // If failure, retry with exponential delay 2, 4, 8, ... minutes
                    KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                            .setDescription(RetrofitUtils.getErrorDescription("Get Business Details Failure.", error))
                            .setFatal(false)
                            .build());

                    CrashlyticsCore.getInstance().logException(new Exception("Get Business Details Failure.", error));

                    int delay_minutes;
                    if (mUpdateBusinessRetries > 8) {
                        delay_minutes = 6 * 60;
                    } else {
                        delay_minutes = (int) Math.pow(2, mUpdateBusinessRetries) * 2;
                    }
                    mScheduler.schedule(mUpdateBusinessDetails, delay_minutes * 60, TimeUnit.SECONDS);
                    mUpdateBusinessRetries++;

                }
            });
        }
    };
    /**
     * Check latest app version on server
     */
    private final Runnable mCheckForAppUpdate = new Runnable() {
        final int INITIAL_RETRY_DELAY_SEC = 30; // Initial retry delay after failure
        final int MAX_RETRY_DELAY_SEC = 2*60*60;

        int delaySecs = INITIAL_RETRY_DELAY_SEC;

        @Override
        public void run() {
            ZippyApi.getInstance().getApiService().latestAppVersion(getApplication().getPackageName(), new retrofit.Callback<LatestAppVersionResponse>() {

                @Override
                public void success(LatestAppVersionResponse latestAppVersionResponse, Response response) {
                    delaySecs = INITIAL_RETRY_DELAY_SEC;
                    if(latestAppVersionResponse!=null &&
                            !TextUtils.isEmpty(latestAppVersionResponse.version)  &&
                            !TextUtils.isEmpty(latestAppVersionResponse.binaryUri) &&
                            Integer.parseInt(latestAppVersionResponse.version) > Utils.getAppVersion(getApplicationContext()) &&
                            Integer.parseInt(latestAppVersionResponse.version) != AppPrefs.getSharedPreferences(KioskActivity.this).getInt(AppPrefs.APP_UPDATE_VERSION,-1)) {
                        // Download apk
                        Bundle bundle = new Bundle();
                        Uri uri = Uri.parse(latestAppVersionResponse.binaryUri);

                        bundle.putString("url", latestAppVersionResponse.binaryUri);
                        bundle.putInt("version", Integer.parseInt(latestAppVersionResponse.version));
                        bundle.putString("version_name", latestAppVersionResponse.versionName);
                        bundle.putString("filename", uri.getLastPathSegment());
                        mDownloadTask = new DownloadFileTask();

                        //noinspection unchecked
                        mDownloadTask.execute(bundle);

                    }else {
                        // Reschedule task
                        mScheduler.schedule(mCheckForAppUpdate, APP_CHECK_PERIOD_SEC, TimeUnit.SECONDS);
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                            .setDescription(RetrofitUtils.getErrorDescription("Check For App Update Failure.", error))
                            .setFatal(false)
                            .build());
                    CrashlyticsCore.getInstance().logException(new Exception("Check For App Update Failure.", error));
                    mScheduler.schedule(mCheckForAppUpdate, delaySecs, TimeUnit.SECONDS);
                    delaySecs *= 2;
                    if(delaySecs>MAX_RETRY_DELAY_SEC) {
                        delaySecs = MAX_RETRY_DELAY_SEC;
                    }
                }
            });
        }
    };

    private final BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateConnectivityState();
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mNetworkStateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateConnectivityState();
        registerReceiver(mNetworkStateReceiver, mConnectivityIntentFilter);

        // Rewards may have changed after checkin, so update when activity resumes, just in case they changed
        final Business business = Business.load(this);
        mRewardsRecyclerAdapter.setList(DB.getRewardsList(business));
    }

    @Override
    protected void onDestroy() {

        KioskApp.tracker.send(new HitBuilders.EventBuilder()
                .setCategory("App Kiosk Mode")
                .setAction("Stopped")
                .setLabel(super.passwordShutdown ? "User Shutdown" : "Shutdown")
                .build());
        if(mDownloadTask!=null) mDownloadTask.cancel(true);
        mScheduler.shutdownNow();
        super.onDestroy();
    }

    private void updateConnectivityState() {
        boolean isConnected = Utils.isNetworkConnected(this);
        mCheckinButton.setEnabled(isConnected);
        mCheckinButton.setVisibility(isConnected ? View.VISIBLE : View.INVISIBLE);
        ViewGroup.LayoutParams params = mCheckinButton.getLayoutParams();
        params.height = isConnected ? Utils.convertDIPToPixels(this,128) : Utils.convertDIPToPixels(this,64);
        mCheckinButton.setLayoutParams(params);
        findViewById(R.id.offlineMessage).setVisibility(isConnected ? View.GONE : View.VISIBLE);

        if(mIsNetworkConnected!=isConnected) {
            if (!isConnected) {
                // When disconnected, send event to indicate how long we were connected
                KioskApp.tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Network")
                        .setAction("Connected")
                        .setValue(System.currentTimeMillis() - mNetworkConnectivityStateTime)
                        .build());
            } else {
                // When disconnected, send event to indicate how long we were disconnected
                KioskApp.tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Network")
                        .setAction(mConnectivityStateCounter>0 ? "Disconnected" : "Startup Connection")
                        .setValue(System.currentTimeMillis() - mNetworkConnectivityStateTime)
                        .build());
            }
            mNetworkConnectivityStateTime = System.currentTimeMillis();
            mConnectivityStateCounter++;
            mIsNetworkConnected = isConnected;
        }

    }

    static public class ListRecyclerAdapter extends RecyclerView.Adapter<ListRowHolder> {
        private List<RewardItem> mItemList;

        public ListRecyclerAdapter(List<RewardItem> itemList) {
            this.mItemList = itemList;
        }

        public void setList(List<RewardItem> itemList) {
            this.mItemList = itemList;
            notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(ListRowHolder listRowHolder, int i) {
            RewardItem listItem = mItemList.get(i);
            listRowHolder.title.setText(listItem.title);
            listRowHolder.points.setText(Integer.toString(listItem.points));
        }

        @Override
        public int getItemCount() {
            return (null != mItemList ? mItemList.size() : 0);
        }


        @Override
        public ListRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_reward, parent, false);
            return new ListRowHolder(v);
        }
    }

    public static final class ListRowHolder extends RecyclerView.ViewHolder {
        protected final TextView title;
        protected final TextView points;

        public ListRowHolder(View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.title);
            this.points = (TextView) view.findViewById(R.id.points);

        }
    }



    private class DownloadFileTask extends AsyncTask<Object , Void, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            Bundle bundle = (Bundle)params[0];

            int apkVersion = bundle.getInt("version", -1);
            String apkVersionName = bundle.getString("version_name", "");
            File tmpFile = null;
            HttpURLConnection httpURLConnection = null;
            FileOutputStream fos=null;
            InputStream instream=null;
            File outputFile = new File(Utils.getAppDownloadDir(KioskApp.getAppContext()), bundle.getString("filename",""));
            if (outputFile.exists()) {
                if(!outputFile.delete()) {
                    Log.e("Could not delete file " + outputFile.getPath());
                    KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                            .setDescription("APK Download failed. Could not delete " + outputFile.getPath())
                            .setFatal(false)
                            .build());

                    CrashlyticsCore.getInstance().logException(new Exception("APK Download failed. Could not delete file " + outputFile.getPath()));
                    return false;
                }
            }

            try {
                tmpFile = Utils.createTempFile(KioskApp.getAppContext());

                URL url = new URL(bundle.getString("url",""));
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                fos = new FileOutputStream(tmpFile);

                instream = httpURLConnection.getInputStream();

                byte[] buffer = new byte[2048];
                int len1;
                while ((len1 = instream.read(buffer)) != -1) {
                    if(isCancelled()) return false; // bail out if cancelled
                    fos.write(buffer, 0, len1);
                }

                instream.close(); instream = null;
                fos.close(); fos = null;
                httpURLConnection.disconnect(); httpURLConnection=null;

                if(!tmpFile.renameTo(outputFile)) {
                    Log.e("Failed to create file " + outputFile.getAbsolutePath());
                }
                SharedPreferences prefs = AppPrefs.getSharedPreferences(KioskApp.getAppContext());
                prefs.edit()
                        .putInt(AppPrefs.APP_UPDATE_VERSION, apkVersion)
                        .putString(AppPrefs.APP_UPDATE_VERSION_NAME, apkVersionName)
                        .putString(AppPrefs.APP_UPDATE_FILE, outputFile.getAbsolutePath())
                        .apply();
                Log.i("App apk update " + apkVersionName + " downloaded");
                return true;

            } catch (IOException e) {
                Log.e(e);
                KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("APK Download failed. " + e.getMessage())
                        .setFatal(false)
                        .build());
                CrashlyticsCore.getInstance().logException(new Exception("APK Download failed. ", e));

            } finally {
                try { if(instream!=null) instream.close(); }catch(Exception e) {Log.e(e);}
                try { if(fos!=null) fos.close(); }catch(Exception e) {Log.e(e);}
                try { if(httpURLConnection!=null) httpURLConnection.disconnect(); }catch(Exception e) {Log.e(e);}
                try { if(tmpFile!=null && tmpFile.exists()) tmpFile.delete(); }catch(Exception e) {Log.e(e);}
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            if(!isCancelled() && !isFinishing()) {
                mScheduler.schedule(mCheckForAppUpdate, result ? APP_CHECK_PERIOD_SEC : APP_DOWNLOAD_FAIL_CHECK_PERIOD_SEC, TimeUnit.SECONDS);
                if(result) {
                    updateAppUpgradeMessage();
                }
                KioskApp.tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("APK")
                        .setAction("Download")
                        .setLabel(result ? "success" : "failed")
                        .build());
            }
            mDownloadTask = null;
        }
    }

}
