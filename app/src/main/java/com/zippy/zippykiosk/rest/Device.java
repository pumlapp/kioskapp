package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;

/**
 * Created by KB on 22/03/15.
 * Copyright 2015 Zippy.com.au.
 */
public class Device {
    @SerializedName("brand") public String brand;
    @SerializedName("manufacturer") public String manufacturer;
    @SerializedName("model") public String model;
    @SerializedName("serial") public String serial;
    @SerializedName("mac") public String mac;
    @SerializedName("imei") public String imei;
    @SerializedName("mobile_number") public String mobile_number;
    @SerializedName("business") public String businessUri; // eg /business/2 or </business/2>
}
