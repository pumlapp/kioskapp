package com.zippy.zippykiosk.rest;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by kimb on 12/07/2015.
 */
public class CheckinBody {
    @SerializedName("business") public String businessUri;
    @SerializedName("user") public String userUri;
    @SerializedName("amount") public String amount;

    public CheckinBody(@NonNull String businessUri, @NonNull String qrCodeSlug, @Nullable String amount) {
        this.businessUri = ZippyApiUtils.formatBusinessUriFromUri(businessUri);
        this.userUri = ZippyApiUtils.formatUserUriFromQRCodeSlug(qrCodeSlug);
        this.amount = amount;
    }
}
