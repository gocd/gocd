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
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DeleteTemplateConfigCommandTest {

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
        pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        result = new HttpLocalizedOperationResult();
    }

    @Test
    public void shouldDeleteTemplateFromTheGivenConfig() throws Exception {
        cruiseConfig.addTemplate(pipelineTemplateConfig);
        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, goConfigService, currentUser);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
    }

    @Test
    public  void shouldValidateWhetherTemplateIsAssociatedWithPipelines() {
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", pipelineTemplateConfig.name().toString(), "s1", "j1");
        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, goConfigService, currentUser);

        thrown.expectMessage("The template 'template' is being referenced by pipeline(s): [p1]");
        assertThat(command.isValid(cruiseConfig), is(false));
    }


    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, goConfigService, currentUser);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldNotContinueWhenTemplateNoLongerExists() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(pipelineTemplateConfig, result, goConfigService, currentUser);

        assertThat(command.canContinue(cruiseConfig), is(false));
    }

}