package com.lx862.summitbot.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtil {
    public static JsonArray getArray(String dotSeparatedPath, JsonObject jsonObject) {
        JsonElement result = getJson(dotSeparatedPath, jsonObject);
        return result == null || !result.isJsonArray() ? new JsonArray() : result.getAsJsonArray();
    }

    public static String getString(String dotSeparatedPath, JsonObject jsonObject, String defaultValue) {
       String result = getString(dotSeparatedPath, jsonObject);
       return result == null ? defaultValue : result;
    }

    public static String getString(String dotSeparatedPath, JsonObject jsonObject) {
        JsonElement result = getJson(dotSeparatedPath, jsonObject);
        return result == null || result.isJsonNull() ? null : result.getAsString();
    }

    public static void expectField(String dotSeparatedPath, JsonObject jsonObject) {
        JsonElement result = getJson(dotSeparatedPath, jsonObject);
        if(result == null) throw new IllegalArgumentException("Missing required field " + dotSeparatedPath + "!");
    }

    public static long getLong(String dotSeparatedPath, JsonObject jsonObject) {
        JsonElement result = getJson(dotSeparatedPath, jsonObject);
        if(result == null) throw new IllegalArgumentException("Cannot find property " + dotSeparatedPath);
        return result.getAsLong();
    }

    public static double getDouble(String dotSeparatedPath, JsonObject jsonObject) {
        JsonElement result = getJson(dotSeparatedPath, jsonObject);
        if(result == null) throw new IllegalArgumentException("Cannot find property " + dotSeparatedPath);
        return result.getAsDouble();
    }

    public static JsonElement getJson(String dotSeparatedPath, JsonObject jsonObject) {
        JsonElement elem = jsonObject;
        for(String path : dotSeparatedPath.split("\\.")) {
            JsonObject object = elem.getAsJsonObject();
            if(!object.has(path)) return null;
            elem = object.get(path);
        }
        return elem;
    }

    public static String toGB(long b, int pow, String unit) {
        return String.format("%.1f%s", (b / Math.pow(1024, pow)), unit);
    }
}
