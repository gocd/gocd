package com.thoughtworks.go.plugin.access.artifactcleanup;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static java.lang.String.format;

@Component
public class ArtifactCleanupExtension {

    private static final Logger LOGGER = Logger.getLogger(ArtifactCleanupExtension.class);

    public static final String NAME = "artifact-cleanup";
    public static final String REQUEST_LIST_OF_STAGES_HANDLED = "list-of-stages-handled";
    public static final String REQUEST_LIST_OF_STAGES_INSTANCES = "list-of-stages-instances";
    private DefaultPluginManager defaultPluginManager;

    @Autowired
    public ArtifactCleanupExtension(DefaultPluginManager defaultPluginManager) {
        this.defaultPluginManager = defaultPluginManager;
    }

    public List<StageConfigDetailsArtifactCleanup> listOfStagesHandledByExtension() {
        ArrayList<StageConfigDetailsArtifactCleanup> list = new ArrayList<StageConfigDetailsArtifactCleanup>();
        for (GoPluginIdentifier plugin : defaultPluginManager.allPluginsOfType(NAME)) {
            GoPluginApiResponse response = null;
            try {
                response = defaultPluginManager.submitTo(plugin.getId(), new DefaultGoPluginApiRequest(NAME, "1.0", REQUEST_LIST_OF_STAGES_HANDLED));
                if (SUCCESS_RESPONSE_CODE == response.responseCode()) {
                    processStageConfigDetailsResponse(list, parseResponse(response.responseBody()));
                } else {
                    LOGGER.warn(format("Unsuccessful response retrieving stage config details from plugin %s. Returned response code %s and response body %s", plugin.getId(), response.responseCode(), response.responseBody()));
                }
            } catch (Exception e) {
                LOGGER.warn(format("Error while retrieving stage config details from plugin %s, with response body %s", plugin.getId(), response == null ? "" : response.responseBody()), e);
            }

        }
        return list;
    }


    public List<StageDetailsArtifactCleanup> listOfStageInstanceIdsForArtifactDeletion() {
        ArrayList<StageDetailsArtifactCleanup> list = new ArrayList<StageDetailsArtifactCleanup>();
        for (GoPluginIdentifier plugin : defaultPluginManager.allPluginsOfType(NAME)) {
            GoPluginApiResponse response = null;
            try {
                response = defaultPluginManager.submitTo(plugin.getId(), new DefaultGoPluginApiRequest(NAME, "1.0", REQUEST_LIST_OF_STAGES_INSTANCES));
                if (SUCCESS_RESPONSE_CODE == response.responseCode()) {
                    processStageInstanceDetailsResponse(list, parseResponse(response.responseBody()));
                } else {
                    LOGGER.warn(format("Unsuccessful response retrieving stage details details from plugin %s. Returned response code %s and response body %s", plugin.getId(), response.responseCode(), response.responseBody()));
                }

            } catch (Exception e) {
                LOGGER.warn(format("Could not process stage details artifact cleanup response from plugin %s with response body %s", plugin.getId(), response == null ? "" : response.responseBody()), e);
            }
        }
        return list;
    }

    private void processStageConfigDetailsResponse(ArrayList<StageConfigDetailsArtifactCleanup> list, List<Map<String, String>> result) {
        for (Map<String, String> stageDetails : result) {
            StageConfigDetailsArtifactCleanup stageConfigDetailsArtifactCleanup = new StageConfigDetailsArtifactCleanup(stageDetails.get("stageName"), stageDetails.get("pipelineName"));
            List<StageConfigDetailsArtifactCleanup> toBeAdded = new ArrayList<StageConfigDetailsArtifactCleanup>();
            if (!list.contains(stageConfigDetailsArtifactCleanup)) {
                toBeAdded.add(stageConfigDetailsArtifactCleanup);
            }
            list.addAll(toBeAdded);
        }
    }


    private void processStageInstanceDetailsResponse(ArrayList<StageDetailsArtifactCleanup> list, List<Map<String, String>> result) {
        for (Map stageInstanceDetails : result) {
            Long stageId = Long.parseLong((String) stageInstanceDetails.get("stageId"));
            String pipelineName = (String) stageInstanceDetails.get("pipelineName");
            int pipelineCounter = Integer.parseInt((String) stageInstanceDetails.get("pipelineCounter"));
            String stageName = (String) stageInstanceDetails.get("stageName");
            int stageCounter = Integer.parseInt((String) stageInstanceDetails.get("stageCounter"));
            List<String> excludePaths = (List<String>) stageInstanceDetails.get("excludePaths");
            List<String> includePaths = (List<String>) stageInstanceDetails.get("includePaths");
            if (excludePaths != null && !excludePaths.isEmpty()) {
                list.add(new StageDetailsArtifactCleanup(stageId, pipelineName, pipelineCounter, stageName, stageCounter, excludePaths, true));
            } else if (includePaths != null && !includePaths.isEmpty()) {
                list.add(new StageDetailsArtifactCleanup(stageId, pipelineName, pipelineCounter, stageName, stageCounter, includePaths, false));
            } else {
                list.add(new StageDetailsArtifactCleanup(stageId, pipelineName, pipelineCounter, stageName, stageCounter));
            }
        }
    }

    private List<Map<String, String>> parseResponse(String responseBody) {
        return (List<Map<String, String>>) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }

}
