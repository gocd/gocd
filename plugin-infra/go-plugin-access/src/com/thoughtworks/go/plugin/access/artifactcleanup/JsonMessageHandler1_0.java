/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.plugin.access.artifactcleanup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMessageHandler1_0 implements JsonMessageHandler {

    @Override
    public String requestGetStageInstancesForArtifactCleanup(List<ArtifactExtensionStageConfiguration> stageConfigurations) {
        List<Map> stageDetails = new ArrayList<Map>();
        for (ArtifactExtensionStageConfiguration stageConfiguration : stageConfigurations) {
            Map stage = new HashMap();
            stage.put("pipeline", stageConfiguration.getPipelineName());
            stage.put("stage", stageConfiguration.getStageName());
            stageDetails.add(stage);
        }
        return toJsonString(stageDetails);
    }

    @Override
    public List<ArtifactExtensionStageInstance> responseGetStageInstancesForArtifactCleanup(String responseBody) {
        List<ArtifactExtensionStageInstance> result = new ArrayList<ArtifactExtensionStageInstance>();
        List<Map> stageInstances = parseResponseToList(responseBody);
        for (Map stageInstance : stageInstances) {
            ArtifactExtensionStageInstance instance = parseToInstance(stageInstance);
            result.add(instance);
        }
        return result;
    }

    private ArtifactExtensionStageInstance parseToInstance(Map stageInstance) {
        Long id = Long.valueOf(((String) stageInstance.get("id")));
        String pipeline = (String) stageInstance.get("pipeline");
        Integer pipelineCounter = Integer.valueOf((String) stageInstance.get("pipeline-counter"));
        String stage = (String) stageInstance.get("stage");
        String stageCounter = (String) stageInstance.get("stage-counter");
        ArtifactExtensionStageInstance instance = new ArtifactExtensionStageInstance(id, pipeline, pipelineCounter, stage, stageCounter);
        updateIncludeExcludePaths(stageInstance, instance);

        return instance;
    }

    private void updateIncludeExcludePaths(Map stageInstance, ArtifactExtensionStageInstance instance) {
        List<String> includePaths = (List<String>) stageInstance.get("include-paths");
        if (includePaths != null && !includePaths.isEmpty()) {
            instance.getIncludePaths().addAll(includePaths);
        }
        List<String> excludePaths = (List<String>) stageInstance.get("exclude-paths");
        if (excludePaths != null && !excludePaths.isEmpty()) {
            instance.getExcludePaths().addAll(excludePaths);
        }
    }

    private static String toJsonString(Object object) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(object);
    }

    private List<Map> parseResponseToList(String responseBody) {
        return (List<Map>) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }
}
