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
package com.thoughtworks.go.server.domain.user;

import com.google.gson.*;
import com.thoughtworks.go.config.CaseInsensitiveString;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static java.lang.String.format;

public class Marshaling {

    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_FILTERS = "filters";

    private static final String TYPE_INCLUDES = "whitelist";
    private static final String TYPE_EXCLUDES = "blacklist";

    public static class FiltersSerializer implements JsonSerializer<Filters> {
        @Override
        public JsonElement serialize(Filters src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            final JsonArray viewFilters = new JsonArray();

            for (DashboardFilter f : src.filters()) {
                viewFilters.add(context.serialize(f, DashboardFilter.class));
            }
            result.add(KEY_FILTERS, viewFilters);
            return result;
        }
    }

    public static class FiltersDeserializer implements JsonDeserializer<Filters> {
        @Override
        public Filters deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final JsonObject j = json.getAsJsonObject();
            final JsonElement filters = j.get(KEY_FILTERS);

            if (null == filters) {
                throw new JsonParseException("Missing filters array!");
            }

            final ArrayList<DashboardFilter> viewFilters = new ArrayList<>();

            filters.getAsJsonArray().forEach((f) -> viewFilters.add(context.deserialize(f, DashboardFilter.class)));

            return new Filters(viewFilters);
        }
    }

    public static class DashboardFilterSerializer implements JsonSerializer<DashboardFilter> {
        @Override
        public JsonElement serialize(DashboardFilter src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonElement serialized;

            if (src instanceof IncludesFilter) {
                serialized = context.serialize(src, IncludesFilter.class);
                serialized.getAsJsonObject().addProperty(KEY_TYPE, TYPE_INCLUDES);
            } else if (src instanceof ExcludesFilter) {
                serialized = context.serialize(src, ExcludesFilter.class);
                serialized.getAsJsonObject().addProperty(KEY_TYPE, TYPE_EXCLUDES);
            } else {
                throw new IllegalArgumentException("Don't know how to handle DashboardFilter implementation: " + src.getClass().getCanonicalName());
            }

            serialized.getAsJsonObject().addProperty(KEY_NAME, src.name());

            return serialized;
        }
    }

    public static class DashboardFilterDeserializer implements JsonDeserializer<DashboardFilter> {
        @Override
        public DashboardFilter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            final String name = defensivelyGetString(jsonObject, KEY_NAME);
            jsonObject.addProperty(KEY_NAME, name);

            final String type = defensivelyGetString(jsonObject, KEY_TYPE);
            if (StringUtils.isBlank(type)) throw new JsonParseException("Missing filter type");

            switch (type) {
                case TYPE_INCLUDES:
                    return context.deserialize(jsonObject,
                            IncludesFilter.class);
                case TYPE_EXCLUDES:
                    return context.deserialize(jsonObject,
                            ExcludesFilter.class);
                default:
                    throw new JsonParseException(format("Don't know how to deserialize filter type: \"%s\"", type));
            }
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

    private static String defensivelyGetString(JsonObject obj, String key) {
        return obj.has(key) ? obj.getAsJsonPrimitive(key).getAsString() : "";
    }
}
