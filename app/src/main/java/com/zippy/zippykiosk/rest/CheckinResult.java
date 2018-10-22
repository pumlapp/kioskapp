package com.zippy.zippykiosk.rest;



import org.parceler.Parcel;

import java.util.Date;

/**
 * Created by kimb on 4/10/2015.
 */
@Parcel
public class CheckinResult {
    public String qrCode;
    public int userId;
    public int userStatus;
    public String userUri;
    public String userEmail;
    public int pointsEarned;
    public int pointsBalance;
    public String denialMessage; // eg. Checkin too soon after last checkin
    public int checkinsBusinessTotal;
    public int checkinsUserTotal;
    public boolean isClaimable;
    public boolean isGuest;
    public boolean isVerified;
    public Reward rewards[];
    public Reward lastClaimedReward;
    public Date lastClaimedRewardDate;

}
