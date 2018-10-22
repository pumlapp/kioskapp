package com.zippy.zippykiosk.rest;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by KB on 23/03/15.
 * Copyright 2015 Zippy.com.au.
 */
class JsonContainerDeserializer<T> implements JsonDeserializer<T> {
    private final Class<T> mClass;
    private final String mKey;

    public JsonContainerDeserializer(Class<T> targetClass, String key) {
        mClass = targetClass;
        mKey = key;
    }

    @Override
    public T deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        JsonElement content = je.getAsJsonObject().get(mKey);
        return new Gson().fromJson(content, mClass);
    }
}
