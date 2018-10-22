package com.zippy.zippykiosk.rest;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/**
 * Created by kimb on 9/07/2015.
 */
public class UserResetEmailBody {
    @SerializedName("user") public String userUri;
    @SerializedName("email") public String email;

    public UserResetEmailBody(@NonNull String uri, @NonNull String email) {
        this.email = email;
        if(uri.startsWith("<") && uri.endsWith(">")) {
            this.userUri = uri;
        }else {
            this.userUri = "<" + uri + ">"; // Server requires brackets <>
        }
    }
}
