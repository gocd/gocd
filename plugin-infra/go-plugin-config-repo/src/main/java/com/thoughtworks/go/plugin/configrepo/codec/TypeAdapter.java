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
package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public abstract class TypeAdapter {
    public static final String ARTIFACT_ORIGIN = "artifact_origin";

    public TypeAdapter() {
    }

    public <T> T determineJsonElementForDistinguishingImplementers(JsonElement json, JsonDeserializationContext context, String field, String origin) {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(field);
        JsonPrimitive originField = (JsonPrimitive) jsonObject.get(origin);
        String typeName = prim.getAsString();

        Class<?> klass = classForName(typeName, originField == null ? "gocd" : originField.getAsString());
        return context.deserialize(jsonObject, klass);
    }

    protected abstract Class<?> classForName(String typeName, String origin);
}
