package com.zippy.zippykiosk.rest;

import android.support.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

public class UserUpdatePromoteBody {
    @SerializedName("status") public int status;
    @SerializedName("origin") public String origin;
    @SerializedName("email") public String email;
    @SerializedName("first_name") public String firstName;
    @SerializedName("last_name") public String lastName;
    @SerializedName("postcode") public String postcode;

    public UserUpdatePromoteBody(int status, @NonNull String email, @NonNull String firstName, @NonNull String lastName, @NonNull String postcode) {
        this.status = status;
        this.origin = "Promoted";
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.postcode = postcode;
    }
}
