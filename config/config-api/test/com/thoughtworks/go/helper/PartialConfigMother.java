package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.PipelineGroups;

/**
 * Created by tomzo on 6/13/15.
 */
public class PartialConfigMother {
    public static PartialConfig empty() {
        return  new PartialConfig();
    }
    public static PartialConfig withPipeline(String name) {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig(name);
        BasicPipelineConfigs pipes = new BasicPipelineConfigs(pipe);
        return new PartialConfig(new PipelineGroups(pipes));
    }

    public static PartialConfig withPipelineInGroup(String pipelineName, String groupName) {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig(pipelineName);
        BasicPipelineConfigs pipes = new BasicPipelineConfigs(groupName,new Authorization(),pipe);
        return new PartialConfig(new PipelineGroups(pipes));
    }
}
