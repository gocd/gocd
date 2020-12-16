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
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.URLService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.apache.http.HttpStatus.*;

@Service
public class SslInfrastructureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslInfrastructureService.class);
    private static final int REGISTER_RETRY_INTERVAL = 5000;
    private static final Marker FATAL = MarkerFactory.getMarker("FATAL");
    private final RemoteRegistrationRequester remoteRegistrationRequester;
    private final GoAgentServerHttpClient httpClient;
    private final TokenRequester tokenRequester;
    private final AgentRegistry agentRegistry;

    @Autowired
    public SslInfrastructureService(URLService urlService, GoAgentServerHttpClient httpClient, AgentRegistry agentRegistry) throws Exception {
        this(new RemoteRegistrationRequester(urlService.getAgentRegistrationURL(), agentRegistry, httpClient),
                httpClient,
                new TokenRequester(urlService.getTokenURL(), agentRegistry, httpClient),
                agentRegistry);
    }

    // For mocking out remote call
    SslInfrastructureService(RemoteRegistrationRequester requester, GoAgentServerHttpClient httpClient, TokenRequester tokenRequester, AgentRegistry agentRegistry) {
        this.remoteRegistrationRequester = requester;
        this.httpClient = httpClient;
        this.tokenRequester = tokenRequester;
        this.agentRegistry = agentRegistry;
    }

    public void createSslInfrastructure() {
        httpClient.reset();
    }

    public void registerIfNecessary(AgentAutoRegistrationProperties agentAutoRegistrationProperties) throws Exception {
        if (isRegistered()) {
            return;
        }
        LOGGER.info("[Agent Registration] Starting to register agent.");
        register(agentAutoRegistrationProperties);
        createSslInfrastructure();
        LOGGER.info("[Agent Registration] Successfully registered agent.");
    }

    protected void getTokenIfNecessary() throws IOException {
        if (isRegistered()) {
            return;
        }
        LOGGER.info("[Agent Registration] Fetching token from server.");
        final String token = tokenRequester.getToken();
        agentRegistry.storeTokenToDisk(token);
        LOGGER.info("[Agent Registration] Got a token from server.");
    }

    public boolean isRegistered() {
        return agentRegistry.tokenPresent();
    }

    protected void register(AgentAutoRegistrationProperties agentAutoRegistrationProperties) throws Exception {
        String hostName = SystemUtil.getLocalhostNameOrRandomNameIfNotFound();
        boolean registered = false;
        while (!registered) {
            try {
                getTokenIfNecessary();
                registered = remoteRegistrationRequester.requestRegistration(hostName, agentAutoRegistrationProperties);
            } catch (Exception e) {
                LOGGER.error("[Agent Registration] There was a problem registering with the go server.", e);
                throw e;
            }

            if ((!registered)) {
                try {
                    LOGGER.debug("[Agent Registration] Retrieved agent key from the GoCD server is not valid.");
                    Thread.sleep(REGISTER_RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    // Ok
                }
            }
        }
        LOGGER.info("[Agent Registration] Retrieved registration from Go server.");
        agentAutoRegistrationProperties.scrubRegistrationProperties();
    }

    public void invalidateAgentCertificate() {
        try {
            httpClient.reset();
        } catch (Exception e) {
            LOGGER.error(FATAL, "[Agent Registration] Error while deleting key from key store", e);
        }
    }

    public static class RemoteRegistrationRequester {
        private final AgentRegistry agentRegistry;
        private final String serverUrl;
        private final GoAgentServerHttpClient httpClient;

        public RemoteRegistrationRequester(String serverUrl, AgentRegistry agentRegistry, GoAgentServerHttpClient httpClient) {
            this.serverUrl = serverUrl;
            this.httpClient = httpClient;
            this.agentRegistry = agentRegistry;
        }

        protected boolean requestRegistration(String agentHostName, AgentAutoRegistrationProperties agentAutoRegisterProperties) throws IOException {
            LOGGER.debug("[Agent Registration] Using URL {} to register.", serverUrl);

            HttpRequestBase postMethod = (HttpRequestBase) RequestBuilder.post(serverUrl)
                    .addParameter("hostname", agentHostName)
                    .addParameter("uuid", agentRegistry.uuid())
                    .addParameter("location", SystemUtil.currentWorkingDirectory())
                    .addParameter("usablespace", String.valueOf(AgentRuntimeInfo.usableSpace(SystemUtil.currentWorkingDirectory())))
                    .addParameter("operatingSystem", new SystemEnvironment().getOperatingSystemCompleteName())
                    .addParameter("agentAutoRegisterKey", agentAutoRegisterProperties.agentAutoRegisterKey())
                    .addParameter("agentAutoRegisterResources", agentAutoRegisterProperties.agentAutoRegisterResources())
                    .addParameter("agentAutoRegisterEnvironments", agentAutoRegisterProperties.agentAutoRegisterEnvironments())
                    .addParameter("agentAutoRegisterHostname", agentAutoRegisterProperties.agentAutoRegisterHostname())
                    .addParameter("elasticAgentId", agentAutoRegisterProperties.agentAutoRegisterElasticAgentId())
                    .addParameter("elasticPluginId", agentAutoRegisterProperties.agentAutoRegisterElasticPluginId())
                    .addParameter("token", agentRegistry.token())
                    .build();

            try (CloseableHttpResponse response = httpClient.execute(postMethod)) {
                switch (getStatusCode(response)) {
                    case SC_ACCEPTED:
                        LOGGER.debug("The server has accepted the registration request.");
                        break;
                    case SC_FORBIDDEN:
                        LOGGER.debug("Server denied registration request due to invalid token. Deleting existing token from disk.");
                        agentRegistry.deleteToken();
                        break;
                    case SC_OK:
                        LOGGER.info("This agent is now approved by the server.");
                        return true;
                    case SC_UNPROCESSABLE_ENTITY:
                        LOGGER.error("Error occurred during agent registration process: {}", responseBody(response));
                        break;
                    default:
                        LOGGER.warn("The server sent a response that we could not understand. The HTTP status was {}. The response body was:\n{}", response.getStatusLine(), responseBody(response));
                }
            } finally {
                postMethod.releaseConnection();
            }
            return false;
        }

        private String responseBody(CloseableHttpResponse response) throws IOException {
            try (InputStream is = response.getEntity() == null ? new NullInputStream(0) : response.getEntity().getContent()) {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            }
        }

        protected int getStatusCode(CloseableHttpResponse response) {
            return response.getStatusLine().getStatusCode();
        }

    }
}
