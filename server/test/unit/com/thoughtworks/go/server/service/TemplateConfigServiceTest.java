/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.CreateTemplateConfigCommand;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.tasks.PluggableTaskService;
import com.thoughtworks.go.server.ui.TemplatesViewModel;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TemplateConfigServiceTest {
    public GoConfigService goConfigService;
    private SecurityService securityService;
    private TemplateConfigService service;
    private EntityHashingService entityHashingService;
    private PluggableTaskService pluggableTaskService;

    @Before
    public void setup() {
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        entityHashingService = mock(EntityHashingService.class);
        pluggableTaskService = mock(PluggableTaskService.class);
        service = new TemplateConfigService(goConfigService, securityService, entityHashingService, pluggableTaskService);
    }

    @Test
    public void shouldReturnAMapOfAllTemplateNamesToPipelinesForAnAdminUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p2", "t2", "s2", "j2");

        Username admin = new Username("admin");
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t1"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t2"), admin)).thenReturn(true);

        Map<CaseInsensitiveString, List<CaseInsensitiveString>> allTemplatesToPipelines = new HashMap<>();
        allTemplatesToPipelines.put(new CaseInsensitiveString("t1"), new ArrayList<>());
        allTemplatesToPipelines.get(new CaseInsensitiveString("t1")).add(new CaseInsensitiveString("p1"));
        allTemplatesToPipelines.put(new CaseInsensitiveString("t2"), new ArrayList<>());
        allTemplatesToPipelines.get(new CaseInsensitiveString("t2")).add(new CaseInsensitiveString("p2"));


        assertThat(service.templatesWithPipelinesForUser(new CaseInsensitiveString("admin")), is(allTemplatesToPipelines));
    }

    @Test
    public void shouldReturnAListOfTemplatesWithAssociatedPipelinesForAnAdminUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p2", "t2", "s2", "j2");

        Username admin = new Username("admin");

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t1"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t2"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("t1"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("t2"), admin)).thenReturn(true);
        when(securityService.isUserAdmin(admin)).thenReturn(true);

        List<TemplateToPipelines> templateToPipelines = new ArrayList<>();
        TemplateToPipelines template1 = new TemplateToPipelines(new CaseInsensitiveString("t1"), true, true);
        template1.add(new PipelineWithAuthorization(new CaseInsensitiveString("p1"), true));
        templateToPipelines.add(template1);
        TemplateToPipelines template2 = new TemplateToPipelines(new CaseInsensitiveString("t2"), true, true);
        template2.add(new PipelineWithAuthorization(new CaseInsensitiveString("p2"), true));
        templateToPipelines.add(template2);

        assertThat(service.getTemplatesList(admin), is(templateToPipelines));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesToPipelinesMapForTemplateAdmin() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", new Authorization(new AdminsConfig(new AdminUser(templateAdmin))), StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        Username admin = new Username(templateAdmin);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t2"), admin)).thenReturn(true);

        Map<CaseInsensitiveString, List<CaseInsensitiveString>> templatesWithAssociatedPipelinesForUser = new HashMap<>();
        templatesWithAssociatedPipelinesForUser.put(new CaseInsensitiveString("t2"), new ArrayList<>());

        assertThat(service.templatesWithPipelinesForUser(templateAdmin), is(templatesWithAssociatedPipelinesForUser));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesWithAssociatedPipelinesForTemplateAdmin() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", new Authorization(new AdminsConfig(new AdminUser(templateAdmin))), StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        Username admin = new Username(templateAdmin);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t2"), admin)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("t2"), admin)).thenReturn(true);
        when(securityService.isUserAdmin(admin)).thenReturn(false);

        List<TemplateToPipelines> templateToPipelines = new ArrayList<>();
        TemplateToPipelines t2 = new TemplateToPipelines(new CaseInsensitiveString("t2"), true, false);
        templateToPipelines.add(t2);

        assertThat(service.getTemplatesList(admin), is(templateToPipelines));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesToPipelinesMapForTemplateViewUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("template-view");
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", new Authorization(new ViewConfig(new AdminUser(templateViewUser))), StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        Username templateView = new Username(templateViewUser);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t2"), templateView)).thenReturn(true);

        Map<CaseInsensitiveString, List<CaseInsensitiveString>> templatesWithAssociatedPipelinesForUser = new HashMap<>();
        templatesWithAssociatedPipelinesForUser.put(new CaseInsensitiveString("t2"), new ArrayList<>());

        assertThat(service.templatesWithPipelinesForUser(templateViewUser), is(templatesWithAssociatedPipelinesForUser));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesWithAssociatedPipelinesForTemplateViewUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("template-view");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", new Authorization(new ViewConfig(new AdminUser(templateViewUser))), StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        Username templateView = new Username(templateViewUser);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t2"), templateView)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("t2"), templateView)).thenReturn(false);
        when(securityService.isUserAdmin(templateView)).thenReturn(false);

        List<TemplateToPipelines> templateToPipelines = new ArrayList<>();
        TemplateToPipelines t2 = new TemplateToPipelines(new CaseInsensitiveString("t2"), false, false);
        templateToPipelines.add(t2);

        assertThat(service.getTemplatesList(templateView), is(templateToPipelines));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesToPipelinesMapForGroupAdmin() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString groupAdmin = new CaseInsensitiveString("group-admin");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineConfigs pipelineConfigs = cruiseConfig.findGroup("defaultGroup"); //defaultGroup is set in GoConfigMother
        pipelineConfigs.setAuthorization(new Authorization(new AdminsConfig(new AdminUser(groupAdmin))));

        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        Username groupAdminUser = new Username(groupAdmin);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t1"), groupAdminUser)).thenReturn(true);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString("t2"), groupAdminUser)).thenReturn(false);
        when(securityService.isUserAdmin(groupAdminUser)).thenReturn(false);

        Map<CaseInsensitiveString, List<CaseInsensitiveString>> templatesWithAssociatedPipelinesForUser = new HashMap<>();
        templatesWithAssociatedPipelinesForUser.put(new CaseInsensitiveString("t1"), new ArrayList<>());
        templatesWithAssociatedPipelinesForUser.get(new CaseInsensitiveString("t1")).add(new CaseInsensitiveString("p1"));

        assertThat(service.templatesWithPipelinesForUser(groupAdmin), is(templatesWithAssociatedPipelinesForUser));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesWithAssociatedPipelinesForGroupAdmin() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString groupAdmin = new CaseInsensitiveString("group-admin");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineConfigs pipelineConfigs = cruiseConfig.findGroup("defaultGroup"); //defaultGroup is set in GoConfigMother
        pipelineConfigs.setAuthorization(new Authorization(new AdminsConfig(new AdminUser(groupAdmin))));

        Username groupAdminUser = new Username(groupAdmin);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t1"), groupAdminUser)).thenReturn(true);

        List<TemplateToPipelines> templateToPipelines = new ArrayList<>();
        TemplateToPipelines t1 = new TemplateToPipelines(new CaseInsensitiveString("t1"), false, false);
        t1.add(new PipelineWithAuthorization(new CaseInsensitiveString("p1"), true));
        templateToPipelines.add(t1);

        assertThat(service.getTemplatesList(groupAdminUser), is(templateToPipelines));
    }

    @Test
    public void shouldReturnAnEmptyMapForARegularUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString regularUser = new CaseInsensitiveString("view");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");

        Username viewUser = new Username(regularUser);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t1"), viewUser)).thenReturn(false);

        assertThat(service.templatesWithPipelinesForUser(regularUser), is(new HashMap<>()));
    }

    @Test
    public void shouldReturnAnEmptyListOfTemplatesForARegularUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString regularUser = new CaseInsensitiveString("view");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");

        Username viewUser = new Username(regularUser);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString("t1"), viewUser)).thenReturn(false);

        assertThat(service.getTemplatesList(viewUser), is(new ArrayList<>()));
    }

    @Test
    public void shouldDeleteATemplateWithAGivenName() {
        PipelineTemplateConfig emptyTemplate = template("empty_template");
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(emptyTemplate);

        service.removeTemplate("empty_template", cruiseConfig, "md5", new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(new DeleteTemplateCommand("empty_template", "md5"));
    }

    @Test
    public void shouldReturn404WhenTheTemplateToBeDeletedIsNotFound() {
        PipelineTemplateConfig emptyTemplate = template("empty_template");
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(emptyTemplate);

        TemplateConfigService service = new TemplateConfigService(goConfigService, securityService, entityHashingService, pluggableTaskService);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.removeTemplate("not_found_template", cruiseConfig, "md5", result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(404));
    }

    @Test
    public void shouldLoadClonedTemplateForEdit() {
        String templateName = "empty_template";
        CaseInsensitiveString templateAdminUser = new CaseInsensitiveString("templateAdminUser");
        Username templateUser = new Username(templateAdminUser);

        PipelineTemplateConfig emptyTemplate = PipelineTemplateConfigMother.createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdminUser))), StageConfigMother.stageConfig("some_stage"));
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(emptyTemplate);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString(templateName), templateUser)).thenReturn(true);
        when(goConfigService.getConfigHolder()).thenReturn(new GoConfigHolder(cruiseConfig, cruiseConfig));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit<PipelineTemplateConfig> configForEdit = service.loadForEdit(templateName, templateUser, result);

        assertThat(configForEdit, is(not(nullValue())));
        CaseInsensitiveString empty_template = new CaseInsensitiveString(templateName);
        assertThat(configForEdit.getConfig().name(), is(empty_template));

        PipelineTemplateConfig template = configForEdit.getConfig();
        PipelineTemplateConfig templateForEdit = configForEdit.getCruiseConfig().findTemplate(empty_template);
        PipelineTemplateConfig processedTemplate = configForEdit.getProcessedConfig().findTemplate(empty_template);
        PipelineTemplateConfig serversTemplate = cruiseConfig.findTemplate(empty_template);
        serversTemplate.add(new StageConfig(new CaseInsensitiveString("stage-one"), new JobConfigs(new JobConfig("job"))));//modify the server's copy
        assertThat(serversTemplate.size(), is(2));
        assertThat(template.size(), is(1));//given copy should remain unmodified
        assertThat(templateForEdit.size(), is(1));
        assertThat(processedTemplate.size(), is(1));
    }

    @Test
    public void shouldErrorOutIfTemplateIsNotFound() {
        PipelineTemplateConfig emptyTemplate = template("empty_template");
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(emptyTemplate);
        when(securityService.isAuthorizedToEditTemplate(any(CaseInsensitiveString.class), any(Username.class))).thenReturn(true);
        when(goConfigService.getConfigHolder()).thenReturn(new GoConfigHolder(cruiseConfig, cruiseConfig));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit<PipelineTemplateConfig> configForEdit = service.loadForEdit("blah", new Username(new CaseInsensitiveString("someuser")), result);

        assertThat(configForEdit, is(nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(404));
    }

    @Test
    public void shouldErrorOutIfUserIsNotAllowedToAdministerTheGivenTemplate() {
        Username username = new Username(new CaseInsensitiveString("user"));
        String templateName = "templateName";
        PipelineTemplateConfig emptyTemplate = PipelineTemplateConfigMother.createTemplate(templateName);
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(emptyTemplate);
        when(securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString(templateName), username)).thenReturn(false);
        when(goConfigService.getConfigHolder()).thenReturn(new GoConfigHolder(cruiseConfig, cruiseConfig));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit<PipelineTemplateConfig> configForEdit = service.loadForEdit(templateName, username, result);

        assertThat(configForEdit, is(nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(401));

        Localizer localizer = mock(Localizer.class);
        when(localizer.localize("UNAUTHORIZED_TO_EDIT_TEMPLATE", templateName)).thenReturn("No template for you");
        assertThat(result.message(localizer), is("No template for you"));
    }

    @Test
    public void shouldReturnAListOfAllPipelineConfigsThatAreNotUsedInTemplates() {
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);

        PipelineTemplateConfig template = template("first_template");
        PipelineTemplateConfig another = template("another_template");
        PipelineConfig pipelineWithoutTemplateOne = pipelineConfig("first_without_template");
        PipelineConfig pipelineWithoutTemplateTwo = pipelineConfig("another_without_template");
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(createPipelineWithTemplate("first", template),
                createPipelineWithTemplate("second", template),
                pipelineWithoutTemplateOne,
                pipelineWithoutTemplateTwo,
                createPipelineWithTemplate("fifth", another)));

        cruiseConfig.addTemplate(template);
        cruiseConfig.addTemplate(another);
        when(goConfigService.getAllPipelineConfigsForEdit()).thenReturn(cruiseConfig.allPipelines());

        List<PipelineConfig> pipelineConfigs = service.allPipelinesNotUsingTemplates(user, new HttpLocalizedOperationResult());
        assertThat(pipelineConfigs, is(Arrays.asList(pipelineWithoutTemplateOne, pipelineWithoutTemplateTwo)));
    }

    @Test
    public void shouldReturnUnauthorizedIfTheUserIsNotAdmin() {
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(false);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<PipelineConfig> pipelineConfigs = service.allPipelinesNotUsingTemplates(user, result);

        assertThat(result.isSuccessful(), is(false));

        Localizer localizer = mock(Localizer.class);
        when(localizer.localize("UNAUTHORIZED_TO_ADMINISTER", new Object[0])).thenReturn("foo");

        assertThat(result.message(localizer), is("foo"));
        assertThat(pipelineConfigs, is(nullValue()));
    }

    @Test
    public void shouldLoadTemplateForViewing() {
        PipelineTemplateConfig template = template("first_template");
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(createPipelineWithTemplate("first", template), createPipelineWithTemplate("second", template)));
        cruiseConfig.addTemplate(template);
        when(goConfigService.getConfigHolder()).thenReturn(new GoConfigHolder(cruiseConfig, cruiseConfig));
        PipelineTemplateConfig actual = service.loadForView(template.name().toString(), new HttpLocalizedOperationResult());

        assertThat(template, is(actual));
    }

    @Test
    public void shouldPopulateErrorInResultOnFailure() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));
        String templateName = "template-name";
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString(templateName), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        String errorMessage = "invalid template";
        doThrow(new GoConfigInvalidException(new GoConfigMother().defaultCruiseConfig(), errorMessage)).when(goConfigService).updateConfig(any(CreateTemplateConfigCommand.class), any(Username.class));

        service.createTemplateConfig(user, pipelineTemplateConfig, result);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED", "template", templateName, errorMessage));

        assertThat(result.toString(), is(expectedResult.toString()));
    }

    @Test
    public void shouldReturnAMapOfAllTemplatesWithAuthorizationsForAnyUser() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))), null);
        cruiseConfig.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);

        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("template-view");
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        PipelineTemplateConfig template1 = PipelineTemplateConfigMother.createTemplate("t1", new Authorization(new ViewConfig(new AdminUser(templateViewUser))), StageConfigMother.manualStage("foo"));
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2");
        PipelineTemplateConfig template3 = PipelineTemplateConfigMother.createTemplate("t3", new Authorization(new AdminsConfig(new AdminUser(templateAdmin))), StageConfigMother.manualStage("foobar"));
        cruiseConfig.addTemplate(template1);
        cruiseConfig.addTemplate(template2);
        cruiseConfig.addTemplate(template3);

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);

        List<TemplatesViewModel> templatesForSuperAdmins = new ArrayList<>();
        templatesForSuperAdmins.add(new TemplatesViewModel(template1, true, true));
        templatesForSuperAdmins.add(new TemplatesViewModel(template2, true, true));
        templatesForSuperAdmins.add(new TemplatesViewModel(template3, true, true));

        List<TemplatesViewModel> templatesForTemplateAdmin = new ArrayList<>();
        templatesForTemplateAdmin.add(new TemplatesViewModel(template1, false, false));
        templatesForTemplateAdmin.add(new TemplatesViewModel(template2, false, false));
        templatesForTemplateAdmin.add(new TemplatesViewModel(template3, true, true));

        List<TemplatesViewModel> templatesForTemplateViewUser = new ArrayList<>();
        templatesForTemplateViewUser.add(new TemplatesViewModel(template1, true, false));
        templatesForTemplateViewUser.add(new TemplatesViewModel(template2, false, false));
        templatesForTemplateViewUser.add(new TemplatesViewModel(template3, false, false));

        assertThat(service.getTemplateViewModels(new CaseInsensitiveString("admin")), is(templatesForSuperAdmins));
        assertThat(service.getTemplateViewModels(templateAdmin), is(templatesForTemplateAdmin));
        assertThat(service.getTemplateViewModels(templateViewUser), is(templatesForTemplateViewUser));
    }

    private PipelineConfig createPipelineWithTemplate(String pipelineName, PipelineTemplateConfig template) {
        PipelineConfig pipelineConfig = pipelineConfig(pipelineName);
        pipelineConfig.clear();
        pipelineConfig.setTemplateName(template.name());
        pipelineConfig.usingTemplate(template);
        return pipelineConfig;
    }

    private PipelineTemplateConfig template(final String name) {
        return new PipelineTemplateConfig(new CaseInsensitiveString(name), StageConfigMother.stageConfig("some_stage"));
    }

    private BasicCruiseConfig getCruiseConfigWithSecurityEnabled() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig( new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))), null);
        cruiseConfig.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        return cruiseConfig;
    }
}
