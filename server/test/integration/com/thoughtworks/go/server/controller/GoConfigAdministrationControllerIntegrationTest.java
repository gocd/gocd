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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.controller.actions.XmlAction;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.util.json.JsonString;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import static com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET_JSON;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang.StringEscapeUtils.unescapeJavaScript;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/spring-tabs-servlet.xml",
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoConfigAdministrationControllerIntegrationTest {
    @Autowired private GoConfigAdministrationController controller;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private GoFileConfigDataSource dataSource;
    @Autowired private ConfigElementImplementationRegistry registry;
    @Autowired private MetricsProbeService metricsProbeService;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private MockHttpServletResponse response;

    private static final String NEW_STAGE =
            "<stage name=\"dev\">\n"
                    + "  <jobs>\n"
                    + "    <job name=\"build1\" />\n"
                    + "    <job name=\"build2\" />\n"
                    + "  </jobs>\n"
                    + "</stage>";

    private static final String NEW_PIPELINE =
            "<pipeline name=\"cruise\">\n"
                    + "  <materials>\n"
                    + "    <svn url=\"file:///tmp/foo\" checkexternals=\"true\" />\n"
                    + "  </materials>\n"
                    + "  <stage name=\"dev\">\n"
                    + "    <jobs>\n"
                    + "      <job name=\"linux\" />\n"
                    + "      <job name=\"windows\" />\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>";

    private static final String NEW_GROUP =
            "<pipelines group=\"group\">\n"
                    + "  <pipeline name=\"pipeline\">\n"
                    + "    <materials>\n"
                    + "      <svn url=\"file:///tmp/foo\" />\n"
                    + "    </materials>\n"
                    + "    <stage name=\"dev\">\n"
                    + "      <jobs>\n"
                    + "        <job name=\"linux\" />\n"
                    + "        <job name=\"windows\" />\n"
                    + "      </jobs>\n"
                    + "    </stage>\n"
                    + "  </pipeline>\n"
                    + "</pipelines>";

    private static final String NEW_BUILD =
            "        <job name=\"new_job\" />\n";

    private static final String NEW_TEMPLATES =
            "<templates>\n"
                    + "  <pipeline name=\"pipeline\">\n"
                    + "    <stage name=\"dev\">\n"
                    + "      <jobs>\n"
                    + "        <job name=\"linux\" />\n"
                    + "        <job name=\"windows\" />\n"
                    + "      </jobs>\n"
                    + "    </stage>\n"
                    + "  </pipeline>\n"
                    + "</templates>";

    private static final String NEW_PIPELINE_TEMPLATE =
            "<pipeline name=\"cruise\">\n"
                    + "  <stage name=\"dev\">\n"
                    + "    <jobs>\n"
                    + "      <job name=\"linux\" />\n"
                    + "      <job name=\"windows\" />\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>";

    private static final String RENAMED_GROUP =
            "<pipelines group=\"changed\">\n"
                    + "  <pipeline name=\"pipeline\">\n"
                    + "    <materials>\n"
                    + "      <svn url=\"file:///tmp/foo\" />\n"
                    + "    </materials>\n"
                    + "    <stage name=\"dev\">\n"
                    + "      <jobs>\n"
                    + "        <job name=\"linux\" />\n"
                    + "        <job name=\"windows\" />\n"
                    + "      </jobs>\n"
                    + "    </stage>\n"
                    + "  </pipeline>\n"
                    + "</pipelines>";

    private String groupName;
    private SecurityContext originalSecurityContext;

    @Before public void setup() throws Exception {
        dataSource.reloadEveryTime();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        response = new MockHttpServletResponse();
        configHelper.addSecurityWithPasswordFile();
        configHelper.addAdmins("admin");
        setCurrentUser("admin");
    }

    @After public void teardown() throws Exception {
        if (originalSecurityContext != null) {
            SecurityContextHolder.setContext(originalSecurityContext);
        }
        configHelper.onTearDown();
    }

    @Test public void shouldReturnXmlAndErrorMessageWhenInvalidPostOfTemplateAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String newXml = "<job name=\"build3\" />";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postBuildAsXmlPartial("pipeline", "stage", 4, newXml, md5, response);
        assertThat(response.getStatus(), is(SC_NOT_FOUND));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : 'Build does not exist.',"
                        + "  'originalContent' : '" + newXml + "' }"
        );
    }

    @Test public void shouldGetBuildAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");

        String md5 = goConfigDao.md5OfConfigFile();

        controller.getBuildAsXmlPartial("pipeline", "stage", 1, null, response);
        assertValidContentAndStatus(SC_OK, "text/xml", "<job name=\"build2\" />");
        assertThat((String) response.getHeader(XmlAction.X_CRUISE_CONFIG_MD5), is(md5));
    }

    @Test public void shouldGetConfigAsXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");

        controller.getCurrentConfigXml(null, response);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(new ConfigCache(), registry, metricsProbeService).write(goConfigDao.loadForEditing(), os, true);
        assertValidContentAndStatus(SC_OK, "text/xml", os.toString());
        assertThat((String) response.getHeader(XmlAction.X_CRUISE_CONFIG_MD5), is(goConfigDao.md5OfConfigFile()));
    }

    @Test public void shouldConflictWhenGivenMd5IsDifferent() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");

        controller.getCurrentConfigXml("crapy_md5", response);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService).write(goConfigDao.loadForEditing(), os, true);
        assertValidContentAndStatus(SC_CONFLICT, "text/plain; charset=utf-8", CONFIG_CHANGED_PLEASE_REFRESH);
        assertThat((String) response.getHeader(XmlAction.X_CRUISE_CONFIG_MD5), is(goConfigDao.md5OfConfigFile()));
    }

    @Test public void shouldGetErrorMessageWhenBuildDoesNotExist() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1");
        controller.getBuildAsXmlPartial("pipeline", "stage", 1, null, response);
        assertValidContentAndStatus(SC_NOT_FOUND, RESPONSE_CHARSET, "Build does not exist.");
    }

    @Test public void shouldGetErrorMessageWhenPipelineDoesNotExistAndGettingBuild() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1");
        controller.getBuildAsXmlPartial("unknown", "stage", 0, null, response);
        assertValidContentAndStatus(SC_NOT_FOUND, RESPONSE_CHARSET, "Pipeline 'unknown' not found.");
    }

    @Test public void shouldPostBuildAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String newXml = "<job name=\"build3\" />";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postBuildAsXmlPartial("pipeline", "stage", 0, newXml, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : 'JobConfig changed successfully.' }"
        );
    }

    @Test public void shouldReturnErrorMessageIfConfigFileChangesBeforePostBuildAsPartialXml() throws Exception {
        String md5 = goConfigDao.md5OfConfigFile();
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String newXml = "<job name=\"build3\" />";
        ModelAndView mav = controller.postBuildAsXmlPartial("pipeline", "stage", 0, newXml, md5, response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : '" + ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH + "',"
                        + "  'originalContent' : '" + newXml + "' }"
        );
    }

    @Test public void shouldReturnXmlAndErrorMessageWhenInvalidPostOfBuildAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String newXml = "<job name=\"build3\" />";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postBuildAsXmlPartial("pipeline", "stage", 4, newXml, md5, response);
        assertThat(response.getStatus(), is(SC_NOT_FOUND));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : 'Build does not exist.',"
                        + "  'originalContent' : '" + newXml + "' }"
        );
    }

    @Test public void shouldReturnXmlAndErrorMessageWhenPostOfBuildWithDuplicatedNameAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String newXml = "<job name=\"build2\" />";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postBuildAsXmlPartial("pipeline", "stage", 0, newXml, md5, response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        JsonMap expected = new JsonMap();
        expected.put("result", "Duplicate unique value [build2] declared for identity constraint \"uniqueJob\" of element \"jobs\".");
        expected.put("originalContent", newXml);
        assertThat(json, is((Json) expected));
    }

    @Test public void shouldReturnXmlAndErrorMessageWhenPostOfBuildAsInvalidPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String badXml = "<;askldjfa;dsklfja;sdjas;lkdfjob name=\"build3\" />";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postBuildAsXmlPartial("pipeline", "stage", 0, badXml, md5, response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : 'Error on line 1 of document  : The markup in the document preceding the root element must be well-formed. Nested exception: The markup in the document preceding the root element must be well-formed.',"
                        + "  'originalContent' : '" + badXml + "' }"
        );
    }

    @Test public void shouldReturnErrorMessageWhenPostOfJobContainsDotForExecWorkingDir() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String badXml = "<job name=\"build3\" >"
                + "<tasks>"
                + "<exec command=\"ant\" workingdir=\".\"/>"
                + "</tasks>"
                + "</job>";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postBuildAsXmlPartial("pipeline", "stage", 0, badXml, md5, response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        JsonValue jsonValue = JsonUtils.from(json);
        assertThat(unescapeJavaScript(jsonValue.getString("originalContent")), is(badXml));
        assertThat(unescapeJavaScript(jsonValue.getString("result")), containsString("File path is invalid"));
    }

    @Test public void shouldGetStageAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "dev", "linux", "windows");
        controller.getStageAsXmlPartial("pipeline", 0, null, response);
        assertValidContentAndStatus(SC_OK, "text/xml",
                "<stage name=\"dev\">\n"
                        + "  <jobs>\n"
                        + "    <job name=\"linux\" />\n"
                        + "    <job name=\"windows\" />\n"
                        + "  </jobs>\n"
                        + "</stage>");
    }

    @Test public void shouldGetGroupAsPartialXml() throws Exception {
        configHelper.addPipelineWithGroup("group", "pipeline", "dev", "linux");
        controller.getGroupAsXmlPartial("group", null, response);
        assertValidContentAndStatus(SC_OK, "text/xml",
                "<pipelines group=\"group\">\n"
                        + "  <pipeline name=\"pipeline\">\n"
                        + "    <materials>\n"
                        + "      <svn url=\"svn:///user:pass@tmp/foo\" />\n"
                        + "    </materials>\n"
                        + "    <stage name=\"dev\">\n"
                        + "      <jobs>\n"
                        + "        <job name=\"linux\" />\n"
                        + "      </jobs>\n"
                        + "    </stage>\n"
                        + "  </pipeline>\n"
                        + "</pipelines>"
        );
    }

    @Test public void shouldGetGroupWithTemplatesAsXml() throws Exception {
        configHelper.addTemplate("template-1", "dev");
        configHelper.addPipelineWithGroup("group", "pipeline", "dev", "linux");
        PipelineConfig pipelineWithTemplate = configHelper.addPipelineWithTemplate("group", "pipeline-with-template", "template-1");
        assertThat(pipelineWithTemplate.size(), is(0)); //should not expect mutation of pipeline config passed in
        assertThat(configHelper.currentConfig().pipelineConfigByName(new CaseInsensitiveString("pipeline-with-template")).size(), is(1));

        CruiseConfig config = goConfigService.currentCruiseConfig();
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline-with-template"));
        assertThat("Should not modify the original template", pipelineConfig.size(), is(1));

        controller.getGroupAsXmlPartial("group", null, response);
        String content = "<pipelines group=\"group\">\n"
                + "  <pipeline name=\"pipeline\">\n"
                + "    <materials>\n"
                + "      <svn url=\"svn:///user:pass@tmp/foo\" />\n"
                + "    </materials>\n"
                + "    <stage name=\"dev\">\n"
                + "      <jobs>\n"
                + "        <job name=\"linux\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "  <pipeline name=\"pipeline-with-template\" template=\"template-1\">\n"
                + "    <materials>\n"
                + "      <svn url=\"svn:///user:pass@tmp/foo\" />\n"
                + "    </materials>\n"
                + "  </pipeline>\n"
                + "</pipelines>";
        assertValidContentAndStatus(SC_OK, "text/xml", content);

        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        controller.postGroupAsXmlPartial("group", content, null, postResponse);

        config = goConfigService.currentCruiseConfig();
        pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline-with-template"));
        assertThat("Should not modify the original template", pipelineConfig.size(), is(1));

    }

    @Test public void shouldGetErrorMessageWhenStageDoesNotExist() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1");
        controller.getStageAsXmlPartial("pipeline", 1, null, response);
        assertValidContentAndStatus(SC_NOT_FOUND, RESPONSE_CHARSET, "Stage does not exist.");
    }

    @Test public void shouldGetErrorMessageWhenPipelineDoesNotExistAndGettingStage() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1");
        controller.getStageAsXmlPartial("unknown", 0, null, response);
        assertValidContentAndStatus(SC_NOT_FOUND, RESPONSE_CHARSET, "Pipeline 'unknown' not found.");
    }

    @Test public void shouldPostStageAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String newXml = NEW_STAGE;
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postStageAsXmlPartial("pipeline", 0, newXml, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : 'Stage changed successfully.' }"
        );
    }

    @Test public void shouldPostGroupAsPartialXml() throws Exception {
        configHelper.addPipelineWithGroup("group", "pipeline", "dev", "linux", "windows");
        String newXml = NEW_GROUP;
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postGroupAsXmlPartial("group", newXml, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        assertResponseMessage(mav, "Group changed successfully.");
    }

    @Test public void shouldPostGroupWithTemplateAsPartialXml() throws Exception {
        configHelper.addTemplate("template-1", "dev");
        configHelper.addPipelineWithGroup("group", "pipeline", "dev", "linux", "windows");
        configHelper.addPipelineWithTemplate("group", "pipeline-with-template", "template-1");

        String newXml = NEW_GROUP;
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postGroupAsXmlPartial("group", newXml, md5, response);
        assertResponseMessage(mav, "Group changed successfully.");
        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
    }

    @Test
    public void shouldIntroduceTemplatesTagIfANewTemplateIsIntroduced() throws Exception {
        configHelper.addPipelineWithGroup("some-group", "some-pipeline", "some-dev", "some-linux", "some-windows");
        configHelper.addAgent("some-home", "UUID");
        String newXml = NEW_TEMPLATES;
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postGroupAsXmlPartial("Pipeline Templates", newXml, md5, response);
        assertResponseMessage(mav, "Template changed successfully.");
        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        assertThat(configHelper.currentConfig().getTemplates().size(), is(1));
    }

    @Test
    public void shouldModifyExistingPipelineTemplateWhenPostedAsPartial() throws Exception {
        configHelper.addPipelineWithGroup("some-group", "some-pipeline", "some-dev", "some-linux", "some-windows");
        configHelper.addAgent("some-home", "UUID");
        String newXml = NEW_TEMPLATES;
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postGroupAsXmlPartial("Pipeline Templates", newXml, md5, response);
        assertResponseMessage(mav, "Template changed successfully.");
        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        assertThat(configHelper.currentConfig().getTemplates().size(), is(1));
    }

    private void assertResponseMessage(ModelAndView mav, String message) {
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : '" + message + "' }"
        );
    }

    @Test public void shouldPostGroupWithChangedNameAsPartialXml() throws Exception {
        configHelper.addPipelineWithGroup("group", "pipeline", "dev", "linux", "windows");
        String newXml = RENAMED_GROUP;
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postGroupAsXmlPartial("group", newXml, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        assertResponseMessage(mav, "Group changed successfully.");

    }


    @Test public void shouldReturnXmlAndErrorMessageWhenInvalidPostOfStageAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postStageAsXmlPartial("pipeline", 4, NEW_STAGE, md5, response);
        assertThat(response.getStatus(), is(SC_NOT_FOUND));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        JsonMap jsonMap = new JsonMap();
        jsonMap.put("result", "Stage does not exist.");
        jsonMap.put("originalContent", NEW_STAGE);
        assertThat(json, is((Json) jsonMap));
    }

    @Test public void shouldReturnXmlAndErrorMessageWhenPostOfStageAsInvalidPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String badXml = "<;askldjfa;dsklfja;sdjas;lkdf";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postStageAsXmlPartial("pipeline", 0, badXml, md5, response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : 'Error on line 1 of document  : The markup in the document preceding the root element must be well-formed. Nested exception: The markup in the document preceding the root element must be well-formed.',"
                        + "  'originalContent' : '" + badXml + "' }"
        );
    }

    @Test
    public void shouldGetTemplateAsPartialXmlOnlyIfUserHasAdminRights() throws Exception {
        //get the pipeline XML
        configHelper.addPipeline("pipeline", "dev", "linux", "windows");
        configHelper.addTemplate("new-template", "dev");
        controller.getPipelineAsXmlPartial(0, TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME, null, response);
        String xml = response.getContentAsString();
        assertThat(xml, containsString("new-template"));

        //save the pipeline XML
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        String modifiedXml = xml.replace("new-template", "new-name-for-template");
        controller.postPipelineAsXmlPartial(0, TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME, modifiedXml, goConfigDao.md5OfConfigFile(), postResponse);

        //get the pipeline XML again
        MockHttpServletResponse getResponse = new MockHttpServletResponse();
        controller.getPipelineAsXmlPartial(0, TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME, null, getResponse);
        assertThat(getResponse.getContentAsString(), containsString("new-name-for-template"));
        assertThat(getResponse.getContentAsString(), is(modifiedXml));

        setCurrentUser("user");
        MockHttpServletResponse nonAdminResponse = new MockHttpServletResponse();
        controller.getPipelineAsXmlPartial(0, TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME, null, nonAdminResponse);
        assertThat(nonAdminResponse.getStatus(), is(SC_UNAUTHORIZED));
        assertThat(nonAdminResponse.getContentAsString(), is("User 'user' does not have permission to administer pipeline templates"));
    }

    @Test public void shouldGetPipelineAsPartialXml() throws Exception {
        //get the pipeline XML
        configHelper.addPipeline("pipeline", "dev", "linux", "windows");
        groupName = BasicPipelineConfigs.DEFAULT_GROUP;
        controller.getPipelineAsXmlPartial(0, groupName, null, response);
        String xml = response.getContentAsString();
        assertThat(xml, containsString("pass"));

        //save the pipeline XML
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        String modifiedXml = xml.replace("pass", "secret");
        controller.postPipelineAsXmlPartial(0, groupName, modifiedXml, goConfigDao.md5OfConfigFile(), postResponse);

        //get the pipeline XML again
        MockHttpServletResponse getResponse = new MockHttpServletResponse();
        controller.getPipelineAsXmlPartial(0, groupName, null, getResponse);
        assertThat(getResponse.getContentAsString(), containsString("secret"));
        assertThat(getResponse.getContentAsString(), is(modifiedXml));
    }

    @Test public void shouldGetErrorMessageWhenPipelineDoesNotExist() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1");
        controller.getPipelineAsXmlPartial(1, groupName, null, response);
        assertValidContentAndStatus(SC_NOT_FOUND, RESPONSE_CHARSET, "Pipeline does not exist.");
    }

    @Test public void shouldPostPipelineAsPartialXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String newXml = NEW_PIPELINE;
        String md5 = goConfigDao.md5OfConfigFile();
        groupName = BasicPipelineConfigs.DEFAULT_GROUP;
        ModelAndView mav = controller.postPipelineAsXmlPartial(0, groupName, newXml, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : 'Pipeline changed successfully.' }"
        );
    }

    @Test
    public void postGroupAsXmlPartial_shouldEnforcePipelineGroupAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.postGroupAsXmlPartial("studios", NEW_GROUP, md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));

        controller.postGroupAsXmlPartial("consulting", NEW_GROUP, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test
    public void postGroupAsXmlPartial_shouldEnforcePipelineGroupAdminPermissionsForPipelineTemplates() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        XmlUtils.validate(new FileInputStream(configHelper.getConfigFile()), GoConfigSchema.getCurrentSchema(), new XsdErrorTranslator(), new SAXBuilder(), registry.xsds());
        controller.postGroupAsXmlPartial(TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME, NEW_TEMPLATES, md5, response);

        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));
        XmlUtils.validate(new FileInputStream(configHelper.getConfigFile()), GoConfigSchema.getCurrentSchema(), new XsdErrorTranslator(), new SAXBuilder(), registry.xsds());

        setCurrentUser("admin");
        controller.postGroupAsXmlPartial(TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME, NEW_TEMPLATES, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test
    public void postPipelineAsXmlPartial_shouldEnforcePipelineGroupAdminPermissionsForPipelineTemplates() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.postPipelineAsXmlPartial(0, TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME, NEW_PIPELINE_TEMPLATE, md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));

        setCurrentUser("admin");
        controller.postPipelineAsXmlPartial(0, TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME, NEW_PIPELINE_TEMPLATE, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test public void postPipelineAsXmlPartial_shouldEnforcePipelineGroupAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.postPipelineAsXmlPartial(0, "studios", NEW_PIPELINE, md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));

        controller.postPipelineAsXmlPartial(0, "consulting", NEW_PIPELINE, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test public void postStageAsXmlPartial_shouldEnforcePipelineGroupAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.postStageAsXmlPartial("go",0, NEW_STAGE, md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));

        controller.postStageAsXmlPartial("bcg", 0, NEW_STAGE, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test public void postBuildAsXmlPartial_shouldEnforcePipelineGroupAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.postBuildAsXmlPartial("go", "stage", 0, NEW_BUILD, md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));

        controller.postBuildAsXmlPartial("bcg", "stage", 0, NEW_BUILD, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test
    public void getGroupAsXmlPartial_shouldEnforcePipelineGroupAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.getGroupAsXmlPartial("studios", md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));
        assertThat(response.getContentAsString(), is("User 'ram' does not have permissions to administer pipeline group 'studios'"));

        controller.getGroupAsXmlPartial("consulting", md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test
    public void getPipelineAsXmlPartial_shouldEnforcePipelineGroupAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.getPipelineAsXmlPartial(0, "studios", md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));
        assertThat(response.getContentAsString(), is("User 'ram' does not have permissions to administer pipeline group 'studios'"));

        controller.getPipelineAsXmlPartial(0, "consulting", md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test
    public void getStageAsXmlPartial_shouldEnforcePipelineGroupAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.getStageAsXmlPartial("go", 0, md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));
        assertThat(response.getContentAsString(), is("User 'ram' does not have permissions to administer pipeline group 'studios'"));

        controller.getStageAsXmlPartial("bcg", 0, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test
    public void getBuildAsXmlPartial_shouldEnforcePipelineGroupAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.getBuildAsXmlPartial("go", "stage", 0, md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));
        assertThat(response.getContentAsString(), is("User 'ram' does not have permissions to administer pipeline group 'studios'"));

        controller.getBuildAsXmlPartial("bcg", "stage", 0, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test
    public void getPipelineTemplateAsXmlPartial_shouldEnforceAdminPermissions() throws Exception {
        String md5 = setUpPipelineGroupsWithAdminPermissions();

        controller.getBuildAsXmlPartial("go", "stage", 0, md5, response);
        assertThat(response.getStatus(), is(SC_UNAUTHORIZED));
        assertThat(response.getContentAsString(), is("User 'ram' does not have permissions to administer pipeline group 'studios'"));

        controller.getBuildAsXmlPartial("bcg", "stage", 0, md5, response);
        assertThat(response.getStatus(), is(SC_OK));
    }

    private String setUpPipelineGroupsWithAdminPermissions() throws IOException {
        setCurrentUser("ram");
        assertThat(CaseInsensitiveString.str(UserHelper.getUserName().getUsername()), is("ram"));


        configHelper.addPipelineWithGroup("studios", "go", "stage", "build1", "build2");
        configHelper.setAdminPermissionForGroup("studios", "barrow");

        configHelper.addPipelineWithGroup("consulting", "bcg", "stage", "build1", "build2");
        configHelper.setAdminPermissionForGroup("consulting", "ram");
        configHelper.addTemplate("newTemplate", "stage");

        return goConfigDao.md5OfConfigFile();
    }

    private void setCurrentUser(String username) {
        originalSecurityContext = SecurityContextHolder.getContext();
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new User(username, "", true, new GrantedAuthority[]{}), null));
        SecurityContextHolder.setContext(context);
    }

    @Test public void shouldReturnXmlAndErrorMessageWhenInvalidPostOfPipelineAsPartialXml() throws Exception {
        groupName = BasicPipelineConfigs.DEFAULT_GROUP;
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postPipelineAsXmlPartial(4, groupName, NEW_PIPELINE, md5, response);
        assertThat(response.getStatus(), is(SC_NOT_FOUND));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        JsonMap jsonMap = new JsonMap();
        jsonMap.put("result", "Pipeline does not exist.");
        jsonMap.put("originalContent", NEW_PIPELINE);
        assertThat(json, is((Json) jsonMap));
    }

    @Test public void shouldReturnXmlAndErrorMessageWhenPostOfPipelineAsInvalidXml() throws Exception {
        groupName = BasicPipelineConfigs.DEFAULT_GROUP;
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String badXml = "<;askldjfa;dsklfja;sdjas;lkdf";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postPipelineAsXmlPartial(0, groupName, badXml, md5, response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        Json json = (Json) mav.getModel().get("json");
        new JsonTester(json).shouldContain(
                "{ 'result' : 'Error on line 1 of document  : The markup in the document preceding the root element must be well-formed. Nested exception: The markup in the document preceding the root element must be well-formed.',"
                        + "  'originalContent' : '" + badXml + "' }"
        );
    }

    @Test public void shouldReturnXmlAndErrorMessageWhenPostOfPipelineAsInvalidPartialXml() throws Exception {
        groupName = BasicPipelineConfigs.DEFAULT_GROUP;
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");
        String badXml = "<pipeline name=\"cruise\" labeltemplate=\"invalid\">\n"
                + "  <materials>\n"
                + "    <svn url=\"file:///tmp/foo\" checkexternals=\"true\" />\n"
                + "  </materials>\n"
                + "  <stage name=\"dev\">\n"
                + "    <jobs>\n"
                + "      <job name=\"linux\" />\n"
                + "      <job name=\"windows\" />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>";
        String md5 = goConfigDao.md5OfConfigFile();
        ModelAndView mav = controller.postPipelineAsXmlPartial(0, groupName, badXml, md5, response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        assertThat(response.getContentType(), is(RESPONSE_CHARSET_JSON));
        JsonMap json = (JsonMap) mav.getModel().get("json");
        assertThat(json.get("result").toString(), containsString("Label is invalid"));
        assertThat(json.get("originalContent"), is((Json) new JsonString(badXml)));
    }

    @Test public void shouldReturnConflictIfGetRequestUsesIncorrectConfigMD5() throws Exception {
        String oldMd5 = goConfigDao.md5OfConfigFile();

        configHelper.addPipeline("pipeline", "stage", "build1", "build2");

        String newMd5 = goConfigDao.md5OfConfigFile();

        controller.getBuildAsXmlPartial("pipeline", "stage", 1, oldMd5, response);
        assertValidContentAndStatus(SC_CONFLICT, RESPONSE_CHARSET, ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH);
        assertThat((String) response.getHeader(XmlAction.X_CRUISE_CONFIG_MD5), is(newMd5));
    }

    private void assertValidContentAndStatus(int status, String contentType, String content) throws Exception {
        RestfulActionTestHelper.assertValidContentAndStatus(response, status, contentType, content);
    }

}
