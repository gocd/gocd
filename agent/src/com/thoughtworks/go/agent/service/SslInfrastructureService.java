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
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
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
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.thoughtworks.go.security.CertificateUtil.md5Fingerprint;
import static com.thoughtworks.go.security.SelfSignedCertificateX509TrustManager.CRUISE_SERVER;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.apache.http.HttpStatus.SC_ACCEPTED;

@Service
public class SslInfrastructureService {

    private static final String CHAIN_ALIAS = "agent";
    private static final Logger LOGGER = Logger.getLogger(SslInfrastructureService.class);
    private static final int REGISTER_RETRY_INTERVAL = 5000;
    private final RemoteRegistrationRequester remoteRegistrationRequester;
    private final KeyStoreManager keyStoreManager;
    private final GoAgentServerHttpClient httpClient;
    private transient boolean registered = false;

    @Autowired
    public SslInfrastructureService(URLService urlService, GoAgentServerHttpClient httpClient, AgentRegistry agentRegistry) throws Exception {
        this(new RemoteRegistrationRequester(urlService.getAgentRegistrationURL(), agentRegistry, httpClient), httpClient);
    }

    // For mocking out remote call
    SslInfrastructureService(RemoteRegistrationRequester requester, GoAgentServerHttpClient httpClient)
            throws Exception {
        this.remoteRegistrationRequester = requester;
        this.httpClient = httpClient;
        this.keyStoreManager = new KeyStoreManager();
        this.keyStoreManager.preload(GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE, httpClientBuilder().keystorePassword());
    }

    private GoAgentServerHttpClientBuilder httpClientBuilder() {
        return new GoAgentServerHttpClientBuilder(new SystemEnvironment());
    }

    public void createSslInfrastructure() throws IOException {
        httpClientBuilder().initialize();
        httpClient.reset();
    }

    public void registerIfNecessary(AgentAutoRegistrationProperties agentAutoRegistrationProperties) throws Exception {
        registered = keyStoreManager.hasCertificates(CHAIN_ALIAS, GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE,
                httpClientBuilder().keystorePassword()) && GuidService.guidPresent();
        if (!registered) {
            LOGGER.info("[Agent Registration] Starting to register agent.");
            register(agentAutoRegistrationProperties);
            createSslInfrastructure();
            registered = true;
            LOGGER.info("[Agent Registration] Successfully registered agent.");
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
            keyStoreManager.storeCertificate(CHAIN_ALIAS, GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE, httpClientBuilder().keystorePassword(), keyEntry);
            LOGGER.info(String.format("[Agent Registration] Stored registration for cert with hash code: %s not valid before: %s", md5Fingerprint(keyEntry.getFirstCertificate()),
                    keyEntry.getCertificateNotBeforeDate()));
        } catch (Exception e) {
            throw bomb("Couldn't save agent key into store", e);
        }
    }

    public void invalidateAgentCertificate() {
        try {
            httpClient.reset();
            keyStoreManager.deleteEntry(CHAIN_ALIAS, GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE, httpClientBuilder().keystorePassword());
            keyStoreManager.deleteEntry(CRUISE_SERVER, GoAgentServerHttpClientBuilder.AGENT_TRUST_FILE, httpClientBuilder().keystorePassword());
        } catch (Exception e) {
            LOGGER.fatal("[Agent Registration] Error while deleting key from key store", e);
            deleteKeyStores();
        }
    }

    private void deleteKeyStores() {
        FileUtils.deleteQuietly(GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE);
        FileUtils.deleteQuietly(GoAgentServerHttpClientBuilder.AGENT_TRUST_FILE);
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

        protected Registration requestRegistration(String agentHostName, AgentAutoRegistrationProperties agentAutoRegisterProperties) throws IOException, ClassNotFoundException {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("[Agent Registration] Using URL %s to register.", serverUrl));
            }

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
                    .build();

            try {
                CloseableHttpResponse response = httpClient.execute(postMethod);
                if (getStatusCode(response) == SC_ACCEPTED) {
                    LOGGER.debug("The server has accepted the registration request.");
                    return Registration.createNullPrivateKeyEntry();
                }

                try (InputStream is = response.getEntity() == null ? new NullInputStream(0) : response.getEntity().getContent()) {
                    String responseBody = IOUtils.toString(is, StandardCharsets.UTF_8);

                    if (getStatusCode(response) == 200) {
                        LOGGER.info("This agent is now approved by the server.");
                        return readResponse(responseBody);
                    } else {
                        LOGGER.warn(String.format("The server sent a response that we could not understand. The HTTP status was %s. The response body was:\n%s", response.getStatusLine(), responseBody));
                        return Registration.createNullPrivateKeyEntry();
                    }
                }
            } finally {
                postMethod.releaseConnection();
            }
        }

        protected Registration readResponse(String responseBody) {
            return RegistrationJSONizer.fromJson(responseBody);
        }

        protected int getStatusCode(CloseableHttpResponse response) {
            return response.getStatusLine().getStatusCode();
        }

    }
}
