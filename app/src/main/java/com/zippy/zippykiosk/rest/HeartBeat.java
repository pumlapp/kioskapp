package com.zippy.zippykiosk.rest;

/**
 * Created by KB on 23/03/15.
 * Copyright 2015 Zippy.com.au.
 */
public class HeartBeat {
    public int battery;
    public Android android;
    public App app;
    public GeoPoint location;
    public boolean charge_usb;
    public boolean charge_power;

    static public class Android {
        String version_release;
        String version_sdk;
    }
    static public class App {
        String version;
        String version_name;
    }
    static public class GeoPoint {
        String latitude;
        String longitude;
    }
}
