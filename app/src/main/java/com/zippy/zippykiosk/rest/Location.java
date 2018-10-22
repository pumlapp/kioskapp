package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;

/**
 * Created by kimb on 29/04/2015.
 */
public class Location {
    @SerializedName("latitude") public double latitude;
    @SerializedName("longitude") public double longitude;
}
