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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.server.Jetty9Server;
import com.thoughtworks.go.util.ArrayUtil;
import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.SystemEnvironment;
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
    private final File keystore;
    private final File truststore;
    private final File agentKeystore;
    private final Connector connector;

    public GoSslSocketConnector(Jetty9Server server, String password, SystemEnvironment systemEnvironment) {
        this.password = password;
        this.systemEnvironment = systemEnvironment;
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

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(keystore.getPath());
        sslContextFactory.setKeyStorePassword(password);
        sslContextFactory.setKeyManagerPassword(password);
        sslContextFactory.setTrustStorePath(truststore.getPath());
        sslContextFactory.setTrustStorePassword(password);
        sslContextFactory.setWantClientAuth(true);

        if(!ArrayUtil.isEmpty(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS))) {
            sslContextFactory.setIncludeCipherSuites(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS));
        }
        if(!ArrayUtil.isEmpty(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS))) {
            sslContextFactory.setExcludeCipherSuites(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS));
        }
        if(!ArrayUtil.isEmpty(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS))) {
            sslContextFactory.setExcludeProtocols(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS));
        }
        if(!ArrayUtil.isEmpty(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS))) {
            sslContextFactory.setIncludeProtocols(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS));
        }
        sslContextFactory.setRenegotiationAllowed(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED));
        LOGGER.info("Included ciphers: {}", ArrayUtil.join(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS)));
        LOGGER.info("Excluded ciphers: {}", ArrayUtil.join(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS)));
        LOGGER.info("Included protocols: {}", ArrayUtil.join(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS)));
        LOGGER.info("Excluded protocols: {}", ArrayUtil.join(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS)));
        LOGGER.info("Renegotiation Allowed: {}", systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED));
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
