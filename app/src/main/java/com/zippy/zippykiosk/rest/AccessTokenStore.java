package com.zippy.zippykiosk.rest;

import android.support.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;

import com.zippy.zippykiosk.Log;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Created by KB on 27/02/15.
 * Copyright 2015 Zippy.com.au.
 */
public class AccessTokenStore {
    private static final String UTF8 = "utf-8";
    private static final char[] SEKRIT = {0, 20, 45, 21, 64, 76, 31, 212, 213, 21, 20, 0, 29};

    private static final String ACCESS_TOKEN_STORE = "access_token_store";
    private static final String KEY_ACCESS_TOKEN = "zippy_access_token";
    private static final String KEY_REFRESH_TOKEN = "zippy_refresh_token";
    private static final String KEY_TOKEN_SCOPE = "zippy_token_scope";
    private static final String KEY_EXPIRES_IN = "zippy_token_expires_at";
    private static final String KEY_RETRIEVED_AT = "zippy_token_retrieved_at";
    private static final String KEY_TOKEN_TYPE = "zippy_token_type";

    // Server wipes ability to getAccessToken refresh token each time new deployment
    // This is just during development, so store login details so we can always
    // getAccessToken another access for this user. Could be encrypted.
    //private static final String KEY_LOGIN_SERVER = "zippy_login_server"; //  deprecated.
    private static final String KEY_LOGIN_EMAIL = "zippy_login_email";
    private static final String KEY_LOGIN_PASSWORD = "zippy_login_password";

    private static final String KEY_DEVICE_ID = "device.id";

    private final Application mApplication;

    public AccessTokenStore(Application application) {
        mApplication = application;
    }

    @SuppressLint("CommitPrefEdits")
    public synchronized void save(AccessToken accessToken) {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        pref.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken.accessToken)
                .putString(KEY_REFRESH_TOKEN, accessToken.refreshToken)
                .putString(KEY_TOKEN_TYPE, accessToken.tokenType)
                .putString(KEY_TOKEN_SCOPE, accessToken.tokenScope)
                .putLong(KEY_EXPIRES_IN, accessToken.expiresIn * 1000)
                .putLong(KEY_RETRIEVED_AT, System.currentTimeMillis())
                .commit();
    }

    public void saveEmail(@Nullable String email) {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        pref.edit()
                .putString(KEY_LOGIN_EMAIL, encrypt(email))
                .apply();
    }

    public void savePassword(@Nullable String password) {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        pref.edit()
                .putString(KEY_LOGIN_PASSWORD, encrypt(password))
                .apply();
    }

    public void saveDeviceId(int deviceId) {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        pref.edit()
                .putInt(KEY_DEVICE_ID, deviceId)
                .apply();
    }

    public AccessToken getAccessToken() {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        AccessToken token = new AccessToken();
        token.accessToken = pref.getString(KEY_ACCESS_TOKEN, null);
        token.refreshToken = pref.getString(KEY_REFRESH_TOKEN, null);
        token.tokenType = pref.getString(KEY_TOKEN_TYPE, "Bearer");
        token.tokenScope = pref.getString(KEY_TOKEN_SCOPE, null);
        token.expiresIn = pref.getLong(KEY_EXPIRES_IN, 0);
        return token;
    }

    public
    @Nullable
    String getEmail() {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        String email = pref.getString(KEY_LOGIN_EMAIL, null);
        return (email != null ? decrypt(email) : null);
    }

    public
    @Nullable
    String getPassword() {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        String pw = pref.getString(KEY_LOGIN_PASSWORD, null);
        return (pw != null ? decrypt(pw) : null);
    }

    public int getDeviceId() {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);

        return (pref.getInt(KEY_DEVICE_ID, 0));
    }

    public boolean isExpired() {
        final int EXPIRED_TIME_MARGIN = 2 * 60 * 1000; // Call it expired if less than 2 minutes remaining.
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        long current = System.currentTimeMillis();
        long retrieved = pref.getLong(KEY_RETRIEVED_AT, 0);
        long duration = pref.getLong(KEY_EXPIRES_IN, 0);
        return current > (retrieved + duration - EXPIRED_TIME_MARGIN);
    }


    @SuppressLint("CommitPrefEdits")
    public synchronized void clear() {
        SharedPreferences pref = mApplication.getSharedPreferences(ACCESS_TOKEN_STORE, Context.MODE_PRIVATE);
        pref.edit()
                .clear()
                .commit();
    }

    /**
     * Warning, this gives a false sense of security.  If an attacker has enough access to
     * acquire your password store, then he almost certainly has enough access to acquire your
     * source binary and figure out your encryption key.  However, it will prevent casual
     * investigators from acquiring passwords, and thereby may prevent undesired negative
     * publicity.
     * Adapted from:
     * http://stackoverflow.com/questions/785973/what-is-the-most-appropriate-way-to-store-user-settings-in-android-application/6393502#6393502
     */
    private
    @Nullable
    String encrypt(@Nullable String value) {
        try {
            final byte[] bytes = value != null ? value.getBytes(UTF8) : new byte[0];
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.ENCRYPT_MODE,
                    key,
                    new PBEParameterSpec(Settings.Secure.getString(mApplication.getContentResolver(), Settings.Secure.ANDROID_ID).getBytes(UTF8), 20));
            return new String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP), UTF8);

        } catch (Exception e) {
            Log.e("encrypt exception", e);
            return null;
        }
    }


    private
    @Nullable
    String decrypt(@Nullable String value) {
        try {
            final byte[] bytes = value != null ? Base64.decode(value, Base64.DEFAULT) : new byte[0];
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.DECRYPT_MODE,
                    key,
                    new PBEParameterSpec(Settings.Secure.getString(mApplication.getContentResolver(), Settings.Secure.ANDROID_ID).getBytes(UTF8), 20));
            return new String(pbeCipher.doFinal(bytes), UTF8);

        } catch (Exception e) {
            Log.e("decrypt exception", e);
            return null;
        }
    }
}
