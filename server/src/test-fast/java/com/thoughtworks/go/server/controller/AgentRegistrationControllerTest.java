/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.JarDetector;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentConfigService;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import static com.thoughtworks.go.util.SystemEnvironment.AGENT_EXTRA_PROPERTIES;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.CONFLICT;

public class AgentRegistrationControllerTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private AgentService agentService;
    private GoConfigService goConfigService;
    private AgentRegistrationController controller;
    private SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;
    private AgentConfigService agentConfigService;
    private File pluginZipFile;

    @Before
    public void setUp() throws Exception {
        agentService = mock(AgentService.class);
        agentConfigService = mock(AgentConfigService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        goConfigService = mock(GoConfigService.class);
        pluginZipFile = temporaryFolder.newFile("plugins.zip");
        FileUtils.writeStringToFile(pluginZipFile, "content", UTF_8);
        when(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)).thenReturn(pluginZipFile.getAbsolutePath());
        when(systemEnvironment.getSslServerPort()).thenReturn(8443);
        when(systemEnvironment.get(AGENT_EXTRA_PROPERTIES)).thenReturn("");
        pluginsZip = mock(PluginsZip.class);
        controller = new AgentRegistrationController(agentService, goConfigService, systemEnvironment, pluginsZip, agentConfigService);
        controller.populateAgentChecksum();
        controller.populateLauncherChecksum();
        controller.populateTFSSDKChecksum();
    }

    @Test
    public void shouldRegisterWithProvidedAgentInformation() {
        when(goConfigService.hasAgent("blahAgent-uuid")).thenReturn(false);
        ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.agentUsername("blahAgent-uuid", request.getRemoteAddr(), "blahAgent-host")).thenReturn(new Username("some-agent-login-name"));

        controller.agentRequest("blahAgent-host", "blahAgent-uuid", "blah-location", "34567", "osx", "", "", "", "", "", "", token("blahAgent-uuid", serverConfig.getTokenGenerationKey()), request);

        verify(agentService).requestRegistration(new Username("some-agent-login-name"), AgentRuntimeInfo.fromServer(new AgentConfig("blahAgent-uuid", "blahAgent-host", request.getRemoteAddr()), false, "blah-location", 34567L, "osx"));
    }

    @Test
    public void shouldAutoRegisterAgent() {
        String uuid = "uuid";
        final ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        final String token = token(uuid, serverConfig.getTokenGenerationKey());

        when(goConfigService.hasAgent(uuid)).thenReturn(false);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        when(agentService.agentUsername(uuid, request.getRemoteAddr(), "host")).thenReturn(new Username("some-agent-login-name"));
        when(agentConfigService.updateAgent(any(UpdateConfigCommand.class), eq(uuid), any(HttpOperationResult.class), eq(new Username("some-agent-login-name"))))
                .thenReturn(new AgentConfig(uuid, "host", request.getRemoteAddr()));
        controller.agentRequest("host", uuid, "location", "233232", "osx", "someKey", "", "", "", "", "", token, request);

        verify(agentService).requestRegistration(new Username("some-agent-login-name"), AgentRuntimeInfo.fromServer(new AgentConfig(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx"));
        verify(agentConfigService).updateAgent(any(UpdateConfigCommand.class), eq(uuid), any(HttpOperationResult.class), eq(new Username("some-agent-login-name")));
    }

    @Test
    public void shouldAutoRegisterAgentWithHostnameFromAutoRegisterProperties() {
        String uuid = "uuid";
        when(goConfigService.hasAgent(uuid)).thenReturn(false);
        ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.agentUsername(uuid, request.getRemoteAddr(), "autoregister-hostname")).thenReturn(new Username("some-agent-login-name"));
        when(agentConfigService.updateAgent(any(UpdateConfigCommand.class), eq(uuid), any(HttpOperationResult.class), eq(new Username("some-agent-login-name"))))
                .thenReturn(new AgentConfig(uuid, "autoregister-hostname", request.getRemoteAddr()));

        controller.agentRequest("host", uuid, "location", "233232", "osx", "someKey", "", "", "autoregister-hostname", "", "", token(uuid, serverConfig.getTokenGenerationKey()), request);

        verify(agentService).requestRegistration(new Username("some-agent-login-name"), AgentRuntimeInfo.fromServer(
                new AgentConfig(uuid, "autoregister-hostname", request.getRemoteAddr()), false, "location", 233232L, "osx"));
        verify(agentConfigService).updateAgent(any(UpdateConfigCommand.class), eq(uuid), any(HttpOperationResult.class), eq(new Username("some-agent-login-name")));
    }

    @Test
    public void shouldNotAutoRegisterAgentIfKeysDoNotMatch() {
        String uuid = "uuid";
        when(goConfigService.hasAgent(uuid)).thenReturn(false);
        ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        when(agentService.agentUsername(uuid, request.getRemoteAddr(), "host")).thenReturn(new Username("some-agent-login-name"));
        controller.agentRequest("host", uuid, "location", "233232", "osx", "", "", "", "", "", "", token(uuid, serverConfig.getTokenGenerationKey()), request);

        verify(agentService).requestRegistration(new Username("some-agent-login-name"), AgentRuntimeInfo.fromServer(new AgentConfig(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx"));
        verify(goConfigService, never()).updateConfig(any(UpdateConfigCommand.class));
    }

    @Test
    public void checkAgentStatusShouldIncludeMd5Checksum_forAgent_forLauncher_whenChecksumsAreCached() throws Exception {
        when(pluginsZip.md5()).thenReturn("plugins-zip-md5");
        when(systemEnvironment.get(AGENT_EXTRA_PROPERTIES)).thenReturn("extra=property");

        controller.checkAgentStatus(response);

        try (InputStream stream = JarDetector.tfsJar(systemEnvironment).getJarURL().openStream()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader(SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER));
        }

        try (InputStream stream = JarDetector.create(systemEnvironment, "agent-launcher.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER));
        }

        try (InputStream stream = JarDetector.create(systemEnvironment, "agent.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER));
        }

        assertEquals("plugins-zip-md5", response.getHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER));
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
    }

    @Test
    public void checkAgentStatusShouldIncludeExtraPropertiesInBase64() {
        final String extraPropertiesValue = "extra=property another=extra.property";
        final String base64ExtraPropertiesValue = java.util.Base64.getEncoder().encodeToString(extraPropertiesValue.getBytes(UTF_8));

        when(pluginsZip.md5()).thenReturn("plugins-zip-md5");
        when(systemEnvironment.get(AGENT_EXTRA_PROPERTIES)).thenReturn(extraPropertiesValue);

        controller.checkAgentStatus(response);

        assertEquals(base64ExtraPropertiesValue, response.getHeader(SystemEnvironment.AGENT_EXTRA_PROPERTIES_HEADER));
    }

    @Test
    public void headShouldIncludeMd5ChecksumAndServerUrl_forAgent() throws Exception {
        controller.checkAgentVersion(response);
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));

        try (InputStream stream = JarDetector.create(systemEnvironment, "agent.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
    }

    @Test
    public void headShouldIncludeMd5ChecksumAndServerUrl_forAgentLauncher() throws Exception {
        controller.checkAgentLauncherVersion(response);
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));

        try (InputStream stream = JarDetector.create(systemEnvironment, "agent-launcher.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
    }

    @Test
    public void contentShouldIncludeMd5Checksum_forAgent() throws Exception {
        controller.downloadAgent(response);
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
        assertEquals("application/octet-stream", response.getContentType());

        try (InputStream stream = JarDetector.create(systemEnvironment, "agent.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
        try (InputStream is = JarDetector.create(systemEnvironment, "agent.jar").invoke()) {
            assertTrue(Arrays.equals(IOUtils.toByteArray(is), response.getContentAsByteArray()));
        }
    }

    @Test
    public void contentShouldIncludeExtraAgentPropertiesInBase64_forAgent() throws IOException {
        final String extraPropertiesValue = "extra=property another=extra.property";
        final String base64ExtraPropertiesValue = getEncoder().encodeToString(extraPropertiesValue.getBytes(UTF_8));

        when(systemEnvironment.get(AGENT_EXTRA_PROPERTIES)).thenReturn(extraPropertiesValue);

        controller.downloadAgent(response);

        assertEquals(base64ExtraPropertiesValue, response.getHeader(SystemEnvironment.AGENT_EXTRA_PROPERTIES_HEADER));
    }

    @Test
    public void shouldSendAnEmptyStringInBase64_AsAgentExtraProperties_IfTheValueIsTooBigAfterConvertingToBase64() throws IOException {
        final String longExtraPropertiesValue = StringUtils.rightPad("", AgentRegistrationController.MAX_HEADER_LENGTH, "z");
        final String expectedValueToBeUsedForProperties = "";
        final String expectedBase64ExtraPropertiesValue = getEncoder().encodeToString(expectedValueToBeUsedForProperties.getBytes(UTF_8));

        when(systemEnvironment.get(AGENT_EXTRA_PROPERTIES)).thenReturn(longExtraPropertiesValue);

        controller.downloadAgent(response);

        assertEquals(expectedBase64ExtraPropertiesValue, response.getHeader(SystemEnvironment.AGENT_EXTRA_PROPERTIES_HEADER));
    }

    @Test
    public void contentShouldIncludeMd5Checksum_forAgentLauncher() throws Exception {
        controller.downloadAgentLauncher(response);
        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
        assertEquals("application/octet-stream", response.getContentType());

        try (InputStream stream = JarDetector.create(systemEnvironment, "agent-launcher.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
        try (InputStream is = JarDetector.create(systemEnvironment, "agent-launcher.jar").invoke()) {
            assertTrue(Arrays.equals(IOUtils.toByteArray(is), response.getContentAsByteArray()));
        }
    }

    @Test
    public void headShouldIncludeMd5Checksum_forPluginsZip() {
        when(pluginsZip.md5()).thenReturn("md5");
        controller.checkAgentPluginsZipStatus(response);

        assertEquals("8443", response.getHeader("Cruise-Server-Ssl-Port"));
        assertEquals("md5", response.getHeader("Content-MD5"));
        verify(pluginsZip).md5();
    }

    @Test
    public void shouldReturnAgentPluginsZipWhenRequested() throws Exception {
        when(pluginsZip.md5()).thenReturn("md5");

        controller.downloadPluginsZip(response);

        String actual = response.getContentAsString();
        assertEquals("application/octet-stream", response.getContentType());
        assertEquals("content", actual);
    }

    @Test
    public void shouldReturnChecksumOfTfsJar() throws Exception {
        controller.checkTfsImplVersion(response);
        try (InputStream stream = JarDetector.tfsJar(systemEnvironment).getJarURL().openStream()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
    }

    @Test
    public void shouldRenderTheTfsJar() throws Exception {
        controller.downloadTfsImplJar(response);
        assertEquals("application/octet-stream", response.getContentType());

        try (InputStream stream = JarDetector.tfsJar(systemEnvironment).getJarURL().openStream()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
        try (InputStream is = JarDetector.tfsJar(systemEnvironment).getJarURL().openStream()) {
            assertTrue(Arrays.equals(IOUtils.toByteArray(is), response.getContentAsByteArray()));
        }
    }

    @Test
    public void shouldGenerateToken() {
        final ServerConfig serverConfig = mockedServerConfig("agent-auto-register-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.findAgent("uuid-from-agent")).thenReturn(AgentInstanceMother.idle());
        when(goConfigService.hasAgent("uuid-from-agent")).thenReturn(false);

        final ResponseEntity responseEntity = controller.getToken("uuid-from-agent");

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(responseEntity.getBody(), is("JCmJaW6YbEA4fIUqf8L9lRV81ua10wV+wRYOFdaBLcM="));
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfAgentIsInPendingState() {
        final ServerConfig serverConfig = mockedServerConfig("agent-auto-register-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.findAgent("uuid-from-agent")).thenReturn(AgentInstanceMother.pendingInstance());
        when(goConfigService.hasAgent("uuid-from-agent")).thenReturn(false);

        final ResponseEntity responseEntity = controller.getToken("uuid-from-agent");

        assertThat(responseEntity.getStatusCode(), is(CONFLICT));
        assertThat(responseEntity.getBody(), is("A token has already been issued for this agent."));
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfAgentIsInConfig() {
        final ServerConfig serverConfig = mockedServerConfig("agent-auto-register-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.findAgent("uuid-from-agent")).thenReturn(AgentInstanceMother.idle());
        when(goConfigService.hasAgent("uuid-from-agent")).thenReturn(true);

        final ResponseEntity responseEntity = controller.getToken("uuid-from-agent");

        assertThat(responseEntity.getStatusCode(), is(CONFLICT));
        assertThat(responseEntity.getBody(), is("A token has already been issued for this agent."));
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfUUIDIsEmpty() {
        final ResponseEntity responseEntity = controller.getToken("               ");

        assertThat(responseEntity.getStatusCode(), is(CONFLICT));
        assertThat(responseEntity.getBody(), is("UUID cannot be blank."));
    }

    @Test
    public void shouldRejectRegistrationRequestWhenInvalidTokenProvided() {
        when(goConfigService.hasAgent("blahAgent-uuid")).thenReturn(false);
        ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.agentUsername("blahAgent-uuid", request.getRemoteAddr(), "blahAgent-host")).thenReturn(new Username("some-agent-login-name"));

        ResponseEntity responseEntity = controller.agentRequest("blahAgent-host", "blahAgent-uuid", "blah-location", "34567", "osx", "", "", "", "", "", "", "an-invalid-token", request);

        assertThat(responseEntity.getBody(), is("Not a valid token."));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.FORBIDDEN));

        verify(serverConfig, times(0)).shouldAutoRegisterAgentWith("someKey");
        verifyZeroInteractions(agentService);
        verifyZeroInteractions(agentConfigService);
    }

    private String token(String uuid, String tokenGenerationKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(tokenGenerationKey.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            return Base64.getEncoder().encodeToString(mac.doFinal(uuid.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private ServerConfig mockedServerConfig(String tokenGenerationKey, String agentAutoRegisterKey) {
        final ServerConfig serverConfig = mock(ServerConfig.class);
        when(serverConfig.getTokenGenerationKey()).thenReturn(tokenGenerationKey);
        when(serverConfig.getAgentAutoRegisterKey()).thenReturn(agentAutoRegisterKey);
        when(serverConfig.shouldAutoRegisterAgentWith(agentAutoRegisterKey)).thenReturn(true);
        when(serverConfig.security()).thenReturn(new SecurityConfig());
        return serverConfig;
    }
}
