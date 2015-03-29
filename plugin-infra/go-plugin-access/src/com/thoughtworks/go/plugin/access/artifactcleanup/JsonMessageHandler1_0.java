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
