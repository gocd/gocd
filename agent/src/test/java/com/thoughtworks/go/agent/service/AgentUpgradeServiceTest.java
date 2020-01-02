/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.URLService;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TestRule;

import java.util.Map;
import java.util.Map.Entry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class AgentUpgradeServiceTest {
    @Rule
    public final TestRule restoreSystemProperties = new RestoreSystemProperties();

    private SystemEnvironment systemEnvironment;
    private AgentUpgradeService agentUpgradeService;
    private CloseableHttpResponse closeableHttpResponse;
    private AgentUpgradeService.JvmExitter jvmExitter;

    @BeforeEach
    void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        URLService urlService = mock(URLService.class);
        GoAgentServerHttpClient httpClient = mock(GoAgentServerHttpClient.class);
        jvmExitter = mock(AgentUpgradeService.JvmExitter.class);
        agentUpgradeService = spy(new AgentUpgradeService(urlService, httpClient, systemEnvironment, jvmExitter));

        HttpGet httpMethod = mock(HttpGet.class);
        doReturn(httpMethod).when(agentUpgradeService).getAgentLatestStatusGetMethod();
        closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpClient.execute(httpMethod)).thenReturn(closeableHttpResponse);
    }

    @Test
    void checkForUpgradeShouldNotKillAgentIfAllDownloadsAreCompatible() throws Exception {
        setupForNoChangesToMD5();

        agentUpgradeService.checkForUpgradeAndExtraProperties();

        verify(jvmExitter, never()).jvmExit(anyString(), anyString(), anyString());
    }

    @Test
    void checkForUpgradeShouldKillAgentIfAgentMD5doesNotMatch() {
        when(systemEnvironment.getAgentMd5()).thenReturn("old-agent-md5");
        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "new-agent-md5");

        RuntimeException toBeThrown = new RuntimeException("Boo!");
        doThrow(toBeThrown).when(jvmExitter).jvmExit(anyString(), anyString(), anyString());

        try {
            agentUpgradeService.checkForUpgradeAndExtraProperties();
            fail("should have done jvm exit");
        } catch (Exception e) {
            assertThat(toBeThrown).isSameAs(e);
        }

        verify(jvmExitter).jvmExit("itself", "old-agent-md5", "new-agent-md5");
    }

    @Test
    void checkForUpgradeShouldKillAgentIfLauncherMD5doesNotMatch() {
        when(systemEnvironment.getAgentMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "not-changing");

        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("old-launcher-md5");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "new-launcher-md5");

        RuntimeException toBeThrown = new RuntimeException("Boo!");
        doThrow(toBeThrown).when(jvmExitter).jvmExit(anyString(), anyString(), anyString());

        try {
            agentUpgradeService.checkForUpgradeAndExtraProperties();
            fail("should have done jvm exit");
        } catch (Exception e) {
            assertThat(toBeThrown).isSameAs(e);
        }

        verify(jvmExitter).jvmExit("launcher", "old-launcher-md5", "new-launcher-md5");
    }

    @Test
    void checkForUpgradeShouldKillAgentIfPluginZipMd5doesNotMatch() {
        when(systemEnvironment.getAgentMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "not-changing");

        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "not-changing");

        when(systemEnvironment.getAgentPluginsMd5()).thenReturn("old-plugins-md5");
        expectHeaderValue(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "new-plugins-md5");

        RuntimeException toBeThrown = new RuntimeException("Boo!");
        doThrow(toBeThrown).when(jvmExitter).jvmExit(anyString(), anyString(), anyString());

        try {
            agentUpgradeService.checkForUpgradeAndExtraProperties();
            fail("should have done jvm exit");
        } catch (Exception e) {
            assertThat(toBeThrown).isSameAs(e);
        }

        verify(jvmExitter).jvmExit("plugins", "old-plugins-md5", "new-plugins-md5");
    }

    @Test
    void checkForUpgradeShouldKillAgentIfTfsMd5doesNotMatch() {
        when(systemEnvironment.getAgentMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "not-changing");

        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "not-changing");

        when(systemEnvironment.getAgentPluginsMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "not-changing");

        when(systemEnvironment.getTfsImplMd5()).thenReturn("old-tfs-md5");
        expectHeaderValue(SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER, "new-tfs-md5");

        RuntimeException toBeThrown = new RuntimeException("Boo!");
        doThrow(toBeThrown).when(jvmExitter).jvmExit(anyString(), anyString(), anyString());

        try {
            agentUpgradeService.checkForUpgradeAndExtraProperties();
            fail("should have done jvm exit");
        } catch (Exception e) {
            assertThat(toBeThrown).isSameAs(e);
        }

        verify(jvmExitter).jvmExit("tfs-impl jar", "old-tfs-md5", "new-tfs-md5");
    }

    @Test
    void shouldSetAnyExtraPropertiesSentByTheServer() throws Exception {
        setupForNoChangesToMD5();

        expectHeaderValue(SystemEnvironment.AGENT_EXTRA_PROPERTIES_HEADER, encodeBase64String("abc=def%20ghi  jkl%20mno=pqr%20stu".getBytes(UTF_8)));
        agentUpgradeService.checkForUpgradeAndExtraProperties();

        assertThat(System.getProperty("abc")).isEqualTo("def ghi");
        assertThat(System.getProperty("jkl mno")).isEqualTo("pqr stu");
    }

    @Test
    void shouldFailQuietlyWhenExtraPropertiesHeaderValueIsInvalid() throws Exception {
        setupForNoChangesToMD5();

        final Map<Object, Object> before = System.getProperties().entrySet().stream().collect(toMap(Entry::getKey, Entry::getValue));

        expectHeaderValue(SystemEnvironment.AGENT_EXTRA_PROPERTIES_HEADER, encodeBase64String("this_is_invalid".getBytes(UTF_8)));
        agentUpgradeService.checkForUpgradeAndExtraProperties();

        final Map<Object, Object> after = System.getProperties().entrySet().stream().collect(toMap(Entry::getKey, Entry::getValue));

        assertThat(after).isEqualTo(before);
    }

    private void setupForNoChangesToMD5() {
        when(systemEnvironment.getAgentMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getAgentPluginsMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getTfsImplMd5()).thenReturn("latest-md5");

        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER, "latest-md5");
    }

    private void expectHeaderValue(final String headerName, final String headerValue) {
        expectHeader(headerName, new BasicHeader(headerName, headerValue));
    }

    private void expectHeader(String headerName, BasicHeader header) {
        when(closeableHttpResponse.getFirstHeader(headerName)).thenReturn(header);
    }
}
