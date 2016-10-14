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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.domain.materials.tfs.TFSJarDetector;
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
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static com.thoughtworks.go.util.FileDigester.md5DigestOfStream;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AgentRegistrationControllerTest {
    private static final String AGENT_CHECKSUM_FIELD = "agentChecksum";
    private static final String AGENT_LAUNCHER_CHECKSUM_FIELD = "agentLauncherChecksum";
    private static final String EXPECTED = "test";
    private static final String EXPECTED_MD5 = "CY9rzUYh03PK3k6DJie09g==";
    private static final String EXPECTED_LAUNCHER = "test-launcher";
    private static final String EXPECTED_LAUNCHER_MD5 = "z3ouYEnfWUG8ewU/izre9g==";
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

        when(agentService.agentJarInputStream()).thenAnswer(new Answer<ByteArrayInputStream>() {
            @Override
            public ByteArrayInputStream answer(InvocationOnMock invocation) throws Throwable {
                return new ByteArrayInputStream(EXPECTED.getBytes());
            }
        });
        when(agentService.agentLauncherJarInputStream()).thenAnswer(new Answer<ByteArrayInputStream>() {
            @Override
            public ByteArrayInputStream answer(InvocationOnMock invocation) throws Throwable {
                return new ByteArrayInputStream(EXPECTED_LAUNCHER.getBytes());
            }
        });

        when(systemEnvironment.getSslServerPort()).thenReturn(8443);
        when(systemEnvironment.get(SystemEnvironment.TFS_SDK_10)).thenReturn(true);
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
        controller.downloadAgent(response);
        String actual = response.getContentAsString();
        assertEquals(EXPECTED, actual);
    }

    @Test
    public void shouldReturnCorrectContentType() throws Exception {
        controller.downloadAgent(response);
        assertEquals("application/octet-stream", response.getContentType());
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

        try (InputStream stream = new TFSJarDetector.DevelopmentServerTFSJarDetector(systemEnvironment).getJarURL().openStream()) {
            assertEquals(md5DigestOfStream(stream), response.getHeader(SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER));
        }

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
        controller.downloadAgent(response);
        String actual = response.getHeader("Content-MD5");
        assertEquals(StringUtil.md5Digest(EXPECTED.getBytes()), actual);
    }

    @Test
    public void contentShouldIncludeMd5Checksum_forAgentLauncher() throws Exception {
        controller.downloadAgentLauncher(response);
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
        when(pluginsZip.md5()).thenReturn("md5");
        when(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)).thenReturn(pluginZipFile.getAbsolutePath());

        controller.downloadPluginsZip(response);

        String actual = response.getContentAsString();
        assertEquals("application/octet-stream", response.getContentType());
        assertEquals("content", actual);
    }

    @Test
    public void shouldReturnChecksumOfTfsJar() throws Exception {
        controller.checkTfsImplVersion(response);
        try (InputStream stream = new TFSJarDetector.DevelopmentServerTFSJarDetector(systemEnvironment).getJarURL().openStream()) {
            assertEquals(md5DigestOfStream(stream), response.getHeader("Content-MD5"));
        }
    }

    @Test
    public void shouldRenderTheTfsJar() throws Exception {
        controller.downloadTfsImplJar(response);
        assertEquals("application/octet-stream", response.getContentType());

        try (InputStream stream = new TFSJarDetector.DevelopmentServerTFSJarDetector(systemEnvironment).getJarURL().openStream()) {
            assertEquals(md5DigestOfStream(stream), response.getHeader("Content-MD5"));
        }
        try (InputStream is = new TFSJarDetector.DevelopmentServerTFSJarDetector(systemEnvironment).getJarURL().openStream()) {
            assertTrue(Arrays.equals(IOUtils.toByteArray(is), response.getContentAsByteArray()));
        }
    }
}
