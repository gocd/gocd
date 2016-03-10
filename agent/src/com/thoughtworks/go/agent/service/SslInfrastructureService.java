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

import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.security.AuthSSLProtocolSocketFactory;
import com.thoughtworks.go.security.KeyStoreManager;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.URLService;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.thoughtworks.go.security.CertificateUtil.md5Fingerprint;
import static com.thoughtworks.go.security.SelfSignedCertificateX509TrustManager.CRUISE_SERVER;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Service
public class SslInfrastructureService {

    public static final File AGENT_CERTIFICATE_FILE = new File("config", "agent.jks");
    public static final File AGENT_TRUST_FILE = new File("config", "trust.jks");
    public static final String CHAIN_ALIAS = "agent";
    public static final String AGENT_STORE_PASSWORD = "agent5s0repa55w0rd";
    private static final Logger LOGGER = Logger.getLogger(SslInfrastructureService.class);
    private static final int REGISTER_RETRY_INTERVAL = 5000;
    private final RemoteRegistrationRequester remoteRegistrationRequester;
    private final KeyStoreManager keyStoreManager;
    private final HttpClient httpClient;
    private AuthSSLProtocolSocketFactory protocolSocketFactory;
    private transient boolean registered = false;
    private HttpConnectionManagerParams httpConnectionManagerParams;

    @Autowired
    public SslInfrastructureService(URLService urlService, HttpClient httpClient, HttpConnectionManagerParams httpConnectionManagerParams, AgentRegistry agentRegistry) throws Exception {
        this(new RemoteRegistrationRequester(urlService.getAgentRegistrationURL(), agentRegistry, new HttpClient()), httpClient, httpConnectionManagerParams);
    }

    // For mocking out remote call
    SslInfrastructureService(RemoteRegistrationRequester requester, HttpClient httpClient, HttpConnectionManagerParams httpConnectionManagerParams)
            throws Exception {
        this.remoteRegistrationRequester = requester;
        this.httpClient = httpClient;
        this.httpConnectionManagerParams = httpConnectionManagerParams;
        this.keyStoreManager = new KeyStoreManager();
        this.keyStoreManager.preload(AGENT_CERTIFICATE_FILE, AGENT_STORE_PASSWORD);
    }

    public void createSslInfrastructure() throws IOException {
        File parentFile = AGENT_TRUST_FILE.getParentFile();
        if (parentFile.exists() || parentFile.mkdirs()) {
            protocolSocketFactory = new AuthSSLProtocolSocketFactory(
                    AGENT_TRUST_FILE, AGENT_CERTIFICATE_FILE, AGENT_STORE_PASSWORD);
            protocolSocketFactory.registerAsHttpsProtocol();
        } else {
            bomb("Unable to create folder " + parentFile.getAbsolutePath());
        }
    }

    public void registerIfNecessary(AgentAutoRegistrationProperties agentAutoRegistrationProperties) throws Exception {
        registered = keyStoreManager.hasCertificates(CHAIN_ALIAS, AGENT_CERTIFICATE_FILE,
                AGENT_STORE_PASSWORD) && GuidService.guidPresent();
        if (!registered) {
            LOGGER.info("[Agent Registration] Starting to register agent");
            register(agentAutoRegistrationProperties);
            createSslInfrastructure();
            registered = true;
            LOGGER.info("[Agent Registration] Successfully registered agent");
        }
    }

    public boolean isRegistered() {
        return registered;
    }

    private void register(AgentAutoRegistrationProperties agentAutoRegistrationProperties) throws Exception {
        String hostName = SystemUtil.getLocalhostNameOrRandomNameIfNotFound();
        Registration keyEntry = null;
        while (keyEntry == null || keyEntry.getChain().length == 0) {
            try {
                keyEntry = remoteRegistrationRequester.requestRegistration(hostName, agentAutoRegistrationProperties);
            } catch (Exception e) {
                LOGGER.error("[Agent Registration] There was a problem registering with the go server.", e);
                throw e;
            } finally {
                agentAutoRegistrationProperties.scrubRegistrationProperties();
            }

            try {
                Thread.sleep(REGISTER_RETRY_INTERVAL);
            } catch (InterruptedException e) {
                // Ok
            }
        }
        LOGGER.info("[Agent Registration] Retrieved registration from Go server.");
        storeChainIntoAgentStore(keyEntry);
    }

    void storeChainIntoAgentStore(Registration keyEntry) {
        try {
            keyStoreManager.storeCertificate(CHAIN_ALIAS, AGENT_CERTIFICATE_FILE, AGENT_STORE_PASSWORD, keyEntry);
            LOGGER.info(String.format("[Agent Registration] Stored registration for cert with hash code: %s not valid before: %s", md5Fingerprint(keyEntry.getFirstCertificate()),
                    keyEntry.getCertificateNotBeforeDate()));
        } catch (Exception e) {
            throw bomb("Couldn't save agent key into store", e);
        }
    }

    public void invalidateAgentCertificate() {
        resetHttpConnectionManager();
        try {
            keyStoreManager.deleteEntry(CHAIN_ALIAS, AGENT_CERTIFICATE_FILE, AGENT_STORE_PASSWORD);
            keyStoreManager.deleteEntry(CRUISE_SERVER, AGENT_TRUST_FILE, AGENT_STORE_PASSWORD);
        } catch (Exception e) {
            LOGGER.fatal("[Agent Registration] Error while deleting key from key store", e);
            deleteKeyStores();
        }
    }

    private void resetHttpConnectionManager() {
        MultiThreadedHttpConnectionManager httpConnectionManager =
                (MultiThreadedHttpConnectionManager) httpClient.getHttpConnectionManager();
        httpConnectionManager.shutdown();
        httpConnectionManager = new MultiThreadedHttpConnectionManager();
        httpConnectionManager.setParams(httpConnectionManagerParams);
        httpClient.setHttpConnectionManager(httpConnectionManager);
    }

    public void deleteKeyStores() {
        FileUtils.deleteQuietly(AGENT_CERTIFICATE_FILE);
        FileUtils.deleteQuietly(AGENT_TRUST_FILE);
    }

    public static class RemoteRegistrationRequester {
        private final AgentRegistry agentRegistry;
        private String serverUrl;
        private HttpClient httpClient;

        public RemoteRegistrationRequester(String serverUrl, AgentRegistry agentRegistry, HttpClient httpClient) {
            this.serverUrl = serverUrl;
            this.httpClient = httpClient;
            this.agentRegistry = agentRegistry;
        }

        protected Registration requestRegistration(String agentHostName, AgentAutoRegistrationProperties agentAutoRegisterProperties) throws IOException, ClassNotFoundException {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("[Agent Registration] Using URL %s to register.", serverUrl));
            }
            PostMethod postMethod = new PostMethod(serverUrl);
            postMethod.addParameter("hostname", agentHostName);
            postMethod.addParameter("uuid", agentRegistry.uuid());
            String workingdir = SystemUtil.currentWorkingDirectory();
            postMethod.addParameter("location", workingdir);
            postMethod.addParameter("usablespace",
                    String.valueOf(AgentRuntimeInfo.usableSpace(workingdir)));
            postMethod.addParameter("operatingSystem", new SystemEnvironment().getOperatingSystemName());
            postMethod.addParameter("agentAutoRegisterKey", agentAutoRegisterProperties.agentAutoRegisterKey());
            postMethod.addParameter("agentAutoRegisterResources", agentAutoRegisterProperties.agentAutoRegisterResources());
            postMethod.addParameter("agentAutoRegisterEnvironments", agentAutoRegisterProperties.agentAutoRegisterEnvironments());
            postMethod.addParameter("agentAutoRegisterHostname", agentAutoRegisterProperties.agentAutoRegisterHostname());
            postMethod.addParameter("elasticAgentId", agentAutoRegisterProperties.agentAutoRegisterElasticAgentId());
            postMethod.addParameter("elasticPluginId", agentAutoRegisterProperties.agentAutoRegisterElasticPluginId());
            try {
                httpClient.executeMethod(postMethod);
                InputStream is = postMethod.getResponseBodyAsStream();
                return readResponse(is);
            } finally {
                postMethod.releaseConnection();
            }
        }

        protected Registration readResponse(InputStream is) throws IOException, ClassNotFoundException {
            return Registration.fromJson(IOUtils.toString(is));
        }
    }
}
