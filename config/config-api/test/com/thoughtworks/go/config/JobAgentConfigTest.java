/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JobAgentConfigTest {

    @Test
    public void shouldNotAllowNullPluginId() throws Exception {
        JobAgentConfig config = new JobAgentConfig(null);

        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, null, PipelineConfigMother.pipelineConfig("pipeline"), StageConfigMother.manualStage("stage"), JobConfigMother.job());
        config.validate(context);
        assertThat(config.errors().size(), is(1));
        assertThat(config.errors().on(JobAgentConfig.PLUGIN_ID), is("Agent config on job 'pipeline::stage::defaultJob' cannot have a blank plugin id."));
    }

    @Test
    public void shouldValidateConfigPropertyNameUniqueness() throws Exception {
        ConfigurationProperty prop1 = ConfigurationPropertyMother.create("USERNAME");
        ConfigurationProperty prop2 = ConfigurationPropertyMother.create("USERNAME");
        JobAgentConfig config = new JobAgentConfig("cd.go-contrib.elasticagent.docker", prop1, prop2);

        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, null, PipelineConfigMother.pipelineConfig("pipeline"), StageConfigMother.manualStage("stage"), JobConfigMother.job());
        config.validate(context);

        assertThat(config.errors().size(), is(0));

        assertThat(prop1.errors().size(), is(1));
        assertThat(prop2.errors().size(), is(1));

        assertThat(prop1.errors().on(ConfigurationProperty.CONFIGURATION_KEY), is("Duplicate key 'USERNAME' found for agent config of job 'pipeline::stage::defaultJob'"));
        assertThat(prop2.errors().on(ConfigurationProperty.CONFIGURATION_KEY), is("Duplicate key 'USERNAME' found for agent config of job 'pipeline::stage::defaultJob'"));
    }
}
