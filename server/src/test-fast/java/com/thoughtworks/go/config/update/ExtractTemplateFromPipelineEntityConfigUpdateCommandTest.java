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
import com.thoughtworks.go.config.exceptions.ConflictException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExtractTemplateFromPipelineEntityConfigUpdateCommandTest {

    @Test
    void shouldUpdateConfigWhenCurrentUserIsGroupAdmin() throws Exception {
        SecurityService securityService = mock(SecurityService.class);
        Username currentUser = new Username("bob");
        when(securityService.isUserGroupAdmin(currentUser)).thenReturn(true);

        ExtractTemplateFromPipelineEntityConfigUpdateCommand command = new ExtractTemplateFromPipelineEntityConfigUpdateCommand(securityService, "some-pipeline", "new-template", currentUser);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addPipeline("blah", PipelineConfigMother.pipelineConfig("some-pipeline"));

        assertThat(cruiseConfig.getTemplates()).isEmpty();
        command.update(cruiseConfig);

        PipelineTemplateConfig template = cruiseConfig.getTemplates().templateByName(new CaseInsensitiveString("new-template"));

        assertThat(template)
                .isNotNull()
                .hasSize(1)
                .element(0).satisfies(stageConfig -> assertThat(stageConfig.name()).isEqualTo(new CaseInsensitiveString("mingle")));

        assertThat(template.getAuthorization()).isEqualTo(new Authorization(new AdminsConfig(new AdminUser(currentUser.getUsername()))));
    }

    @Test
    void shouldUpdateConfigWhenCurrentUserIsNotGroupAdmin() throws Exception {
        SecurityService securityService = mock(SecurityService.class);
        Username currentUser = new Username("bob");
        when(securityService.isUserGroupAdmin(currentUser)).thenReturn(false);

        ExtractTemplateFromPipelineEntityConfigUpdateCommand command = new ExtractTemplateFromPipelineEntityConfigUpdateCommand(securityService, "some-pipeline", "new-template", currentUser);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addPipeline("blah", PipelineConfigMother.pipelineConfig("some-pipeline"));

        assertThat(cruiseConfig.getTemplates()).isEmpty();
        command.update(cruiseConfig);

        PipelineTemplateConfig template = cruiseConfig.getTemplates().templateByName(new CaseInsensitiveString("new-template"));

        assertThat(template)
                .isNotNull()
                .hasSize(1)
                .element(0).satisfies(stageConfig -> assertThat(stageConfig.name()).isEqualTo(new CaseInsensitiveString("mingle")));

        assertThat(template.getAuthorization()).isEqualTo(new Authorization());
    }

    @Nested
    class CanContinue {

        @Test
        void shouldReturnTrueIfEverythingIsGood() {
            ExtractTemplateFromPipelineEntityConfigUpdateCommand command = new ExtractTemplateFromPipelineEntityConfigUpdateCommand(null, "some-pipeline", "new-template", null);
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("blah", PipelineConfigMother.pipelineConfig("some-pipeline"));
            assertThat(command.canContinue(cruiseConfig)).isTrue();
        }

        @Test
        void shouldBlowUpIfPipelineDoesNotExist() {
            ExtractTemplateFromPipelineEntityConfigUpdateCommand command = new ExtractTemplateFromPipelineEntityConfigUpdateCommand(null, "non-existent-pipeline", "new-template", null);
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();

            assertThatCode(() -> command.canContinue(cruiseConfig))
                    .isInstanceOf(RecordNotFoundException.class)
                    .hasMessage("Pipeline with name 'non-existent-pipeline' was not found!");
        }

        @Test
        void shouldBlowUpIfPipelineAlreadyUsesTemplateDoesNotExist() {
            ExtractTemplateFromPipelineEntityConfigUpdateCommand command = new ExtractTemplateFromPipelineEntityConfigUpdateCommand(null, "some-pipeline", "new-template", null);
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("blah", PipelineConfigMother.pipelineConfigWithTemplate("some-pipeline", "blah-template"));

            assertThatCode(() -> command.canContinue(cruiseConfig))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Pipeline with name 'some-pipeline' already uses a template.");
        }

        @Test
        void shouldBlowUpIfTemplateWithSameNameAlreadyExist() {
            ExtractTemplateFromPipelineEntityConfigUpdateCommand command = new ExtractTemplateFromPipelineEntityConfigUpdateCommand(null, "some-pipeline", "existing-template", null);
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("blah", PipelineConfigMother.pipelineConfig("some-pipeline"));
            cruiseConfig.addTemplate(PipelineTemplateConfigMother.createTemplate("existing-template"));

            assertThatCode(() -> command.canContinue(cruiseConfig))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Template with name 'existing-template' already exists!");
        }
    }
}
