/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.security.SslSelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class GoJetty6SslSocketConnector {

    private static Logger LOGGER = Logger.getLogger(GoJetty6SslSocketConnector.class);

    private final String password;
    private final SystemEnvironment systemEnvironment;
    private final GoJetty6CipherSuite goCipherSuite;
    private final int sslPort;
    private final File keystore;
    private final File truststore;
    private final File agentKeystore;
    private static final int MAX_IDLE_TIME = 30000;

    public GoJetty6SslSocketConnector(String password, SystemEnvironment systemEnvironment, GoJetty6CipherSuite goCipherSuite) {
        this.password = password;
        this.systemEnvironment = systemEnvironment;
        this.goCipherSuite = goCipherSuite;
        this.sslPort = systemEnvironment.getSslServerPort();
        this.keystore = systemEnvironment.keystore();
        this.truststore = systemEnvironment.truststore();
        this.agentKeystore = systemEnvironment.agentkeystore();
    }

    public Connector sslConnector() {
        if (!keystore.exists()) { storeX509Certificate(); }

        //NOTE: There is no common class that includes all this set stuff.
        //If only we had duck typing
        if (systemEnvironment.useNioSslSocket()) {
            LOGGER.info(String.format("Using SSL socket selector SslSelectChannelConnector (NIO) with maxIdleTime=%s", MAX_IDLE_TIME));
            SslSelectChannelConnector nioConnector = new SslSelectChannelConnector();
            nioConnector.setPort(sslPort);
            nioConnector.setHost(systemEnvironment.getListenHost());
            nioConnector.setMaxIdleTime(MAX_IDLE_TIME);
            nioConnector.setKeystore(keystore.getPath());
            nioConnector.setPassword(password);
            nioConnector.setKeyPassword(password);
            nioConnector.setTruststore(truststore.getPath());
            nioConnector.setTrustPassword(password);
            nioConnector.setWantClientAuth(true);
            nioConnector.setExcludeCipherSuites(goCipherSuite.getExcludedCipherSuites());
            return nioConnector;
        }
        else {
            LOGGER.info(String.format("Using SSL socket selector SslSocketConnector (NOT NIO) with maxIdleTime=%s", MAX_IDLE_TIME));
            SslSocketConnector socketConnector = new SslSocketConnector();
            socketConnector.setPort(sslPort);
            socketConnector.setHost(systemEnvironment.getListenHost());
            socketConnector.setMaxIdleTime(MAX_IDLE_TIME);
            socketConnector.setKeystore(keystore.getPath());
            socketConnector.setPassword(password);
            socketConnector.setKeyPassword(password);
            socketConnector.setTruststore(truststore.getPath());
            socketConnector.setTrustPassword(password);
            socketConnector.setWantClientAuth(true);
            socketConnector.setExcludeCipherSuites(goCipherSuite.getExcludedCipherSuites());
            return socketConnector;
        }

    }

    private void storeX509Certificate() {
        String principalDn = "ou=Cruise server webserver certificate, cn=" + getHostname();

        X509CertificateGenerator generator = new X509CertificateGenerator();
        generator.createAndStoreX509Certificates(keystore, truststore, agentKeystore, password, principalDn);
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw bomb(e);
        }
    }
}
