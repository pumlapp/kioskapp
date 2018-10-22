package com.zippy.zippykiosk.rest;

/**
 * Created by KB on 30/01/15.
 * Copyright 2015 Zippy.com.au.
 */
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.zippy.zippykiosk.KioskApp;
import com.zippy.zippykiosk.Log;
import com.zippy.zippykiosk.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit.RetrofitError;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

final public class RetrofitUtils {
    private static final int BUFFER_SIZE = 0x1000;
    private static final Pattern CHARSET = Pattern.compile("\\Wcharset=([^\\s;]+)", CASE_INSENSITIVE);

    /**
     * Creates a {@code byte[]} from reading the entirety of an {@link java.io.InputStream}. May return an
     * empty array but never {@code null}.
     * <p>
     * Copied from Guava's {@code ByteStreams} class.
     */
    private static byte[] streamToBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (stream != null) {
            byte[] buf = new byte[BUFFER_SIZE];
            int r;
            while ((r = stream.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
        }
        return baos.toByteArray();
    }

    /**
     * Conditionally replace a {@link Request} with an identical copy whose body is backed by a
     * byte[] rather than an input stream.
     */
    static Request readBodyToBytesIfNecessary(Request request) throws IOException {
        TypedOutput body = request.getBody();
        if (body == null || body instanceof TypedByteArray) {
            return request;
        }

        String bodyMime = body.mimeType();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        body.writeTo(baos);
        body = new TypedByteArray(bodyMime, baos.toByteArray());

        return new Request(request.getMethod(), request.getUrl(), request.getHeaders(), body);
    }

    /**
     * Conditionally replace a {@link Response} with an identical copy whose body is backed by a
     * byte[] rather than an input stream.
     */
    private static Response readBodyToBytesIfNecessary(Response response) throws IOException {
        TypedInput body = response.getBody();
        if (body == null || body instanceof TypedByteArray) {
            return response;
        }

        String bodyMime = body.mimeType();
        InputStream is = body.in();
        try {
            byte[] bodyBytes = RetrofitUtils.streamToBytes(is);
            body = new TypedByteArray(bodyMime, bodyBytes);

            return replaceResponseBody(response, body);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static Response replaceResponseBody(Response response, TypedInput body) {
        return new Response(response.getUrl(), response.getStatus(), response.getReason(),
                response.getHeaders(), body);
    }

    static <T> void validateServiceClass(Class<T> service) {
        if (!service.isInterface()) {
            throw new IllegalArgumentException("Only interface endpoint definitions are supported.");
        }
        // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
        // Android (http://b.android.com/58753) but it forces composition of API declarations which is
        // the recommended pattern.
        if (service.getInterfaces().length > 0) {
            throw new IllegalArgumentException("Interface definitions must not extend other interfaces.");
        }
    }

    public static String getBodyString(Response response) throws IOException {
        if (response == null) {
            return null;
        }
        TypedInput body = response.getBody();
        if (body != null) {

            if ( !(body instanceof TypedByteArray)) {
                response = readBodyToBytesIfNecessary(response);
                body = response.getBody();
            }

            byte[] bodyBytes = ((TypedByteArray) body).getBytes();
            String bodyMime = body.mimeType();
            String bodyCharset = parseCharset(bodyMime);

            return new String(bodyBytes, bodyCharset);
        }

        return null;
    }
    public static String getResponseBodyString(RetrofitError error) {
        String bodyString=null;
        try {
            bodyString = RetrofitUtils.getBodyString(error.getResponse());

        } catch (Exception e) {
            Log.e(e);
        }
        return bodyString;
    }

    /** Parse the MIME type from a {@code Content-Type} header value. */
    private static String parseCharset(String mimeType) {
        Matcher match = CHARSET.matcher(mimeType);
        if (match.find()) {
            return match.group(1).replaceAll("[\"\\\\]", "");
        }
        return "UTF-8";
    }

    private RetrofitUtils() {
        // No instances.
    }

    public static String getErrorDescription(String title, RetrofitError error) {
        StringBuilder sb = new StringBuilder(title);

        if(error!=null) {
            if(error.getResponse()!=null) {
                sb.append(" _status:").append(error.getResponse().getStatus());
            }
            if(error.getMessage()!=null){
                sb.append(" _msg:").append(error.getMessage());
            }
            if(error.getKind()!=null) {
                sb.append(" _kind:").append(error.getKind().toString());
            }
            if(error.getCause()!=null && error.getCause().getMessage()!=null) {
                sb.append(" _cause:").append(error.getCause().getMessage());
            }
            if(error.getResponse()!=null) {
                sb.append(" _reason:").append(error.getResponse().getReason());
                String respBodyString = getResponseBodyString(error);
                if(!TextUtils.isEmpty(respBodyString)) {
                    if(respBodyString.length()>800) {
                        respBodyString = respBodyString.substring(0,800);
                    }
                    sb.append(" _resp_body:").append(respBodyString);
                }
            }
        }
        return sb.toString();
    }

    @NonNull
    public static String getUserErrorDescription( RetrofitError error) {
        String errorDescription=null;

        if(error!=null) {
            if(error.getKind()!=null && error.getKind() == RetrofitError.Kind.NETWORK) {
                if(error.getCause() instanceof SocketTimeoutException) {
                    errorDescription = "Connection timeout. Please try again.";
                }else if(!Utils.isNetworkConnected(KioskApp.getAppContext())) {
                    errorDescription = "No network connection. Please try again.";
                }else if(error.getCause() instanceof ConnectException) {
                    errorDescription = "Could not connect to Zippy. Please try again."; // Server offline?
                }else if(error.getCause() instanceof UnknownHostException){
                    errorDescription = "Network error. Please try again.";
                }else {
                    errorDescription = "Network error. Please try again.";
                }
            }else {
                //errorDescription = error.getLocalizedMessage();
                errorDescription = "Please try again.";
            }
        }
        if(TextUtils.isEmpty(errorDescription)) {
            errorDescription = "Please try again.";
        }
        return errorDescription;
    }
}