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
package com.thoughtworks.go.api.representers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;

public class JsonReader {

    private final JsonObject jsonObject;

    public JsonReader(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public String getString(String property) {
        return optString(property)
                .orElseThrow(() -> haltBecauseMissingJsonProperty(property, jsonObject));
    }

    public boolean getBoolean(String property) {
        return optBoolean(property)
                .orElseThrow(() -> haltBecauseMissingJsonProperty(property, jsonObject));
    }

    public Integer getInt(String property) {
        return optInt(property)
                .orElseThrow(() -> haltBecauseMissingJsonProperty(property, jsonObject));
    }

    public boolean getBooleanOrDefault(String property, boolean defaultValue) {
        return optBoolean(property).orElse(defaultValue);
    }

    public String getStringOrDefault(String property, String defaultValue) {
        return optString(property).orElse(defaultValue);
    }

    public Optional<Long> optLong(String property) {
        if (hasJsonObject(property)) {
            try {
                return Optional.ofNullable(jsonObject.get(property).getAsLong());
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonString(property, jsonObject);
            }
        }
        return Optional.empty();
    }

    public Optional<Double> optDouble(String property) {
        if (hasJsonObject(property)) {
            try {
                return Optional.ofNullable(jsonObject.get(property).getAsDouble());
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonString(property, jsonObject);
            }
        }
        return Optional.empty();
    }

    public Optional<String> optString(String property) {
        if (hasJsonObject(property)) {
            try {
                return Optional.ofNullable(jsonObject.get(property).getAsString());
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonString(property, jsonObject);
            }
        }
        return Optional.empty();
    }

    public Optional<CaseInsensitiveString> optCaseInsensitiveString(String property) {
        if (hasJsonObject(property)) {
            try {
                return Optional.of(new CaseInsensitiveString(jsonObject.get(property).getAsString()));
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonString(property, jsonObject);
            }
        }
        return Optional.empty();
    }

    public Optional<JsonArray> optJsonArray(String property) {
        if (hasJsonObject(property)) {
            try {
                return Optional.ofNullable(jsonObject.getAsJsonArray(property));
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonArray(property, jsonObject);
            }
        }
        return Optional.empty();
    }

    public Optional<Boolean> optBoolean(String property) {
        if (jsonObject.has(property)) {
            try {
                return Optional.of(jsonObject.getAsJsonPrimitive(property).getAsBoolean());
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonBoolean(property, jsonObject);
            }
        }
        return Optional.empty();
    }

    public Optional<Integer> optInt(String property) {
        if (jsonObject.has(property)) {
            try {
                return Optional.of(jsonObject.getAsJsonPrimitive(property).getAsInt());
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonInt(property, jsonObject);
            }
        }
        return Optional.empty();
    }

    public Optional<JsonReader> optJsonObject(String property) {
        if (hasJsonObject(property)) {
            try {
                return Optional.of(new JsonReader(jsonObject.getAsJsonObject(property)));
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonObject(property, jsonObject);
            }
        }
        return Optional.empty();
    }

    public JsonReader readJsonObject(String property) {
        return optJsonObject(property)
                .orElseThrow(() -> haltBecauseMissingJsonProperty(property, jsonObject));
    }

    public boolean hasJsonObject(String property) {
        return jsonObject.has(property) && !(jsonObject.get(property) instanceof JsonNull);
    }

    public JsonReader readStringIfPresent(String key, Consumer<String> setterMethod) {
        optString(key).ifPresent(setterMethod);
        return this;
    }

    public JsonReader readIntIfPresent(String key, Consumer<Integer> setterMethod) {
        optInt(key).ifPresent(setterMethod);
        return this;
    }

    public JsonReader readCaseInsensitiveStringIfPresent(String key, Consumer<CaseInsensitiveString> setterMethod) {
        optCaseInsensitiveString(key).ifPresent(setterMethod);
        return this;
    }

    public JsonReader readArrayIfPresent(String key, Consumer<JsonArray> setterMethod) {
        optJsonArray(key).ifPresent(setterMethod);
        return this;
    }

    public Optional<List<String>> readStringArrayIfPresent(String property) {
        if (jsonObject.has(property)) {
            try {
                Spliterator<JsonElement> iterator = jsonObject.getAsJsonArray(property).spliterator();
                return Optional.of(
                        StreamSupport.stream(iterator, false)
                                .map(JsonElement::getAsString)
                                .collect(Collectors.toList()));
            } catch (Exception e) {
                throw haltBecausePropertyIsNotAJsonStringArray(property, jsonObject);
            }
        }

        return Optional.empty();
    }

    public boolean hasJsonArray(String property) {
        return jsonObject.has(property) && (jsonObject.get(property) instanceof JsonArray);
    }

    public JsonReader readBooleanIfPresent(String key, Consumer<Boolean> consumer) {
        optBoolean(key).ifPresent(consumer);
        return this;
    }

    public JsonReader readDoubleIfPresent(String key, Consumer<Double> consumer) {
        optDouble(key).ifPresent(consumer);
        return this;
    }
}
