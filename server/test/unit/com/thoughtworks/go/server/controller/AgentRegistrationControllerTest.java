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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentConfigService;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AgentRegistrationControllerTest {
    private static final String AGENT_CHECKSUM_FIELD = "agentChecksum";
    private static final String AGENT_LAUNCHER_CHECKSUM_FIELD = "agentLauncherChecksum";
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
    private PluginsZip pluginsZip;
    private AgentConfigService agentConfigService;

    @Before
    public void setUp() throws Exception {
        agentService = mock(AgentService.class);
        agentConfigService = mock(AgentConfigService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        goConfigService = mock(GoConfigService.class);

        when(agentService.agentJarInputStream()).thenReturn(new ByteArrayInputStream(EXPECTED.getBytes()));
        when(agentService.agentLauncherJarInputStream()).thenReturn(new ByteArrayInputStream(EXPECTED_LAUNCHER.getBytes()));

        when(systemEnvironment.getSslServerPort()).thenReturn(8443);
        pluginsZip = mock(PluginsZip.class);
        controller = new AgentRegistrationController(agentService, goConfigService, systemEnvironment, pluginsZip, agentConfigService);
    }

    @Test
    public void shouldRegisterWithProvidedAgentInformation() throws Exception {
        when(goConfigService.hasAgent("blahAgent-uuid")).thenReturn(false);
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "1", null);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.agentUsername("blahAgent-uuid", request.getRemoteAddr(), "blahAgent-host")).thenReturn(new Username("some-agent-login-name"));

        ModelAndView modelAndView = controller.agentRequest("blahAgent-host", "blahAgent-uuid", "blah-location", "34567", "osx", "", "", "", "", "", "", false, request);
        assertThat(modelAndView.getView().getContentType(), is("application/json"));

        verify(agentService).requestRegistration(new Username("some-agent-login-name"), AgentRuntimeInfo.fromServer(new AgentConfig("blahAgent-uuid", "blahAgent-host", request.getRemoteAddr()), false, "blah-location", 34567L, "osx", false));
    }

    @Test
    public void shouldAutoRegisterAgent() throws Exception {
        String uuid = "uuid";
        when(goConfigService.hasAgent(uuid)).thenReturn(false);
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "1", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        when(agentService.agentUsername(uuid, request.getRemoteAddr(), "host")).thenReturn(new Username("some-agent-login-name"));
        when(agentConfigService.updateAgent(any(UpdateConfigCommand.class), eq(uuid), any(HttpOperationResult.class), eq(new Username("some-agent-login-name"))))
                .thenReturn(new AgentConfig(uuid, "host", request.getRemoteAddr()));
        controller.agentRequest("host", uuid, "location", "233232", "osx", "someKey", "", "", "", "", "", false, request);

        verify(agentService).requestRegistration(new Username("some-agent-login-name"), AgentRuntimeInfo.fromServer(new AgentConfig(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx", false));
        verify(agentConfigService).updateAgent(any(UpdateConfigCommand.class), eq(uuid), any(HttpOperationResult.class), eq(new Username("some-agent-login-name")));
    }

    @Test
    public void shouldAutoRegisterAgentWithHostnameFromAutoRegisterProperties() throws Exception {
        String uuid = "uuid";
        when(goConfigService.hasAgent(uuid)).thenReturn(false);
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "1", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.agentUsername(uuid, request.getRemoteAddr(), "autoregister-hostname")).thenReturn(new Username("some-agent-login-name"));
        when(agentConfigService.updateAgent(any(UpdateConfigCommand.class), eq(uuid), any(HttpOperationResult.class), eq(new Username("some-agent-login-name"))))
                .thenReturn(new AgentConfig(uuid, "autoregister-hostname", request.getRemoteAddr()));

        controller.agentRequest("host", uuid, "location", "233232", "osx", "someKey", "", "", "autoregister-hostname", "", "", false, request);

        verify(agentService).requestRegistration(new Username("some-agent-login-name"), AgentRuntimeInfo.fromServer(
                new AgentConfig(uuid, "autoregister-hostname", request.getRemoteAddr()), false, "location", 233232L, "osx", false));
        verify(agentConfigService).updateAgent(any(UpdateConfigCommand.class), eq(uuid), any(HttpOperationResult.class), eq(new Username("some-agent-login-name")));
    }

    @Test
    public void shouldNotAutoRegisterAgentIfKeysDoNotMatch() throws Exception {
        String uuid = "uuid";
        when(goConfigService.hasAgent(uuid)).thenReturn(false);
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "1", "");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        when(agentService.agentUsername(uuid, request.getRemoteAddr(), "host")).thenReturn(new Username("some-agent-login-name"));
        controller.agentRequest("host", uuid, "location", "233232", "osx", "", "", "", "", "", "", false, request);

        verify(agentService).requestRegistration(new Username("some-agent-login-name"), AgentRuntimeInfo.fromServer(new AgentConfig(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx", false));
        verify(goConfigService, never()).updateConfig(any(UpdateConfigCommand.class));
    }

    @Test
    public void shouldReturnAgentJarWhenRequested() throws Exception {
        ModelAndView modelAndView = controller.downloadAgent();
        modelAndView.getView().render(null, request, response);
        String actual = response.getContentAsString();
        assertEquals(EXPECTED, actual);
    }

    @Test
    public void shouldReturnCorrectContentType() throws Exception {
        ModelAndView modelAndView = controller.downloadAgent();
        assertEquals("application/octet-stream", modelAndView.getView().getContentType());
    }

    @Test
    public void headShouldIncludeMd5Checksum_forAgent_whenCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, EXPECTED_MD5);

        controller.checkAgentVersion(response);
        assertEquals(EXPECTED_MD5, response.getHeader("Content-MD5"));
    }

    @Test
    public void checkAgentStatusShouldIncludeMd5Checksum_forAgent_forLauncher_whenChecksumsAreNotCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, null);
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, null);

        when(pluginsZip.md5()).thenReturn("md5");

        controller.checkAgentStatus(response);
        assertEquals(EXPECTED_MD5, response.getHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER));
        assertEquals(EXPECTED_LAUNCHER_MD5, response.getHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER));
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void checkAgentStatusShouldIncludeMd5Checksum_forAgent_forLauncher_whenChecksumsAreCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, "foo");
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, "bar");

        when(pluginsZip.md5()).thenReturn("md5");

        controller.checkAgentStatus(response);
        assertEquals("foo", response.getHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER));
        assertEquals("bar", response.getHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER));
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void checkAgentStatusShouldIncludeComponentVersionsOnServer() throws IOException {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, "foo");
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, "bar");

        when(pluginsZip.md5()).thenReturn("md5");
        controller.latestAgentStatus(response);

        assertEquals("foo", response.getHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER));
        assertEquals("bar", response.getHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER));
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void headShouldIncludeMd5Checksum_forAgent_whenNotCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, null);

        controller.checkAgentVersion(response);
        assertEquals(EXPECTED_MD5, response.getHeader("Content-MD5"));
    }

    @Test
    public void headShouldIncludeServerUrl_forAgent() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, EXPECTED_MD5);

        controller.checkAgentVersion(response);
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void headShouldIncludeMd5Checksum_forAgentLauncher_whenCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, EXPECTED_LAUNCHER_MD5);

        controller.checkAgentLauncherVersion(response);
        assertEquals(EXPECTED_LAUNCHER_MD5, response.getHeader("Content-MD5"));
    }

    @Test
    public void headShouldIncludeMd5Checksum_forAgentLauncher_whenNotCached() throws Exception {
        ReflectionUtil.setField(controller, AGENT_LAUNCHER_CHECKSUM_FIELD, null);

        controller.checkAgentLauncherVersion(response);
        assertEquals(EXPECTED_LAUNCHER_MD5, response.getHeader("Content-MD5"));
    }

    @Test
    public void headShouldIncludeServerUrl_forAgentLauncher() throws Exception {
        ReflectionUtil.setField(controller, AGENT_CHECKSUM_FIELD, EXPECTED_MD5);

        controller.checkAgentLauncherVersion(response);
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void contentShouldIncludeMd5Checksum_forAgent() throws Exception {
        ModelAndView modelAndView = controller.downloadAgent();
        modelAndView.getView().render(null, request, response);
        String actual = response.getHeader("Content-MD5");
        assertEquals(StringUtil.md5Digest(EXPECTED.getBytes()), actual);
    }

    @Test
    public void contentShouldIncludeMd5Checksum_forAgentLauncher() throws Exception {
        ModelAndView modelAndView = controller.downloadAgentLauncher();
        modelAndView.getView().render(null, request, response);
        String actual = response.getHeader("Content-MD5");
        assertEquals(StringUtil.md5Digest(EXPECTED_LAUNCHER.getBytes()), actual);
    }

    @Test
    public void checkAgentStatusShouldIncludeMd5Checksum_forAllPlugins() throws Exception {
        when(pluginsZip.md5()).thenReturn("md5");
        controller.checkAgentStatus(response);
        assertThat(response.getHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER), is("md5"));
        verify(pluginsZip).md5();
    }

    @Test
    public void headShouldIncludeMd5Checksum_forPluginsZip() throws Exception {
        when(pluginsZip.md5()).thenReturn("md5");
        controller.checkAgentPluginsZipStatus(response);
        assertEquals("md5", response.getHeader("Content-MD5"));
        verify(pluginsZip).md5();
    }

    @Test
    public void shouldReturnAgentPluginsZipWhenRequested() throws Exception {
        File pluginZipFile = TestFileUtil.createTempFile("plugins.zip");
        FileUtils.writeStringToFile(pluginZipFile, "content");
        when(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)).thenReturn(pluginZipFile.getAbsolutePath());

        ModelAndView modelAndView = controller.downloadPluginsZip();

        modelAndView.getView().render(null, request, response);
        String actual = response.getContentAsString();
        assertEquals("application/octet-stream", modelAndView.getView().getContentType());
        assertEquals("content", actual);
    }

}
