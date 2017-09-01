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
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AgentUpgradeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentUpgradeService.class);
    static final Marker FATAL = MarkerFactory.getMarker("FATAL");
    private final GoAgentServerHttpClient httpClient;
    private final SystemEnvironment systemEnvironment;
    private URLService urlService;
    private JvmExitter jvmExitter;

    interface JvmExitter {
        void jvmExit(String type, String oldChecksum, String newChecksum);
    }

    static class DefaultJvmExitter implements JvmExitter {
        @Override
        public void jvmExit(String type, String oldChecksum, String newChecksum) {
            LOGGER.error(FATAL,"[Agent Upgrade] Agent needs to upgrade {}. Currently has md5 {} but server version has md5 {}. Exiting.", type, oldChecksum, newChecksum);
            System.exit(0);
        }
    }

    @Autowired
    AgentUpgradeService(URLService urlService, GoAgentServerHttpClient httpClient, SystemEnvironment systemEnvironment) throws Exception {
        this(urlService, httpClient, systemEnvironment, new DefaultJvmExitter());
    }

    AgentUpgradeService(URLService urlService, GoAgentServerHttpClient httpClient, SystemEnvironment systemEnvironment, JvmExitter jvmExitter) throws Exception {
        this.httpClient = httpClient;
        this.systemEnvironment = systemEnvironment;
        this.urlService = urlService;
        this.jvmExitter = jvmExitter;
    }

    public void checkForUpgrade() throws Exception {
        if (!"".equals(systemEnvironment.getAgentMd5())) {
            checkForUpgrade(systemEnvironment.getAgentMd5(), systemEnvironment.getGivenAgentLauncherMd5(), systemEnvironment.getAgentPluginsMd5(), systemEnvironment.getTfsImplMd5());
        }
    }

    void checkForUpgrade(String agentMd5, String launcherMd5, String agentPluginsMd5, String tfsImplMd5) throws Exception {
        HttpGet method = getAgentLatestStatusGetMethod();
        try (final CloseableHttpResponse response = httpClient.execute(method)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.error("[Agent Upgrade] Got status %d {} from Go", response.getStatusLine().getStatusCode(), response.getStatusLine());
                return;
            }
            validateMd5(agentMd5, response, SystemEnvironment.AGENT_CONTENT_MD5_HEADER, "itself");
            validateMd5(launcherMd5, response, SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, "launcher");
            validateMd5(agentPluginsMd5, response, SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, "plugins");
            validateMd5(tfsImplMd5, response, SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER, "tfs-impl jar");
        } catch (IOException ioe) {
            String message = String.format("[Agent Upgrade] Couldn't connect to: %s: %s", urlService.getAgentLatestStatusUrl(), ioe.toString());
            LOGGER.error(message);
            LOGGER.debug(message, ioe);
            throw ioe;
        } finally {
            method.releaseConnection();
        }
    }

    private void validateMd5(String currentMd5, CloseableHttpResponse response, String agentContentMd5Header, String what) {
        final Header md5Header = response.getFirstHeader(agentContentMd5Header);
        if (!"".equals(currentMd5)) {
            if (!currentMd5.equals(md5Header.getValue())) {
                jvmExitter.jvmExit(what, currentMd5, md5Header.getValue());
            }
        }
    }

    HttpGet getAgentLatestStatusGetMethod() {
        return new HttpGet(urlService.getAgentLatestStatusUrl());
    }

}
