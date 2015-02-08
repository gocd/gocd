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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class GoSslSocketConnector {

    private static Logger LOGGER = Logger.getLogger(GoSslSocketConnector.class);

    private final String password;
    private final SystemEnvironment systemEnvironment;
    private final GoCipherSuite goCipherSuite;
	private final String host;
	private final int plainPort;
    private final int sslPort;
    private final File keystore;
    private final File truststore;
    private final File agentKeystore;
    private static final int MAX_IDLE_TIME = 30000;
	private static final int RESPONSE_BUFFER_SIZE = 32768;

    public GoSslSocketConnector(String password, SystemEnvironment systemEnvironment, GoCipherSuite goCipherSuite) {
        this.password = password;
        this.systemEnvironment = systemEnvironment;
        this.goCipherSuite = goCipherSuite;
		this.host = systemEnvironment.getListenHost();
		this.plainPort = systemEnvironment.getServerPort();
        this.sslPort = systemEnvironment.getSslServerPort();
        this.keystore = systemEnvironment.keystore();
        this.truststore = systemEnvironment.truststore();
        this.agentKeystore = systemEnvironment.agentkeystore();
    }

	public Connector plainConnector(Server server) {
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setOutputBufferSize(RESPONSE_BUFFER_SIZE); // 32 MB

		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
		// http.setHost(host);
		http.setPort(plainPort);
		http.setIdleTimeout(MAX_IDLE_TIME);

		return http;
	}

	public Connector sslConnector(Server server) {
		if (!keystore.exists()) {
			storeX509Certificate();
		}

		HttpConfiguration httpsConfig = new HttpConfiguration();
		httpsConfig.setOutputBufferSize(RESPONSE_BUFFER_SIZE); // 32 MB
		httpsConfig.addCustomizer(new SecureRequestCustomizer());

		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(keystore.getPath());
		sslContextFactory.setKeyStorePassword(password);
		sslContextFactory.setKeyManagerPassword(password);
		sslContextFactory.setTrustStorePath(truststore.getPath());
		sslContextFactory.setTrustStorePassword(password);
		sslContextFactory.setWantClientAuth(true);
		sslContextFactory.setExcludeCipherSuites(goCipherSuite.getExcludedCipherSuites());

		ServerConnector https = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
		// https.setHost(host);
		https.setPort(sslPort);
		https.setIdleTimeout(MAX_IDLE_TIME);

		return https;
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
