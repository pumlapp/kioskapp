package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;

/**
 * Created by KB on 27/02/15.
 * Copyright 2015 Zippy.com.au.
 */
public class AccessToken {
    @SerializedName("access_token") public String accessToken;
    @SerializedName("expires_in") public long expiresIn;
    @SerializedName("token_type") public String tokenType;
    @SerializedName("scope") public String tokenScope;
    @SerializedName("refresh_token") public String refreshToken;

    @Override
    public String toString() {
        return tokenType + " token(" + accessToken + ") will expires in " + expiresIn;
    }
}