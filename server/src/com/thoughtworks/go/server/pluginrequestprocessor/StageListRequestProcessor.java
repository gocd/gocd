package com.thoughtworks.go.server.pluginrequestprocessor;


import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.server.domain.StageStatusListener;
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
public class StageListRequestProcessor implements GoPluginApiRequestProcessor, ConfigChangedListener {

    static final String REQUEST = "stage-list";
    StageService stageService;
    private GoConfigService goConfigService;
    private Map<CaseInsensitiveString, Map> stagesMap = null;
    private final StageListRequestProcessorStageStatusListener stageStatusListener;

    @Autowired
    public StageListRequestProcessor(DefaultGoApplicationAccessor goApplicationAccessor, StageService stageService, GoConfigService goConfigService) {
        this.stageService = stageService;
        this.goConfigService = goConfigService;
        goApplicationAccessor.registerProcessorFor(REQUEST, this);
        stageStatusListener = new StageListRequestProcessorStageStatusListener(this);
        stageService.addStageStatusListener(stageStatusListener);
        goConfigService.register(this);
    }

    @Override
    public GoApiResponse process(GoApiRequest goPluginApiRequest) {
        return success(toJsonString(stageDetailsAsMap()));
    }

    private List<Map> stageDetailsAsMap() {
        if (stagesMap == null) {
            synchronized (this) {
                if (stagesMap == null) {
                    stagesMap = new HashMap<CaseInsensitiveString, Map>();
                    List<StageConfigIdentifier> allDistinctStages = stageService.getAllDistinctStages();
                    Map<CaseInsensitiveString, List<StageConfigIdentifier>> pipelineAndStageMap = groupByPipelineName(allDistinctStages);
                    for (CaseInsensitiveString pipelineName : pipelineAndStageMap.keySet()) {
                        String pipelineGroup = goConfigService.findGroupNameByPipeline(pipelineName);
                        PipelineConfig pipelineConfig = pipelineConfigBy(pipelineName);
                        for (StageConfigIdentifier stageConfigIdentifier : pipelineAndStageMap.get(pipelineName)) {
                            HashMap stageDetails = buildStageDetails(pipelineGroup, stageConfigIdentifier.getStageName(), pipelineName.toString(), stageExistsInConfig(pipelineConfig, stageConfigIdentifier));
                            stagesMap.put(new CaseInsensitiveString(stageConfigIdentifier.concatedStageAndPipelineName()), stageDetails);
                        }
                    }
                }
            }
        }
        return new ArrayList<Map>(stagesMap.values());
    }

    private HashMap buildStageDetails(String pipelineGroup, String stageName, String pipelineName, boolean stageExistsInConfig) {
        HashMap stageDetails = new HashMap();
        stageDetails.put("stageName", stageName);
        stageDetails.put("pipelineName", pipelineName);
        stageDetails.put("pipelineGroup", pipelineGroup);
        stageDetails.put("existInConfig", stageExistsInConfig);
        return stageDetails;
    }

    private boolean stageExistsInConfig(PipelineConfig pipelineConfig, StageConfigIdentifier stageConfigIdentifier) {
        if (pipelineConfig == null) return false;
        StageConfig stage = pipelineConfig.getStage(new CaseInsensitiveString(stageConfigIdentifier.getStageName()));
        return stage != null;
    }

    private PipelineConfig pipelineConfigBy(CaseInsensitiveString pipelineName) {
        try {
            return goConfigService.pipelineConfigNamed(pipelineName);
        } catch (PipelineNotFoundException e) {
            return null;
        }
    }

    private PipelineConfig pipelineConfigBy(CruiseConfig cruiseConfig, CaseInsensitiveString pipelineName) {
        try {
            return cruiseConfig.pipelineConfigByName(pipelineName);
        } catch (PipelineNotFoundException e) {
            return null;
        }
    }

    private Map<CaseInsensitiveString, List<StageConfigIdentifier>> groupByPipelineName(List<StageConfigIdentifier> allDistinctStages) {
        HashMap<CaseInsensitiveString, List<StageConfigIdentifier>> pipelineAndStagesMap = new HashMap<CaseInsensitiveString, List<StageConfigIdentifier>>();
        for (StageConfigIdentifier stageConfigIdentifier : allDistinctStages) {
            CaseInsensitiveString pipelineName = new CaseInsensitiveString(stageConfigIdentifier.getPipelineName());
            if (!pipelineAndStagesMap.containsKey(pipelineName)) {
                pipelineAndStagesMap.put(pipelineName, new ArrayList<StageConfigIdentifier>());
            }
            pipelineAndStagesMap.get(pipelineName).add(stageConfigIdentifier);
        }
        return pipelineAndStagesMap;
    }

    public void stageStatusChanged(Stage stage) {
        synchronized (this) {
            if (stagesMap != null) {
                String pipelineName = stage.getIdentifier().getPipelineName();
                StageConfigIdentifier stageConfigIdentifier = new StageConfigIdentifier(pipelineName, stage.getName());
                CaseInsensitiveString stageAndPipeline = new CaseInsensitiveString(stageConfigIdentifier.concatedStageAndPipelineName());
                if (!stagesMap.containsKey(stageAndPipeline)) {
                    String pipelineGroup = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
                    HashMap stageDetails = buildStageDetails(pipelineGroup, stageConfigIdentifier.getStageName(), pipelineName, true);
                    stagesMap.put(stageAndPipeline, stageDetails);
                }
            }
        }
    }

    @Override
    public void onConfigChange(CruiseConfig cruiseConfig) {
        synchronized (this) {
            for (CaseInsensitiveString stageAndPipeline : stagesMap.keySet()) {
                String stageName = (String) stagesMap.get(stageAndPipeline).get("stageName");
                String pipelineName = (String) stagesMap.get(stageAndPipeline).get("pipelineName");
                PipelineConfig pipelineConfig = pipelineConfigBy(cruiseConfig, new CaseInsensitiveString(pipelineName));
                if (pipelineConfig != null && pipelineConfig.findBy(new CaseInsensitiveString(stageName)) != null) {
                    stagesMap.get(stageAndPipeline).put("pipelineGroup", goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName)));
                    stagesMap.get(stageAndPipeline).put("existInConfig", true);
                } else {
                    stagesMap.get(stageAndPipeline).put("pipelineGroup", "");
                    stagesMap.get(stageAndPipeline).put("existInConfig", false);
                }
            }
        }

    }
}

class StageListRequestProcessorStageStatusListener implements StageStatusListener {

    private StageListRequestProcessor stageListRequestProcessor;

    StageListRequestProcessorStageStatusListener(StageListRequestProcessor stageListRequestProcessor) {
        this.stageListRequestProcessor = stageListRequestProcessor;
    }

    @Override
    public void stageStatusChanged(Stage stage) {
        stageListRequestProcessor.stageStatusChanged(stage);
    }
}


