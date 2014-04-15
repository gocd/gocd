/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.controller;

import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.helpers.SecurityContextHelper;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.controller.beans.MaterialFactory;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.CheckConnectionSubprocessExecutionContext;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.materials.MaterialConnectivityService;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.JsonTester;
import com.thoughtworks.go.util.command.UrlArgument;
import com.thoughtworks.go.util.json.JsonMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SimplePipelineWizardControllerTest {
    private SimplePipelineWizardController controller;
    private GoConfigService goConfigService;
    private SecurityContext originalSecurityContext;
    private MaterialFactory materialFactory;
    private SecurityService securityService;
    private String pipelineName;
    private HttpServletResponse response;
    public PipelinePauseService pipelinePauseService;
    private MaterialConnectivityService materialConnectivityService;

    @Before
    public void setUp() throws Exception {
        response = new MockHttpServletResponse();
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        materialFactory = mock(MaterialFactory.class);
        pipelinePauseService = mock(PipelinePauseService.class);
        materialConnectivityService = mock(MaterialConnectivityService.class);
        controller = new SimplePipelineWizardController(goConfigService, securityService, materialFactory, pipelinePauseService, materialConnectivityService);
        originalSecurityContext = SecurityContextHolder.getContext();        
    }

    @After
    public void tearDown() {
        if (originalSecurityContext != null) {
            SecurityContextHolder.setContext(originalSecurityContext);
        }
    }

    @Test
    public void shouldCheckConnectionOnTheMaterial() {
        SvnMaterialConfig mockMaterialConfig = mock(SvnMaterialConfig.class);
        when(mockMaterialConfig.errors()).thenReturn(new ConfigErrors());
        when(materialConnectivityService.checkConnection(eq(mockMaterialConfig), any(CheckConnectionSubprocessExecutionContext.class))).thenReturn(ValidationBean.valid());

        String view = SimplePipelineWizardController.DUMMY_VIEW;
        when(materialFactory.getMaterial("svn", "url", "username", "password", false, view, null, null, null)).thenReturn(mockMaterialConfig);
        controller.checkSCMConnection(null, "svn", "url", "username", "password", "false", null, null, view, mock(HttpServletResponse.class));
        verify(materialConnectivityService).checkConnection(eq(mockMaterialConfig), any(CheckConnectionSubprocessExecutionContext.class));
    }

    @Test
    public void shouldCheckConnectionOnTheMaterialAndShowTheErrorsIfPresent() {
        SvnMaterialConfig mockMaterialConfig = mock(SvnMaterialConfig.class);
        ConfigErrors configErrors = new ConfigErrors();
        configErrors.add("FOO", "There is an error");
        when(mockMaterialConfig.errors()).thenReturn(configErrors);
        String view = SimplePipelineWizardController.DUMMY_VIEW;
        when(materialFactory.getMaterial("svn", "url", "username", "password", false, view, null, null, null)).thenReturn(mockMaterialConfig);

        ModelAndView modelAndView = controller.checkSCMConnection(null, "svn", "url", "username", "password", "false", null, null, view, mock(HttpServletResponse.class));
        verify(mockMaterialConfig).errors();
        verifyNoMoreInteractions(mockMaterialConfig);
        Map json = (Map) modelAndView.getModelMap().get("json");
        assertThat(json.get("isValid").toString(), is("\"false\""));
        assertThat(json.get(GoConstants.ERROR_FOR_JSON).toString(), containsString("There is an error"));
    }

    @Test
    public void shouldReturnJsonWithBadRequestIfPipelineNameIsInvalid() throws Exception {
        pipelineName = "_$%^*&%$$invalidName";
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false);

        ModelAndView modelAndView = controller.createPipeline(pipelineName, DEFAULT_GROUP, null, "http://test/", "", "", "", "", "", null, null, null, "", "", false, "", null, null, null, response);
        Map data = modelAndView.getModel();
        JsonMap json = (JsonMap) data.get("json");
        new JsonTester(json).shouldContain("{ 'isValid' : 'false' }");
    }

    @Test
    public void shouldReturnJsonWithUnauthorizedWhenUserIsNotAnAdminAndANewPipelineGroupIsSent() {
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("AnnaBond"))).thenReturn(false);
        SecurityContextHelper.setCurrentUser("ram");
        Username ram = new Username(new CaseInsensitiveString("ram"));
        when(securityService.isUserAdmin(ram)).thenReturn(false);
        when(securityService.isUserAdminOfGroup(ram.getUsername(), "newGroup")).thenReturn(false);

        ModelAndView modelAndView = controller.createPipeline("AnnaBond", "newGroup", null, "http://test/", "", "", "", "", "", null, null, null, "", "", false, "", null, null, null, response);
        Map data = modelAndView.getModel();
        JsonMap json = (JsonMap) data.get("json");
        new JsonTester(json).shouldContain("{ 'error' : 'User \\'ram\\' is not authorised to add pipelines to pipeline group newGroup' }");
    }

    @Test
    public void shouldCreatePipelineForAnyGroupIfUserIsAdmin() {
        SvnMaterialConfig mockMaterialConfig = mock(SvnMaterialConfig.class);
        String view = SimplePipelineWizardController.DUMMY_VIEW;
        pipelineName = "AnnaBond";
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false);
        SecurityContextHelper.setCurrentUser("barrow");
        Username barrow = new Username(new CaseInsensitiveString("barrow"));
        when(securityService.isUserAdmin(barrow)).thenReturn(true);
        when(securityService.isUserAdminOfGroup(barrow.getUsername(), "newGroup")).thenReturn(false);
        when(materialFactory.getMaterial("svn", "http://test/", "username", "password", false, view, null, null, null)).thenReturn(mockMaterialConfig);

        ModelAndView modelAndView = controller.createPipeline(pipelineName, "newGroup", "svn", "http://test/", "username", "password", "", "", "", null, null, null, "", "", false, view, null, null, null, response);
        assertCreatedPipelineSuccessfully(modelAndView);

        Username userNameBarrow = new Username(new CaseInsensitiveString("barrow"));
        verify(pipelinePauseService).pause(pipelineName, "Under construction", userNameBarrow);
    }

    @Test
    public void shouldCreatePipelineForTheGroupIfUserIsGroupAdminForThatGroup() {
        SvnMaterialConfig mockMaterialConfig = mock(SvnMaterialConfig.class);
        String view = SimplePipelineWizardController.DUMMY_VIEW;
        pipelineName = "AnnaBond";
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false);
        SecurityContextHelper.setCurrentUser("barrow");
        Username barrow = new Username(new CaseInsensitiveString("barrow"));
        when(securityService.isUserAdmin(barrow)).thenReturn(false);
        when(securityService.isUserAdminOfGroup(barrow.getUsername(), "newGroup")).thenReturn(true);

        when(materialFactory.getMaterial("svn", "http://test/", "username", "password", false, view, null, null, null)).thenReturn(mockMaterialConfig);

        ModelAndView modelAndView = controller.createPipeline(pipelineName, "newGroup", "svn", "http://test/", "username", "password", "", "", "", null, null, null, "", "", false, view, null, null, null, response);
        assertCreatedPipelineSuccessfully(modelAndView);

        Username userNameBarrow = new Username(new CaseInsensitiveString("barrow"));
        verify(pipelinePauseService).pause(pipelineName, "Under construction", userNameBarrow);
    }

    @Test
    public void shouldCreateNewPipelineForTfsMaterial() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://tfs.com"), "tfsuser", "tfsdomain", "tfspassword", "tfsprojectpath");
        PipelineConfig expected = new PipelineConfig(new CaseInsensitiveString("tfs_pipeline"), new MaterialConfigs(), StageConfigMother.manualStage("tfs_stage"));
        expected.addMaterialConfig(tfsMaterialConfig);
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("tfs_pipeline"))).thenReturn(false);

        SecurityContextHelper.setCurrentUser("admin");
        Username admin = new Username(new CaseInsensitiveString("admin"));
        when(securityService.isUserAdmin(admin)).thenReturn(true);
        when(securityService.isUserAdminOfGroup(admin.getUsername(), "group")).thenReturn(true);
        when(materialFactory.getMaterial("tfs", "http://tfs.com", "tfsuser", "tfspassword", null, null, null, "tfsprojectpath", "tfsdomain")).thenReturn(tfsMaterialConfig);

        ModelAndView modelAndView = controller.createPipeline(expected.name().toString(), "group" , "tfs", tfsMaterialConfig.getUrl(), tfsMaterialConfig.getUsername(), tfsMaterialConfig.getPassword(), null, null, null,
                null, null, null, "", "", null, null, null, tfsMaterialConfig.getProjectPath(), tfsMaterialConfig.getDomain(), response);
        assertCreatedPipelineSuccessfully(modelAndView);
        verify(materialFactory).getMaterial("tfs", tfsMaterialConfig.getUrl(), tfsMaterialConfig.getUsername(), tfsMaterialConfig.getPassword(), null, null, null, tfsMaterialConfig.getProjectPath(), tfsMaterialConfig.getDomain());
    }

    private void assertCreatedPipelineSuccessfully(ModelAndView modelAndView) {
        Map data = modelAndView.getModel();
        JsonMap json = (JsonMap) data.get("json");
        JsonTester jsonTester = new JsonTester(json);
        jsonTester.shouldContain("{ 'isValid' : 'true' }");
        jsonTester.shouldContain("{ 'messageId' : 'Pipeline successfully created.' }");
    }
}