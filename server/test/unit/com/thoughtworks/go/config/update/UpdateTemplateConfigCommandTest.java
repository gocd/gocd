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
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.helper.GoConfigMother;
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        result = new HttpLocalizedOperationResult();
        pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
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
    public void shouldThrowAnExceptionIfTemplateConfigNotFound() throws Exception {
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        thrown.expectMessage("The template with name 'template' is not found.");
        command.update(cruiseConfig);
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineTemplateConfig)).thenReturn("another-md5");

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("STALE_RESOURCE_CONFIG"));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfObjectIsNotFound() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(pipelineTemplateConfig, currentUser, goConfigService, result, "md5", entityHashingService);

        thrown.expectMessage("The template with name 'template' is not found.");
        command.canContinue(cruiseConfig);
    }
}
