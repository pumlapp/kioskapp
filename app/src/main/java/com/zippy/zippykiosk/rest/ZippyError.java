package com.zippy.zippykiosk.rest;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.zippy.zippykiosk.Log;

import java.io.IOException;
import java.io.InputStreamReader;

import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by kimb on 18/10/2015.
 */
public class ZippyError {
    public static final String CONFLICT = "CONFLICT";
    public static final String UNPROCESSABLE_ENTITY_INVALID = "UNPROCESSABLE_ENTITY_INVALID";
    public static final String PRECONDITION_FAILED_TARGET_USER_DISABLED = "PRECONDITION_FAILED_TARGET_USER_DISABLED";
    public static final String BAD_REQUEST_MERGE_USER_INVALID = "BAD_REQUEST_MERGE_USER_INVALID";
    public static final String BAD_REQUEST_MERGE_MAY_NOT_BE_TARGET = "BAD_REQUEST_MERGE_MAY_NOT_BE_TARGET";
    public static final String PRECONDITION_FAILED_MUST_BE_GUEST = "PRECONDITION_FAILED_MUST_BE_GUEST";
    public static final String BAD_REQUEST_PARAMETER_EMAIL_NOT_BLANK = "BAD_REQUEST_PARAMETER_EMAIL_NOT_BLANK";
    public static final String BAD_REQUEST_PARAMETER_AMOUNT_INVALID = "BAD_REQUEST_PARAMETER_AMOUNT_INVALID";
    public static final String PRECONDITION_FAILED_INSUFFICIENT_POINTS = "PRECONDITION_FAILED_INSUFFICIENT_POINTS";
    public static final String PRECONDITION_FAILED_MUST_NOT_BE_GUEST = "PRECONDITION_FAILED_MUST_NOT_BE_GUEST";
    public static final String BAD_REQUEST_RESOURCE_NOT_FOUND = "BAD_REQUEST_RESOURCE_NOT_FOUND";
    public static final String BAD_REQUEST_RESOURCE_NOT_URI = "BAD_REQUEST_RESOURCE_NOT_URI";
    public int httpStatus;
    public ZippyErrorResponse resp;

    public static class ZippyErrorResponse {
        @SerializedName("code") public String code;
        @SerializedName("message")public String message;
        @SerializedName("errors") public ErrorDetail errors[];

        static class ErrorDetail {
            @SerializedName("code") public String code;
            @SerializedName("message") public String message;
            @SerializedName("path") public String path;
        }
    }

    @Nullable
    public static ZippyError createZippyError(@NonNull RetrofitError error) {
        Response resp = error.getResponse();
        if(resp==null) {
            return null;
        }
        ZippyError zippyError = new ZippyError();
        zippyError.httpStatus = resp.getStatus();
        TypedInput body = resp.getBody();
        if (body != null) {
            try {
                JsonReader reader = new JsonReader(new InputStreamReader(body.in()));
                JsonParser parser = new JsonParser();
                JsonObject jo = parser.parse(reader).getAsJsonObject();
                Gson gson = new Gson();
                zippyError.resp = gson.fromJson(jo, ZippyError.ZippyErrorResponse.class);
            } catch (IOException e) {
                Log.e(e);
            }
        }
        return zippyError;
    }
    public boolean isErrorCode(@NonNull String code) {
        return(resp!=null && code.equals(resp.code));
    }
}
