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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateTemplateConfigCommandTest {

    @Mock
    private SecurityService securityService;

    @Mock
    private ExternalArtifactsService externalArtifactsService;


    private HttpLocalizedOperationResult result;

    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PipelineTemplateConfig pipelineTemplateConfig;

    @BeforeEach
    public void setup() {
        result = new HttpLocalizedOperationResult();
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
    }

    @Test
    public void shouldAddNewTemplateToGivenConfig() throws Exception {
        CreateTemplateConfigCommand createTemplateConfigCommand = new CreateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        createTemplateConfigCommand.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
    }

    @Test
    public void shouldAddNewTemplateToConfigWithAuthorizationSetForGroupAdmin() throws Exception {
        when(securityService.isUserGroupAdmin(currentUser)).thenReturn(true);
        CreateTemplateConfigCommand createTemplateConfigCommand = new CreateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        createTemplateConfigCommand.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        assertThat(pipelineTemplateConfig.getAuthorization(), is(new Authorization(new AdminsConfig(new AdminUser(currentUser.getUsername())))));
    }

    @Test
    public void shouldAddNewTemplateToConfigWithoutAuthorizationForSuperAdmin() throws Exception {
        when(securityService.isUserGroupAdmin(currentUser)).thenReturn(false);
        CreateTemplateConfigCommand createTemplateConfigCommand = new CreateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, externalArtifactsService);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(false));
        createTemplateConfigCommand.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        assertThat(pipelineTemplateConfig.getAuthorization(), is(new Authorization()));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(securityService.isUserAdmin(currentUser)).thenReturn(false);

        CreateTemplateConfigCommand command = new CreateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, externalArtifactsService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), equalTo(EntityType.Template.forbiddenToEdit(pipelineTemplateConfig.name(), currentUser.getUsername())));
    }

    @Test
    public void shouldContinueWithConfigSaveIsUserIsAGroupAdmin() {
        when(securityService.isUserAdmin(currentUser)).thenReturn(false);
        when(securityService.isUserGroupAdmin(currentUser)).thenReturn(true);

        CreateTemplateConfigCommand command = new CreateTemplateConfigCommand(pipelineTemplateConfig, currentUser, securityService, result, externalArtifactsService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }


    @Test
    public void shouldEncryptSecurePropertiesOfPipelineConfig() {
        PipelineTemplateConfig template = mock(PipelineTemplateConfig.class);
        CreateTemplateConfigCommand command = new CreateTemplateConfigCommand(template, null, securityService, result, externalArtifactsService);

        when(template.name()).thenReturn(new CaseInsensitiveString("p1"));
        CruiseConfig preprocessedConfig = mock(CruiseConfig.class);
        when(preprocessedConfig.findTemplate(new CaseInsensitiveString("p1"))).thenReturn(template);

        command.encrypt(preprocessedConfig);

        verify(template).encryptSecureProperties(eq(preprocessedConfig), any(PipelineTemplateConfig.class));
    }
}
