package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Created by kimb on 29/04/2015.
 */
@Parcel
public class Reward {
    @SerializedName("id") public int id;
    @SerializedName("name") public String name;
    @SerializedName("points") public int points;
    @SerializedName("status") public int status;
}
