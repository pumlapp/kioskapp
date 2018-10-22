package com.zippy.zippykiosk.rest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by KB on 23/03/15.
 * Copyright 2015 Zippy.com.au.
 */

class JsonContainerSerializer<T> implements JsonSerializer<T> {
    private final String mKey;

    public JsonContainerSerializer(String key) {
        mKey = key;
    }


    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add(mKey, new Gson().toJsonTree(src,typeOfSrc));
        return jsonObject;
    }
}