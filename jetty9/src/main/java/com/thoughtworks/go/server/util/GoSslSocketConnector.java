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
package com.thoughtworks.go.server.util;

import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.server.Jetty9Server;
import com.thoughtworks.go.server.config.GoSSLConfig;
import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class GoSslSocketConnector implements GoSocketConnector {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GoSslSocketConnector.class.getName());
    private final String password;
    private final SystemEnvironment systemEnvironment;
    private final GoSSLConfig goSSLConfig;
    private final File keystore;
    private final File truststore;
    private final File agentKeystore;
    private final Connector connector;

    public GoSslSocketConnector(Jetty9Server server, String password, SystemEnvironment systemEnvironment, GoSSLConfig goSSLConfig) {
        this.password = password;
        this.systemEnvironment = systemEnvironment;
        this.goSSLConfig = goSSLConfig;
        this.keystore = systemEnvironment.keystore();
        this.truststore = systemEnvironment.truststore();
        this.agentKeystore = systemEnvironment.agentkeystore();
        connector = sslConnector(server.getServer());
    }

    private Connector sslConnector(Server server) {
        ensureX509Certificates();

        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setOutputBufferSize(systemEnvironment.get(SystemEnvironment.RESPONSE_BUFFER_SIZE));
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        httpsConfig.setSendServerVersion(false);
        httpsConfig.addCustomizer(new ForwardedRequestCustomizer());

        SslContextFactory sslContextFactory = new CustomSslContextFactory();
        if(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_CLEAR_JETTY_DEFAULT_EXCLUSIONS)){
            sslContextFactory.setExcludeProtocols();
            sslContextFactory.setExcludeCipherSuites();
        }
        sslContextFactory.setKeyStorePath(keystore.getPath());
        sslContextFactory.setKeyStorePassword(password);
        sslContextFactory.setKeyManagerPassword(password);
        sslContextFactory.setTrustStorePath(truststore.getPath());
        sslContextFactory.setTrustStorePassword(password);
        sslContextFactory.setWantClientAuth(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_JETTY_WANT_CLIENT_AUTH));
        sslContextFactory.setEndpointIdentificationAlgorithm(null);

        if(!ArrayUtils.isEmpty(goSSLConfig.getCipherSuitesToBeIncluded())) sslContextFactory.setIncludeCipherSuites(goSSLConfig.getCipherSuitesToBeIncluded());
        if(!ArrayUtils.isEmpty(goSSLConfig.getCipherSuitesToBeExcluded())) sslContextFactory.setExcludeCipherSuites(goSSLConfig.getCipherSuitesToBeExcluded());
        if(!ArrayUtils.isEmpty(goSSLConfig.getProtocolsToBeExcluded())) sslContextFactory.setExcludeProtocols(goSSLConfig.getProtocolsToBeExcluded());
        if(!ArrayUtils.isEmpty(goSSLConfig.getProtocolsToBeIncluded())) sslContextFactory.setIncludeProtocols(goSSLConfig.getProtocolsToBeIncluded());
        sslContextFactory.setRenegotiationAllowed(goSSLConfig.isRenegotiationAllowed());
        LOGGER.info("Included ciphers: {}", StringUtils.join(goSSLConfig.getCipherSuitesToBeIncluded(), ","));
        LOGGER.info("Excluded ciphers: {}", StringUtils.join(goSSLConfig.getCipherSuitesToBeExcluded(), ","));
        LOGGER.info("Included protocols: {}", StringUtils.join(goSSLConfig.getProtocolsToBeIncluded(), ","));
        LOGGER.info("Excluded protocols: {}", StringUtils.join(goSSLConfig.getProtocolsToBeExcluded(), ","));
        LOGGER.info("Renegotiation Allowed: {}", goSSLConfig.isRenegotiationAllowed());
        ServerConnector https = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
        https.setHost(systemEnvironment.getListenHost());
        https.setPort(systemEnvironment.getSslServerPort());
        https.setIdleTimeout(systemEnvironment.get(SystemEnvironment.IDLE_TIMEOUT));

        return https;
    }

    private void ensureX509Certificates() {
        String principalDn = "ou=Cruise server webserver certificate, cn=" + getHostname();

        X509CertificateGenerator generator = new X509CertificateGenerator();
        generator.createAndStoreX509Certificates(keystore, truststore, agentKeystore, password, principalDn);
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw ExceptionUtils.bomb(e);
        }
    }

    @Override
    public Connector getConnector() {
        return connector;
    }
}
