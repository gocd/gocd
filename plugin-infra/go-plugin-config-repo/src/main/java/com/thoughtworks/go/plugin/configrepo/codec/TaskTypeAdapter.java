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
import com.thoughtworks.go.plugin.configrepo.contract.tasks.*;

import java.lang.reflect.Type;

public class TaskTypeAdapter extends TypeAdapter implements JsonDeserializer<CRTask>, JsonSerializer<CRTask> {
    private static final String TYPE = "type";

    @Override
    public CRTask deserialize(JsonElement json,
                              Type type,
                              JsonDeserializationContext context) throws JsonParseException {
        return determineJsonElementForDistinguishingImplementers(json, context, TYPE, ARTIFACT_ORIGIN);
    }

    @Override
    protected Class<?> classForName(String typeName, String origin) {
        if (typeName.equals(CRExecTask.TYPE_NAME))
            return CRExecTask.class;
        if (typeName.equals(CRBuildFramework.rake.toString()))
            return CRBuildTask.class;
        if (typeName.equals(CRBuildFramework.ant.toString()))
            return CRBuildTask.class;
        if (typeName.equals(CRBuildFramework.nant.toString()))
            return CRNantTask.class;
        if (typeName.equals(CRPluggableTask.TYPE_NAME))
            return CRPluggableTask.class;

        if (typeName.equals(CRAbstractFetchTask.TYPE_NAME)) {
            return CRAbstractFetchTask.ArtifactOrigin.getArtifactOrigin(origin).getArtifactTaskClass();
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
