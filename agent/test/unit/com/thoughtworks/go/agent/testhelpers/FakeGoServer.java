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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.util.TestFileUtil;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

public class FakeGoServer {
    private Server server;
    public static final String PASSWORD = "Crui3CertSigningPassword";
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
                "org.mortbay.jetty.webapp.WebInfConfiguration",
                "org.mortbay.jetty.webapp.WebXmlConfiguration",
                "org.mortbay.jetty.webapp.JettyWebXmlConfiguration",
        });
        addStopServlet(server, wac);
        addFakeArtifactiPublisherServlet(wac);
        addFakeAgentCertificateServlet(wac);
        server.addHandler(wac);
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

    private static void addStopServlet(Server server, WebAppContext wac) {
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new StopTestingServerServlet(server));
        wac.addServlet(holder, "/jetty/stop");
    }

    public void stop() throws Exception {
        server.stop();
        server.join();
    }


    public SslSocketConnector sslConnector(File keystore, File truststore, int sslPort) {
        SslSocketConnector sslSocketConnector = new SslSocketConnector();
        sslSocketConnector.setPort(sslPort);
        sslSocketConnector.setMaxIdleTime(30000);
        sslSocketConnector.setKeystore(keystore.getAbsolutePath());
        sslSocketConnector.setPassword(PASSWORD);
        sslSocketConnector.setKeyPassword(PASSWORD);
        sslSocketConnector.setTruststore(truststore.getAbsolutePath());
        sslSocketConnector.setTrustPassword(PASSWORD);
        return sslSocketConnector;
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

class StopTestingServerServlet extends HttpServlet {
    private final Server stoppingServer;

    public StopTestingServerServlet(Server jettyServer) {
        stoppingServer = jettyServer;
    }

    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        try {
            Thread thread = new Thread() {
                public void run() {
                    try {
                        stoppingServer.stop();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            thread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


