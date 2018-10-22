package com.zippy.zippykiosk.rest;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

/**
 * Created by KB on 8/05/15.
 * Copyright 2015 Zippy.com.au.
 */
public class QRCode {
    @SerializedName("slug") public String slug;
    @SerializedName("status") public int status;

    // Compares two QRCode objects and returns true if the slug and status are equal
    boolean deepEquals(QRCode code) {
        if(code == null) return false;
        if(!TextUtils.equals(slug, code.slug)) return false;
        return(status == code.status);
    }
}
