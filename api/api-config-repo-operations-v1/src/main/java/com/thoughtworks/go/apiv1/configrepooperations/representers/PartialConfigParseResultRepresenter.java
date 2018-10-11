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

package com.thoughtworks.go.apiv1.configrepooperations.representers;

import com.google.gson.*;
import com.thoughtworks.go.config.PartialConfigParseResult;

import java.lang.reflect.Type;

public class PartialConfigParseResultRepresenter {
    private static Gson GSON = new GsonBuilder().
            registerTypeAdapter(PartialConfigParseResult.class, new PartialConfigParseResultSerializer()).
            create();

    public static String toJSON(PartialConfigParseResult result) {
        if (null == result) {
            return "{\"revision\":null}";
        }

        return GSON.toJson(result);
    }

    private static class PartialConfigParseResultSerializer implements JsonSerializer<PartialConfigParseResult> {
        @Override
        public JsonElement serialize(PartialConfigParseResult src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("revision", src.getRevision());
            jsonObject.addProperty("success", src.isSuccessful());

            jsonObject.addProperty("error", src.isSuccessful() ? null : src.getLastFailure().getMessage());
            return jsonObject;
        }
    }
}
