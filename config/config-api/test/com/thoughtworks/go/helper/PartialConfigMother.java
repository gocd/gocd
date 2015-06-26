package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
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
        PartialConfig partialConfig = new PartialConfig(new PipelineGroups(pipes));
        partialConfig.setOrigin(createRepoOrigin());
        return partialConfig;
    }

    public static PartialConfig withPipelineInGroup(String pipelineName, String groupName) {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig(pipelineName);
        BasicPipelineConfigs pipes = new BasicPipelineConfigs(groupName,new Authorization(),pipe);
        PartialConfig partialConfig = new PartialConfig(new PipelineGroups(pipes));
        partialConfig.setOrigin(createRepoOrigin());
        return partialConfig;
    }

    private static RepoConfigOrigin createRepoOrigin() {
        return new RepoConfigOrigin(new ConfigRepoConfig(new GitMaterialConfig("http://some.git"),"myplugin"),"1234fed");
    }

    public static PartialConfig withEnvironment(String name) {
        BasicEnvironmentConfig env = EnvironmentConfigMother.environment(name);
        PartialConfig partialConfig = new PartialConfig();
        partialConfig.getEnvironments().add(env);
        partialConfig.setOrigin(createRepoOrigin());
        return partialConfig;
    }
}
