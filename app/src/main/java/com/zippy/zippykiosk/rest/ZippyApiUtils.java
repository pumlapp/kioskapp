package com.zippy.zippykiosk.rest;

import android.support.annotation.NonNull;

/**
 * Created by KB on 8/12/2015.
 * Copyright 2015 Zippy.com.au.
 */
public class ZippyApiUtils {

    @NonNull
    public static String formatBusinessUriFromUri(@NonNull String businessUri) {
        if (businessUri.startsWith("<") && businessUri.endsWith(">")) {
            return businessUri;
        } else {
            return "<" + businessUri + ">"; // Server requires brackets <>
        }
    }

    @NonNull
    public static String formatUserUriFromQRCodeSlug(@NonNull String qrCodeSlug) {
        return "</qrcodes/" + qrCodeSlug + "/resolve/user>";
    }
}
