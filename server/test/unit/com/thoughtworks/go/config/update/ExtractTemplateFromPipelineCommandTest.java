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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ExtractTemplateFromPipelineCommandTest {
    @Mock
    private GoConfigService goConfigService;


    private HttpLocalizedOperationResult result;

    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private String pipelineToExtractFrom;
    private PipelineConfig templatePipeline;
    private PipelineTemplateConfig template;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setup() {
        initMocks(this);
        result = new HttpLocalizedOperationResult();
        currentUser = new Username(new CaseInsensitiveString("user"));
        pipelineToExtractFrom = "up42";
        templatePipeline = PipelineConfigMother.pipelineConfig(pipelineToExtractFrom);
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        cruiseConfig.addPipeline("default", templatePipeline);
        template = new PipelineTemplateConfig(new CaseInsensitiveString("template"));
    }

    @Test
    public void shouldAddNewTemplateToGivenConfig() throws Exception {
        ExtractTemplateFromPipelineCommand command = new ExtractTemplateFromPipelineCommand(template, pipelineToExtractFrom,currentUser, goConfigService, result);
        assertThat(cruiseConfig.getTemplates().contains(template), is(false));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(template), is(true));
    }

    @Test
    public void shouldExtractTemplateAndUseItInPipeline() throws Exception {
        ExtractTemplateFromPipelineCommand command = new ExtractTemplateFromPipelineCommand(template, pipelineToExtractFrom,currentUser, goConfigService, result);
        assertThat(cruiseConfig.getTemplates().contains(template), is(false));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(template), is(true));
        assertThat(cruiseConfig.find(templatePipeline.name().toString()).hasTemplate(), is(true));
    }

    @Test
    public void shouldNotExtractTemplateIfSpecifiedPipelineDoesNotExists() throws Exception {
        String pipeline = "non-existing-pipeline";
        ExtractTemplateFromPipelineCommand command = new ExtractTemplateFromPipelineCommand(template, pipeline,currentUser, goConfigService, result);
        assertThat(cruiseConfig.getTemplates().contains(template), is(false));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(template), is(false));
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(LocalizedMessage.string("PIPELINE_NOT_FOUND", pipeline));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotExtractATemplateIfThePipelineHasATemplate() throws Exception {
        pipelineToExtractFrom ="up43";
        templatePipeline = PipelineConfigMother.pipelineConfigWithTemplate(pipelineToExtractFrom, "extract");
        cruiseConfig.addPipeline("default", templatePipeline);
        ExtractTemplateFromPipelineCommand command = new ExtractTemplateFromPipelineCommand(template, templatePipeline.name().toString(),currentUser, goConfigService, result);
        assertThat(cruiseConfig.getTemplates().contains(template), is(false));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(template), is(false));
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(LocalizedMessage.string("CANNOT_EXTRACT_TEMPLATE"));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotExtractATemplateIfThePipelineisARemotePipeline() throws Exception {
        pipelineToExtractFrom ="up44";
        templatePipeline = PipelineConfigMother.pipelineConfigWithTemplate(pipelineToExtractFrom, "extract");
        templatePipeline.setOrigin(new RepoConfigOrigin());
        cruiseConfig.addPipeline("default", templatePipeline);
        ExtractTemplateFromPipelineCommand command = new ExtractTemplateFromPipelineCommand(template, templatePipeline.name().toString(),currentUser, goConfigService, result);
        assertThat(cruiseConfig.getTemplates().contains(template), is(false));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getTemplates().contains(template), is(false));
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(LocalizedMessage.string("CANNOT_EXTRACT_TEMPLATE_FROM_REMOTE"));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        ExtractTemplateFromPipelineCommand command = new ExtractTemplateFromPipelineCommand(template, pipelineToExtractFrom,currentUser, goConfigService, result);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }
}
