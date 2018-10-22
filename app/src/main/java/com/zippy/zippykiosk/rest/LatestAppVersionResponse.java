package com.zippy.zippykiosk.rest;

import com.google.gson.annotations.SerializedName;

/**
 * Created by kimb on 21/06/2015.
 */
public class LatestAppVersionResponse {
    @SerializedName("id") public int id;
    @SerializedName("app_id") public String appID;
    @SerializedName("version") public String version;
    @SerializedName("version_name") public String versionName;
    @SerializedName("binary_uri") public String binaryUri;
}
