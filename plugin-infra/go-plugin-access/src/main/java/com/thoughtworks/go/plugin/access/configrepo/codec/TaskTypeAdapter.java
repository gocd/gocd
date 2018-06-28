/*
 * Copyright 2017 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.configrepo.codec;

import com.google.gson.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;

import java.lang.reflect.Type;

public class TaskTypeAdapter extends TypeAdapter implements JsonDeserializer<CRTask>, JsonSerializer<CRTask> {
    private static final String TYPE = "type";

    @Override
    public CRTask deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return determineJsonElementForDistinguishingImplementers(json, context, TYPE, ORIGIN);
    }

    @Override
    protected Class<?> classForName(String typeName, String origin) {
        if (typeName.equals(CRExecTask.TYPE_NAME))
            return CRExecTask.class;
        if (typeName.equals(CRBuildTask.RAKE_TYPE_NAME))
            return CRBuildTask.class;
        if (typeName.equals(CRBuildTask.ANT_TYPE_NAME))
            return CRBuildTask.class;
        if (typeName.equals(CRBuildTask.NANT_TYPE_NAME))
            return CRNantTask.class;
        if (typeName.equals(CRPluggableTask.TYPE_NAME))
            return CRPluggableTask.class;

        if (typeName.equals(CRAbstractFetchTask.TYPE_NAME)) {
            if (CRFetchArtifactTask.ORIGIN.equals(origin)) {
                return CRFetchArtifactTask.class;
            }
            if (CRFetchPluggableArtifactTask.ORIGIN.equals(origin)) {
                return CRFetchPluggableArtifactTask.class;
            }
            throw new JsonParseException(String.format("Invalid origin '%s' for fetch task.", origin));
        }

        throw new JsonParseException(String.format("Invalid or unknown task type '%s'.", typeName));
    }

    @Override
    public JsonElement serialize(CRTask crTask, Type type, JsonSerializationContext context) {
        JsonObject retValue = context.serialize(crTask).getAsJsonObject();
        CRTask onCancel = crTask.getOnCancel();
        if (onCancel != null) {
            retValue.remove("onCancel");
            retValue.add("onCancel", context.serialize(onCancel));
        }

        return retValue;
    }
}