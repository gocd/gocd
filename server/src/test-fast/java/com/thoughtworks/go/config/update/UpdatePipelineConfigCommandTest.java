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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
        localizedOperationResult = new HttpLocalizedOperationResult();
        pipelineConfig = PipelineConfigMother.pipelineConfig("p1");
        when(username.getUsername()).thenReturn(new CaseInsensitiveString("Bob"));
    }

    @Test
    void shouldDisallowStaleRequest() {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, entityHashingService,
                pipelineConfig, "group1", username, "stale_digest", localizedOperationResult, externalArtifactsService);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(goConfigService.canEditPipeline(pipelineConfig.name().toString(), username, localizedOperationResult, "group1")).thenReturn(true);
        when(entityHashingService.hashForEntity(pipelineConfig, "group1")).thenReturn("latest_digest");

        BasicCruiseConfig basicCruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig));
        assertThat(command.canContinue(basicCruiseConfig)).isFalse();
    }

    @Test
    void shouldDisallowUpdateIfPipelineEditIsDisAllowed() throws Exception {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, "", username, "stale_digest", localizedOperationResult, externalArtifactsService);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(goConfigService.canEditPipeline(pipelineConfig.name().toString(), username, localizedOperationResult, "group1")).thenReturn(false);
        assertThat(command.canContinue(mock(CruiseConfig.class))).isFalse();

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.forbidden("User 'Bob' does not have permission to edit pipeline with name 'p1'", HealthStateType.forbidden());

        assertThat(localizedOperationResult).isEqualTo(expectedResult);
    }

    @Test
    void shouldInvokeUpdateMethodOfCruiseConfig() throws Exception {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, "group1", username, "stale_digest", localizedOperationResult, externalArtifactsService);

        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");

        command.update(cruiseConfig);
        verify(cruiseConfig).update("group1", pipelineConfig.name().toString(), pipelineConfig);
    }

    @Test
    void shouldEncryptSecurePropertiesOfPipelineConfig() {
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, "group1", username, "stale_digest", localizedOperationResult, externalArtifactsService);

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
                pipeline, "group", username, "stale_digest", localizedOperationResult, externalArtifactsService);

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
                pipeline, "group", username, "stale_digest", localizedOperationResult, externalArtifactsService);

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

        assertThat(pipelineGroups.hasGroup("updated_group")).isFalse();

        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, "updated_group", username, "stale_digest", localizedOperationResult, externalArtifactsService);

        command.update(cruiseConfig);
        verify(cruiseConfig).update("group1", pipelineConfig.name().toString(), pipelineConfig);

        assertThat(pipelineGroups.findGroup("group1").size()).isEqualTo(0);
        assertThat(pipelineGroups.hasGroup("updated_group")).isTrue();
        assertThat(pipelineGroups.findGroup("updated_group").size()).isEqualTo(1);
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
                pipelineConfig, "group2", username, "digest", localizedOperationResult, externalArtifactsService);

        boolean canContinue = command.canContinue(cruiseConfig);

        assertThat(canContinue).isFalse();

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.forbidden("User 'Bob' does not have permission to edit pipeline group with name 'group2'", HealthStateType.forbidden());

        assertThat(localizedOperationResult).isEqualTo(expectedResult);
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
        when(entityHashingService.hashForEntity(pipelineConfig, "group1")).thenReturn("digest");

        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, entityHashingService,
                pipelineConfig, "group2", username, "digest", localizedOperationResult, externalArtifactsService);

        boolean canContinue = command.canContinue(cruiseConfig);

        assertThat(canContinue).isTrue();
    }

    @Test
    void shouldReturnInvalidIfTheOnCancelTaskHasAnError() {
        JobConfig job = new JobConfig(new CaseInsensitiveString("job_name"));
        ExecTask task = new ExecTask("ls", "", "working_dir");
        task.setCancelTask(new ExecTask("", "", ""));
        job.addTask(task);
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job)));

        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null, pipeline, "group", username, "stale_digest", localizedOperationResult, externalArtifactsService);
        BasicCruiseConfig preprocessedConfig = GoConfigMother.defaultCruiseConfig();
        preprocessedConfig.addPipelineWithoutValidation("group", pipeline);

        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("P1"))).thenReturn("group");

        boolean isValid = command.isValid(preprocessedConfig);

        assertThat(isValid).isFalse();
        Task firstTask = preprocessedConfig.pipelineConfigByName(new CaseInsensitiveString("P1")).getFirstStageConfig().getJobs().get(0).getTasks().get(0);
        assertThat(firstTask.cancelTask().errors().size()).isEqualTo(1);
        assertThat(firstTask.cancelTask().errors().on(ExecTask.COMMAND)).isEqualTo("Command cannot be empty");
    }
}
