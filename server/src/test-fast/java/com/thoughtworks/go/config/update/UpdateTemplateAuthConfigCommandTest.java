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
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateTemplateAuthConfigCommandTest {
    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private ExternalArtifactsService externalArtifactsService;

    @Mock
    private SecurityService securityService;

    private PipelineTemplateConfig pipelineTemplateConfig;
    private Authorization authorization;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        initMocks(this);
        pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("user"))));
        pipelineTemplateConfig.setAuthorization(authorization);
    }

    @Test
    public void shouldReplaceOnlyTemplateAuthorizationWhileUpdatingTheTemplate() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));;
        Authorization templateAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("foo"))));
        updatedTemplateConfig.setAuthorization(templateAuthorization);
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateAuthConfigCommand command = new UpdateTemplateAuthConfigCommand(updatedTemplateConfig, templateAuthorization, new Username(new CaseInsensitiveString("user")), securityService, new HttpLocalizedOperationResult(), "md5", entityHashingService, externalArtifactsService);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig), is(false));
        Authorization expectedTemplateAuthorization = cruiseConfig.getTemplateByName(pipelineTemplateConfig.name()).getAuthorization();
        assertNotEquals(expectedTemplateAuthorization, authorization);
        assertThat(expectedTemplateAuthorization, is(templateAuthorization));

    }

    @Test
    public void shouldCopyOverErrorsOnAuthorizationFromThePreprocessedTemplateConfig() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));;
        Authorization templateAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString(""))));
        updatedTemplateConfig.setAuthorization(templateAuthorization);
        cruiseConfig.addTemplate(updatedTemplateConfig);

        UpdateTemplateAuthConfigCommand command = new UpdateTemplateAuthConfigCommand(updatedTemplateConfig, templateAuthorization, new Username(new CaseInsensitiveString("user")), securityService, new HttpLocalizedOperationResult(), "md5", entityHashingService, externalArtifactsService);
        assertFalse(command.isValid(cruiseConfig));

        assertThat(templateAuthorization.getAllErrors().get(0).getAllOn("roles"), is(Arrays.asList("Role \"\" does not exist.")));
    }
}
