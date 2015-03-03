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

package com.thoughtworks.go.server;

import com.thoughtworks.go.server.util.GoJetty6CipherSuite;
import com.thoughtworks.go.server.util.GoJetty6SslSocketConnector;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.HttpStatus;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.management.MBeanContainer;
import org.mortbay.xml.XmlConfiguration;
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
import java.util.Map;

public class Jetty6Server extends AppServer {

    private final Server server = new Server();
    private final Jetty6GoWebXmlConfiguration configuration = new Jetty6GoWebXmlConfiguration();
    private final GoJetty6CipherSuite goCipherSuite;
    private WebAppContext webAppContext;
    private static final Logger LOG = Logger.getLogger(Jetty6Server.class);

    public Jetty6Server(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory) {
        super(systemEnvironment, password, sslSocketFactory);
        goCipherSuite = new GoJetty6CipherSuite(sslSocketFactory);
    }

    @Override
    void addExtraJarsToClasspath(String extraClasspath) {
        webAppContext.setExtraClasspath(extraClasspath);
    }

    @Override
    void setCookieExpirePeriod(int cookieExpirePeriod) {
        webAppContext.getSessionHandler().getSessionManager().setMaxCookieAge(cookieExpirePeriod);
    }

    @Override
    void setInitParameter(String name, String value) {
        Map existingParams = webAppContext.getInitParams();
        existingParams.put("rails.root", "/WEB-INF/rails.new");
        webAppContext.setInitParams(existingParams);
    }

    @Override
    void addStopServlet() {
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new StopJettyFromLocalhostServlet(this));
        webAppContext.addServlet(holder, "/jetty/stop");
    }

    @Override
    Throwable getUnavailableException() {
        return webAppContext.getUnavailableException();
    }

    @Override
    void configure() throws Exception {
        server.getContainer().addEventListener(mbeans());

        server.addConnector(selectChannelConnector());
        server.addConnector(sslConnector());

        server.addHandler(welcomeFileHandler());
        server.addHandler(legacyRequestHandler());
        WebAppContext webAppContext = webApp();
        addResourceHandler(webAppContext);
        server.addHandler(webAppContext);
        this.webAppContext = webAppContext;
        performCustomConfiguration();
        server.setStopAtShutdown(true);
    }

    private void addResourceHandler(WebAppContext webAppContext) throws IOException {
        if (!systemEnvironment.useCompressedJs())
            return;
        Jetty6AssetsContextHandler handler = new Jetty6AssetsContextHandler(systemEnvironment);
        server.addHandler(handler);
        webAppContext.addLifeCycleListener(new Jetty6AssetsContextHandlerInitializer(handler, webAppContext));
    }

    private void performCustomConfiguration() throws Exception {
        File jettyConfig = systemEnvironment.getJettyConfigFile();
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

    private MBeanContainer mbeans() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanContainer mbeans = new MBeanContainer(platformMBeanServer);
        mbeans.start();
        return mbeans;
    }

    private Connector sslConnector() {
        return new GoJetty6SslSocketConnector(password, systemEnvironment, goCipherSuite).sslConnector();
    }

    private SelectChannelConnector selectChannelConnector() {
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(systemEnvironment.getServerPort());
        connector.setHost(systemEnvironment.getListenHost());
        return connector;
    }

    WebAppContext webApp() throws IOException, SAXException, ClassNotFoundException, UnavailableException {
        WebAppContext wac = new WebAppContext();
        configuration.setWebAppContext(wac);
        configuration.initialize(systemEnvironment.getCruiseWar());

        wac.setConfigurationClasses(new String[]{
                "org.mortbay.jetty.webapp.WebInfConfiguration",
                "org.mortbay.jetty.webapp.WebXmlConfiguration",
                "org.mortbay.jetty.webapp.JettyWebXmlConfiguration",
        });
        wac.setContextPath(new SystemEnvironment().getWebappContextPath());
        wac.setWar(systemEnvironment.getCruiseWar());
        wac.setParentLoaderPriority(new SystemEnvironment().getParentLoaderPriority());
        return wac;
    }

    ContextHandler welcomeFileHandler() {
        return new GoServerWelcomeFileHandler();
    }

    ContextHandler legacyRequestHandler() {
        return new LegacyUrlRequestHandler();
    }

    class LegacyUrlRequestHandler extends ContextHandler {

        LegacyUrlRequestHandler() {
            super(GoConstants.OLD_URL_CONTEXT);
        }

        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
            if (target.startsWith(GoConstants.OLD_URL_CONTEXT + "/") || target.equals(GoConstants.OLD_URL_CONTEXT)) {
                if (shouldRedirect(request)) {
                    response.setHeader("Location", target.replaceFirst(GoConstants.OLD_URL_CONTEXT, GoConstants.GO_URL_CONTEXT));
                    response.setStatus(HttpStatus.ORDINAL_301_Moved_Permanently);
                } else {
                    response.setStatus(HttpStatus.ORDINAL_404_Not_Found);
                }
                response.setHeader("Content-Type", "text/plain");
                PrintWriter writer = response.getWriter();
                writer.write(String.format("Url(s) starting in '%s' have been permanently moved to '%s', please use the new path.", GoConstants.OLD_URL_CONTEXT, GoConstants.GO_URL_CONTEXT));
                writer.close();
            }
        }

        boolean shouldRedirect(HttpServletRequest request) {
            String method = request.getMethod();
            return method.equals(HttpMethods.GET) || method.equals(HttpMethods.HEAD);
        }
    }

    class GoServerWelcomeFileHandler extends ContextHandler {
        GoServerWelcomeFileHandler() {
            super("/");
        }

        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
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

    @Override
    void start() throws Exception {
        server.start();
    }

    @Override
    void stop() throws Exception {
        server.stop();
    }
}
