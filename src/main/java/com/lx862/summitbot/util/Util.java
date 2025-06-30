package com.lx862.summitbot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Util {
    public static JsonElement getJson(String dotSeparatedPath, JsonObject jsonObject) {
        JsonElement obj = jsonObject;
        for(String path : dotSeparatedPath.split("\\.")) {
            obj = obj.getAsJsonObject().get(path);
        }
        return obj;
    }

    public static String toGB(long b, int pow, String unit) {
        return String.format("%.1f%s", (b / Math.pow(1024, pow)), unit);
    }
}
