package com.thoughtworks.go.server.pluginrequestprocessor;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.server.service.ArtifactCleanupExtensionMapBuilder;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StageHistoryRequestProcessor implements GoPluginApiRequestProcessor {

    private static final String REQUEST_STAGE_HISTORY = "stage-history";

    private StageService stageService;

    private ArtifactCleanupExtensionMapBuilder artifactCleanupExtensionMapBuilder;

    @Autowired
    public StageHistoryRequestProcessor(DefaultGoApplicationAccessor goApplicationAccessor, StageService stageService) {
        this.stageService = stageService;
        goApplicationAccessor.registerProcessorFor(REQUEST_STAGE_HISTORY, this);
    }

    @Override
    public GoApiResponse process(GoApiRequest goPluginApiRequest) {
        Map requestDetails = JsonHelper.fromJson(goPluginApiRequest.requestBody(), Map.class);

        boolean ascending = ascending(requestDetails);
        Long fromId = fromId(requestDetails);
        Long toId = toId(requestDetails);
        List<StageConfigIdentifier> includeStages = includeStages(requestDetails);
        List<StageConfigIdentifier> excludeStages = excludeStages(requestDetails);

        List<Stage> stageInstancesWithArtifacts = stageService.getStagesWithArtifacts(includeStages, excludeStages, fromId, toId, ascending);

        Map<StageConfigIdentifier, List<Stage>> stageNameToInstances = groupStageInstancesByName(stageInstancesWithArtifacts);

        Map<StageConfigIdentifier, Long> uncleanedArtifactInstanceCount = stageService.getStagesInstanceCount(new ArrayList<StageConfigIdentifier>(stageNameToInstances.keySet()), true);

        List response = buildResponseMap(stageNameToInstances, uncleanedArtifactInstanceCount);

        return DefaultGoApiResponse.success(JsonHelper.toJsonString(response));
    }

    private List buildResponseMap(Map<StageConfigIdentifier, List<Stage>> stageNameToInstanceMap, Map<StageConfigIdentifier, Long> uncleanedArtifactStageCount) {
        List result = new ArrayList();
        for (StageConfigIdentifier stageConfigIdentifier : stageNameToInstanceMap.keySet()) {
            Map stageDetails = new HashMap();
            stageDetails.put("name", stageConfigIdentifier.getStageName());
            stageDetails.put("pipeline", stageConfigIdentifier.getPipelineName());
            stageDetails.put("uncleanedArtifactsInstanceCount", uncleanedArtifactStageCount.get(stageConfigIdentifier));
            stageDetails.put("instances", buildStageInstanceDetailsFor(stageNameToInstanceMap.get(stageConfigIdentifier)));
            result.add(stageDetails);
        }

        return result;
    }

    private List buildStageInstanceDetailsFor(List<Stage> stagesInstances) {
        List stageInstanceDetailsList = new ArrayList();
        for (Stage stage : stagesInstances) {
            Map stageInstanceDetails = new HashMap();
            stageInstanceDetails.put("id", stage.getId());
            stageInstanceDetails.put("locator", stage.stageLocator());
            stageInstanceDetails.put("lastTransitionedTime", stage.latestTransitionDate());
            stageInstanceDetails.put("result", stage.getResult());
            stageInstanceDetails.put("latestRun", stage.isLatestRun());
            stageInstanceDetailsList.add(stageInstanceDetails);
        }
        return stageInstanceDetailsList;
    }

    private Map<StageConfigIdentifier, List<Stage>> groupStageInstancesByName(List<Stage> stageInstances) {
        HashMap<StageConfigIdentifier, List<Stage>> result = new HashMap<StageConfigIdentifier, List<Stage>>();
        for (Stage stageInstance : stageInstances) {
            StageConfigIdentifier stageConfigIdentifier = stageInstance.getStageConfigIdentifier();
            if (!result.containsKey(stageConfigIdentifier)) {
                result.put(stageConfigIdentifier, new ArrayList());
            }
            result.get(stageConfigIdentifier).add(stageInstance);
        }
        return result;
    }

    private boolean ascending(Map requestDetails) {
        return "ASC".equalsIgnoreCase((String) requestDetails.get("orderBy")) ? true : false;
    }

    private Long toId(Map requestDetails) {
        String toId = (String) requestDetails.get("toId");
        return StringUtils.isEmpty(toId) ? null : Long.parseLong(toId);
    }

    private Long fromId(Map requestDetails) {
        String toId = (String) requestDetails.get("fromId");
        return StringUtils.isEmpty(toId) ? null : Long.parseLong(toId);
    }

    private List<StageConfigIdentifier> excludeStages(Map requestDetails) {
        return parseToStageConfigIdentifierList((List<Map>) requestDetails.get("excludeStages"));
    }

    private List<StageConfigIdentifier> includeStages(Map requestDetails) {
        return parseToStageConfigIdentifierList((List<Map>) requestDetails.get("includeStages"));
    }

    private List<StageConfigIdentifier> parseToStageConfigIdentifierList(List<Map> stageDetailsAsMap) {
        if (stageDetailsAsMap == null || stageDetailsAsMap.isEmpty()) return null;
        ArrayList<StageConfigIdentifier> result = new ArrayList<StageConfigIdentifier>();
        for (Map map : stageDetailsAsMap) {
            String pipeline = (String) map.get("pipeline");
            String stage = (String) map.get("stage");
            result.add(new StageConfigIdentifier(pipeline, stage));
        }
        return result;
    }
}


