/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.helper.GoConfigMother;
import org.apache.commons.collections.map.SingletonMap;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class EnvironmentConfigBasicTest extends EnvironmentConfigTestBase {
    @Before
    public void setUp() throws Exception {
        environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
    }

    @Test
    public void shouldReturnEmptyForRemotePipelinesWhenIsLocal()
    {
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe"));
        assertThat(environmentConfig.getRemotePipelines().isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAllPipelinesForRemotePipelinesWhenIsRemote()
    {
        environmentConfig.setOrigins(new RepoConfigOrigin());
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe"));
        assertThat(environmentConfig.getRemotePipelines().isEmpty(), is(false));
    }

    @Test
    public void shouldReturnTrueThatLocalWhenOriginIsNotSet()
    {
        environmentConfig.setOrigins(null);
        assertThat(environmentConfig.isLocal(), is(true));
    }
    @Test
    public void shouldReturnTrueThatLocalWhenOriginIsFile()
    {
        environmentConfig.setOrigins(new FileConfigOrigin());
        assertThat(environmentConfig.isLocal(),is(true));
    }
    @Test
    public void shouldReturnFalseThatLocalWhenOriginIsConfigRepo()
    {
        environmentConfig.setOrigins(new RepoConfigOrigin());
        assertThat(environmentConfig.isLocal(),is(false));
    }
    @Test
    public void shouldReturnSelfAsLocalPartWhenOriginIsFile()
    {
        environmentConfig.setOrigins(new FileConfigOrigin());
        assertSame(environmentConfig,environmentConfig.getLocal());
    }

    @Test
    public void shouldReturnSelfAsLocalPartWhenOriginIsUI()
    {
        environmentConfig.setOrigins(new UIConfigOrigin());
        assertSame(environmentConfig,environmentConfig.getLocal());

    }

    @Test
    public void shouldReturnNullAsLocalPartWhenOriginIsConfigRepo()
    {
        environmentConfig.setOrigins(new RepoConfigOrigin());
        assertNull(environmentConfig.getLocal());
    }

    @Test
    public void shouldUpdateName() {
        environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.NAME_FIELD, "PROD"));
        assertThat(environmentConfig.name(), is(new CaseInsensitiveString("PROD")));
    }

    @Test
    public void validate_shouldNotAllowToReferencePipelineDefinedInConfigRepo_WhenEnvironmentDefinedInFile()
    {
        ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
        ConfigOrigin envOrigin = new FileConfigOrigin();

        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
        cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) this.environmentConfig;
        environmentConfig.setOrigins(envOrigin);
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe1"));
        cruiseConfig.addEnvironment(environmentConfig);

        environmentConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig, environmentConfig));
        EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

        assertThat(reference.errors().isEmpty(),is(false));
        assertThat(reference.errors().on(EnvironmentPipelineConfig.ORIGIN),startsWith("Environment defined in"));
    }

    @Test
    public void validate_shouldAllowToReferencePipelineDefinedInConfigRepo_WhenEnvironmentDefinedInConfigRepo()
    {
        ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
        ConfigOrigin envOrigin = new RepoConfigOrigin();

        passReferenceValidationHelper(pipelineOrigin, envOrigin);
    }

    @Test
    public void validate_shouldAllowToReferencePipelineDefinedInFile_WhenEnvironmentDefinedInFile()
    {
        ConfigOrigin pipelineOrigin = new FileConfigOrigin();
        ConfigOrigin envOrigin = new FileConfigOrigin();

        passReferenceValidationHelper(pipelineOrigin, envOrigin);
    }

    @Test
    public void validate_shouldAllowToReferencePipelineDefinedInFile_WhenEnvironmentDefinedInConfigRepo()
    {
        ConfigOrigin pipelineOrigin = new FileConfigOrigin();
        ConfigOrigin envOrigin = new RepoConfigOrigin();

        passReferenceValidationHelper(pipelineOrigin, envOrigin);
    }

    @Test
    public void shouldValidateWhenPipelineNotFound()
    {
        ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
        ConfigOrigin envOrigin = new FileConfigOrigin();

        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
        cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) this.environmentConfig;
        environmentConfig.setOrigins(envOrigin);
        environmentConfig.addPipeline(new CaseInsensitiveString("unknown"));
        cruiseConfig.addEnvironment(environmentConfig);

        environmentConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig, environmentConfig));
        EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

        assertThat(reference.errors().isEmpty(),is(true));
    }


    private void passReferenceValidationHelper(ConfigOrigin pipelineOrigin, ConfigOrigin envOrigin) {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
        cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) this.environmentConfig;
        environmentConfig.setOrigins(envOrigin);
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe1"));
        cruiseConfig.addEnvironment(environmentConfig);

        environmentConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig, environmentConfig));
        EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

        assertThat(reference.errors().isEmpty(),is(true));
    }

}
