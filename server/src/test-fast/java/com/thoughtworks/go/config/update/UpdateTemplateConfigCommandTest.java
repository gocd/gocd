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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
class UpdateTemplateConfigCommandTest {

    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private ExternalArtifactsService externalArtifactsService;

    @Mock
    private SecurityService securityService;

    private HttpLocalizedOperationResult result;
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PipelineTemplateConfig pipelineTemplateConfig;
    private Authorization authorization;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeEach
    void setup() {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        result = new HttpLocalizedOperationResult();
        pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("user"))));
        pipelineTemplateConfig.setAuthorization(authorization);
    }

    @Test
    void shouldUpdateExistingTemplate() {
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig)).isTrue();
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig)).isFalse();
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig)).isTrue();
    }

    @Test
    void shouldAllowSubmittingInvalidElasticProfileId() {
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.stageConfig("stage", new JobConfigs(new JobConfig("job"))));
        JobConfig jobConfig = updatedTemplateConfig.findBy(new CaseInsensitiveString("stage")).jobConfigByConfigName(new CaseInsensitiveString("job"));
        jobConfig.setElasticProfileId("invalidElasticProfileId");

        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig)).isTrue();
        command.update(cruiseConfig);
        assertThat(command.isValid(cruiseConfig)).isTrue();
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig)).isFalse();
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig)).isTrue();
    }

    @Test
    void shouldValidateElasticProfileIdWhenTemplateIsUsedInAPipeline() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        PipelineConfig up42 = PipelineConfigMother.pipelineConfigWithTemplate("up42", pipelineTemplateConfig.name().toString());
        cruiseConfig.addPipeline("first", up42);

        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.stageConfig("stage", new JobConfigs(new JobConfig("job"))));
        JobConfig jobConfig = updatedTemplateConfig.findBy(new CaseInsensitiveString("stage")).jobConfigByConfigName(new CaseInsensitiveString("job"));
        jobConfig.setElasticProfileId("invalidElasticProfileId");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig)).isTrue();
        command.update(cruiseConfig);
        MagicalGoConfigXmlLoader.preprocess(cruiseConfig);
        assertThat(command.isValid(cruiseConfig)).isFalse();
        assertThat(updatedTemplateConfig.getAllErrors().size()).isEqualTo(1);
        String message = "No profile defined corresponding to profile_id 'invalidElasticProfileId'";
        assertThat(updatedTemplateConfig.getAllErrors().get(0).asString()).isEqualTo(message);
    }

    @Test
    void shouldThrowAnExceptionIfTemplateConfigNotFound() {
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);

        thrown.expect(RecordNotFoundException.class);
        thrown.expectMessage(EntityType.Template.notFoundMessage(pipelineTemplateConfig.name()));
        command.update(cruiseConfig);
    }

    @Test
    void shouldCopyOverAuthorizationAsIsWhileUpdatingTemplateStageConfig() {
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));;
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig)).isFalse();
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig)).isTrue();
        assertThat(cruiseConfig.getTemplateByName(updatedTemplateConfig.name()).getAuthorization()).isEqualTo(authorization);
    }

    @Test
    void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        CaseInsensitiveString templateName = new CaseInsensitiveString("template");
        PipelineTemplateConfig oldTemplate = new PipelineTemplateConfig(templateName, StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(oldTemplate);
        when(entityHashingService.md5ForEntity(oldTemplate)).thenReturn("md5");
        when(securityService.isAuthorizedToEditTemplate(templateName, currentUser)).thenReturn(false);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.Template.forbiddenToEdit(pipelineTemplateConfig.name(), currentUser.getUsername()));
    }

    @Test
    void shouldContinueWithConfigSaveifUserIsAuthorized() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("template"), currentUser)).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineTemplateConfig)).thenReturn("md5");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);

        assertThat(command.canContinue(cruiseConfig)).isTrue();
    }

    @Test
    void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(entityHashingService.md5ForEntity(pipelineTemplateConfig)).thenReturn("another-md5");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result.toString()).contains("Someone has modified the configuration for");
    }

    @Test
    void shouldNotContinueWithConfigSaveIfObjectIsNotFound() {
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService, externalArtifactsService);

        thrown.expectMessage(EntityType.Template.notFoundMessage(pipelineTemplateConfig.name()));
        command.canContinue(cruiseConfig);
    }

    @Test
    void shouldEncryptSecurePropertiesOfPipelineConfig() {
        PipelineTemplateConfig pipelineTemplateConfig = mock(PipelineTemplateConfig.class);
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, null,
                securityService, result, "stale_md5", entityHashingService, externalArtifactsService);

        when(pipelineTemplateConfig.name()).thenReturn(new CaseInsensitiveString("p1"));
        CruiseConfig preprocessedConfig = mock(CruiseConfig.class);
        when(preprocessedConfig.findTemplate(new CaseInsensitiveString("p1"))).thenReturn(pipelineTemplateConfig);

        command.encrypt(preprocessedConfig);

        verify(pipelineTemplateConfig).encryptSecureProperties(eq(preprocessedConfig), any(PipelineTemplateConfig.class));
    }

    @Test
    void updateTemplateConfigShouldValidateAllExternalArtifacts() {
        PluggableArtifactConfig s3 = new PluggableArtifactConfig("id1", "s3");
        PluggableArtifactConfig docker = new PluggableArtifactConfig("id2", "docker");

        JobConfig job1 = JobConfigMother.jobWithNoResourceRequirement();
        JobConfig job2 = JobConfigMother.jobWithNoResourceRequirement();

        job1.artifactTypeConfigs().add(s3);
        job2.artifactTypeConfigs().add(docker);

        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(template, null,
                securityService, result, "stale_md5", entityHashingService, externalArtifactsService);

        BasicCruiseConfig preprocessedConfig = GoConfigMother.defaultCruiseConfig();
        preprocessedConfig.addTemplate(template);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "P1");
        preprocessedConfig.addPipelineWithoutValidation("group", pipelineConfig);
        ArtifactStores artifactStores = mock(ArtifactStores.class);
        preprocessedConfig.setArtifactStores(artifactStores);
        when(artifactStores.find(anyString())).thenReturn(new ArtifactStore("id", "pluginId"));
        new TemplateExpansionPreprocessor().process(preprocessedConfig);

        command.isValid(preprocessedConfig);

        verify(externalArtifactsService, times(2)).validateExternalArtifactConfig(any(PluggableArtifactConfig.class), eq(new ArtifactStore("id", "pluginId")), eq(true));
    }

    @Test
    void updateTemplateConfigShouldValidateAllFetchExternalArtifactTasks() {
        JobConfig job1 = JobConfigMother.jobWithNoResourceRequirement();
        JobConfig job2 = JobConfigMother.jobWithNoResourceRequirement();

        FetchPluggableArtifactTask fetchS3Task = new FetchPluggableArtifactTask(new CaseInsensitiveString("p0"), new CaseInsensitiveString("s0"), new CaseInsensitiveString("j0"), "s3");
        FetchPluggableArtifactTask fetchDockerTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("p0"), new CaseInsensitiveString("s0"), new CaseInsensitiveString("j0"), "docker");

        job1.addTask(fetchS3Task);
        job2.addTask(fetchDockerTask);

        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(template, null,
                securityService, result, "stale_md5", entityHashingService, externalArtifactsService);

        BasicCruiseConfig preprocessedConfig = GoConfigMother.defaultCruiseConfig();
        preprocessedConfig.addTemplate(template);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "P1");
        preprocessedConfig.addPipelineWithoutValidation("group", pipelineConfig);
        new TemplateExpansionPreprocessor().process(preprocessedConfig);

        command.isValid(preprocessedConfig);

        verify(externalArtifactsService, times(2)).validateFetchExternalArtifactTask(any(FetchPluggableArtifactTask.class), any(PipelineTemplateConfig.class), eq(preprocessedConfig));
    }
}
