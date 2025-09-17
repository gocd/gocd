/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TemplateConfigCommandTest {

    @Mock
    private SecurityService securityService;

    @Mock
    private ExternalArtifactsService externalArtifactsService;

    @Mock
    private LocalizedOperationResult result;

    private BasicCruiseConfig cruiseConfig;
    private Username currentUser;

    @BeforeEach
    public void setup() {
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldValidateTemplateName() {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("@#$#"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        cruiseConfig.addTemplate(templateConfig);
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThat(command.isValid(cruiseConfig)).isFalse();
        assertThat(templateConfig.errors().getAllOn("name")).isEqualTo(List.of("Invalid template name '@#$#'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateIfTemplateNameIsNull() {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(null, StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        cruiseConfig.addTemplate(templateConfig);
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .hasMessageContaining("Template name cannot be null.");
    }

    @Test
    public void shouldThrowAnExceptionIfTemplateConfigCannotBeFound() {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("non-existent-template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThatThrownBy(() -> command.isValid(cruiseConfig));
    }

    @Test
    public void shouldValidateUniquenessOfTemplateName() {
        PipelineTemplateConfig templateConfig1 = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        PipelineTemplateConfig templateConfig2 = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        cruiseConfig.addTemplate(templateConfig1);
        cruiseConfig.addTemplate(templateConfig2);
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig2, currentUser, securityService, result, externalArtifactsService);
        assertThat(command.isValid(cruiseConfig)).isFalse();
        assertThat(templateConfig2.errors().getAllOn("name")).isEqualTo(List.of("Template name 'template' is not unique"));
    }

    @Test
    public void shouldValidateStageNameUniquenessWithinATemplate() {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        cruiseConfig.addTemplate(templateConfig);
        TemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThat(command.isValid(cruiseConfig)).isFalse();
        assertThat(templateConfig.getStage(new CaseInsensitiveString("stage")).errors().getAllOn("name")).isEqualTo(List.of("You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique."));
    }


}
