package com.zippy.zippykiosk.rest;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;


/**
 * Created by KB on 4/02/15.
 * Copyright 2015 Zippy.com.au.
 */
public class User {
    // User status bit values
    static final int STATUS_ENABLED     = 1;
    static final int STATUS_GUEST       = 2;
    static final int STATUS_LEGACY      = 4;
    static final int STATUS_PROMOTED    = 8;
    static final int STATUS_VERIFIED    = 16;
    static final int STATUS_SYSTEM      = 512;
    static final int STATUS_TESTONLY    = 1024;
    public static int setPromotedStatus(int userStatusFlags) {
        int status = userStatusFlags;
        status &= ~User.STATUS_GUEST;   // Remove Guest flag
        status |= User.STATUS_PROMOTED; // Set promoted flag
        return status;
    }

    @SerializedName("id") public int id;
    @SerializedName("email") public String email;
    @SerializedName("first_name") public String firstName;
    @SerializedName("last_name") public String lastName;
    @SerializedName("profile_picture") public String profilePictureUrl;
    @SerializedName("status") public int status;
    @SerializedName("business") public Business business[]; // Businesses linked to the user. These are businesses that the user owns or has edit rights

    
    static public @NonNull ArrayList<Business> getEnabledBusinesses(@Nullable User user) {
        ArrayList<Business> businessArrayList = new ArrayList<>();
        if(user!=null && user.business != null && user.business.length>0) {
            for(Business b: user.business) {
                if((b.status & 1) == 1) {
                    // Add business to array list if status is enabled
                    businessArrayList.add(b);
                }
            }
        }
        return businessArrayList;
    }
}
