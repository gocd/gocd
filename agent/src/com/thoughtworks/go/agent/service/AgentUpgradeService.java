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
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AgentUpgradeService {
    private static final Logger LOGGER = Logger.getLogger(AgentUpgradeService.class);
    private final GoAgentServerHttpClient httpClient;
    private final SystemEnvironment systemEnvironment;
    private URLService urlService;

    @Autowired
    public AgentUpgradeService(URLService urlService, GoAgentServerHttpClient httpClient, SystemEnvironment systemEnvironment) throws Exception {
        this.httpClient = httpClient;
        this.systemEnvironment = systemEnvironment;
        this.urlService = urlService;
    }

    public void checkForUpgrade() throws Exception {
        if (!"".equals(systemEnvironment.getAgentMd5())) {
            checkForUpgrade(systemEnvironment.getAgentMd5(), systemEnvironment.getGivenAgentLauncherMd5(), systemEnvironment.getAgentPluginsMd5());
        }
    }

    void checkForUpgrade(String md5, String launcherMd5, String agentPluginsMd5) throws Exception {
        HttpGet method = getAgentLatestStatusGetMethod();
        try (final CloseableHttpResponse response = httpClient.execute(method)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.error(String.format("[Agent Upgrade] Got status %d %s from Go", response.getStatusLine().getStatusCode(), response.getStatusLine()));
                return;
            }
            validateIfLatestAgent(md5, response);
            validateIfLatestLauncher(launcherMd5, response);
            validateIfLatestPluginZipAvailable(agentPluginsMd5, response);
        } catch (IOException ioe) {
            String message = String.format("[Agent Upgrade] Couldn't connect to: %s: %s", urlService.getAgentLatestStatusUrl(), ioe.toString());
            LOGGER.error(message);
            LOGGER.debug(message, ioe);
            throw ioe;
        } finally {
            method.releaseConnection();
        }
    }

    private void validateIfLatestPluginZipAvailable(String agentPluginsMd5, CloseableHttpResponse response) {
        final Header newLauncherMd5 = response.getFirstHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER);
        if (!"".equals(agentPluginsMd5)) {
            if (!agentPluginsMd5.equals(newLauncherMd5.getValue())) {
                LOGGER.fatal(
                        String.format("[Agent Launcher Upgrade] Agent needs to upgrade its plugins. Currently agents plugins has md5 [%s] but server's latest plugins md5 has md5 [%s]. Exiting.",
                                agentPluginsMd5,
                                newLauncherMd5));
                jvmExit();
            }
        }
    }

    private void validateIfLatestLauncher(String launcherMd5, CloseableHttpResponse response) {
        final Header newLauncherMd5 = response.getFirstHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER);
        if (!"".equals(launcherMd5)) {
            if (!launcherMd5.equals(newLauncherMd5.getValue())) {
                LOGGER.fatal(
                        String.format("[Agent Launcher Upgrade] Agent needs to upgrade its launcher. Currently launcher has md5 [%s] but server's latest launcher has md5 [%s]. Exiting.", launcherMd5,
                                newLauncherMd5));
                jvmExit();
            }
        }
    }

    private void validateIfLatestAgent(String md5, CloseableHttpResponse response) {
        final Header newAgentMd5 = response.getFirstHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER);
        if (!md5.equals(newAgentMd5.getValue())) {
            LOGGER.fatal(String.format("[Agent Upgrade] Agent needs to upgrade itself. Currently has md5 [%s] but server version has md5 [%s]. Exiting.", md5, newAgentMd5));
            jvmExit();
        }
    }

    HttpGet getAgentLatestStatusGetMethod() {
        return new HttpGet(urlService.getAgentLatestStatusUrl());
    }

    void jvmExit() {
        System.exit(0);
    }
}
