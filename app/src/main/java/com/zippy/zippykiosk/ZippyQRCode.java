package com.zippy.zippykiosk;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

// Created by kb on 17/09/2015.

// QR Code usually in the form http://qr.zippy.com.au/LJ5R11A95500/user
// QR Code in future zip.ac/LJ5R11A95500
// QR code from earlier iOS app No longer supported. form http://zippy.com.au/users/E09254FAF68A4D2F8A5AADFECD72432EDCEB882CBBFF852E173BE432FD612FFE01

public class ZippyQRCode {
    public String slug;
    public int type;
    public static final int INVALID_CODE = 0;
    public static final int ZIPPY_CODE = 1;
    public static final int REWARDLE_CODE = 2;
    public static final int BELLYCARD_CODE = 3;

    @Nullable
    static public ZippyQRCode createFromZippyQRCodeUrl(@Nullable final String qrCodeUrl) {
        if(qrCodeUrl!=null) {
            ZippyQRCode zippyQRCode = new ZippyQRCode();
            if(zippyQRCode.validate(qrCodeUrl)) {
                return zippyQRCode;
            }
        }
        return null;
    }

    private boolean validate(final @NonNull String code) {
        slug = parseZippyQRCodeUrl(code);
        if (slug == null) {
            slug = code;
        }
        if (validateZippyCode(slug)) {
            Log.i("Zippy Code " + slug);
            type = ZIPPY_CODE;
            return true;
        } else if (validateRewardleCode(slug)) {
            Log.i("Rewardle Code " + slug);
            type = REWARDLE_CODE;
            return true;
        } else if (validateBellyTyped(slug) || validateBellyScanned(slug)) {
            Log.i("BellyCard Code " + slug);
            type = BELLYCARD_CODE;
            return true;
        } else {
            Log.i("Invalid Code " + slug);
            type = INVALID_CODE;
            return false;
        }
    }

    @Nullable
    static public String parseZippyQRCodeUrl(@NonNull String zippyQRCodeUrl) {
        if (zippyQRCodeUrl.toLowerCase().contains("qr.zippy.com.au/")) {
            Pattern pattern = Pattern.compile("(qr.zippy.com.au\\/)([A-Z0-9]{6,})(\\/user)$", CASE_INSENSITIVE);
            Matcher m = pattern.matcher(zippyQRCodeUrl);
            if (m.find()) {
                return m.group(2);
            }
        }
        if (zippyQRCodeUrl.toLowerCase().contains("zip.ac/")) {
            Pattern pattern = Pattern.compile("(zip.ac\\/)([A-Z0-9]{6,})$", CASE_INSENSITIVE);
            Matcher m = pattern.matcher(zippyQRCodeUrl);
            if (m.find()) {
                return m.group(2);
            }
        }
        return null;
    }

    static public boolean validateZippyCode(String slug) {
        final int SLUG_VERSION_LENGTH = 2;
        String secret;
        int checksumStart;
        int checksumEnd;

        if(slug.length()<6) {
            return false;
        }
        String encodedVersion = slug.substring(slug.length()-SLUG_VERSION_LENGTH);
        switch(encodedVersion) {
            case "00":
                secret = "Ahb6miDMIR919U4fRjaBqh1MHX34VhfHwz8yQX2hqRRCx0NbtT7zah1r9Qx49yK";
                checksumStart = 0;
                checksumEnd = 6;
                break;
            case "01":
                secret = "0PGHisfkIH4TV2AsDfWUeFiLpItavb4cIEydGVM7WOu8KTW2jTFwpRL05k34kng";
                checksumStart = 0;
                checksumEnd = 8;
                break;
            case "02":
                secret = "obJN8ZIX3t25FZ2gmrMDyem8H8iOJ5S0YNXYyv5dw0NQvLuIWCy9DshdIqrjJhw";
                checksumStart = 0;
                checksumEnd = 6;
                break;
            default:
                return false;
        }
        int checksumLength = checksumEnd - checksumStart;
        Pattern pattern = Pattern.compile("^([0-9A-Z]+?)([0-9A-Z]{" + checksumLength + "})([0-9A-Z]{" + SLUG_VERSION_LENGTH + "})$",CASE_INSENSITIVE);

        Matcher m = pattern.matcher(slug);
        if (m.find()) {
            try {
                String slugBase = m.group(1);
                String slugChecksum = m.group(2);
                String slugType = m.group(3);
                String checksum = ZippyQRCode.s256Hash(slugBase + secret);
                if (checksum != null && checksum.length() >= checksumLength) {
                    checksum = checksum.toUpperCase(Locale.US);
                    if (checksum.length() > checksumLength) {
                        String shortChecksum = checksum.substring(checksumStart, checksumEnd);
                        if (slugType.equals(encodedVersion) && shortChecksum.equals(slugChecksum)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(e);
            }
        }
        return false;
    }


    static public boolean validateRewardleCode(String slug) {
        Pattern pattern = Pattern.compile("^07([A-Z])20([0-9]{7})([A-Z])([A-Z]{4})$");
        Matcher m = pattern.matcher(slug);
        if (m.find()) {
            try {
                String char1 = m.group(1);
                //String numeric = m.group(2);
                String char2 = m.group(3);
                //String alpha = m.group(4);
                if(char1.equals(char2)) {
                    return true;
                }else {
                    Log.i("Split chars do not match, does not look like a rewardle card");
                }
            } catch (Exception e) {
                Log.e(e);
            }
        }
        return false;
    }

    static public boolean validateBellyTyped(String slug) {
        Pattern pattern = Pattern.compile("^([0-9]+?)-([a-z0-9]+)$");
        Matcher m = pattern.matcher(slug);
        if (m.find()) {
            try {
                String seed = m.group(1);
                String hashPartial = m.group(2);
                seed += "\n";
                String md5 = ZippyQRCode.md5Hash(seed);
                if(md5 != null) {
                    String matchPartialHash = md5.substring(25, 25+4);
                    if (hashPartial.equals(matchPartialHash)) {
                        return true;
                    } else {
                        Log.i("Hashed Seed does not match");
                    }
                }
            } catch (Exception e) {
                Log.e(e);
            }
        }
        return false;
    }

    static public boolean validateBellyScanned(String slug) {
        Pattern pattern = Pattern.compile("^([0-9]+?)\\|([a-z0-9]+)\\|.*$");

        Matcher m = pattern.matcher(slug);
        if (m.find()) {
            try {
                String seed = m.group(1);
                String hashPartial = m.group(2);
                seed += "\n";
                String md5 = ZippyQRCode.md5Hash(seed);
                if(md5!=null) {
                    if (hashPartial.equals(md5.substring(25, 25+4))) {
                        return true;
                    } else {
                        Log.i("Hashed Seed does not match");
                    }
                }
            } catch (Exception e) {
                Log.e(e);
            }
        }
        return false;
    }

    @Nullable
    static private String s256Hash(@NonNull String value) {

        try {
            // Create S256 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(value.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                hexString.append(String.format("%02x", aMessageDigest));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(e);
        }
        return null;
    }

    @Nullable
    static private String md5Hash(@NonNull String value) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(value.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                hexString.append(String.format("%02x", aMessageDigest));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(e);
        }
        return null;
    }
}
