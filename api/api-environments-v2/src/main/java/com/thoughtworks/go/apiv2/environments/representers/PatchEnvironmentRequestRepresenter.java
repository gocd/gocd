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
import java.util.List;

import static java.util.Collections.emptyList;

public class PatchEnvironmentRequestRepresenter {
    public static PatchEnvironmentRequest fromJSON(JsonReader jsonReader) {
        List<String> pipelineToAdd = new ArrayList<>();
        List<String> pipelineToRemove = new ArrayList<>();
        List<String> agentsToAdd = new ArrayList<>();
        List<String> agentsToRemove = new ArrayList<>();
        List<EnvironmentVariableConfig> envVariablesToAdd = new ArrayList<>();
        List<String> envVariableToRemove = new ArrayList<>();


        jsonReader.optJsonObject("pipelines").ifPresent(reader -> {
            pipelineToAdd.addAll(reader.readStringArrayIfPresent("add").orElse(emptyList()));
            pipelineToRemove.addAll(reader.readStringArrayIfPresent("remove").orElse(emptyList()));
        });


        jsonReader.optJsonObject("agents").ifPresent(reader -> {
            agentsToAdd.addAll(reader.readStringArrayIfPresent("add").orElse(emptyList()));
            agentsToRemove.addAll(reader.readStringArrayIfPresent("remove").orElse(emptyList()));
        });

        jsonReader.optJsonObject("environment_variables").ifPresent(reader -> {
            envVariableToRemove.addAll(reader.readStringArrayIfPresent("remove").orElse(emptyList()));

            reader.readArrayIfPresent("add", array ->
                    array.forEach(envVariable -> envVariablesToAdd
                            .add(EnvironmentVariableRepresenter.fromJSON(new JsonReader(envVariable.getAsJsonObject())))
                    ));
        });

        PatchEnvironmentRequest patchRequest = new PatchEnvironmentRequest(
                pipelineToAdd, pipelineToRemove, agentsToAdd, agentsToRemove, envVariablesToAdd, envVariableToRemove
        );

        return patchRequest;
    }
}