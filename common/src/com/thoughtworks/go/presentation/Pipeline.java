package com.thoughtworks.go.presentation;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;

import java.util.ArrayList;
import java.util.List;

public class Pipeline {
    public String getName() {
        return name;
    }

    public List<Stage> getStages() {
        return stages;
    }

    private String name;
    private List<Stage> stages;



    public Pipeline(PipelineConfig pipelineConfig) {
        this.name = pipelineConfig.name().toString();
        this.stages = stagesFrom(pipelineConfig.getStages());
    }

    private List<Stage> stagesFrom(List<StageConfig> stageConfigs) {
        ArrayList<Stage> stages = new ArrayList<>();
        for(StageConfig  stageConfig : stageConfigs) {
            stages.add(new Stage(stageConfig));
        }
        return stages;
    }
}
