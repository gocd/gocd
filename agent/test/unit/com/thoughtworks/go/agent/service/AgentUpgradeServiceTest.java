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

package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.URLService;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
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
    private GetMethod httpMethod;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        urlService = mock(URLService.class);
        HttpClient httpClient = mock(HttpClient.class);
        agentUpgradeService = spy(new AgentUpgradeService(urlService, httpClient, systemEnvironment));

        httpMethod = mock(GetMethod.class);
        doReturn(httpMethod).when(agentUpgradeService).getAgentLatestStatusGetMethod();
        when(httpClient.executeMethod(httpMethod)).thenReturn(200);
    }

    @Test
    public void checkForUpgradeShouldKillAgentIfAgentMD5doesNotMatch() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("old-md5");

        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "new-md5");

        doThrow(new RuntimeException("Agent md5 mismatch")).when(agentUpgradeService).jvmExit();

        try {
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), Is.is("Agent md5 mismatch"));
        }
        verify(httpMethod, never()).getResponseHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER);

    }

    @Test
    public void checkForUpgradeShouldKillAgentIfLauncherMD5doesNotMatch() throws Exception {
        when(systemEnvironment.getAgentMd5()).thenReturn("latest-md5");
        when(systemEnvironment.getGivenAgentLauncherMd5()).thenReturn("old-md5");

        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "new-md5");

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

        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "latest-md5");

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

        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "latest-md5");
        expectHeaderValue(httpMethod, SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "latest-md5");


        doThrow(new RuntimeException("Agent Plugins md5 mismatch")).when(agentUpgradeService).jvmExit();

        try {
            agentUpgradeService.checkForUpgrade();
            fail("should have done jvm exit");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), Is.is("Agent Plugins md5 mismatch"));
        }
    }

    private void expectHeaderValue(GetMethod getMethod, final String headerName, final String headerValue) {
        Header header = mock(Header.class);
        when(getMethod.getResponseHeader(headerName)).thenReturn(header);
        when(header.getValue()).thenReturn(headerValue);
    }
}
