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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ElasticProfileNotFoundException;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticAgentProfileDeleteCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;
    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }


    @Test
    public void shouldDeleteAProfile() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");
        cruiseConfig.server().getElasticConfig().getProfiles().add(elasticProfile);

        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(elasticProfile, null, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().getElasticConfig().getProfiles(), is(empty()));
    }

    @Test
    public void shouldRaiseExceptionInCaseProfileDoesNotExist() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");

        assertThat(cruiseConfig.server().getElasticConfig().getProfiles(), is(empty()));
        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(elasticProfile, null, null, new HttpLocalizedOperationResult());

        thrown.expect(ElasticProfileNotFoundException.class);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().getElasticConfig().getProfiles(), is(empty()));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(elasticProfile, goConfigService, currentUser, result);
        assertThat(cruiseConfig.server().getElasticConfig().getProfiles().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldNotValidateIfProfileIsInUseByPipeline() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithJobConfigs("build-linux");
        pipelineConfig.getStages().first().getJobs().first().resources().clear();
        pipelineConfig.getStages().first().getJobs().first().setElasticProfileId("foo");
        cruiseConfig.addPipeline("all", pipelineConfig);

        assertThat(cruiseConfig.server().getElasticConfig().getProfiles(), is(empty()));
        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(elasticProfile, null, null, new HttpLocalizedOperationResult());
        thrown.expect(GoConfigInvalidException.class);
        thrown.expectMessage("The elastic agent profile 'foo' is being referenced by pipeline(s): [JobConfigIdentifier[build-linux:mingle:defaultJob]].");
        command.isValid(cruiseConfig);
    }

    @Test
    public void shouldValidateIfProfileIsNotInUseByPipeline() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");

        assertThat(cruiseConfig.server().getElasticConfig().getProfiles(), is(empty()));
        ElasticAgentProfileDeleteCommand command = new ElasticAgentProfileDeleteCommand(elasticProfile, null, null, new HttpLocalizedOperationResult());
        assertTrue(command.isValid(cruiseConfig));
    }
}
