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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.domain.JarDetector;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.thoughtworks.go.util.SystemEnvironment.AGENT_EXTRA_PROPERTIES;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.CONFLICT;

public class AgentRegistrationControllerTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private AgentService agentService;
    private GoConfigService goConfigService;
    private AgentRegistrationController controller;
    private SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;
    private EphemeralAutoRegisterKeyService ephemeralAutoRegisterKeyService;

    @BeforeEach
    public void setUp(@TempDir Path temporaryFolder) throws Exception {
        agentService = mock(AgentService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        goConfigService = mock(GoConfigService.class);
        ephemeralAutoRegisterKeyService = mock(EphemeralAutoRegisterKeyService.class);
        File pluginZipFile = Files.createFile(temporaryFolder.resolve("plugins.zip")).toFile();
        Files.writeString(pluginZipFile.toPath(), "content", UTF_8);
        when(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)).thenReturn(pluginZipFile.getAbsolutePath());
        when(systemEnvironment.get(AGENT_EXTRA_PROPERTIES)).thenReturn("");
        pluginsZip = mock(PluginsZip.class);
        controller = new AgentRegistrationController(agentService, goConfigService, systemEnvironment, pluginsZip, ephemeralAutoRegisterKeyService);
        controller.populateAgentChecksum();
        controller.populateLauncherChecksum();
        controller.populateTFSSDKChecksum();
    }

    @Test
    public void shouldRegisterWithProvidedAgentInformation() {
        when(agentService.isRegistered("blahAgent-uuid")).thenReturn(false);
        ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.createAgentUsername("blahAgent-uuid", request.getRemoteAddr(), "blahAgent-host")).thenReturn(new Username("some-agent-login-name"));

        controller.agentRequest("blahAgent-host", "blahAgent-uuid", "blah-location", "34567", "osx", "", "", "", "", "", "", controller.hmacOf("blahAgent-uuid"), request);

        verify(agentService).requestRegistration(AgentRuntimeInfo.fromServer(new Agent("blahAgent-uuid", "blahAgent-host", request.getRemoteAddr()), false, "blah-location", 34567L, "osx"));
    }

    @Test
    public void shouldAutoRegisterAgent() {
        String uuid = "uuid";
        final ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        final String token = controller.hmacOf(uuid);

        when(agentService.isRegistered(uuid)).thenReturn(false);
        when(agentService.createAgentUsername(uuid, request.getRemoteAddr(), "host")).thenReturn(new Username("some-agent-login-name"));

        controller.agentRequest("host", uuid, "location", "233232", "osx", "someKey", "", "", "", "", "", token, request);

        verify(agentService).requestRegistration(AgentRuntimeInfo.fromServer(new Agent(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx"));
        verify(agentService).register(any(Agent.class));
    }

    @Test
    public void shouldAutoRegisterAgentWithHostnameFromAutoRegisterProperties() {
        String uuid = "uuid";
        when(agentService.isRegistered(uuid)).thenReturn(false);
        ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.createAgentUsername(uuid, request.getRemoteAddr(), "autoregister-hostname")).thenReturn(new Username("some-agent-login-name"));

        controller.agentRequest("host", uuid, "location", "233232", "osx", "someKey", "", "", "autoregister-hostname", "", "", controller.hmacOf(uuid), request);

        verify(agentService).requestRegistration(AgentRuntimeInfo.fromServer(
                new Agent(uuid, "autoregister-hostname", request.getRemoteAddr()), false, "location", 233232L, "osx"));
        verify(agentService).register(any(Agent.class));
    }

    @Test
    public void shouldNotAutoRegisterAgentIfKeysDoNotMatch() {
        String uuid = "uuid";
        when(agentService.isRegistered(uuid)).thenReturn(false);
        ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        when(agentService.createAgentUsername(uuid, request.getRemoteAddr(), "host")).thenReturn(new Username("some-agent-login-name"));
        controller.agentRequest("host", uuid, "location", "233232", "osx", "", "", "", "", "", "", controller.hmacOf(uuid), request);

        verify(agentService).requestRegistration(AgentRuntimeInfo.fromServer(new Agent(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx"));
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

        try (InputStream stream = JarDetector.createFromRelativeDefaultFile(systemEnvironment, "agent-launcher.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER));
        }

        try (InputStream stream = JarDetector.createFromRelativeDefaultFile(systemEnvironment, "agent.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER));
        }

        assertEquals("plugins-zip-md5", response.getHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER));
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

        try (InputStream stream = JarDetector.createFromRelativeDefaultFile(systemEnvironment, "agent.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
    }

    @Test
    public void headShouldIncludeMd5ChecksumAndServerUrl_forAgentLauncher() throws Exception {
        controller.checkAgentLauncherVersion(response);

        try (InputStream stream = JarDetector.createFromRelativeDefaultFile(systemEnvironment, "agent-launcher.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
    }

    @Test
    public void contentShouldIncludeMd5Checksum_forAgent() throws Exception {
        controller.downloadAgent(response);
        assertEquals("application/octet-stream", response.getContentType());

        try (InputStream stream = JarDetector.createFromRelativeDefaultFile(systemEnvironment, "agent.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
        try (InputStream is = JarDetector.createFromRelativeDefaultFile(systemEnvironment, "agent.jar").invoke()) {
            assertArrayEquals(is.readAllBytes(), response.getContentAsByteArray());
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
        assertEquals("application/octet-stream", response.getContentType());

        try (InputStream stream = JarDetector.createFromRelativeDefaultFile(systemEnvironment, "agent-launcher.jar").invoke()) {
            assertEquals(DigestUtils.md5Hex(stream), response.getHeader("Content-MD5"));
        }
        try (InputStream is = JarDetector.createFromRelativeDefaultFile(systemEnvironment, "agent-launcher.jar").invoke()) {
            assertArrayEquals(is.readAllBytes(), response.getContentAsByteArray());
        }
    }

    @Test
    public void headShouldIncludeMd5Checksum_forPluginsZip() {
        when(pluginsZip.md5()).thenReturn("md5");
        controller.checkAgentPluginsZipStatus(response);

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
            assertArrayEquals(is.readAllBytes(), response.getContentAsByteArray());
        }
    }

    @Test
    public void shouldGenerateToken() {
        final ServerConfig serverConfig = mockedServerConfig("agent-auto-register-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.findAgent("uuid-from-agent")).thenReturn(AgentInstanceMother.idle());
        when(agentService.isRegistered("uuid-from-agent")).thenReturn(false);

        final ResponseEntity<String> responseEntity = controller.getToken("uuid-from-agent");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo("JCmJaW6YbEA4fIUqf8L9lRV81ua10wV+wRYOFdaBLcM=");
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfAgentIsInPendingState() {
        final ServerConfig serverConfig = mockedServerConfig("agent-auto-register-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.findAgent("uuid-from-agent")).thenReturn(AgentInstanceMother.pendingInstance());
        when(agentService.isRegistered("uuid-from-agent")).thenReturn(false);

        final ResponseEntity<String> responseEntity = controller.getToken("uuid-from-agent");

        assertThat(responseEntity.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(responseEntity.getBody()).isEqualTo("A token has already been issued for this agent.");
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfAgentIsInConfig() {
        final ServerConfig serverConfig = mockedServerConfig("agent-auto-register-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.findAgent("uuid-from-agent")).thenReturn(AgentInstanceMother.idle());
        when(agentService.isRegistered("uuid-from-agent")).thenReturn(true);

        final ResponseEntity<String> responseEntity = controller.getToken("uuid-from-agent");

        assertThat(responseEntity.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(responseEntity.getBody()).isEqualTo("A token has already been issued for this agent.");
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfUUIDIsEmpty() {
        final ResponseEntity<String> responseEntity = controller.getToken("               ");

        assertThat(responseEntity.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(responseEntity.getBody()).isEqualTo("UUID cannot be blank.");
    }

    @Test
    public void shouldRejectRegistrationRequestWhenInvalidTokenProvided() {
        when(agentService.isRegistered("blahAgent-uuid")).thenReturn(false);
        ServerConfig serverConfig = mockedServerConfig("token-generation-key", "someKey");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(agentService.createAgentUsername("blahAgent-uuid", request.getRemoteAddr(), "blahAgent-host")).thenReturn(new Username("some-agent-login-name"));

        ResponseEntity<String> responseEntity = controller.agentRequest("blahAgent-host", "blahAgent-uuid", "blah-location", "34567", "osx", "", "", "", "", "", "", "an-invalid-token", request);

        assertThat(responseEntity.getBody()).isEqualTo("Not a valid token.");
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        verify(serverConfig, times(0)).shouldAutoRegisterAgentWith("someKey");
        verifyNoMoreInteractions(agentService);
    }

    @Test
    public void shouldAutoRegisterElasticAgentIfEphemeralAutoRegisterKeyIsValid() {
        String uuid = "elastic-uuid";
        final ServerConfig serverConfig = mockedServerConfig("token-generation-key", "auto_register_key");
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        final String token = controller.hmacOf(uuid);

        when(agentService.isRegistered(uuid)).thenReturn(false);
        when(agentService.createAgentUsername(uuid, request.getRemoteAddr(), "host")).thenReturn(new Username("some-agent-login-name"));
        when(ephemeralAutoRegisterKeyService.validateAndRevoke("someKey")).thenReturn(true);

        String elasticAgentId = "elastic-agent-id";
        String elasticPluginId = "elastic-plugin-id";
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new Agent(uuid, "host", request.getRemoteAddr()), false, "location", 233232L, "osx");

        controller.agentRequest("host", uuid, "location", "233232", "osx", "someKey", "", "e1", "", elasticAgentId, elasticPluginId, token, request);

        verify(agentService).findElasticAgent(elasticAgentId, elasticPluginId);
        verify(agentService, times(2)).isRegistered(uuid);
        verify(agentService).register(any(Agent.class));
        verify(agentService).requestRegistration(ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, elasticAgentId, elasticPluginId));
    }

    @Test
    public void shouldNotRelyOnAutoRegisterKeyForRegisteringElasticAgents() {
        String uuid = "elastic-uuid";
        String autoRegisterKey = "auto_register_key";

        final ServerConfig serverConfig = mockedServerConfig("token-generation-key", autoRegisterKey);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        final String token = controller.hmacOf(uuid);

        when(agentService.isRegistered(uuid)).thenReturn(false);
        when(agentService.createAgentUsername(uuid, request.getRemoteAddr(), "host")).thenReturn(new Username("some-agent-login-name"));
        when(ephemeralAutoRegisterKeyService.validateAndRevoke(any())).thenReturn(false);

        controller.agentRequest("host", uuid, "location", "233232", "osx", autoRegisterKey,
                "", "e1", "", "elastic-agent-id",
                "elastic-plugin-id", token, request);

        verify(agentService, never()).register(any(Agent.class));
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
