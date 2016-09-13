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

package com.thoughtworks.go.server.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TemplateConfigServiceTest {
    public GoConfigService goConfigService;
    private SecurityService securityService;
    private TemplateConfigService service;
    private EntityHashingService entityHashingService;

    @Before
    public void setup() {
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        entityHashingService = mock(EntityHashingService.class);
        service = new TemplateConfigService(goConfigService, securityService, entityHashingService);
    }

    @Test
    public void shouldReturnAMapOfTemplateNamesToListOfAssociatedPipelines() {
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        PipelineTemplateConfig template = template("first_template");
        PipelineTemplateConfig emptyTemplate = template("empty_template");
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(createPipelineWithTemplate("first", template), createPipelineWithTemplate("second", template)));

        cruiseConfig.addTemplate(template);
        cruiseConfig.addTemplate(emptyTemplate);

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

        Map<CaseInsensitiveString, List<CaseInsensitiveString>> templateWithPipelines = service.templatesWithPipelinesForUser("user");

        assertThat(templateWithPipelines.size(), is(2));
        assertThat(templateWithPipelines.get(new CaseInsensitiveString("first_template")), is(Arrays.asList(new CaseInsensitiveString("first"), new CaseInsensitiveString("second"))));
        assertThat(templateWithPipelines.get(new CaseInsensitiveString("empty_template")), is((List<CaseInsensitiveString>) new ArrayList<CaseInsensitiveString>()));
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

        TemplateConfigService service = new TemplateConfigService(goConfigService, securityService, entityHashingService);

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
        when(securityService.isAuthorizedToEditTemplate(templateName, templateUser)).thenReturn(true);
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
        when(securityService.isAuthorizedToEditTemplate(anyString(), any(Username.class))).thenReturn(true);
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
        when(securityService.isAuthorizedToEditTemplate(templateName, username)).thenReturn(false);
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
    public void shouldLoadTemplateForViewing(){
        PipelineTemplateConfig template = template("first_template");
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(createPipelineWithTemplate("first", template), createPipelineWithTemplate("second", template)));
        cruiseConfig.addTemplate(template);
        when(goConfigService.getConfigHolder()).thenReturn(new GoConfigHolder(cruiseConfig, cruiseConfig));
        PipelineTemplateConfig actual = service.loadForView(template.name().toString(), new HttpLocalizedOperationResult());

        assertThat(template, is(actual));
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
}
