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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.FileDigester;
import com.thoughtworks.go.util.JsonTester;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.json.JsonMap;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.jmock.Expectations.equal;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgentRegistrationControllerTest {
    private static final String AGENT_CHECKSUM_FIELD = "agentChecksum";
    private static final String AGENT_LAUNCHER_CHECKSUM_FIELD = "agentLauncherChecksum";
    private static final String AGENT_PLUGINS_CHECKSUM_FIELD = "agentPluginsChecksum";
    private static final String UUID = "uuid";
    private static final String EXPECTED = "test";
    private static final String EXPECTED_MD5 = "CY9rzUYh03PK3k6DJie09g==";
    private static final String EXPECTED_LAUNCHER = "test-launcher";
    private static final String EXPECTED_LAUNCHER_MD5 = "z3ouYEnfWUG8ewU/izre9g==";
    private static final Username USERNAME = new Username(new CaseInsensitiveString("anonymous"));
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private AgentService agentService;
    private GoConfigService goConfigService;
    private AgentRegistrationController controller;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        agentService = mock(AgentService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        goConfigService = mock(GoConfigService.class);

        when(agentService.agentJarInputStream()).thenReturn(new ByteArrayInputStream(EXPECTED.getBytes()));
        when(agentService.agentLauncherJarInputStream()).thenReturn(new ByteArrayInputStream(EXPECTED_LAUNCHER.getBytes()));

        when(systemEnvironment.getSslServerPort()).thenReturn(8443);
        controller = new AgentRegistrationController(agentService, goConfigService, systemEnvironment);
        ReflectionUtil.setField(controller, AGENT_PLUGINS_CHECKSUM_FIELD, "default");
    }

    @Test
    public void shouldRegisterWithProvidedAgentInformation() throws Exception {
        when(goConfigService.hasAgent("blahAgent-uuid")).thenReturn(false);
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "1", null);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        controller.agentRequest("blahAgent-host", "blahAgent-uuid", "blah-location", "34567", "osx", "", "", "", request, response);
        verify(agentService).requestRegistration(AgentRuntimeInfo.fromServer(new AgentConfig("blahAgent-uuid", "blahAgent-host", request.getRemoteAddr()), false, "blah-location", 34567L, "osx"));
    }

    @Test
    public void shouldAutoRegisterAgent() throws Exception {
        String uuid = "uuid";
        when(goConfigService.hasAgent(uuid)).thenReturn(false);
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "1", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        controller.agentRequest("host", uuid, "location", "233232", "osx", "someKey", "", "", request, response);

        verify(agentService).requestRegistration(AgentRuntimeInfo.fromServer(new AgentConfig(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx"));
        verify(goConfigService).updateConfig(any(UpdateConfigCommand.class));
    }

    @Test
    public void shouldNotAutoRegisterAgentIfKeysDoNotMatch() throws Exception {
        String uuid = "uuid";
        when(goConfigService.hasAgent(uuid)).thenReturn(false);
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "1", "");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        controller.agentRequest("host", uuid, "location", "233232", "osx", "", "", "", request, response);

        verify(agentService).requestRegistration(AgentRuntimeInfo.fromServer(new AgentConfig(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx"));
        verify(goConfigService, never()).updateConfig(any(UpdateConfigCommand.class));
    }

    @Test
    public void shouldReturnAgentJarWhenRequested() throws Exception {
        ModelAndView modelAndView = controller.downloadAgent(null, null);
        modelAndView.getView().render(null, request, response);
        String actual = response.getContentAsString();
        assertEquals(EXPECTED, actual);
    }

    @Test
    public void shouldReturnCorrectContentType() throws Exception {
        ModelAndView modelAndView = controller.downloadAgent(null, null);
        assertEquals("application/octet-stream", modelAndView.getView().getContentType());
    }

    @Test
    public void headShouldIncludeMd5Checksum_forAgent_whenCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, EXPECTED_MD5);

        controller.checkAgentVersion(request, response);
        assertEquals(EXPECTED_MD5, response.getHeader("Content-MD5"));
    }

    @Test
    public void checkAgentStatusShouldIncludeMd5Checksum_forAgent_forLauncher_whenChecksumsAreNotCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, null);
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, null);

        controller.checkAgentStatus(request, response);
        assertEquals(EXPECTED_MD5, response.getHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER));
        assertEquals(EXPECTED_LAUNCHER_MD5, response.getHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER));
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void checkAgentStatusShouldIncludeMd5Checksum_forAgent_forLauncher_whenChecksumsAreCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, "foo");
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, "bar");

        controller.checkAgentStatus(request, response);
        assertEquals("foo", response.getHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER));
        assertEquals("bar", response.getHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER));
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void checkAgentStatusShouldIncludeComponentVersionsOnServer() throws IOException {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, "foo");
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, "bar");

        controller.latestAgentStatus(request, response);

        assertEquals("foo", response.getHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER));
        assertEquals("bar", response.getHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER));
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void headShouldIncludeMd5Checksum_forAgent_whenNotCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, null);

        controller.checkAgentVersion(request, response);
        assertEquals(EXPECTED_MD5, response.getHeader("Content-MD5"));
    }

    @Test
    public void headShouldIncludeServerUrl_forAgent() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, EXPECTED_MD5);

        controller.checkAgentVersion(request, response);
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void headShouldIncludeMd5Checksum_forAgentLauncher_whenCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, EXPECTED_LAUNCHER_MD5);

        controller.checkAgentLauncherVersion(request, response);
        assertEquals(EXPECTED_LAUNCHER_MD5, response.getHeader("Content-MD5"));
    }

    @Test
    public void headShouldIncludeMd5Checksum_forAgentLauncher_whenNotCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, null);

        controller.checkAgentLauncherVersion(request, response);
        assertEquals(EXPECTED_LAUNCHER_MD5, response.getHeader("Content-MD5"));
    }

    @Test
    public void headShouldIncludeServerUrl_forAgentLauncher() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, EXPECTED_MD5);

        controller.checkAgentLauncherVersion(request, response);
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void contentShouldIncludeMd5Checksum_forAgent() throws Exception {
        ModelAndView modelAndView = controller.downloadAgent(null, null);
        modelAndView.getView().render(null, request, response);
        String actual = response.getHeader("Content-MD5");
        assertEquals(StringUtil.md5Digest(EXPECTED.getBytes()), actual);
    }

    @Test
    public void contentShouldIncludeMd5Checksum_forAgentLauncher() throws Exception {
        ModelAndView modelAndView = controller.downloadAgentLauncher(null, null);
        modelAndView.getView().render(null, request, response);
        String actual = response.getHeader("Content-MD5");
        assertEquals(StringUtil.md5Digest(EXPECTED_LAUNCHER.getBytes()), actual);
    }

    @Test
    public void shouldRegisterUnregisteredAgentWhenRequestRegister() throws Exception {

        request.setMethod("POST");
        request.addParameter("uuid", UUID);
        ModelAndView modelAndView = controller.registerAgent(response, UUID);
        JsonMap result = (JsonMap) modelAndView.getModel().get("json");

        new JsonTester(result).is(
                "{ 'result' : 'success' }"
        );
        verify(agentService).approve(UUID);
        assertThat(HttpServletResponse.SC_CREATED, is(equal(response.getStatus())));
    }

    @Test
    public void shouldRejectUnregisteredAgentWhenRequestReject() throws Exception {


        request.setMethod("POST");
        request.addParameter("uuid", UUID);
        ModelAndView modelAndView = controller.denyAgent(response, UUID);
        JsonMap result = (JsonMap) modelAndView.getModel().get("json");

        new JsonTester(result).is(
                "{ 'result' : 'success' }"
        );

        assertThat(HttpServletResponse.SC_CREATED, is(equal(response.getStatus())));
        verify(agentService).disableAgents(eq(USERNAME), any(HttpOperationResult.class), eq(Arrays.asList(UUID)));
    }

    @Test
    public void shouldReturnHelpfulMessageIfExceptionOccurs() throws Exception {
        final String exceptionMessage = "The agent at test.com is already registered.";
        doThrow(new RuntimeException(exceptionMessage)).when(agentService).approve(UUID);
        request.setMethod("POST");
        request.addParameter("uuid", UUID);
        ModelAndView modelAndView = controller.registerAgent(response, UUID);
        JsonMap result = (JsonMap) modelAndView.getModel().get("json");

        new JsonTester(result).is(
                "{ 'result' : 'failed',"
                        + "  '" + ERROR_FOR_JSON + "' : '" + exceptionMessage + "' }"
        );
    }

    @Test
    public void checkAgentStatusShouldIncludeMd5Checksum_forAllPlugins_whenChecksumsAreNotCached() throws Exception {
        try {
            ReflectionUtil.setField(controller, AGENT_PLUGINS_CHECKSUM_FIELD, null);
            File pluginZipFile = TestFileUtil.createTempFile("plugins.zip");
            when(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)).thenReturn(pluginZipFile.getAbsolutePath());
            controller.checkAgentStatus(request, response);
            assertEquals(FileDigester.md5DigestOfFile(pluginZipFile), response.getHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER));
        } finally {
            TestFileUtil.cleanTempFiles();
        }
    }

    @Test
    public void checkAgentStatusShouldIncludeMd5Checksum_forAllPlugins_whenChecksumsAreCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_PLUGINS_CHECKSUM_FIELD, "default");
        controller.checkAgentStatus(request, response);
        assertEquals("default", response.getHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER));
        verify(systemEnvironment, never()).get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH);
    }

    @Test
    public void headShouldIncludeMd5Checksum_forPluginsZip_whenCached() throws Exception {
        try {
            ReflectionUtil.setField(controller, AGENT_PLUGINS_CHECKSUM_FIELD, null);
            File pluginZipFile = TestFileUtil.createTempFile("plugins.zip");
            when(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)).thenReturn(pluginZipFile.getAbsolutePath());
            controller.checkAgentPluginsZipStatus(request, response);
            assertEquals(FileDigester.md5DigestOfFile(pluginZipFile), response.getHeader("Content-MD5"));
        } finally {
            TestFileUtil.cleanTempFiles();
        }
    }

    @Test
    public void headShouldIncludeMd5Checksum_forPluginsZip_whenNotCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_PLUGINS_CHECKSUM_FIELD, "default");
        controller.checkAgentPluginsZipStatus(request, response);
        assertEquals("default", response.getHeader("Content-MD5"));
        verify(systemEnvironment, never()).get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH);
    }

    @Test
    public void shouldReturnAgentPluginsZipWhenRequested() throws Exception {
        File pluginZipFile = TestFileUtil.createTempFile("plugins.zip");
        FileUtils.writeStringToFile(pluginZipFile, "content");
        when(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)).thenReturn(pluginZipFile.getAbsolutePath());

        ModelAndView modelAndView = controller.downloadPluginsZip(null, null);

        modelAndView.getView().render(null, request, response);
        String actual = response.getContentAsString();
        assertEquals("application/octet-stream", modelAndView.getView().getContentType());
        assertEquals("content", actual);
    }

}