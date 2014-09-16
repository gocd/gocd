package com.thoughtworks.go.server.pluginrequestprocessor;


import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.server.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.json.JsonHelper.DATE_FORMAT;
import static com.thoughtworks.go.util.json.JsonHelper.toJsonString;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.math.NumberUtils.isDigits;

@Component
public class StageHistoryRequestProcessor implements GoPluginApiRequestProcessor {

    static final String PIPELINE_NAME = "pipeline-name";
    static final String STAGE_NAME = "stage-name";
    static final String FROM_ID = "from-id";
    static final String TO_ID = "to-id";
    static final String ORDER = "order";
    static final String REQUEST = "stage-history";
    StageService stageService;

    @Autowired
    public StageHistoryRequestProcessor(DefaultGoApplicationAccessor goApplicationAccessor, StageService stageService) {
        this.stageService = stageService;
        goApplicationAccessor.registerProcessorFor(REQUEST, this);
    }

    @Override
    public GoApiResponse process(GoApiRequest goPluginApiRequest) {
        try {
            validateRequestParams(goPluginApiRequest.requestParameters());
        } catch (Exception exception) {
            return DefaultGoApiResponse.incompleteRequest(exception.getMessage());
        }

        String pipeLineName = goPluginApiRequest.requestParameters().get(PIPELINE_NAME);
        String stageName = goPluginApiRequest.requestParameters().get(STAGE_NAME);
        Long fromId = isEmpty(goPluginApiRequest.requestParameters().get(FROM_ID)) ? null : Long.parseLong(goPluginApiRequest.requestParameters().get(FROM_ID));
        Long toId = isEmpty(goPluginApiRequest.requestParameters().get(TO_ID)) ? null : Long.parseLong(goPluginApiRequest.requestParameters().get(TO_ID));
        boolean ascending = "DESC".equalsIgnoreCase(goPluginApiRequest.requestParameters().get(ORDER)) ? false : true;

        List<Stage> stageInstances = stageService.getStagesWithArtifactsGivenPipelineAndStage(pipeLineName, stageName, fromId, toId, ascending);

        return DefaultGoApiResponse.success(toJsonString(toMap(stageInstances), DATE_FORMAT));
    }

    private List<Map> toMap(List<Stage> stageInstances) {
        ArrayList<Map> result = new ArrayList<Map>();
        for (Stage stageInstance : stageInstances) {
            HashMap map = new HashMap();
            map.put("stageId", valueOf(stageInstance.getId()));
            map.put("stageName", stageInstance.getName());
            map.put("stageCounter", valueOf(stageInstance.getCounter()));
            map.put("pipelineId", valueOf(stageInstance.getPipelineId()));
            map.put("pipelineName", stageInstance.getIdentifier().getPipelineName());
            map.put("pipelineCounter", valueOf(stageInstance.getIdentifier().getPipelineCounter()));
            map.put("stageResult", stageInstance.getResult().toString());
            map.put("lastTransitionTime", stageInstance.getLastTransitionedTime());
            result.add(map);
        }
        return result;
    }

    private void validateRequestParams(Map<String, String> requestParams) {
        String pipeLineName = requestParams.get(PIPELINE_NAME);
        String stageName = requestParams.get(STAGE_NAME);
        if ((isEmpty(pipeLineName) && !isEmpty(stageName)) || (!isEmpty(pipeLineName) && isEmpty(stageName))) {
            throw new RuntimeException(String.format("Expected to provide both pipeline name and stage name, but got Pipeline name as [%s] and stage name as [%s]", pipeLineName, stageName));
        }
        String fromId = requestParams.get(FROM_ID);
        if (!isEmpty(fromId) && !isDigits(fromId)) {
            throw new RuntimeException("Invalid from-id");
        }
        String toId = requestParams.get(TO_ID);
        if (!isEmpty(toId) && !isDigits(toId)) {
            throw new RuntimeException("Invalid to-id");
        }
    }
}