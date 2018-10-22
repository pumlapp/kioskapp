package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;

/**
 * Created by KB on 7/05/15.
 * Copyright 2015 Zippy.com.au.
 */
public class BusinessConfig {
    @SerializedName("checkin_points") public int checkinPoints;
    @SerializedName("checkin_max") public int checkinMax;
    @SerializedName("checkin_max_interval") public String checkinMaxInterval;
    @SerializedName("checkin_interval") public String checkinInterval;
    @SerializedName("display_qrcode") public int displayQRCode;
    @SerializedName("value_conversion_rate") public String valueConversionRate;
    @SerializedName("access_pin") public String accessPin;
}
