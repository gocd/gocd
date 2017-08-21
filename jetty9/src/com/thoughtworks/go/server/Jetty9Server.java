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

package com.thoughtworks.go.server;

import com.thoughtworks.go.server.config.GoSSLConfig;
import com.thoughtworks.go.server.util.GoPlainSocketConnector;
import com.thoughtworks.go.server.util.GoSslSocketConnector;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.*;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.management.MBeanServer;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.management.ManagementFactory;

import static java.text.MessageFormat.format;

public class Jetty9Server extends AppServer {
    protected static String JETTY_XML_LOCATION_IN_JAR = "/defaultFiles/config";
    private static final String JETTY_XML = "jetty.xml";
    private static final String JETTY_VERSION = "jetty-v9.2.3";
    private Server server;
    private WebAppContext webAppContext;
    private static final Logger LOG = LoggerFactory.getLogger(Jetty9Server.class);
    private GoSSLConfig goSSLConfig;
    private final DeploymentManager deploymentManager;

    public Jetty9Server(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory) {
        this(systemEnvironment, password, sslSocketFactory, new Server(), new DeploymentManager());
    }

    Jetty9Server(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory, Server server, DeploymentManager deploymentManager) {
        super(systemEnvironment, password, sslSocketFactory);
        systemEnvironment.set(SystemEnvironment.JETTY_XML_FILE_NAME, JETTY_XML);
        goSSLConfig = new GoSSLConfig(sslSocketFactory, systemEnvironment);

        this.server = server;
        this.deploymentManager = deploymentManager;
    }

    @Override
    public void configure() throws Exception {
        server.addEventListener(mbeans());
        server.addConnector(plainConnector());
        server.addConnector(sslConnector());
        ContextHandlerCollection handlers = new ContextHandlerCollection();
        deploymentManager.setContexts(handlers);

        createWebAppContext();

        JettyCustomErrorPageHandler errorHandler = new JettyCustomErrorPageHandler();
        webAppContext.setErrorHandler(errorHandler);

        server.addBean(deploymentManager);
        server.setHandler(handlers);

        performCustomConfiguration();
        server.setStopAtShutdown(true);
    }

    @Override
    public void start() throws Exception {
        server.start();

        startHandlers();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void addExtraJarsToClasspath(String extraClasspath) {
        extraClasspath = new StringBuilder(extraClasspath).append(",").append(systemEnvironment.configDir().getAbsoluteFile()).toString();
        webAppContext.setExtraClasspath(extraClasspath);
    }

    @Override
    public void setCookieExpirePeriod(int cookieExpirePeriod) {
        SessionCookieConfig cookieConfig = webAppContext.getSessionHandler().getSessionManager().getSessionCookieConfig();
        cookieConfig.setHttpOnly(true);
        cookieConfig.setMaxAge(cookieExpirePeriod);
    }

    @Override
    public void setInitParameter(String name, String value) {
        webAppContext.setInitParameter(name, value);
    }

    @Override
    public Throwable getUnavailableException() {
        return webAppContext.getUnavailableException();
    }

    protected void startHandlers() throws IOException {
        WebAppProvider webAppProvider = new WebAppProvider();

        deploymentManager.addApp(new App(deploymentManager, webAppProvider, "welcomeHandler", welcomeFileHandler()));

        if (systemEnvironment.useCompressedJs()) {
            AssetsContextHandler assetsContextHandler = new AssetsContextHandler(systemEnvironment);
            deploymentManager.addApp(new App(deploymentManager, webAppProvider, "assetsHandler", assetsContextHandler));
            webAppContext.addLifeCycleListener(new AssetsContextHandlerInitializer(assetsContextHandler, webAppContext));
        }

        deploymentManager.addApp(new App(deploymentManager, webAppProvider, "realApp", webAppContext));
    }

    private MBeanContainer mbeans() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        return new MBeanContainer(platformMBeanServer);
    }

    ContextHandler welcomeFileHandler() {
        return new GoServerWelcomeFileHandler();
    }

    private Connector plainConnector() {
        return new GoPlainSocketConnector(this, systemEnvironment).getConnector();
    }

    private Connector sslConnector() {
        return new GoSslSocketConnector(this, password, systemEnvironment, goSSLConfig).getConnector();
    }

    class GoServerWelcomeFileHandler extends ContextHandler {
        public GoServerWelcomeFileHandler() {
            setContextPath("/");
            setHandler(new Handler());
        }

        private class Handler extends AbstractHandler {
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

                if ("/go".equals(request.getPathInfo()) || request.getPathInfo().startsWith("/go/")) {
                    return;
                }

                response.setHeader("X-XSS-Protection", "1; mode=block");
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "SAMEORIGIN");
                response.setHeader("X-UA-Compatible", "chrome=1");

                if ("/".equals(target)) {
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
        File jettyConfig = systemEnvironment.getJettyConfigFile();
        if (jettyConfig.exists()) {
            replaceJettyXmlIfItBelongsToADifferentVersion(jettyConfig);
            LOG.info("Configuring Jetty using {}", jettyConfig.getAbsolutePath());
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

    protected void replaceJettyXmlIfItBelongsToADifferentVersion(File jettyConfig) throws IOException {
        if (FileUtil.readContentFromFile(jettyConfig).contains(JETTY_VERSION)) return;
        replaceFileWithPackagedOne(jettyConfig);
    }

    private void replaceFileWithPackagedOne(File jettyConfig) {
        InputStream inputStream = null;
        try {
            inputStream = getClass().getResourceAsStream(JETTY_XML_LOCATION_IN_JAR + "/" + jettyConfig.getName());
            if (inputStream == null) {
                throw new RuntimeException(format("Resource {0}/{1} does not exist in the classpath", JETTY_XML_LOCATION_IN_JAR, jettyConfig.getName()));
            }
            FileUtil.writeToFile(inputStream, systemEnvironment.getJettyConfigFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
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
