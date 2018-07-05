/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.domain.user;

import com.google.gson.*;
import com.thoughtworks.go.config.CaseInsensitiveString;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Marshaling {

    public static class FiltersSerializer implements JsonSerializer<Filters> {
        @Override
        public JsonElement serialize(Filters src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            final JsonArray viewFilters = new JsonArray();

            for (DashboardFilter f : src.filters()) {
                viewFilters.add(context.serialize(f, DashboardFilter.class));
            }
            result.add("filters", viewFilters);
            return result;
        }
    }

    public static class FiltersDeserializer implements JsonDeserializer<Filters> {
        @Override
        public Filters deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final JsonObject j = json.getAsJsonObject();
            final JsonElement f = j.get("filters");

            if (null == f) {
                throw new JsonParseException("Missing filters array!");
            }

            final ArrayList<DashboardFilter> viewFilters = new ArrayList<>();

            f.getAsJsonArray().forEach((filt) -> {
                viewFilters.add(context.deserialize(filt, DashboardFilter.class));
            });

            return new Filters(viewFilters);
        }
    }

    public static class DashboardFilterSerializer implements JsonSerializer<DashboardFilter> {
        @Override
        public JsonElement serialize(DashboardFilter src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonElement serialized;

            if (src instanceof WhitelistFilter) {
                serialized = context.serialize(src, WhitelistFilter.class);
                serialized.getAsJsonObject().addProperty("type", "whitelist");
            } else if (src instanceof BlacklistFilter) {
                serialized = context.serialize(src, BlacklistFilter.class);
                serialized.getAsJsonObject().addProperty("type", "blacklist");
            } else {
                throw new IllegalArgumentException("Don't know how to handle DashboardFilter implementation: " + src.getClass().getCanonicalName());
            }

            return serialized;
        }
    }

    public static class DashboardFilterDeserializer implements JsonDeserializer<DashboardFilter> {
        @Override
        public DashboardFilter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonElement type = jsonObject.get("type");

            if (type != null) {
                switch (type.getAsString()) {
                    case "whitelist":
                        return context.deserialize(jsonObject,
                                WhitelistFilter.class);
                    case "blacklist":
                        return context.deserialize(jsonObject,
                                BlacklistFilter.class);
                    default:
                        throw new JsonParseException("Don't know how to deserialize filter type:" + type.getAsString());
                }
            }

            throw new JsonParseException("Missing filter type!");
        }
    }

    public static class CaseInsensitiveStringSerializer implements JsonSerializer<CaseInsensitiveString> {
        @Override
        public JsonElement serialize(CaseInsensitiveString src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    public static class CaseInsensitiveStringDeserializer implements JsonDeserializer<CaseInsensitiveString> {
        @Override
        public CaseInsensitiveString deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new CaseInsensitiveString(json.getAsString());
        }
    }
}
