/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.util.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class JsonHelper {
    private static final Gson GSON_DEFAULT = new Gson();
    private static final Gson GSON_EXPOSE_ONLY = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static final Gson GSON_W_NULLS = new GsonBuilder().serializeNulls().create();

    public static JsonElement toJsonTree(Map<?, ?> map) {
        return GSON_DEFAULT.toJsonTree(map);
    }

    public static String toJson(Object object) {
        return GSON_DEFAULT.toJson(object);
    }

    public static String toJsonExposeOnly(Object object) {
        return GSON_EXPOSE_ONLY.toJson(object);
    }

    public static String toJsonWithNulls(Object object) {
        return GSON_W_NULLS.toJson(object);
    }

    public static <T> T fromJson(String responseBody, Class<T> clazzOfT) {
        return GSON_DEFAULT.fromJson(responseBody, clazzOfT);
    }

    public static <T> T fromJson(String responseBody, Type typeofT) {
        return GSON_DEFAULT.fromJson(responseBody, typeofT);
    }

    public static <T> T fromJson(String responseBody, TypeToken<T> typeOfT) {
        return GSON_DEFAULT.fromJson(responseBody, typeOfT);
    }

    public static <T> T fromJsonExposeOnly(final String jsonString, Class<T> clazz) {
        return GSON_EXPOSE_ONLY.fromJson(jsonString, clazz);
    }

    public static <T> T fromJsonExposeOnly(final String jsonString, Type type) {
        return GSON_EXPOSE_ONLY.fromJson(jsonString, type);
    }

    public static <T> T fromJsonExposeOnly(String responseBody, TypeToken<T> typeOfT) {
        return GSON_EXPOSE_ONLY.fromJson(responseBody, typeOfT);
    }

    public static <T> T safeFromJsonExposeOnly(final String jsonString, Type type) {
        try {
            return fromJsonExposeOnly(jsonString, type);
        } catch (Exception e) {
            return null;
        }
    }
}
