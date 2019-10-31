/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

class UpdatePipelineConfigCommandTest {

    private EntityHashingService entityHashingService;
    private GoConfigService goConfigService;
    private Username username;
    private LocalizedOperationResult localizedOperationResult;
    private PipelineConfig pipelineConfig;
    private ExternalArtifactsService externalArtifactsService;


    @BeforeEach
    void setUp() throws Exception {
        entityHashingService = mock(EntityHashingService.class);
        externalArtifactsService = mock(ExternalArtifactsService.class);
        goConfigService = mock(GoConfigService.class);
        username = mock(Username.class);
        localizedOperationResult = mock(LocalizedOperationResult.class);
        pipelineConfig = PipelineConfigMother.pipelineConfig("p1");
    }

    @Test
    void shouldDisallowStaleRequest() {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, entityHashingService,
                pipelineConfig, "group1", username, "stale_md5", localizedOperationResult, externalArtifactsService);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(goConfigService.canEditPipeline(pipelineConfig.name().toString(), username, localizedOperationResult, "group1")).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineConfig, "group1")).thenReturn("latest_md5");

        BasicCruiseConfig basicCruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig));
        assertFalse(command.canContinue(basicCruiseConfig));
    }

    @Test
    void shouldDisallowUpdateIfPipelineEditIsDisAllowed() throws Exception {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, "", username, "stale_md5", localizedOperationResult, externalArtifactsService);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(goConfigService.canEditPipeline(pipelineConfig.name().toString(), username, localizedOperationResult, "group1")).thenReturn(false);
        assertFalse(command.canContinue(mock(CruiseConfig.class)));
    }

    @Test
    void shouldInvokeUpdateMethodOfCruiseConfig() throws Exception {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, "group1", username, "stale_md5", localizedOperationResult, externalArtifactsService);

        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");

        command.update(cruiseConfig);
        verify(cruiseConfig).update("group1", pipelineConfig.name().toString(), pipelineConfig);
    }

    @Test
    void shouldEncryptSecurePropertiesOfPipelineConfig() {
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, "group1", username, "stale_md5", localizedOperationResult, externalArtifactsService);

        when(pipelineConfig.name()).thenReturn(new CaseInsensitiveString("p1"));
        CruiseConfig preprocessedConfig = mock(CruiseConfig.class);
        when(preprocessedConfig.getPipelineConfigByName(new CaseInsensitiveString("p1"))).thenReturn(mock(PipelineConfig.class));

        command.encrypt(preprocessedConfig);

        verify(pipelineConfig).encryptSecureProperties(eq(preprocessedConfig), any(PipelineConfig.class));
    }

    @Test
    void updatePipelineConfigShouldValidateAllExternalArtifacts() {
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

        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipeline, "group", username, "stale_md5", localizedOperationResult, externalArtifactsService);

        BasicCruiseConfig preprocessedConfig = GoConfigMother.defaultCruiseConfig();
        preprocessedConfig.addPipelineWithoutValidation("group", pipeline);
        command.isValid(preprocessedConfig);

        verify(externalArtifactsService).validateExternalArtifactConfig(eq(s3), any(), eq(true));
        verify(externalArtifactsService).validateExternalArtifactConfig(eq(docker), any(), eq(true));
    }

    @Test
    void updatePipelineConfigShouldValidateAllFetchExternalArtifactTasks() {
        JobConfig job1 = JobConfigMother.jobWithNoResourceRequirement();
        JobConfig job2 = JobConfigMother.jobWithNoResourceRequirement();

        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("P1"))).thenReturn("group");

        FetchPluggableArtifactTask fetchS3Task = new FetchPluggableArtifactTask(new CaseInsensitiveString("p0"), new CaseInsensitiveString("s0"), new CaseInsensitiveString("j0"), "s3");
        FetchPluggableArtifactTask fetchDockerTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("p0"), new CaseInsensitiveString("s0"), new CaseInsensitiveString("j0"), "docker");

        job1.addTask(fetchS3Task);
        job2.addTask(fetchDockerTask);

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipeline, "group", username, "stale_md5", localizedOperationResult, externalArtifactsService);

        BasicCruiseConfig preprocessedConfig = GoConfigMother.defaultCruiseConfig();
        preprocessedConfig.addPipelineWithoutValidation("group", pipeline);
        command.isValid(preprocessedConfig);


        verify(externalArtifactsService, times(2)).validateFetchExternalArtifactTask(any(FetchPluggableArtifactTask.class), any(PipelineConfig.class), eq(preprocessedConfig));
    }

    @Test
    void shouldAddGroupWithPipelineAndRemoveItFromPreviousIfTheSpecifiedGroupDoesNotExist() {

        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group1", new Authorization(), pipelineConfig);
        PipelineGroups pipelineGroups = new PipelineGroups(pipelineConfigs);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(cruiseConfig.findGroup("group1")).thenReturn(pipelineConfigs);
        when(cruiseConfig.getGroups()).thenReturn(pipelineGroups);

        assertThat(pipelineGroups.hasGroup("updated_group"), is(false));

        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, "updated_group", username, "stale_md5", localizedOperationResult, externalArtifactsService);

        command.update(cruiseConfig);
        verify(cruiseConfig).update("group1", pipelineConfig.name().toString(), pipelineConfig);

        assertThat(pipelineGroups.findGroup("group1").size(), is(0));
        assertThat(pipelineGroups.hasGroup("updated_group"), is(true));
        assertThat(pipelineGroups.findGroup("updated_group").size(), is(1));
    }

    @Test
    void shouldNotAllowToContinueIfTheUserIsNotAnAdminOnTheNewGroup() {
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group1", new Authorization(), pipelineConfig);
        BasicPipelineConfigs pipelineConfigs1 = new BasicPipelineConfigs("group2", new Authorization());
        PipelineGroups pipelineGroups = new PipelineGroups(pipelineConfigs, pipelineConfigs1);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(goConfigService.canEditPipeline(anyString(), any(Username.class), any(LocalizedOperationResult.class), anyString())).thenReturn(true);
        when(goConfigService.groups()).thenReturn(pipelineGroups);
        when(goConfigService.isUserAdminOfGroup(username.getUsername(), "group2")).thenReturn(false);

        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, entityHashingService,
                pipelineConfig, "group2", username, "md5", localizedOperationResult, externalArtifactsService);

        boolean canContinue = command.canContinue(cruiseConfig);

        assertFalse(canContinue);
    }

    @Test
    void shouldAllowToContinueIfTheNewGroupExistAndTheUserIsAnAdminOfTheSame() {
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs("group1", new Authorization(), pipelineConfig);
        BasicPipelineConfigs pipelineConfigs1 = new BasicPipelineConfigs("group2", new Authorization());
        PipelineGroups pipelineGroups = new PipelineGroups(pipelineConfigs, pipelineConfigs1);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(goConfigService.canEditPipeline(anyString(), any(Username.class), any(LocalizedOperationResult.class), anyString())).thenReturn(true);
        when(goConfigService.groups()).thenReturn(pipelineGroups);
        when(username.getUsername()).thenReturn(new CaseInsensitiveString("user"));
        when(goConfigService.isUserAdminOfGroup(any(CaseInsensitiveString.class), eq("group2"))).thenReturn(true);
        when(cruiseConfig.getPipelineConfigByName(pipelineConfig.name())).thenReturn(pipelineConfig);
        when(entityHashingService.md5ForEntity(pipelineConfig, "group1")).thenReturn("md5");

        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, entityHashingService,
                pipelineConfig, "group2", username, "md5", localizedOperationResult, externalArtifactsService);

        boolean canContinue = command.canContinue(cruiseConfig);

        assertTrue(canContinue);
    }
}
