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

package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.URLService;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgentUpgradeServiceTest {

    private SystemEnvironment systemEnvironment;
    private URLService urlService;
    private AgentUpgradeService agentUpgradeService;
    private HttpGet httpMethod;
    private CloseableHttpResponse closeableHttpResponse;
    private AgentUpgradeService.JvmExitter jvmExitter;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        urlService = mock(URLService.class);
        GoAgentServerHttpClient httpClient = mock(GoAgentServerHttpClient.class);
        jvmExitter = mock(AgentUpgradeService.JvmExitter.class);
        agentUpgradeService = spy(new AgentUpgradeService(urlService, httpClient, systemEnvironment, jvmExitter));

        httpMethod = mock(HttpGet.class);
        doReturn(httpMethod).when(agentUpgradeService).getAgentLatestStatusGetMethod();
        closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpClient.execute(httpMethod)).thenReturn(closeableHttpResponse);
    }

    @Test
    public void checkForUpgradeShouldNotKillAgentIfAllDownloadsAreCompatible() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getAgentPluginsMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getTfsImplMd5()).thenReturn("latest-md5");

        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER, "latest-md5");

        agentUpgradeService.checkForUpgrade();
    }

    @Test
    public void checkForUpgradeShouldKillAgentIfAgentMD5doesNotMatch() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("old-agent-md5");
        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "new-agent-md5");

        RuntimeException toBeThrown = new RuntimeException("Boo!");
        doThrow(toBeThrown).when(jvmExitter).jvmExit(anyString(), anyString(), anyString());

        try {
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (Exception e) {
            assertSame(e, toBeThrown);
        }

        verify(jvmExitter).jvmExit("itself", "old-agent-md5", "new-agent-md5");
    }

    @Test
    public void checkForUpgradeShouldKillAgentIfLauncherMD5doesNotMatch() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "not-changing");

        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("old-launcher-md5");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "new-launcher-md5");

        RuntimeException toBeThrown = new RuntimeException("Boo!");
        doThrow(toBeThrown).when(jvmExitter).jvmExit(anyString(), anyString(), anyString());

        try {
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (Exception e) {
            assertSame(e, toBeThrown);
        }

        verify(jvmExitter).jvmExit("launcher", "old-launcher-md5", "new-launcher-md5");
    }

    @Test
    public void checkForUpgradeShouldKillAgentIfPluginZipMd5doesNotMatch() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "not-changing");

        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("not-changing");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "not-changing");

        when(systemEnvironment.getAgentPluginsMd5()).thenReturn("old-plugins-md5");
        expectHeaderValue(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "new-plugins-md5");

        RuntimeException toBeThrown = new RuntimeException("Boo!");
        doThrow(toBeThrown).when(jvmExitter).jvmExit(anyString(), anyString(), anyString());

        try {
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (Exception e) {
            assertSame(e, toBeThrown);
        }

        verify(jvmExitter).jvmExit("plugins", "old-plugins-md5", "new-plugins-md5");
    }

    @Test
    public void checkForUpgradeShouldKillAgentIfTfsMd5doesNotMatch() throws Exception {
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
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (Exception e) {
            assertSame(e, toBeThrown);
        }

        verify(jvmExitter).jvmExit("tfs-impl jar", "old-tfs-md5", "new-tfs-md5");
    }

    private void expectHeaderValue(final String headerName, final String headerValue) {
        when(closeableHttpResponse.getFirstHeader(headerName)).thenReturn(new BasicHeader(headerName, headerValue));
    }
}
