package com.zippy.zippykiosk.rest;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;


/**
 * Created by KB on 27/02/15.
 * Copyright 2015 Zippy.com.au.
 */
public interface ZippyApiService {
    String CLIENT_ID = "55fa25fc4739995b398b4567_zsxqcresbsgogwgoocsooo0cwc80ookck0wckgs00sgs4ckks";
    String CLIENT_SECRET = "680vtwfkhgkkwkwg04oskcwcko0g4so4sk4wwwk8g0gw8c844c";
    String GRANT_TYPE_PASSWORD = "password";
    String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    String RESPONSE_TYPE = "code";

    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @GET("/meta/noop")
    void noop(Callback<Response> callback);

    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @GET("/security/ping")
    void userPing(Callback<Response> callback);

    @Headers("Accept: application/json")
    @PUT("/devices")
    void addDevice(@Body Device device, Callback<Response> callback);

    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @GET("/business/{id}")
    void getBusiness(@Path("id") int businessId, Callback<Business> callback);

    @Headers("Accept: application/json")
    @PUT("/devices/{id}/heartbeat")
    void heartbeat(@Path("id") int deviceId, @Body HeartBeat heartBeat, Callback<Response> callback);

    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @PUT("/checkins")
    void checkin(@Body CheckinBody checkinBody, Callback<CheckinResponse> callback);


    @FormUrlEncoded
    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @POST("/reports/checkin/kioskstate/v1")
    void checkinReport(@Field("business") String businessUri, @Field("user") String userUri, Callback<CheckinReport> callback);

    @FormUrlEncoded
    @Headers({
            "Accept: application/json",
            "X-Quiet: 1"
    })
    @POST("/reports/checkin/kioskstate/v1")
    void checkinFullReport(@Field("business") String businessUri, @Field("user") String userUri, Callback<CheckinReport> callback);

    @FormUrlEncoded
    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @POST("/rewards/{id}/claim")
    void claimReward(@Path("id") int rewardId, @Field("user") String userUri, Callback<ClaimRewardResponse> callback);

    @Headers({
            "Accept: application/json",
            "X-Quiet: 1"
    })
    @GET("/devices/appreleases/latest/{app_id}")
    void latestAppVersion(@Path("app_id") String appID, Callback<LatestAppVersionResponse> callback);

    @FormUrlEncoded
    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @POST("/users/trigger/verify/email")
    void userResendVerificationEmail(@Field("user") String userUri, Callback<Response> callback);

    @FormUrlEncoded
    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @POST("/users/promote")
    void userPromote(@Field("user") String userUri, @Field("email") String email, Callback<Response> callback);


    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @POST("/users/{id}")
    void userUpdatePromote(@Path("id") int userId, @Body UserUpdatePromoteBody userUpdatePromoteBody, Callback<Response> callback);

    @FormUrlEncoded
    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @POST("/users/merge")
    void userMerge(@Field("merge") String guestUri, @Field("target") String userUri, Callback<Response> callback);


    @Headers({
            "Accept: application/json",
            "Accept-Serialize: mobile-detail",
            "X-Quiet: 1"
    })
    @POST("/users/{id}")
    void userUpdateEmail(@Path("id") int userId, @Body UserUpdateEmailBody userUpdateEmailBody, Callback<Response> callback);

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("/security/token")
    AccessToken getAccessToken(@Field("grant_type") String grantType,
                               @Field("client_id") String clientId,
                               @Field("client_secret") String clientSecret,
                               @Field("email") String email,
                               @Field("password") String password);

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("/security/token")
    void getAccessToken(@Field("grant_type") String grantType,
                        @Field("client_id") String clientId,
                        @Field("client_secret") String clientSecret,
                        @Field("email") String email,
                        @Field("password") String password,
                        Callback<AccessToken> callback);

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("/security/token")
    AccessToken refreshAccessToken(@Field("grant_type") String grantType,
                                   @Field("client_id") String clientId,
                                   @Field("client_secret") String clientSecret,
                                   @Field("refresh_token") String refreshToken);

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("/security/token")
    void refreshAccessToken(@Field("grant_type") String grantType,
                            @Field("client_id") String clientId,
                            @Field("client_secret") String clientSecret,
                            @Field("refresh_token") String refreshToken,
                            Callback<AccessToken> callback);
}