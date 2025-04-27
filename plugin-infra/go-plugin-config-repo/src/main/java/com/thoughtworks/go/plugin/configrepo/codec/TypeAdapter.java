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
package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;

public abstract class TypeAdapter {
        public <T> T determineJsonElementForDistinguishingImplementers(JsonElement json, JsonDeserializationContext context, String typePropertyName, String originPropertyName) {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement typeField = Objects.requireNonNull(jsonObject.get(typePropertyName), () -> String.format("JSON element from plugin did not contain [%s] property for determining its type. Check your syntax, or the plugin logic.", typePropertyName));
        JsonElement originField = jsonObject.get(originPropertyName);

        // Check prim is not null
        String typeName = typeField.getAsString();

        Class<?> klass = classForName(typeName, originField == null ? "gocd" : originField.getAsString());
        return context.deserialize(jsonObject, klass);
    }

    public <T> T determineJsonElementForDistinguishingImplementers(JsonElement json, JsonDeserializationContext context, String typePropertyName) {
        return determineJsonElementForDistinguishingImplementers(json, context, typePropertyName, null);
    }

    protected abstract Class<?> classForName(String typeName, String origin);
}
