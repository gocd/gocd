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

import com.thoughtworks.go.agent.common.ssl.GoAgentServerClientBuilder;
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
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.thoughtworks.go.security.CertificateUtil.md5Fingerprint;
import static com.thoughtworks.go.security.SelfSignedCertificateX509TrustManager.CRUISE_SERVER;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jetty.http.HttpStatus.ACCEPTED_202;

@Service
public class SslInfrastructureService {

    private static final String CHAIN_ALIAS = "agent";
    private static final Logger LOGGER = LoggerFactory.getLogger(SslInfrastructureService.class);
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
            keyStoreManager.storeCertificate(CHAIN_ALIAS, GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE, httpClientBuilder().keystorePassword(), keyEntry);
            LOGGER.info("[Agent Registration] Stored registration for cert with hash code: {} not valid before: {}",
                    md5Fingerprint(keyEntry.getFirstCertificate()),
                    keyEntry.getCertificateNotBeforeDate());
        } catch (Exception e) {
            throw bomb("Couldn't save agent key into store", e);
        }
    }

    public void invalidateAgentCertificate() {
        try {
            httpClient.reset();
            keyStoreManager.deleteEntry(CHAIN_ALIAS, GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE, httpClientBuilder().keystorePassword());
            keyStoreManager.deleteEntry(CRUISE_SERVER, GoAgentServerClientBuilder.AGENT_TRUST_FILE, httpClientBuilder().keystorePassword());
        } catch (Exception e) {
            LOGGER.error("[Agent Registration] Error while deleting key from key store", e);
            deleteKeyStores();
        }
    }

    private void deleteKeyStores() {
        FileUtils.deleteQuietly(GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE);
        FileUtils.deleteQuietly(GoAgentServerClientBuilder.AGENT_TRUST_FILE);
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

        protected Registration requestRegistration(String agentHostName, AgentAutoRegistrationProperties agentAutoRegisterProperties) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException, TimeoutException {
            LOGGER.debug("[Agent Registration] Using URL {} to register.", serverUrl);

            Request postMethod = httpClient.newRequest(serverUrl).method(HttpMethod.POST);

            Fields fields = new Fields(true);
            fields.add("hostname", agentHostName);
            fields.add("uuid", agentRegistry.uuid());
            fields.add("location", SystemUtil.currentWorkingDirectory());
            fields.add("usablespace", String.valueOf(AgentRuntimeInfo.usableSpace(SystemUtil.currentWorkingDirectory())));
            fields.add("operatingSystem", new SystemEnvironment().getOperatingSystemCompleteName());
            fields.add("agentAutoRegisterKey", agentAutoRegisterProperties.agentAutoRegisterKey());
            fields.add("agentAutoRegisterResources", agentAutoRegisterProperties.agentAutoRegisterResources());
            fields.add("agentAutoRegisterEnvironments", agentAutoRegisterProperties.agentAutoRegisterEnvironments());
            fields.add("agentAutoRegisterHostname", agentAutoRegisterProperties.agentAutoRegisterHostname());
            fields.add("elasticAgentId", agentAutoRegisterProperties.agentAutoRegisterElasticAgentId());
            fields.add("elasticPluginId", agentAutoRegisterProperties.agentAutoRegisterElasticPluginId());

            postMethod.content(new FormContentProvider(fields));
            ContentResponse response = httpClient.execute(postMethod);
            if (getStatusCode(response) == ACCEPTED_202) {
                LOGGER.debug("The server has accepted the registration request.");
                return Registration.createNullPrivateKeyEntry();
            }

            byte[] content = response.getContent();

            if (content == null) {
                content = new byte[]{};
            }

            String responseBody = new String(content, UTF_8);
            if (getStatusCode(response) == 200) {
                LOGGER.info("This agent is now approved by the server.");
                return readResponse(responseBody);
            } else {
                LOGGER.warn("The server sent a response that we could not understand. The HTTP status was {}. The response body was:\n{}", response.getStatus(), responseBody);
                return Registration.createNullPrivateKeyEntry();
            }
        }

        protected Registration readResponse(String responseBody) {
            return RegistrationJSONizer.fromJson(responseBody);
        }

        protected int getStatusCode(Response response) {
            return response.getStatus();
        }

    }
}
