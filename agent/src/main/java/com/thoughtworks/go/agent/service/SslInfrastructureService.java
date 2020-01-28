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

import com.thoughtworks.go.agent.common.ssl.GoAgentServerClientBuilder;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.security.KeyStoreManager;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.RegistrationJSONizer;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.URLService;
import org.apache.commons.io.FileUtils;
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

import static com.thoughtworks.go.security.CertificateUtil.md5Fingerprint;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.apache.http.HttpStatus.*;

@Service
public class SslInfrastructureService {

    private static final String CHAIN_ALIAS = "agent";
    private static final Logger LOGGER = LoggerFactory.getLogger(SslInfrastructureService.class);
    private static final int REGISTER_RETRY_INTERVAL = 5000;
    private static final Marker FATAL = MarkerFactory.getMarker("FATAL");
    private final RemoteRegistrationRequester remoteRegistrationRequester;
    private final KeyStoreManager keyStoreManager;
    private final GoAgentServerHttpClient httpClient;
    private transient boolean registered = false;
    private TokenRequester tokenRequester;
    private AgentRegistry agentRegistry;

    @Autowired
    public SslInfrastructureService(URLService urlService, GoAgentServerHttpClient httpClient, AgentRegistry agentRegistry) throws Exception {
        this(new RemoteRegistrationRequester(urlService.getAgentRegistrationURL(), agentRegistry, httpClient),
                httpClient,
                new TokenRequester(urlService.getTokenURL(), agentRegistry, httpClient),
                agentRegistry);
    }

    // For mocking out remote call
    SslInfrastructureService(RemoteRegistrationRequester requester, GoAgentServerHttpClient httpClient, TokenRequester tokenRequester, AgentRegistry agentRegistry)
            throws Exception {
        this.remoteRegistrationRequester = requester;
        this.httpClient = httpClient;
        this.tokenRequester = tokenRequester;
        this.agentRegistry = agentRegistry;
        this.keyStoreManager = new KeyStoreManager();
        this.keyStoreManager.preload(GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE, httpClientBuilder().keystorePassword());
    }

    private GoAgentServerHttpClientBuilder httpClientBuilder() {
        return new GoAgentServerHttpClientBuilder(new SystemEnvironment());
    }

    public void createSslInfrastructure() throws IOException {
        httpClientBuilder().initialize();
        httpClient.reset();
    }

    public void registerIfNecessary(AgentAutoRegistrationProperties agentAutoRegistrationProperties) throws Exception {
        registered = keyStoreManager.hasCertificates(CHAIN_ALIAS, GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE,
                httpClientBuilder().keystorePassword()) && agentRegistry.guidPresent();
        if (!registered) {
            LOGGER.info("[Agent Registration] Starting to register agent.");
            register(agentAutoRegistrationProperties);
            createSslInfrastructure();
            registered = true;
            LOGGER.info("[Agent Registration] Successfully registered agent.");
        }
    }

    protected void getTokenIfNecessary() throws IOException, InterruptedException {
        if (!agentRegistry.tokenPresent()) {
            LOGGER.info("[Agent Registration] Fetching token from server.");
            final String token = tokenRequester.getToken();
            agentRegistry.storeTokenToDisk(token);
            LOGGER.info("[Agent Registration] Got a token from server.");
        }
    }

    public boolean isRegistered() {
        return registered;
    }

    private void register(AgentAutoRegistrationProperties agentAutoRegistrationProperties) throws Exception {
        String hostName = SystemUtil.getLocalhostNameOrRandomNameIfNotFound();
        Registration keyEntry = Registration.createNullPrivateKeyEntry();
        while (!keyEntry.isValid()) {
            try {
                getTokenIfNecessary();
                keyEntry = remoteRegistrationRequester.requestRegistration(hostName, agentAutoRegistrationProperties);
            } catch (Exception e) {
                LOGGER.error("[Agent Registration] There was a problem registering with the go server.", e);
                throw e;
            }

            if ((!keyEntry.isValid())) {
                try {
                    LOGGER.debug("[Agent Registration] Retrieved agent key from Go server is not valid.");
                    Thread.sleep(REGISTER_RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    // Ok
                }
            }
        }
        LOGGER.info("[Agent Registration] Retrieved registration from Go server.");
        storeChainIntoAgentStore(keyEntry);
        agentAutoRegistrationProperties.scrubRegistrationProperties();
    }

    private void storeChainIntoAgentStore(Registration keyEntry) {
        try {
            keyStoreManager.storeCertificate(CHAIN_ALIAS, GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE, httpClientBuilder().keystorePassword(), keyEntry);
            LOGGER.info("[Agent Registration] Stored registration for cert with hash code: {} not valid before: {}", md5Fingerprint(keyEntry.getFirstCertificate()), keyEntry.getCertificateNotBeforeDate());
        } catch (Exception e) {
            throw bomb("Couldn't save agent key into store", e);
        }
    }

    public void invalidateAgentCertificate() {
        try {
            httpClient.reset();
            keyStoreManager.deleteEntry(CHAIN_ALIAS, GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE, httpClientBuilder().keystorePassword());
        } catch (Exception e) {
            LOGGER.error(FATAL, "[Agent Registration] Error while deleting key from key store", e);
            deleteKeyStores();
        }
    }

    private void deleteKeyStores() {
        FileUtils.deleteQuietly(GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE);
    }

    public static class RemoteRegistrationRequester {
        private final AgentRegistry agentRegistry;
        private String serverUrl;
        private GoAgentServerHttpClient httpClient;

        public RemoteRegistrationRequester(String serverUrl, AgentRegistry agentRegistry, GoAgentServerHttpClient httpClient) {
            this.serverUrl = serverUrl;
            this.httpClient = httpClient;
            this.agentRegistry = agentRegistry;
        }

        protected Registration requestRegistration(String agentHostName, AgentAutoRegistrationProperties agentAutoRegisterProperties) throws IOException {
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
                Registration key = Registration.createNullPrivateKeyEntry();

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
                        key = RegistrationJSONizer.fromJson(responseBody(response));
                        break;
                    case SC_UNPROCESSABLE_ENTITY:
                        LOGGER.error("Error occurred during agent registration process: {}", responseBody(response));
                        break;
                    default:
                        LOGGER.warn("The server sent a response that we could not understand. The HTTP status was {}. The response body was:\n{}", response.getStatusLine(), responseBody(response));
                }
                return key;
            } finally {
                postMethod.releaseConnection();
            }
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
