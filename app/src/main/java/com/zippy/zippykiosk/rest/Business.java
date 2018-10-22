package com.zippy.zippykiosk.rest;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Created by kimb on 29/04/2015.
 */
public class Business {
    private static final String PREFS_NAME = "Business";
    private static final String BUSINESS_KEY = "Business";

    @SerializedName("id") public int id;
    @SerializedName("uri") public String uri;
    @SerializedName("name") public String name;
    @SerializedName("url") public String url;
    @SerializedName("logo") public String logo;
    @SerializedName("address_line1") public String address;
    @SerializedName("suburb") public String suburb;
    @SerializedName("state") public String state;
    @SerializedName("postcode") public String postcode;
    @SerializedName("status") public int status;
    @SerializedName("qr_code") public QRCode qrCode[];
    @SerializedName("location") public Location location;
    @SerializedName("rewards") public Reward rewards[];
    @SerializedName("config") public BusinessConfig config;




    /**
     *  Saves Business details to the shared preferences.
     *
     *  @param context The context of the preferences whose values are wanted
     *  @param business Business object. If null, settings are removed.
     */
    public static void save(Context context, @Nullable final Business business) {
        SharedPreferences settings;
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if(business!=null) {
            Gson gson = new Gson();
            String jsonBusiness = gson.toJson(business);
            settings.edit()
                    .putString(BUSINESS_KEY, jsonBusiness)
                    .apply();
        }else {
            settings.edit()
                    .remove(BUSINESS_KEY)
                    .apply();
        }
    }

    /**
    *   Loads Business details from the shared preferences.
    *
    *   @return The business object. null if no business details
    */
    @Nullable
    public static Business load(Context context) {
        Business business = null;
        SharedPreferences settings;
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (settings.contains(BUSINESS_KEY)) {
            String jsonBusiness = settings.getString(BUSINESS_KEY, null);
            Gson gson = new Gson();
            business = gson.fromJson(jsonBusiness, Business.class);
        }
        return business;
    }

    /**
     *   Loads Business details from the shared preferences.
     *
     *   @return true if device is registered to a business. null if no business details
     */
    public static boolean isRegistered(Context context) {
        SharedPreferences settings;
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return(settings!=null && settings.contains(BUSINESS_KEY));

    }

    public static boolean logosEqual(Business b1, Business b2) {
        if(b1==b2) return true;
        if(b1==null || b2==null) return false;
        return(b1.logo.equalsIgnoreCase(b2.logo));
    }

    public static boolean rewardsEqual(@Nullable final Business business1,@Nullable final Business business2) {
        // TODO: Fix comparison of rewards
        if(business1==null && business2==null) return true;
        if(business1==null || business2==null) return false;

        return(rewardsEqual(business1.rewards, business2.rewards));
    }
    public static boolean rewardsEqual(@Nullable final Reward[] rewards1,@Nullable final Reward[] rewards2) {
        if(rewards1==null && rewards2==null) return true;
        if(rewards1==null || rewards2==null) return false;
        if(rewards1.length != rewards2.length) return false;
        for(int i=0;i<rewards1.length;i++) {
            if(rewards1[i].id != rewards2[i].id) return false;
            if(rewards1[i].points != rewards2[i].points) return false;
            if(rewards1[i].status != rewards2[i].status) return false;
            if(!rewards1[i].name.equals(rewards2[i].name)) return false;
        }
        return true;
    }

    /**
     * Returns true if the QRCode of two Business objects are equivalent
     * This method only compares the first QRCode in the qrCode[] array
     */
    public static boolean qrCodesEqual(@Nullable Business b1, @Nullable Business b2) {
        if(b1 == b2) return true;
        if(b1 == null || b2 == null) return false;
        if(b1.qrCode == b2.qrCode) return true;
        if (b1.qrCode == null || b2.qrCode == null) return false;
        if(b1.qrCode.length == 0 && b2.qrCode.length == 0) return true;
        if(b1.qrCode.length == 0 || b2.qrCode.length == 0) return false;
        return(b1.qrCode[0].deepEquals(b2.qrCode[0]));
    }

    public static boolean displayQRCodesEqual(@Nullable Business b1, @Nullable Business b2) {
        if(b1==b2) return true;
        if(b1==null || b2==null) return false;
        if(b1.config == b2.config) return true;
        if (b1.config == null || b2.config == null) return false;
        return(b1.config.displayQRCode == b2.config.displayQRCode);
    }
}
