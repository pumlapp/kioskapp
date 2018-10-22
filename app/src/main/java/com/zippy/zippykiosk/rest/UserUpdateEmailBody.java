package com.zippy.zippykiosk.rest;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/**
 * Created by kb on 8/10/2015.
 */

public class UserUpdateEmailBody {
    @SerializedName("email") public String email;

    public UserUpdateEmailBody(@NonNull String email) {
        this.email = email;
    }
}
