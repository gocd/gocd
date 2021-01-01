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
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class TemplateConfigCommandTest {

    @Mock
    private SecurityService securityService;

    @Mock
    private ExternalArtifactsService externalArtifactsService;

    @Mock
    private LocalizedOperationResult result;

    private BasicCruiseConfig cruiseConfig;
    private Username currentUser;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setup() {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
    }

    @Test
    public void shouldValidateTemplateName() {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("@#$#"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        cruiseConfig.addTemplate(templateConfig);
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(templateConfig.errors().getAllOn("name"), is(Arrays.asList("Invalid template name '@#$#'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.")));
    }

    @Test
    public void shouldValidateIfTemplateNameIsNull() {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(null, StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        cruiseConfig.addTemplate(templateConfig);
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        thrown.expectMessage("Template name cannot be null.");
        command.isValid(cruiseConfig);
    }

    @Test
    public void shouldThrowAnExceptionIfTemplateConfigCannotBeFound() {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("non-existent-template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        thrown.expectMessage(EntityType.Template.notFoundMessage(templateConfig.name()));
        command.isValid(cruiseConfig);
        assertThat(result.toString(), containsString(EntityType.Template.notFoundMessage(templateConfig.name())));
    }

    @Test
    public void shouldValidateUniquenessOfTemplateName() {
        PipelineTemplateConfig templateConfig1 = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        PipelineTemplateConfig templateConfig2 = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        cruiseConfig.addTemplate(templateConfig1);
        cruiseConfig.addTemplate(templateConfig2);
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig2, currentUser, securityService, result, externalArtifactsService);
        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(templateConfig2.errors().getAllOn("name"), is(Arrays.asList("Template name 'template' is not unique")));
    }

    @Test
    public void shouldValidateStageNameUniquenessWithinATemplate() {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        cruiseConfig.addTemplate(templateConfig);
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(templateConfig.getStage(new CaseInsensitiveString("stage")).errors().getAllOn("name"), is(Arrays.asList("You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique.")));
    }


}
