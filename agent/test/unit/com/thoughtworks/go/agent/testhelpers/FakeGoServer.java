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

package com.thoughtworks.go.agent.testhelpers;

import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.util.TestFileUtil;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class FakeGoServer {
    public static final String PASSWORD = "Crui3CertSigningPassword";
	private static final int MAX_IDLE_TIME = 30000;
	private static final int RESPONSE_BUFFER_SIZE = 32768;

	private Server server;
    private int serverPort;
    private int sslPort;

    public FakeGoServer(int serverPort, int sslPort) {
        this.serverPort = serverPort;
        this.sslPort = sslPort;
    }

    public void start() throws Exception {
        server = new Server(serverPort);

        File keystore = TestFileUtil.createUniqueTempFile("keystore");
        File truststore = TestFileUtil.createUniqueTempFile("truststore");
        File agentKeystore = TestFileUtil.createUniqueTempFile("agentstore");
        createX509Certificate(keystore, truststore, agentKeystore);

        server.addConnector(sslConnector(keystore, truststore, sslPort));
        WebAppContext wac = new WebAppContext("testdata/goserverstub", "/go");
        wac.setConfigurationClasses(new String[]{
				WebInfConfiguration.class.getCanonicalName(),
				WebXmlConfiguration.class.getCanonicalName(),
				JettyWebXmlConfiguration.class.getCanonicalName()
        });

        addFakeArtifactiPublisherServlet(wac);
        addFakeAgentCertificateServlet(wac);
        server.setHandler(wac);
        server.setStopAtShutdown(true);
        server.start();
    }

    private static void addFakeArtifactiPublisherServlet(WebAppContext wac) {
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new FakeArtifactPublisherServlet());
        wac.addServlet(holder, "/remoting/repository/*");
        wac.addServlet(holder, "/remoting/files/*");
    }

    private static void addFakeAgentCertificateServlet(WebAppContext wac) {
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new FakeAgentCertificateServlet());
        wac.addServlet(holder, "/admin/agent");
    }

    public void stop() throws Exception {
        server.stop();
        server.join();
    }


	public Connector sslConnector(File keystore, File truststore, int sslPort) {
		HttpConfiguration httpsConfig = new HttpConfiguration();
		httpsConfig.setOutputBufferSize(RESPONSE_BUFFER_SIZE);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());

		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(keystore.getAbsolutePath());
		sslContextFactory.setKeyStorePassword(PASSWORD);
		sslContextFactory.setKeyManagerPassword(PASSWORD);
		sslContextFactory.setTrustStorePath(truststore.getAbsolutePath());
		sslContextFactory.setTrustStorePassword(PASSWORD);
		sslContextFactory.setWantClientAuth(true);

		ServerConnector https = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
		// https.setHost(host);
		https.setPort(sslPort);
		https.setIdleTimeout(MAX_IDLE_TIME);

		return https;
	}

    private void createX509Certificate(File keystore, File truststore, File agentKeystore) {
        final String principalDn = "ou=Cruise server webserver certificate, cn=" + getHostname();
        X509CertificateGenerator generator = new X509CertificateGenerator();
        generator.createAndStoreX509Certificates(keystore, truststore, agentKeystore, PASSWORD, principalDn);
    }

    private String getHostname() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return hostname;
    }

}



