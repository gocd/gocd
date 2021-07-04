/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElasticAgentProfileDeleteCommandTest {
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    public void setUp() throws Exception {
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldDeleteAProfile() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "prod-cluster");
        cruiseConfig.getElasticConfig().getProfiles().add(elasticProfile);

        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(null, elasticProfile, null, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.getElasticConfig().getProfiles(), is(empty()));
    }

    @Test
    public void shouldRaiseExceptionInCaseProfileDoesNotExist() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "prod-cluster");

        assertThat(cruiseConfig.getElasticConfig().getProfiles(), is(empty()));
        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(null, elasticProfile, null, null, new HttpLocalizedOperationResult());

        assertThatThrownBy(() -> command.update(cruiseConfig)).isInstanceOf(RecordNotFoundException.class);

        assertThat(cruiseConfig.getElasticConfig().getProfiles(), is(empty()));
    }

    @Test
    public void shouldNotValidateIfProfileIsInUseByPipeline() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "prod-cluster");

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithJobConfigs("build-linux");
        pipelineConfig.getStages().first().getJobs().first().resourceConfigs().clear();
        pipelineConfig.getStages().first().getJobs().first().setElasticProfileId("foo");
        cruiseConfig.addPipeline("all", pipelineConfig);

        assertThat(cruiseConfig.getElasticConfig().getProfiles(), is(empty()));
        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(null, elasticProfile, null, null, new HttpLocalizedOperationResult());
        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .isInstanceOf(GoConfigInvalidException.class)
                .hasMessageContaining("The elastic agent profile 'foo' is being referenced by pipeline(s): JobConfigIdentifier[build-linux:mingle:defaultJob].");
    }

    @Test
    public void shouldValidateIfProfileIsNotInUseByPipeline() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "prod-cluster");

        assertThat(cruiseConfig.getElasticConfig().getProfiles(), is(empty()));
        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(null, elasticProfile, null, null, new HttpLocalizedOperationResult());
        assertTrue(command.isValid(cruiseConfig));
    }
}
