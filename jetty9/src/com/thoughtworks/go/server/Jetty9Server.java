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

package com.thoughtworks.go.server;

import com.thoughtworks.go.server.util.GoCipherSuite;
import com.thoughtworks.go.server.util.GoPlainSocketConnector;
import com.thoughtworks.go.server.util.GoSslSocketConnector;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.xml.sax.SAXException;

import javax.management.MBeanServer;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

public class Jetty9Server extends AppServer {
    private Server server;
    private WebAppContext webAppContext;
    private static final Logger LOG = Logger.getLogger(Jetty9Server.class);
    private GoCipherSuite goCipherSuite;

    public Jetty9Server(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory) {
        this(systemEnvironment, password, sslSocketFactory, new Server());
    }

    Jetty9Server(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory, Server server){
        super(systemEnvironment, password, sslSocketFactory);
        this.server = server;
        goCipherSuite = new GoCipherSuite(sslSocketFactory);
    }

    @Override
    public void configure() throws Exception {
        server.addEventListener(mbeans());
        server.addConnector(plainConnector());
        server.addConnector(sslConnector());
        HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(welcomeFileHandler());
        createWebAppContext();
        addResourceHandler(handlers, webAppContext);
        handlers.addHandler(webAppContext);
        server.setHandler(handlers);
        performCustomConfiguration();
        server.setStopAtShutdown(true);
    }

    @Override
    public void start() throws Exception {
        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void addExtraJarsToClasspath(String extraClasspath) {
        webAppContext.setExtraClasspath(extraClasspath);
    }

    @Override
    public void setCookieExpirePeriod(int cookieExpirePeriod) {
        webAppContext.getSessionHandler().getSessionManager().getSessionCookieConfig().setMaxAge(cookieExpirePeriod);
    }

    @Override
    public void setInitParameter(String name, String value) {
        webAppContext.setInitParameter(name, value);
    }

    @Override
    public void addStopServlet() {
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new StopJettyFromLocalhostServlet(this));
        webAppContext.addServlet(holder, "/jetty/stop");
    }

    @Override
    public Throwable getUnavailableException() {
        return webAppContext.getUnavailableException();
    }

    private MBeanContainer mbeans() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanContainer mbeans = new MBeanContainer(platformMBeanServer);
        return mbeans;
    }

    ContextHandler welcomeFileHandler() {
        return new GoServerWelcomeFileHandler();
    }

    private Connector plainConnector() {
        return new GoPlainSocketConnector(this, systemEnvironment).getConnector();
    }

    private Connector sslConnector() {
        return new GoSslSocketConnector(this, password, systemEnvironment, goCipherSuite).getConnector();
    }

    class GoServerWelcomeFileHandler extends ContextHandler {
        public GoServerWelcomeFileHandler() {
            setContextPath("/");
            setHandler(new Handler());
        }

        private class Handler extends AbstractHandler {
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if (target.equals("/")) {
                    response.setHeader("Location", GoConstants.GO_URL_CONTEXT + "/home");
                    response.setStatus(301);
                    response.setHeader("Content-Type", "text/html");
                    PrintWriter writer = response.getWriter();
                    writer.write("redirecting..");
                    writer.close();
                }
            }
        }
    }


    private void performCustomConfiguration() throws Exception {
        File jettyConfig = new File(systemEnvironment.getConfigDir(), "jetty.xml");

        if (jettyConfig.exists()) {
            LOG.info("Configuring Jetty using " + jettyConfig.getAbsolutePath());
            FileInputStream serverConfiguration = new FileInputStream(jettyConfig);
            XmlConfiguration configuration = new XmlConfiguration(serverConfiguration);
            configuration.configure(server);
        } else {
            String message = String.format(
                    "No custom jetty configuration (%s) found, using defaults.",
                    jettyConfig.getAbsolutePath());
            LOG.info(message);
        }
    }

    private void addResourceHandler(HandlerCollection handlers, WebAppContext webAppContext) throws IOException {
        if (!systemEnvironment.useCompressedJs()) return;
        AssetsContextHandler handler = new AssetsContextHandler(systemEnvironment);
        handlers.addHandler(handler);
        webAppContext.addLifeCycleListener(new AssetsContextHandlerInitializer(handler, webAppContext));
    }


    private String getWarFile() {
        return systemEnvironment.getCruiseWar();
    }

    WebAppContext createWebAppContext() throws IOException, SAXException, ClassNotFoundException, UnavailableException {
        webAppContext = new WebAppContext();
        webAppContext.setDefaultsDescriptor(GoWebXmlConfiguration.configuration(getWarFile()));

        webAppContext.setConfigurationClasses(new String[]{
                WebInfConfiguration.class.getCanonicalName(),
                WebXmlConfiguration.class.getCanonicalName(),
                JettyWebXmlConfiguration.class.getCanonicalName()
        });
        webAppContext.setContextPath(systemEnvironment.getWebappContextPath());
        webAppContext.setWar(getWarFile());
        webAppContext.setParentLoaderPriority(systemEnvironment.getParentLoaderPriority());
        return webAppContext;
    }

    public Server getServer() {
        return server;
    }
}
