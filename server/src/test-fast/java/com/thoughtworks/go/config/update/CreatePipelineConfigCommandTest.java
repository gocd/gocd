/*
 * Copyright 2020 ThoughtWorks, Inc.
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


import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class CreatePipelineConfigCommandTest {

    private GoConfigService goConfigService;
    private Username username;
    private LocalizedOperationResult localizedOperationResult;
    private PipelineConfig pipelineConfig;
    private ExternalArtifactsService externalArtifactsService;

    @Before
    public void setUp() {
        externalArtifactsService = mock(ExternalArtifactsService.class);
        goConfigService = mock(GoConfigService.class);
        username = new Username("view");
        localizedOperationResult = mock(LocalizedOperationResult.class);
        pipelineConfig = PipelineConfigMother.pipelineConfig("p1");
    }

    @Test
    public void shouldInvokeAddPipelineMethodOfCruiseConfig() {
        CreatePipelineConfigCommand command = new CreatePipelineConfigCommand(goConfigService,
                pipelineConfig, username, localizedOperationResult, "group1", externalArtifactsService);

        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");

        command.update(cruiseConfig);
        verify(cruiseConfig).addPipelineWithoutValidation("group1", pipelineConfig);
    }

    @Test
    public void shouldDisallowAddIfUserIsNotAGroupAdmin() {
        CreatePipelineConfigCommand command = new CreatePipelineConfigCommand(goConfigService,
                pipelineConfig, username, localizedOperationResult, "group1", externalArtifactsService);

        PipelineGroups mock = mock(PipelineGroups.class);
        when(goConfigService.groups()).thenReturn(mock);
        when(mock.hasGroup("group1")).thenReturn(true);
        when(goConfigService.isUserAdminOfGroup(username.getUsername(), "group1")).thenReturn(false);

        assertFalse(command.canContinue(mock(CruiseConfig.class)));
    }

    @Test
    public void shouldEncryptSecurePropertiesOfPipelineConfig() {
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        CreatePipelineConfigCommand command = new CreatePipelineConfigCommand(goConfigService,
                pipelineConfig, username, localizedOperationResult, "group1", externalArtifactsService);

        when(pipelineConfig.name()).thenReturn(new CaseInsensitiveString("p1"));
        CruiseConfig preprocessedConfig = mock(CruiseConfig.class);
        when(preprocessedConfig.getPipelineConfigByName(new CaseInsensitiveString("p1"))).thenReturn(mock(PipelineConfig.class));

        command.encrypt(preprocessedConfig);

        verify(pipelineConfig).encryptSecureProperties(eq(preprocessedConfig), any(PipelineConfig.class));
    }

    @Test
    public void createPipelineConfigShouldValidateAllExternalArtifacts() {
        PluggableArtifactConfig s3 = mock(PluggableArtifactConfig.class);
        PluggableArtifactConfig docker = mock(PluggableArtifactConfig.class);
        when(goConfigService.artifactStores()).thenReturn(mock(ArtifactStores.class));
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("P1"))).thenReturn("group");
        ConfigErrors configErrors = new ConfigErrors();
        when(s3.errors()).thenReturn(configErrors);
        when(docker.errors()).thenReturn(configErrors);
        JobConfig job1 = JobConfigMother.jobWithNoResourceRequirement();
        JobConfig job2 = JobConfigMother.jobWithNoResourceRequirement();

        job1.artifactTypeConfigs().add(s3);
        job2.artifactTypeConfigs().add(docker);

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        CreatePipelineConfigCommand command = new CreatePipelineConfigCommand(goConfigService, pipeline, username, localizedOperationResult, "group", externalArtifactsService);

        BasicCruiseConfig preprocessedConfig = GoConfigMother.defaultCruiseConfig();
        preprocessedConfig.addPipelineWithoutValidation("group", pipeline);
        command.isValid(preprocessedConfig);

        verify(externalArtifactsService).validateExternalArtifactConfig(eq(s3), any(), eq(true));
        verify(externalArtifactsService).validateExternalArtifactConfig(eq(docker), any(), eq(true));
    }

    @Test
    public void createPipelineConfigShouldValidateAllFetchExternalArtifactTasks() {
        JobConfig job1 = JobConfigMother.jobWithNoResourceRequirement();
        JobConfig job2 = JobConfigMother.jobWithNoResourceRequirement();

        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("P1"))).thenReturn("group");

        FetchPluggableArtifactTask fetchS3Task = new FetchPluggableArtifactTask(new CaseInsensitiveString("p0"), new CaseInsensitiveString("s0"), new CaseInsensitiveString("j0"), "s3");
        FetchPluggableArtifactTask fetchDockerTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("p0"), new CaseInsensitiveString("s0"), new CaseInsensitiveString("j0"), "docker");

        job1.addTask(fetchS3Task);
        job2.addTask(fetchDockerTask);

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        CreatePipelineConfigCommand command = new CreatePipelineConfigCommand(goConfigService, pipeline, username, localizedOperationResult, "group", externalArtifactsService);


        BasicCruiseConfig preprocessedConfig = GoConfigMother.defaultCruiseConfig();
        preprocessedConfig.addPipelineWithoutValidation("group", pipeline);
        command.isValid(preprocessedConfig);


        verify(externalArtifactsService, times(2)).validateFetchExternalArtifactTask(any(FetchPluggableArtifactTask.class), any(PipelineConfig.class), eq(preprocessedConfig));
    }
}
