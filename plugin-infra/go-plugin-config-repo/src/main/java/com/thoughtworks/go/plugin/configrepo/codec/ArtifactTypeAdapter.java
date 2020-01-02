/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.google.gson.*;
import com.thoughtworks.go.plugin.configrepo.contract.CRArtifact;
import com.thoughtworks.go.plugin.configrepo.contract.CRBuiltInArtifact;
import com.thoughtworks.go.plugin.configrepo.contract.CRPluggableArtifact;

import java.lang.reflect.Type;

public class ArtifactTypeAdapter extends TypeAdapter implements JsonDeserializer<CRArtifact>, JsonSerializer<CRArtifact> {
    private static final String TYPE = "type";

    @Override
    public CRArtifact deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return (CRArtifact) determineJsonElementForDistinguishingImplementers(json, context, TYPE, TypeAdapter.ARTIFACT_ORIGIN);
    }

    @Override
    protected Class<?> classForName(String typeName, String origin) {
        if(typeName.equals("external"))
            return CRPluggableArtifact.class;
        if (typeName.equals("build") || typeName.equals("test"))
            return CRBuiltInArtifact.class;
        else
            throw new JsonParseException(
                    String.format("Invalid or unknown task type '%s'",typeName));
    }

    @Override
    public JsonElement serialize(CRArtifact crArtifact, Type type, JsonSerializationContext context) {
        return context.serialize(crArtifact).getAsJsonObject();
    }
}
