package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by kb on 4/10/2015.
 */

public class CheckinReport {

    @SerializedName("user") public CRUser user;

    // Server returns business details for every checkin, just in case rewards or other details they have changed.
    // This saves us having to poll the server or to be notified when they change
    @SerializedName("business") public CRBusiness business;

    @SerializedName("report") public CRReport report;

    static public class CRUser {
        @SerializedName("id") public int id;
        @SerializedName("uri") public String uri;
        @SerializedName("email") public String email;
        @SerializedName("first_name") public String firstName;
        @SerializedName("last_name") public String lastName;
        //@SerializedName("gender") public String gender;
        //@SerializedName("profile_picture") public String profilePictureUrl;
        @SerializedName("status") public int status;
    }
    static public class CRBusiness {
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
    }

    static public class CRReport {
        @SerializedName("checkin") public CheckinCheckin checkin;
        @SerializedName("point") public CheckinPoint point;
        @SerializedName("reward") public CheckinReward reward;
        @SerializedName("user") public CheckinUser user;

    }
    static public class CheckinCheckin {
        @SerializedName("remain") public int remain;
        @SerializedName("businessTotal") public int businessTotal;
        @SerializedName("userTotal") public int userTotal;
    }

    static public class CheckinPoint {
        @SerializedName("balance") public int balance;
    }
    static public class CheckinReward {
        @SerializedName("claimable") public boolean claimable;
        @SerializedName("claimableMessage") public String claimableMessage;
        @SerializedName("lastClaimed") public CheckinLastClaimed lastClaimed;
    }
    static public class CheckinUser {
        @SerializedName("guest") public boolean guest;
        @SerializedName("verified") public boolean verified;
    }

    static public class CheckinLastClaimed {
        @SerializedName("reward") public Reward reward;
        @SerializedName("created") public Date created;
    }

}