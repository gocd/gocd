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

package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.URLService;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgentUpgradeServiceTest {

    private SystemEnvironment systemEnvironment;
    private URLService urlService;
    private AgentUpgradeService agentUpgradeService;
    private HttpGet httpMethod;
    private CloseableHttpResponse closeableHttpResponse;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        urlService = mock(URLService.class);
        GoAgentServerHttpClient httpClient = mock(GoAgentServerHttpClient.class);
        agentUpgradeService = spy(new AgentUpgradeService(urlService, httpClient, systemEnvironment));

        httpMethod = mock(HttpGet.class);
        doReturn(httpMethod).when(agentUpgradeService).getAgentLatestStatusGetMethod();
        closeableHttpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpClient.execute(httpMethod)).thenReturn(closeableHttpResponse);
    }

    @Test
    public void checkForUpgradeShouldKillAgentIfAgentMD5doesNotMatch() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("old-md5");

        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "new-md5");

        doThrow(new RuntimeException("Agent md5 mismatch")).when(agentUpgradeService).jvmExit();

        try {
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), Is.is("Agent md5 mismatch"));
        }
        verify(closeableHttpResponse, never()).getHeaders(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER);

    }

    @Test
    public void checkForUpgradeShouldKillAgentIfLauncherMD5doesNotMatch() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("old-md5");

        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "new-md5");

        doThrow(new RuntimeException("Agent Launcher md5 mismatch")).when(agentUpgradeService).jvmExit();

        try {
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), Is.is("Agent Launcher md5 mismatch"));
        }
    }

    @Test
    public void checkForUpgradeShouldNotKillAgentIfServerVersionsAreCompatible() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getAgentPluginsMd5()).thenReturn("latest-md5");

        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "latest-md5");

        doThrow(new RuntimeException("Agent Launcher md5 mismatch")).when(agentUpgradeService).jvmExit();

        try {
            agentUpgradeService.checkForUpgrade();
        } catch (Exception e) {
            fail("should not have done jvm exit");
        }
    }

    @Test
    public void checkForUpgradeShouldKillAgentIfPluginZipMd5doesNotMatch() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getAgentPluginsMd5()).thenReturn("old-md5");

        expectHeaderValue(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "latest-md5");


        doThrow(new RuntimeException("Agent Plugins md5 mismatch")).when(agentUpgradeService).jvmExit();

        try {
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), Is.is("Agent Plugins md5 mismatch"));
        }
    }

    private void expectHeaderValue(final String headerName, final String headerValue) {
        when(closeableHttpResponse.getFirstHeader(headerName)).thenReturn(new BasicHeader(headerName, headerValue));
    }
}
