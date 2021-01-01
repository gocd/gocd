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
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DeleteTemplateConfigCommandTest {

    @Mock
    private SecurityService securityService;

    @Mock
    private ExternalArtifactsService externalArtifactsService;

    private HttpLocalizedOperationResult result;
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PipelineTemplateConfig pipelineTemplateConfig;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setup() {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        result = new HttpLocalizedOperationResult();
    }

    @Test
    public void shouldDeleteTemplateFromTheGivenConfig() throws Exception {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, securityService, currentUser, externalArtifactsService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
    }

    @Test
    public  void shouldValidateWhetherTemplateIsAssociatedWithPipelines() {
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", pipelineTemplateConfig.name().toString(), "s1", "j1");
        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, securityService, currentUser, externalArtifactsService);

        thrown.expectMessage("The template 'template' is being referenced by pipeline(s): [p1]");
        assertThat(command.isValid(cruiseConfig), is(false));
    }


    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("template"), currentUser)).thenReturn(false);

        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, securityService, currentUser, externalArtifactsService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), equalTo(EntityType.Template.forbiddenToDelete(pipelineTemplateConfig.name(), currentUser.getUsername())));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAuthorized() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("template"), currentUser)).thenReturn(true);

        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, securityService, currentUser, externalArtifactsService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldNotContinueWhenTemplateNoLongerExists() {
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("template"), currentUser)).thenReturn(true);

        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, securityService, currentUser, externalArtifactsService);

        assertThat(command.canContinue(cruiseConfig), is(false));
    }

}
