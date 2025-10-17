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
package com.thoughtworks.go.api.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.util.json.JsonHelper;

import java.lang.reflect.Type;
import java.util.Map;

public class GsonTransformer {

    private GsonTransformer() {
    }

    public static GsonTransformer getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public JsonReader jsonReaderFrom(String string) {
        try {
            if (string == null || string.isBlank()) {
                string = "{}";
            }
            return new JsonReader(JsonHelper.fromJson(string, JsonElement.class).getAsJsonObject());
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }

    public JsonReader jsonReaderFrom(Map<?, ?> map) {
        try {
            return new JsonReader(JsonHelper.toJsonTree(map).getAsJsonObject());
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }

    public <T> T fromJson(String string, Class<T> classOfT) {
        return JsonHelper.fromJson(string, classOfT);
    }

    public <T> T fromJson(String string, Type classOfT) {
        return JsonHelper.fromJson(string, classOfT);
    }

    private static class SingletonHolder {
        private static final GsonTransformer INSTANCE = new GsonTransformer();
    }
}
