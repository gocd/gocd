package com.thoughtworks.go.server.pluginrequestprocessor;


import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse.success;
import static com.thoughtworks.go.util.json.JsonHelper.toJsonString;

@Component
public class StageListRequestProcessor implements GoPluginApiRequestProcessor {

    static final String REQUEST = "stage-list";
    StageService stageService;
    private GoConfigService goConfigService;

    @Autowired
    public StageListRequestProcessor(DefaultGoApplicationAccessor goApplicationAccessor, StageService stageService, GoConfigService goConfigService) {
        this.stageService = stageService;
        this.goConfigService = goConfigService;
        goApplicationAccessor.registerProcessorFor(REQUEST, this);
    }

    @Override
    public GoApiResponse process(GoApiRequest goPluginApiRequest) {
        return success(toJsonString(toMap()));
    }

    private List<Map> toMap() {
        List<Map> result = new ArrayList<Map>();
        List<StageConfigIdentifier> allDistinctStages = stageService.getAllDistinctStages();
        for (StageConfigIdentifier stage : allDistinctStages) {
            HashMap map = new HashMap();
            map.put("stageName", stage.getStageName());
            map.put("pipelineName", stage.getPipelineName());
            map.put("existInConfig", goConfigService.stageExists(stage.getPipelineName(), stage.getStageName()));
            result.add(map);
        }
        return result;
    }
}
