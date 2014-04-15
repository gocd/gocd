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

import java.io.IOException;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.URLService;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentUpgradeService {
    private static final Logger LOGGER = Logger.getLogger(AgentUpgradeService.class);
    private final HttpClient httpClient;
    private final SystemEnvironment systemEnvironment;
    private URLService urlService;

    @Autowired
    public AgentUpgradeService(URLService urlService, HttpClient httpClient, SystemEnvironment systemEnvironment) throws Exception {
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
        HttpMethod method = getAgentLatestStatusGetMethod();
        try {
            final int status = httpClient.executeMethod(method);
            if (status != 200) {
                LOGGER.error(String.format("[Agent Upgrade] Got status %d %s from Go", status, method.getStatusText()));
                return;
            }
            validateIfLatestAgent(md5, method);
            validateIfLatestLauncher(launcherMd5, method);
            validateIfLatestPluginZipAvailable(agentPluginsMd5, method);
        } catch (IOException ioe) {
            LOGGER.error(String.format("[Agent Upgrade] Couldn't connect to: %s: %s", urlService.getAgentLatestStatusUrl(), ioe.toString()));
            throw ioe;
        } finally {
            method.releaseConnection();
        }
    }

    private void validateIfLatestPluginZipAvailable(String agentPluginsMd5, HttpMethod method) {
        final Header newLauncherMd5 = method.getResponseHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER);
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

    private void validateIfLatestLauncher(String launcherMd5, HttpMethod method) {
        final Header newLauncherMd5 = method.getResponseHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER);
        if (!"".equals(launcherMd5)) {
            if (!launcherMd5.equals(newLauncherMd5.getValue())) {
                LOGGER.fatal(
                        String.format("[Agent Launcher Upgrade] Agent needs to upgrade its launcher. Currently launcher has md5 [%s] but server's latest launcher has md5 [%s]. Exiting.", launcherMd5,
                                newLauncherMd5));
                jvmExit();
            }
        }
    }

    private void validateIfLatestAgent(String md5, HttpMethod method) {
        final Header newAgentMd5 = method.getResponseHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER);
        if (!md5.equals(newAgentMd5.getValue())) {
            LOGGER.fatal(String.format("[Agent Upgrade] Agent needs to upgrade itself. Currently has md5 [%s] but server version has md5 [%s]. Exiting.", md5, newAgentMd5));
            jvmExit();
        }
    }

    GetMethod getAgentLatestStatusGetMethod() {
        return new GetMethod(urlService.getAgentLatestStatusUrl());
    }

    void jvmExit() {
        System.exit(0);
    }
}
