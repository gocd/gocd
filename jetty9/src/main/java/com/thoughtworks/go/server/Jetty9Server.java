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
package com.thoughtworks.go.server;

import com.thoughtworks.go.server.config.GoSSLConfig;
import com.thoughtworks.go.server.util.GoPlainSocketConnector;
import com.thoughtworks.go.server.util.GoSslSocketConnector;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.servlet.SessionCookieConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Jetty9Server extends AppServer {
    protected static String JETTY_XML_LOCATION_IN_JAR = "/defaultFiles/config";
    private static final String JETTY_XML = "jetty.xml";
    private static final String JETTY_VERSION = "jetty-v9.4.8.v20171121";
    private Server server;
    private WebAppContext webAppContext;
    private static final Logger LOG = LoggerFactory.getLogger(Jetty9Server.class);
    private GoSSLConfig goSSLConfig;
    private final DeploymentManager deploymentManager;

    public Jetty9Server(SystemEnvironment systemEnvironment, String password) {
        this(systemEnvironment, password, new Server(), new DeploymentManager());
    }

    Jetty9Server(SystemEnvironment systemEnvironment, String password, Server server, DeploymentManager deploymentManager) {
        super(systemEnvironment, password);
        systemEnvironment.set(SystemEnvironment.JETTY_XML_FILE_NAME, JETTY_XML);
        this.server = server;
        goSSLConfig = new GoSSLConfig(systemEnvironment);
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
        webAppContext.setGzipHandler(gzipHandler());
        server.addBean(errorHandler);
        server.addBean(deploymentManager);

        HandlerCollection serverLevelHandlers = new HandlerCollection();
        serverLevelHandlers.setHandlers(new Handler[]{handlers});
        server.setHandler(serverLevelHandlers);

        performCustomConfiguration();
        server.setStopAtShutdown(true);
    }

    static GzipHandler gzipHandler() {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.addIncludedMimeTypes(
                "application/javascript",
                "application/json",
                "application/vnd.go.cd.v1+json",
                "application/vnd.go.cd.v2+json",
                "application/vnd.go.cd.v3+json",
                "application/vnd.go.cd.v4+json",
                "application/vnd.go.cd.v5+json",
                "application/vnd.go.cd.v6+json",
                "application/vnd.go.cd.v7+json",
                "application/vnd.go.cd.v8+json",
                "application/vnd.go.cd.v9+json",
                "application/xhtml+xml",
                "image/svg+xml",
                "text/css",
                "text/html",
                "text/plain",
                "text/xml"
        );
        gzipHandler.addIncludedMethods("HEAD",
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE");
        return gzipHandler;
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
    public boolean hasStarted() {
        return !webAppContext.isFailed() && webAppContext.getUnavailableException() == null;
    }

    @Override
    public void addExtraJarsToClasspath(String extraClasspath) {
        extraClasspath = new StringBuilder(extraClasspath).append(",").append(systemEnvironment.configDir().getAbsoluteFile()).toString();
        webAppContext.setExtraClasspath(extraClasspath);
    }

    @Override
    public void setSessionConfig() {
        SessionHandler sessionHandler = webAppContext.getSessionHandler();
        SessionCookieConfig sessionCookieConfig = sessionHandler.getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        sessionCookieConfig.setSecure(systemEnvironment.isSessionCookieSecure());
        sessionCookieConfig.setMaxAge(systemEnvironment.sessionCookieMaxAgeInSeconds());
        sessionHandler.setMaxInactiveInterval(systemEnvironment.sessionTimeoutInSeconds());
    }

    @Override
    public void setInitParameter(String name, String value) {
        webAppContext.setInitParameter(name, value);
    }

    @Override
    public Throwable getUnavailableException() {
        return webAppContext.getUnavailableException();
    }

    void startHandlers() {
        WebAppProvider webAppProvider = new WebAppProvider();

        deploymentManager.addApp(new App(deploymentManager, webAppProvider, "welcomeHandler", rootHandler()));

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

    private ContextHandler rootHandler() {
        return new GoServerLoadingIndicationHandler(webAppContext, systemEnvironment);
    }

    private Connector plainConnector() {
        return new GoPlainSocketConnector(this, systemEnvironment).getConnector();
    }

    private Connector sslConnector() {
        return new GoSslSocketConnector(this, password, systemEnvironment, goSSLConfig).getConnector();
    }


    private void performCustomConfiguration() throws Exception {
        File jettyConfig = systemEnvironment.getJettyConfigFile();
        if (jettyConfig.exists()) {
            replaceJettyXmlIfItBelongsToADifferentVersion(jettyConfig);
            LOG.info("Configuring Jetty using {}", jettyConfig.getAbsolutePath());
            XmlConfiguration configuration = new XmlConfiguration(Resource.newResource(jettyConfig));
            configuration.configure(server);
        } else {
            String message = String.format(
                    "No custom jetty configuration (%s) found, using defaults.",
                    jettyConfig.getAbsolutePath());
            LOG.info(message);
        }
    }

    protected void replaceJettyXmlIfItBelongsToADifferentVersion(File jettyConfig) throws IOException {
        if (FileUtils.readFileToString(jettyConfig, UTF_8).contains(JETTY_VERSION)) return;
        replaceFileWithPackagedOne(jettyConfig);
    }

    private void replaceFileWithPackagedOne(File jettyConfig) {
        try (InputStream inputStream = getClass().getResourceAsStream(JETTY_XML_LOCATION_IN_JAR + "/" + jettyConfig.getName())) {
            FileUtils.copyInputStreamToFile(inputStream, systemEnvironment.getJettyConfigFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getWarFile() {
        return systemEnvironment.getCruiseWar();
    }

    private WebAppContext createWebAppContext() {
        webAppContext = new WebAppContext();
        webAppContext.setDefaultsDescriptor(GoWebXmlConfiguration.configuration(getWarFile()));

        webAppContext.setConfigurationClasses(new String[]{
                WebInfConfiguration.class.getCanonicalName(),
                WebXmlConfiguration.class.getCanonicalName(),
                JettyWebXmlConfiguration.class.getCanonicalName()
        });
        webAppContext.setContextPath(systemEnvironment.getWebappContextPath());

        // delegate all logging to parent classloader to avoid initialization of loggers in multiple classloaders
        webAppContext.getSystemClasspathPattern().add("org.apache.log4j.");
        webAppContext.getSystemClasspathPattern().add("org.slf4j.");
        webAppContext.getSystemClasspathPattern().add("org.apache.commons.logging.");

        webAppContext.setWar(getWarFile());
        webAppContext.setParentLoaderPriority(systemEnvironment.getParentLoaderPriority());
        return webAppContext;
    }

    public Server getServer() {
        return server;
    }
}
