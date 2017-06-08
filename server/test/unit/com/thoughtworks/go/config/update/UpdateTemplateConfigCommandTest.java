/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateTemplateConfigCommandTest {

    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private SecurityService securityService;

    private LocalizedOperationResult result;
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PipelineTemplateConfig pipelineTemplateConfig;
    private Authorization authorization;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        result = new HttpLocalizedOperationResult();
        pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("user"))));
        pipelineTemplateConfig.setAuthorization(authorization);
    }

    @Test
    public void shouldUpdateExistingTemplate() throws Exception {
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig), is(true));
    }

    @Test
    public void shouldAllowSubmittingInvalidElasticProfileId() throws Exception {
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.stageConfig("stage", new JobConfigs(new JobConfig("job"))));
        JobConfig jobConfig = updatedTemplateConfig.findBy(new CaseInsensitiveString("stage")).jobConfigByConfigName(new CaseInsensitiveString("job"));
        jobConfig.setElasticProfileId("invalidElasticProfileId");

        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        command.update(cruiseConfig);
        assertThat(command.isValid(cruiseConfig), is(true));
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig), is(true));
    }

    @Test
    public void shouldValidateElasticProfileIdWhenTemplateIsUsedInAPipeline() throws Exception {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        PipelineConfig up42 = PipelineConfigMother.pipelineConfigWithTemplate("up42", pipelineTemplateConfig.name().toString());
        cruiseConfig.addPipeline("first", up42);

        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.stageConfig("stage", new JobConfigs(new JobConfig("job"))));
        JobConfig jobConfig = updatedTemplateConfig.findBy(new CaseInsensitiveString("stage")).jobConfigByConfigName(new CaseInsensitiveString("job"));
        jobConfig.setElasticProfileId("invalidElasticProfileId");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        command.update(cruiseConfig);
        MagicalGoConfigXmlLoader.preprocess(cruiseConfig);
        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(updatedTemplateConfig.getAllErrors().size(), is(1));
        String message = "No profile defined corresponding to profile_id 'invalidElasticProfileId'";
        assertThat(updatedTemplateConfig.getAllErrors().get(0).asString(), is(message));
    }

    @Test
    public void shouldThrowAnExceptionIfTemplateConfigNotFound() throws Exception {
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);

        thrown.expectMessage("The template with name 'template' is not found.");
        command.update(cruiseConfig);
    }

    @Test
    public void shouldCopyOverAuthorizationAsIsWhileUpdatingTemplateStageConfig() throws Exception {
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));;
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig), is(true));
        assertThat(cruiseConfig.getTemplateByName(updatedTemplateConfig.name()).getAuthorization(), is(authorization));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        CaseInsensitiveString templateName = new CaseInsensitiveString("template");
        PipelineTemplateConfig oldTemplate = new PipelineTemplateConfig(templateName, StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(oldTemplate);
        when(entityHashingService.md5ForEntity(oldTemplate)).thenReturn("md5");
        when(securityService.isAuthorizedToEditTemplate(templateName, currentUser)).thenReturn(false);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldContinueWithConfigSaveifUserIsAuthorized() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("template"), currentUser)).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineTemplateConfig)).thenReturn("md5");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(entityHashingService.md5ForEntity(pipelineTemplateConfig)).thenReturn("another-md5");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("STALE_RESOURCE_CONFIG"));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfObjectIsNotFound() {
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, "md5", entityHashingService);

        thrown.expectMessage("The template with name 'template' is not found.");
        command.canContinue(cruiseConfig);
    }
}
