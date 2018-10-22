package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;
import com.zippy.zippykiosk.Log;

/**
 * Created by KB on 7/05/15.
 * Copyright 2015 Zippy.com.au.
 */
public class ClaimRewardResponse {
    @SerializedName("user") public ClaimUser user;
    @SerializedName("reward") public Reward reward;
    @SerializedName("point") public ClaimPoint point;


    static public class ClaimPoint {
        @SerializedName("amount") public int amount; // points spent claiming reward (Negative number)
    }

    static public class ClaimUser {
        @SerializedName("id") public int id;
        @SerializedName("uri") public String uri;
        @SerializedName("email") public String email;
        @SerializedName("first_name") public String firstName;
        @SerializedName("last_name") public String lastName;
        @SerializedName("status") public int status;
        @SerializedName("points") public UserBusinessPoints businessPoints[];
    }
    static public class UserBusinessPoints {
        @SerializedName("points") public String points;
        @SerializedName("business") public String businessUri;
    }

    // Get the points for a business
    public int getPointsForBusiness(int businessId) {
        if(user!=null && user.businessPoints!=null) {
            String uri = "</business/" + businessId + ">";
            for(int i=0;i<user.businessPoints.length;i++) {
                if(uri.equalsIgnoreCase(user.businessPoints[i].businessUri)) {
                    try {
                        return Integer.parseInt(user.businessPoints[i].points);
                    }catch(NumberFormatException e) {
                        Log.e(e);
                    }
                }
            }
        }
        return(0);
    }
    /*
    @SerializedName("info") public ClaimInfo info;

    static public class ClaimInfo {
        @SerializedName("point") public ClaimPoint point;
        @SerializedName("reward") public Reward reward;
        @SerializedName("user") public User user;

    }
    static public class ClaimPoint {
        @SerializedName("balance") public int balance;
        @SerializedName("delta") public int delta;
    }
    static public class Reward {
        @SerializedName("claimable") public boolean claimable;
    }
    static public class User {
        @SerializedName("guest") public boolean guest;
        @SerializedName("verified") public boolean verified;
        @SerializedName("location") public String uri;

    }
    */
}
