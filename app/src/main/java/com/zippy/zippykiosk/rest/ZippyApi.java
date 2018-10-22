package com.zippy.zippykiosk.rest;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.analytics.HitBuilders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.zippy.zippykiosk.BuildConfig;
import com.zippy.zippykiosk.KioskApp;
import com.zippy.zippykiosk.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * Created by KB on 1/03/15.
 * Copyright 2015 Zippy.com.au.

 * Singleton object to deal with the Zippy API and user authorization.
 *
 */
public class ZippyApi {
    public static final String ZIPPY_PRODUCTION_API_ENDPOINT = "https://api.zippy.com.au";
    private static final String ZIPPY_STAGING_API_ENDPOINT = "https://staging.api.zippy.com.au";
    //public static final String ZIPPY_API_ENDPOINT = "http://test.dev/api"; // Localhost testing
    //public static final String ZIPPY_API_ENDPOINT = "http://tablettemp.api.zippy.com.au";
    public static final String ZIPPY_API_ENDPOINT = BuildConfig.DEBUG ? ZIPPY_STAGING_API_ENDPOINT : ZIPPY_PRODUCTION_API_ENDPOINT;

    private static volatile ZippyApi sSingleton;
    private String mZippyApiUrl=null;
    private final AccessTokenStore mTokenStore;
    private ZippyApiService mZippyService;
    private final OkClient mOkClient;

    // All devices are registered using the same user login
    //private final String LOGIN_USERNAME = "kioskuser@zippy.com.au";
    //private final String LOGIN_PASSWORD = "376762";

    /* package */
    private ZippyApi(Application application) {
        mTokenStore = new AccessTokenStore(application);

        //System.setProperty("http.keepAlive", "true");
        OkHttpClient client = new OkHttpClient();
/*
        if (Constants.ENABLE_STETHO) {
            client.networkInterceptors().add(new StethoInterceptor());
        }
  */
        //client.setConnectionPool(new ConnectionPool(5, 5 * 60 * 1000));
        client.setConnectTimeout(20 * 1000, TimeUnit.MILLISECONDS);
        client.setReadTimeout(20 * 1000, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(20 * 1000, TimeUnit.MILLISECONDS);
        client.interceptors().add(new RequestInterceptor());
        mOkClient = new OkClient(client);
    }



    /**
     * Initialize the singleton instance of this class.
     * @param application the application.
     */
    public static void initialize(@NonNull Application application) {
        if (sSingleton == null) {
            synchronized (ZippyApi.class) {
                if (sSingleton == null) {
                    sSingleton = new ZippyApi(application);
                }
            }
        }
    }

    public static synchronized ZippyApi getInstance() {
        if (sSingleton == null) {
            throw new IllegalStateException("ZippyApi is not yet initialized.");
        }
        return sSingleton;
    }

    /**
     * Terminate singleton instance lifetime.
     */
    public static synchronized void destroy() {
        sSingleton = null;
    }


    /**
     * @return The ZippyService instance to access API
     */
    public synchronized ZippyApiService getApiService() {
        if (mZippyService == null) {
            RestAdapter adapter = getApiAdapter();
            mZippyService = adapter.create(ZippyApiService.class);
        }
        return mZippyService;
    }



    /**
     * @return true if user already authorized.
     */
    public boolean isAuthorized() {
        AccessToken token = mTokenStore.getAccessToken();
        return !TextUtils.isEmpty(token.accessToken);
    }

    public void login(final String email, final String password, final Callback<User> callback) {

        getApiService().getAccessToken(ZippyApiService.GRANT_TYPE_PASSWORD, ZippyApiService.CLIENT_ID, ZippyApiService.CLIENT_SECRET, email, password, new Callback<AccessToken>() {

            @Override
            public void success(AccessToken accessToken, Response response) {
                mTokenStore.save(accessToken);
                mTokenStore.saveEmail(email);
                mTokenStore.savePassword(password);

                getApiService().userPing(new Callback<Response>() {

                    @Override
                    public void success(Response response, Response ignore) {
                        try {
                            JsonReader reader = new JsonReader(new InputStreamReader(response.getBody().in()));
                            JsonParser parser = new JsonParser();
                            JsonObject jo = parser.parse(reader).getAsJsonObject();
                            Gson gson = new Gson();
                            User user = gson.fromJson(jo.getAsJsonObject("object").getAsJsonObject("user"), User.class);
                            callback.success(user, response);
                        } catch (IOException e) {
                            Log.e(e);
                            callback.failure(RetrofitError.unexpectedError(response.getUrl(), e));
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        callback.failure(error);
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error);
            }
        });
    }

    public void logout() {
        mTokenStore.clear();
    }


    public void registerDevice(final Device device, final Callback<Integer> callback) {

        getApiService().addDevice(device, new Callback<Response>() {
            @Override
            public void success(Response response1, Response response) {
                Integer devID = 0;
                for (Header header : response.getHeaders()) {
                    // Get the deviceId from the response header
                    // Header -> Location:/devices/{ID}
                    // eg /devices/5
                    if (header.getName().equalsIgnoreCase("Location")) {
                        try {
                            String[] tokens = header.getValue().split("/");
                            if (tokens.length > 0) {
                                String deviceIdString = tokens[tokens.length - 1];
                                devID = Integer.valueOf(deviceIdString);
                            }
                        } catch (Exception e) {
                            // NumberFormatException
                            e.printStackTrace();
                        }
                        break;
                    }
                }
                if (devID != null) {
                    callback.success(devID, response);
                } else {
                    callback.failure(RetrofitError.unexpectedError(response.getUrl(), new Throwable("Unexpected server response. No device ID")));
                }
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error);
            }
        });
    }

    public void checkinAmount(@NonNull final String businessUri, @NonNull final String qrCode, @NonNull final String amount, final Callback<CheckinResult> callback) {

        final CheckinBody checkinBody = new CheckinBody(businessUri, qrCode, amount);

        getApiService().checkin(checkinBody, new Callback<CheckinResponse>() {
            @Override
            public void success(CheckinResponse checkinResponse, Response response) {
                int pointsEarned = checkinResponse.point.pointsEarned;
                String denialMessage = (checkinResponse.attributes != null && checkinResponse.attributes.denial != null) ? checkinResponse.attributes.denial : null;
                checkinResult(pointsEarned, denialMessage, businessUri, qrCode, callback);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error);
            }
        });
    }

    public void checkin(boolean isValueBased, @NonNull final String businessUri, @NonNull final String qrCode, final Callback<CheckinResult> callback) {
        if(isValueBased) {
            // Checkin to value based business
            checkinResult(0, null, businessUri, qrCode, callback);
        }else {
            final CheckinBody checkinBody = new CheckinBody(businessUri, qrCode, null);

            getApiService().checkin(checkinBody, new Callback<CheckinResponse>() {
                @Override
                public void success(CheckinResponse checkinResponse, Response response) {
                    int pointsEarned = checkinResponse.point.pointsEarned;
                    String denialMessage = (checkinResponse.attributes != null && checkinResponse.attributes.denial != null) ? checkinResponse.attributes.denial : null;
                    checkinResult(pointsEarned, denialMessage, businessUri, qrCode, callback);
                }

                @Override
                public void failure(RetrofitError error) {
                    callback.failure(error);
                }
            });
        }
    }

    public void checkinResult(final int pointsEarned, final String denialMessage, final String businessUri, final String qrCode, final Callback<CheckinResult> callback) {
        final String formattedBusinessUri = ZippyApiUtils.formatBusinessUriFromUri(businessUri);
        final String userUri = ZippyApiUtils.formatUserUriFromQRCodeSlug(qrCode);
        getApiService().checkinReport(formattedBusinessUri, userUri, new Callback<CheckinReport>() {
            @Override
            public void success(CheckinReport checkinReport, Response response) {
                CheckinResult checkinResult = new CheckinResult();
                checkinResult.userId = checkinReport.user.id;
                checkinResult.userStatus = checkinReport.user.status;
                checkinResult.userEmail = checkinReport.user.email;
                checkinResult.userUri = checkinReport.user.uri;
                checkinResult.pointsBalance = checkinReport.report.point.balance;
                checkinResult.checkinsBusinessTotal = checkinReport.report.checkin.businessTotal;
                checkinResult.checkinsUserTotal = checkinReport.report.checkin.userTotal;
                checkinResult.isGuest = checkinReport.report.user.guest;
                checkinResult.isVerified = checkinReport.report.user.verified;
                checkinResult.isClaimable = checkinReport.report.reward.claimable;
                checkinResult.qrCode = qrCode;


                checkinResult.pointsEarned = pointsEarned;
                checkinResult.denialMessage = denialMessage;

                checkinResult.rewards = checkinReport.business.rewards;
                if (checkinReport.report.reward.lastClaimed != null) {
                    checkinResult.lastClaimedReward = checkinReport.report.reward.lastClaimed.reward;
                    checkinResult.lastClaimedRewardDate = checkinReport.report.reward.lastClaimed.created;
                }
                callback.success(checkinResult, response);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error);
            }
        });
    }

    // This method should only be called if the user is a guest (User.STATUS_GUEST).
    public void promoteUser(int userID,
                            int mUserStatus,
                            @NonNull String email,
                            @Nullable String name,
                            @Nullable String postcode,
                            final Callback<Response> callback) {

        if(name == null || postcode == null) {
            ZippyApi.getInstance().getApiService().userPromote("</users/" + userID + ">", email, callback);
        }else {
            // Promote user and update name and postcode
            String firstName;
            String lastName;

            int status = User.setPromotedStatus(mUserStatus);
            int index = name.indexOf(" ");
            if (index > 0) {
                firstName = name.substring(0, index);
                lastName = name.substring(index + 1);
            } else {
                firstName = name;
                lastName = "";
            }
            UserUpdatePromoteBody userUpdatePromoteBody = new UserUpdatePromoteBody(status, email, firstName, lastName, postcode);
            ZippyApi.getInstance().getApiService().userUpdatePromote(userID, userUpdatePromoteBody, callback);
        }
    }


    public void sendDeviceStatus(final Callback<Response> callback) {
        HeartBeat heartBeat = new HeartBeat();
        heartBeat.android = new HeartBeat.Android();
        heartBeat.app = new HeartBeat.App();
        heartBeat.location = new HeartBeat.GeoPoint();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = KioskApp.getAppContext().registerReceiver(null, intentFilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        if(scale>0 && level>=0) {
            heartBeat.battery = (int) (100*level / (float) scale);
        }else {
            heartBeat.battery = 0;
        }
        // How are we charging?
        int chargePlug = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) : -1;
        heartBeat.charge_usb = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        heartBeat.charge_power = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        Context context = KioskApp.getAppContext();
        try {
            heartBeat.app.version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0 ).versionName;
            heartBeat.app.version_name = context.getPackageManager().getPackageInfo(context.getPackageName(), 0 ).packageName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(e);
        }
        heartBeat.android.version_release = Build.VERSION.RELEASE;
        heartBeat.android.version_sdk = String.valueOf(Build.VERSION.SDK_INT);

        getApiService().heartbeat(mTokenStore.getDeviceId(), heartBeat, new Callback<Response>() {
            @Override
            public void success(Response resp1, Response response) {
                Log.i("DeviceStatus logged");
                callback.success(resp1, response);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(error);
                callback.failure(error);
            }
        });
    }

    /**
     * Refresh access token in the same thread.
     */
    private void blockingRefreshToken() {
        AccessToken token = mTokenStore.getAccessToken();

        AccessToken newToken = getApiService().refreshAccessToken(ZippyApiService.GRANT_TYPE_REFRESH_TOKEN, ZippyApiService.CLIENT_ID, ZippyApiService.CLIENT_SECRET, token.refreshToken);
        mTokenStore.save(newToken);
    }

    /**
     * Get new access token in the same thread.
     */
    private void blockingNewToken() {
        AccessToken newToken = getApiService().getAccessToken(ZippyApiService.GRANT_TYPE_PASSWORD, ZippyApiService.CLIENT_ID, ZippyApiService.CLIENT_SECRET, mTokenStore.getEmail(), mTokenStore.getPassword());
        mTokenStore.save(newToken);
    }
    public AccessTokenStore getTokenStore() {
        return mTokenStore;
    }

    private RestAdapter getApiAdapter() {
        final GsonConverter gsonConverter = new GsonConverter(new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

                .registerTypeAdapter(User.class, new JsonContainerDeserializer<>(User.class, "object"))
                .registerTypeAdapter(User.class, new JsonContainerSerializer<>("object"))

                .registerTypeAdapter(Device.class, new JsonContainerDeserializer<>(Device.class, "object"))
                .registerTypeAdapter(Device.class, new JsonContainerSerializer<>("object"))

                .registerTypeAdapter(HeartBeat.class, new JsonContainerDeserializer<>(HeartBeat.class, "object"))
                .registerTypeAdapter(HeartBeat.class, new JsonContainerSerializer<>("object"))

                .registerTypeAdapter(Business.class, new JsonContainerDeserializer<>(Business.class, "object"))

                .registerTypeAdapter(LatestAppVersionResponse.class, new JsonContainerDeserializer<>(LatestAppVersionResponse.class, "object"))

                .registerTypeAdapter(CheckinBody.class, new JsonContainerSerializer<>("object"))
                .registerTypeAdapter(CheckinResponse.class, new JsonContainerDeserializer<>(CheckinResponse.class, "object"))

                .registerTypeAdapter(ClaimRewardResponse.class, new JsonContainerDeserializer<>(ClaimRewardResponse.class, "object"))

                .registerTypeAdapter(UserUpdateEmailBody.class, new JsonContainerSerializer<>("object"))

                .registerTypeAdapter(UserUpdatePromoteBody.class, new JsonContainerSerializer<>("object"))

                .create());

        return new RestAdapter.Builder()
                .setLogLevel(BuildConfig.DEBUG ? RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE)
                .setConverter(gsonConverter)
                .setClient(mOkClient)
                .setEndpoint(ZIPPY_API_ENDPOINT)
                .build();
    }

    private boolean authenticate() {
        Log.i("Access token expired. Refreshing token.");

        boolean fNewToken = false;
        try {
            blockingRefreshToken();
            fNewToken = true;
            Log.i("Access token refreshed.");
        }catch(RetrofitError error) {
            Log.i("Failed to refresh auth token. ");
            if(error.getResponse()!=null) {
                int status = error.getResponse().getStatus();

                // We get 400 Bad Request if refresh_token is no longer valid
                if(status ==400 || status == 401 || status == 403) {

                    // try to get new access token using password
                    try {
                        Log.i("Trying to get new access token.");
                        blockingNewToken();
                        fNewToken = true;
                        Log.i("New access token.");
                    } catch (RetrofitError error2) {
                        if (error2.getResponse() != null) {
                            int status2 = error2.getResponse().getStatus();
                            if(status2 == 400 || status2 == 401 || status2 == 403) {
                                Log.i("Failed to get new access token.");
                            }
                        }
                    }
                }
            }
        }
        return(fNewToken);

    }

    private class RequestInterceptor implements Interceptor {

        @Override
        public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
            com.squareup.okhttp.Response response;
            com.squareup.okhttp.Request request = chain.request();

            boolean authapi = request.urlString().contains("security/token") ||
                    (request.urlString().endsWith("/users") && request.method().equals("PUT")) ||
                    request.urlString().endsWith("/users/resetpassword/request") ;

            boolean retry=false;
            if(!authapi) {
                // Get access token if we don't have one or if it has expired
                // Using synchronized access so if two api calls at the same time we don't update the
                // token twice. Second api call blocked until first calls gets the access token
                synchronized (mTokenStore) {
                    if (mTokenStore.isExpired()) {
                        Log.i("Access token expired. Getting new access token.");
                        authenticate();
                    }else {
                        Log.i("Access token not expired.");
                    }
                }
            }

            do {
                // Add additional headers to the request
                if(!authapi) {
                    // For Authorization header, don't use tokenType from server which is lowercase 'bearer' as this causes failure
                    request = request.newBuilder()
                            .header("Authorization", "Bearer " + mTokenStore.getAccessToken().accessToken)
                            .build();
                }
                long t1 = System.currentTimeMillis();

                response = chain.proceed(request);

                long t2 = System.currentTimeMillis();


                KioskApp.tracker.send(new HitBuilders.TimingBuilder()
                        .setCategory("API Request")
                        .setValue(t2 - t1) // ms
                        .setLabel(response.isSuccessful() ? "success" : "failed")
                        .setVariable(getUrlAnalyticsLabel(request.urlString()))
                        .build());
                if(!response.isSuccessful()) {
                    KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                            .setDescription(getUrlAnalyticsLabel(request.urlString()) + ". status: " + response.code())
                            .setFatal(false)
                            .build());
                }

                // Get new access token if 401 or 403 response, and try once more.
                if(response.code() == 401 || response.code() == 403) {
                    if (!authapi && !retry) {
                        Log.i("Server returned " +  response.code() + ". We are going to try and get another access token and retry.");
                        // Should be rare if ever that we get here, as token already refreshed if expired
                        // Unexpired token would have to be rejected by the server. This could occur during development if server wipes tokens.
                        synchronized (mTokenStore) {
                            retry = authenticate();
                        }
                    }else {

                        retry = false;
                    }
                }else {
                    retry = false;
                }

            }while(retry);

            return response;
        }
    }


    public static String getServerResponseErrorMessage(RetrofitError error) {
        String msg=null;
        try {
            String bodyString = RetrofitUtils.getBodyString(error.getResponse());
            JsonParser parser = new JsonParser();
            JsonObject jo = parser.parse(bodyString).getAsJsonObject();
            if(jo!=null && jo.has("message")) {
                msg = jo.get("message").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }



    private static String getUrlAnalyticsLabel(String url) {
        if(url != null) {
            int domainEnd = url.indexOf('/', 8); // skip past https://
            String serverAddress;
            if (domainEnd > 0) {
                serverAddress = " " + url.substring(0, domainEnd);
            } else {
                serverAddress = "";
            }

            if (url.contains("/security/token")) {
                // Could be getting new token or refreshing token
                return "Get AccessToken." + serverAddress;
            } else if (url.contains("/heartbeat")) {
                return "Send Heartbeat." + serverAddress;
            } else if (url.contains("/reports/checkin/kioskstate")) {
                return "Checkin Report." + serverAddress;
            } else if (url.contains("/checkins")) {
                return "Checkin." + serverAddress;
            } else if (url.contains("/rewards/")) {
                return "Claim Reward." + serverAddress;
            } else if (url.contains("/security/ping")) {
                return "Ping." + serverAddress;
            } else if (url.contains("/business")) {
                return "Get Business Details." + serverAddress;
             }else if (url.contains("/devices/appreleases/")) {
                return "Get Latest App Release." + serverAddress;
            } else if (url.contains("/devices")) {
                return "Add Device." + serverAddress;
            } else if (url.contains("/users/trigger/verify")) {
                return "Resend Verify Email." + serverAddress;
            } else if (url.contains("/users/promote")) {
                return "Promote User." + serverAddress;
            } else if (url.contains("/users/merge")) {
                return "Merge User." + serverAddress;
            } else if (url.contains("/users/")) {
                return "Update User." + serverAddress;
            } else if (url.contains("/meta/noop")) {
                return "Noop." + serverAddress;
            }
        }
        return url;
    }



    // Adds base scheme to URL if missing
    public @Nullable String addBaseUrlScheme(@Nullable String urlString) {
        if(urlString!=null && !isHttpUrl(urlString) && !isHttpsUrl(urlString)) {
            try {
                String urlFullString;
                URL url = new URL(ZIPPY_API_ENDPOINT);
                if(urlString.startsWith("//")) {
                    urlFullString = url.getProtocol() + ":" + urlString;
                }else {
                    urlFullString = url.getProtocol() + "://" + urlString;
                }
                return urlFullString;
            } catch (MalformedURLException e) {
                Log.e(e);
            }
        }
        return(urlString);
    }

    /**
     * @return True iff the url is an http: url.
     */
    private static boolean isHttpUrl(String url) {
        return (null != url) &&
                (url.length() > 6) &&
                url.substring(0, 7).equalsIgnoreCase("http://");
    }

    /**
     * @return True iff the url is an https: url.
     */
    private static boolean isHttpsUrl(String url) {
        return (null != url) &&
                (url.length() > 7) &&
                url.substring(0, 8).equalsIgnoreCase("https://");
    }


}