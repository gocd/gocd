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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.tasks.PluggableTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class TemplateConfigServiceTest {
    public GoConfigService goConfigService;
    private SecurityService securityService;
    private TemplateConfigService service;

    @BeforeEach
    public void setup() {
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        service = new TemplateConfigService(goConfigService, securityService, mock(EntityHashingService.class), mock(PluggableTaskService.class), mock(ExternalArtifactsService.class));
    }

    @Test
    public void shouldReturnAListOfTemplatesWithAssociatedPipelinesForAnAdminUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p2", "t2", "s2", "j2");

        Username admin = new Username("admin");

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(goConfigService.isPipelineEditable(cis("p1"))).thenReturn(true);
        when(goConfigService.isPipelineEditable(cis("p2"))).thenReturn(true);
        when(securityService.isAuthorizedToViewTemplate(cis("t1"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToViewTemplate(cis("t2"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(cis("t1"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(cis("t2"), admin)).thenReturn(true);
        when(securityService.isUserAdmin(admin)).thenReturn(true);

        List<TemplateToPipelines> templateToPipelines = new ArrayList<>();
        TemplateToPipelines template1 = new TemplateToPipelines(cis("t1"), true, true);
        template1.add(new PipelineEditabilityInfo(cis("p1"), true, true));
        templateToPipelines.add(template1);
        TemplateToPipelines template2 = new TemplateToPipelines(cis("t2"), true, true);
        template2.add(new PipelineEditabilityInfo(cis("p2"), true, true));
        templateToPipelines.add(template2);

        assertThat(service.getTemplatesList(admin)).isEqualTo(templateToPipelines);
    }

    @Test
    public void shouldReturnASubsetOfTemplatesWithAssociatedPipelinesForTemplateAdmin() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateAdmin = cis("template-admin");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", new Authorization(new AdminsConfig(new AdminUser(templateAdmin))), StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        Username admin = new Username(templateAdmin);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(cis("t2"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(cis("t2"), admin)).thenReturn(true);
        when(securityService.isUserAdmin(admin)).thenReturn(false);

        List<TemplateToPipelines> templateToPipelines = new ArrayList<>();
        TemplateToPipelines t2 = new TemplateToPipelines(cis("t2"), true, false);
        templateToPipelines.add(t2);

        assertThat(service.getTemplatesList(admin)).isEqualTo(templateToPipelines);
    }

    @Test
    public void shouldReturnASubsetOfTemplatesWithAssociatedPipelinesForTemplateViewUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateViewUser = cis("template-view");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", new Authorization(new ViewConfig(new AdminUser(templateViewUser))), StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        Username templateView = new Username(templateViewUser);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(cis("t2"), templateView)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(cis("t2"), templateView)).thenReturn(false);
        when(securityService.isUserAdmin(templateView)).thenReturn(false);

        List<TemplateToPipelines> templateToPipelines = new ArrayList<>();
        TemplateToPipelines t2 = new TemplateToPipelines(cis("t2"), false, false);
        templateToPipelines.add(t2);

        assertThat(service.getTemplatesList(templateView)).isEqualTo(templateToPipelines);
    }

    @Test
    public void shouldReturnASubsetOfTemplatesWithAssociatedPipelinesForGroupAdmin() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString groupAdmin = cis("group-admin");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineConfigs pipelineConfigs = cruiseConfig.findGroup("defaultGroup"); //defaultGroup is set in GoConfigMother
        pipelineConfigs.setAuthorization(new Authorization(new AdminsConfig(new AdminUser(groupAdmin))));

        Username groupAdminUser = new Username(groupAdmin);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(goConfigService.isPipelineEditable(cis("p1"))).thenReturn(true);
        when(securityService.isAuthorizedToViewTemplate(cis("t1"), groupAdminUser)).thenReturn(true);

        List<TemplateToPipelines> templateToPipelines = new ArrayList<>();
        TemplateToPipelines t1 = new TemplateToPipelines(cis("t1"), false, false);
        t1.add(new PipelineEditabilityInfo(cis("p1"), true, true));
        templateToPipelines.add(t1);

        assertThat(service.getTemplatesList(groupAdminUser)).isEqualTo(templateToPipelines);
    }

    @Test
    public void shouldReturnAnEmptyListOfTemplatesForARegularUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString regularUser = cis("view");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");

        Username viewUser = new Username(regularUser);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(cis("t1"), viewUser)).thenReturn(false);

        assertThat(service.getTemplatesList(viewUser)).isEqualTo(new ArrayList<>());
    }

    @Test
    public void shouldLoadTemplateForViewing() {
        PipelineTemplateConfig template = template("first_template");
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(createPipelineWithTemplate("first", template), createPipelineWithTemplate("second", template)));
        cruiseConfig.addTemplate(template);
        when(goConfigService.findTemplateByName(template.name())).thenReturn(template);
        PipelineTemplateConfig actual = service.loadForView(template.name().toString(), new HttpLocalizedOperationResult());

        assertThat(template).isEqualTo(actual);
    }

    @Test
    public void shouldPopulateErrorInResultOnFailure() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(cis("user"));
        String templateName = "template-name";
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig(cis(templateName), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        String errorMessage = "invalid template";
        doThrow(new GoConfigInvalidException(GoConfigMother.defaultCruiseConfig(), errorMessage)).when(goConfigService).updateConfig(any(), any());

        service.createTemplateConfig(user, pipelineTemplateConfig, result);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(EntityType.Template.entityConfigValidationFailed(templateName, errorMessage));

        assertThat(result.toString()).isEqualTo(expectedResult.toString());
    }

    @Test
    public void shouldReturnAllTemplatesThatCanBeEditedByUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString username = cis("template-edit-user");

        PipelineTemplateConfig editableTemplate = PipelineTemplateConfigMother.createTemplate(
                "editable-template",
                new Authorization(new ViewConfig(new AdminUser(username))), StageConfigMother.manualStage("foo"));
        PipelineTemplateConfig notEditableTemplate = PipelineTemplateConfigMother.createTemplate("not-editable-template");
        TemplatesConfig templates = new TemplatesConfig();
        templates.add(editableTemplate);
        templates.add(notEditableTemplate);
        cruiseConfig.setTemplates(templates);

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToEditTemplate(cis("editable-template"), new Username(username))).thenReturn(true);

        TemplatesConfig actual = service.templateConfigsThatCanBeEditedBy(new Username(username));
        TemplatesConfig expected = new TemplatesConfig(editableTemplate);
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void shouldReturnAllTemplatesThatCanBeViewedByUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString username = cis("template-user");

        PipelineTemplateConfig viewableTemplate = PipelineTemplateConfigMother.createTemplate(
                "template",
                new Authorization(new ViewConfig(new AdminUser(username))), StageConfigMother.manualStage("foo"));
        PipelineTemplateConfig notViewableTemplate = PipelineTemplateConfigMother.createTemplate("not-viewable-template");
        TemplatesConfig templates = new TemplatesConfig();
        templates.add(viewableTemplate);
        templates.add(notViewableTemplate);
        cruiseConfig.setTemplates(templates);

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(cis("template"), new Username(username))).thenReturn(true);

        TemplatesConfig actual = service.templateConfigsThatCanBeViewedBy(new Username(username));
        TemplatesConfig expected = new TemplatesConfig(viewableTemplate);
        assertThat(expected).isEqualTo(actual);
    }

    private PipelineConfig createPipelineWithTemplate(String pipelineName, PipelineTemplateConfig template) {
        PipelineConfig pipelineConfig = pipelineConfig(pipelineName);
        pipelineConfig.clear();
        pipelineConfig.setTemplateName(template.name());
        pipelineConfig.usingTemplate(template);
        return pipelineConfig;
    }

    @SuppressWarnings("SameParameterValue")
    private PipelineTemplateConfig template(final String name) {
        return new PipelineTemplateConfig(cis(name), StageConfigMother.stageConfig("some_stage"));
    }

    private BasicCruiseConfig getCruiseConfigWithSecurityEnabled() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(cis("admin")))), null);
        cruiseConfig.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        return cruiseConfig;
    }
}
