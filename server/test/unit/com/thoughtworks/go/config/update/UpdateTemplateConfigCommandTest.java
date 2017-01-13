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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
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
    private GoConfigService goConfigService;

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

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig), is(true));
    }

    @Test
    public void shouldNotAllowEditingOfStageNameUsedAsAMaterialByAnotherPipeline() throws Exception {
        PipelineConfig up42 = PipelineConfigMother.pipelineConfigWithTemplate("up42", pipelineTemplateConfig.name().toString());
        PipelineConfig dependentPipeline = PipelineConfigMother.createPipelineConfig("depenedent", "defaultStage");
        dependentPipeline.addMaterialConfig(new DependencyMaterialConfig(up42.name(), pipelineTemplateConfig.getStages().get(0).name()));

        cruiseConfig.addPipeline("first", up42);
        cruiseConfig.addPipeline("first", dependentPipeline);

        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        command.update(cruiseConfig);

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(updatedTemplateConfig.getAllErrors().size(), is(1));
        String message = "Can not update stage name as it is used as a material `up42 [stage]` in pipeline `depenedent`";
        assertThat(updatedTemplateConfig.getAllErrors().get(0).asString(), is(message));
    }

    @Test
    public void shouldAllowEditingOfStageNameWhenItIsNotUsedAsDependencyMaterial() throws Exception {
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig), is(true));
    }

    @Test
    public void shouldThrowAnExceptionIfTemplateConfigNotFound() throws Exception {
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        thrown.expectMessage("The template with name 'template' is not found.");
        command.update(cruiseConfig);
    }

    @Test
    public void shouldCopyOverAuthorizationAsIsWhileUpdatingTemplateStageConfig() throws Exception {
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));;
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(updatedTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig), is(true));
        assertThat(cruiseConfig.getTemplateByName(updatedTemplateConfig.name()).getAuthorization(), is(authorization));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(goConfigService.isAuthorizedToEditTemplate("template", currentUser)).thenReturn(false);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldContinueWithConfigSaveifUserIsAuthorized() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(goConfigService.isAuthorizedToEditTemplate("template", currentUser)).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineTemplateConfig)).thenReturn("md5");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(goConfigService.isAuthorizedToEditTemplate("template", currentUser)).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineTemplateConfig)).thenReturn("another-md5");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("STALE_RESOURCE_CONFIG"));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfObjectIsNotFound() {
        when(goConfigService.isAuthorizedToEditTemplate("template", currentUser)).thenReturn(true);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        thrown.expectMessage("The template with name 'template' is not found.");
        command.canContinue(cruiseConfig);
    }
}
