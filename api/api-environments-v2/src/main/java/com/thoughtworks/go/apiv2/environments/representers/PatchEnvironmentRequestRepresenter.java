/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.environments.representers;

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv2.environments.model.PatchEnvironmentRequest;
import com.thoughtworks.go.config.EnvironmentVariableConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatchEnvironmentRequestRepresenter {
    public static PatchEnvironmentRequest fromJSON(JsonReader jsonReader) {
        List<String> pipelineToAdd = extractListFromJson(jsonReader, "pipelines", "add");
        List<String> pipelineToRemove = extractListFromJson(jsonReader, "pipelines", "remove");
        List<String> agentsToAdd = extractListFromJson(jsonReader, "agents", "add");
        List<String> agentsToRemove = extractListFromJson(jsonReader, "agents", "remove");
        List<String> envVariablesToRemove = extractListFromJson(jsonReader, "environment_variables", "remove");

        List<EnvironmentVariableConfig> environmentVariablesToAdd = new ArrayList<>();

        if (jsonReader.hasJsonObject("environment_variables")) {
            jsonReader.readJsonObject("environment_variables").readArrayIfPresent("add",
                    array ->
                            array.forEach(envVariable -> environmentVariablesToAdd
                                    .add(EnvironmentVariableRepresenter.fromJSON(envVariable.getAsJsonObject()))
                            ));
        }

        PatchEnvironmentRequest patchRequest = new PatchEnvironmentRequest(
            pipelineToAdd, pipelineToRemove, agentsToAdd, agentsToRemove, environmentVariablesToAdd, envVariablesToRemove
        );

        return patchRequest;
    }

    private static List<String> extractListFromJson(JsonReader jsonReader, String parentKey, String childKey) {
        return (jsonReader.hasJsonObject(parentKey))
                ? jsonReader.readJsonObject(parentKey).readStringArrayIfPresent(childKey).orElseGet(Collections::emptyList)
                : Collections.emptyList();
    }
}