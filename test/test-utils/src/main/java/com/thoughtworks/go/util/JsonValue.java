/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.util;

import static java.lang.String.format;
import java.io.StringReader;

import com.sdicons.json.model.JSONArray;
import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONValue;
import com.sdicons.json.parser.JSONParser;

public class JsonValue {
    private JSONValue jsonValue;

    public JsonValue(String json) {
        this(parseJsonValue(json));
    }

    public JsonValue(JSONValue json) {
        if (json == null) {
            throw new NullPointerException();
        }
        this.jsonValue = json;
    }

    public String getString(Object... keys) {
        String jsString = getValue(keys).render(false);
        return jsString.substring(1, jsString.length() - 1);
    }

    public JSONValue getValue(Object... keys) {
        return getObject(keys).jsonValue;
    }

    public JsonValue getObject(Object... keys) {
        JsonValue current = this;
        for (Object key : keys) {
            if (key instanceof String) {
                if (!(current.jsonValue instanceof JSONObject)) {
                    throw new IllegalArgumentException(format("Key '%s' does not refer to any attribute of %s",
                            key, current));
                }
                JSONValue value = ((JSONObject) current.jsonValue).get((String) key);
                if (value == null) {
                    throw new IllegalArgumentException(format("Key '%s' does not refer to any attribute of %s",
                            key, current));
                }
                current = new JsonValue(value);
            } else if (key instanceof Number) {
                int index = ((Number) key).intValue();
                try {
                    current = current.getItem(index);
                } catch (Exception e) {
                    String message = String.format("Can not find #%s element in %s", index, current);
                    throw new RuntimeException(message, e);
                }
            } else {
                throw new RuntimeException("Could not understand key " + key);
            }
            if (current == null) {
                throw new RuntimeException("No such key " + key + "\n Possible keys are : " + current);
            }
        }
        return current;
    }

    public JsonValue getItem(int index) {
        JSONValue item = ((JSONArray) jsonValue).get(index);
        return new JsonValue(item);
    }

    @Override
    public String toString() {
        return jsonValue.getClass().getSimpleName() + ": " + jsonValue.render(false);
    }

    public static JSONValue parseJsonValue(String json) {
        JSONParser parser = new JSONParser(new StringReader(json));
        try {
            return parser.nextValue();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing json: [" + json + "]", e);
        }
    }

}
