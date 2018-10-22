package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;

/**
 * Created by KB on 7/05/15.
 * Copyright 2015 Zippy.com.au.
 */
public class CheckinResponse {

    @SerializedName("attributes") public CheckinAttributes attributes; // Not attributes can be null
    @SerializedName("amount") public String amount;
    @SerializedName("point") public CheckinPoint point;

    static public class CheckinAttributes {
        @SerializedName("denial") public String denial; // eg. Checkin too soon after last checkin
    }
    static public class CheckinPoint {
        @SerializedName("amount") public int pointsEarned; // This is the points earned for the checkin
        @SerializedName("description") public String description;
    }
    /*
    // Server returns rewards for every checkin, just in case they have changed.
    // This saves us having to poll the server or to be notified when they change
    @SerializedName("rewardsCollection") public Reward[] rewards;

    @SerializedName("info") public CheckinInfo info;
    static public class CheckinInfo {
        @SerializedName("point") public CheckinPoint point;
        @SerializedName("reward") public CheckinReward reward;
        @SerializedName("user") public CheckinUser user;
        @SerializedName("checkin") public CheckinCheckin checkin;
    }
    static public class CheckinPoint {
        @SerializedName("balance") public long balance;
        @SerializedName("delta") public long delta;
    }
    static public class CheckinReward {
        @SerializedName("claimable") public boolean claimable;
    }
    static public class CheckinUser {
        @SerializedName("guest") public boolean guest;
        @SerializedName("verified") public boolean verified;
        @SerializedName("location") public String uri;
    }
    static public class CheckinCheckin {
        @SerializedName("remain") public int remain;
        @SerializedName("total") public int total;
        @SerializedName("message") public String message;
    }
    */
}
