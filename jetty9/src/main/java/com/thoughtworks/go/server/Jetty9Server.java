/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.management.MBeanServer;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.SessionCookieConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.MessageFormat.format;

public class Jetty9Server extends AppServer {
    protected static String JETTY_XML_LOCATION_IN_JAR = "/defaultFiles/config";
    private static final String JETTY_XML = "jetty.xml";
    private static final String JETTY_VERSION = "jetty-v9.4.8.v20171121";
    private Server server;
    private WebAppContext webAppContext;
    private static final Logger LOG = LoggerFactory.getLogger(Jetty9Server.class);
    private GoSSLConfig goSSLConfig;

    public Jetty9Server(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory) {
        this(systemEnvironment, password, sslSocketFactory, new Server());
    }

    Jetty9Server(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory, Server server) {
        super(systemEnvironment, password, sslSocketFactory);
        systemEnvironment.set(SystemEnvironment.JETTY_XML_FILE_NAME, JETTY_XML);
        this.server = server;
        goSSLConfig = new GoSSLConfig(systemEnvironment);
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

        handlers.addHandler(gzipHandler(webAppContext));
        JettyCustomErrorPageHandler errorHandler = new JettyCustomErrorPageHandler();
        webAppContext.setErrorHandler(errorHandler);
        server.addBean(errorHandler);
        server.setHandler(handlers);
        performCustomConfiguration();
        server.setStopAtShutdown(true);
    }

    static GzipHandler gzipHandler(Handler handler) {
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
        gzipHandler.setHandler(handler);
        return gzipHandler;
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

    private MBeanContainer mbeans() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanContainer mbeans = new MBeanContainer(platformMBeanServer);
        return mbeans;
    }

    ContextHandler welcomeFileHandler() {
        return new GoServerWelcomeFileHandler(systemEnvironment);
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
        if (FileUtils.readFileToString(jettyConfig, UTF_8).contains(JETTY_VERSION)) return;
        replaceFileWithPackagedOne(jettyConfig);
    }

    private void replaceFileWithPackagedOne(File jettyConfig) {
        InputStream inputStream = null;
        try {
            inputStream = getClass().getResourceAsStream(JETTY_XML_LOCATION_IN_JAR + "/" + jettyConfig.getName());
            if (inputStream == null) {
                throw new RuntimeException(format("Resource {0}/{1} does not exist in the classpath", JETTY_XML_LOCATION_IN_JAR, jettyConfig.getName()));
            }
            FileUtils.copyInputStreamToFile(inputStream, systemEnvironment.getJettyConfigFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
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

    WebAppContext createWebAppContext() throws IOException, SAXException {
        webAppContext = new WebAppContext();
        webAppContext.setDefaultsDescriptor(GoWebXmlConfiguration.configuration(getWarFile()));

        webAppContext.setConfigurationClasses(new String[]{
                WebInfConfiguration.class.getCanonicalName(),
                WebXmlConfiguration.class.getCanonicalName(),
                JettyWebXmlConfiguration.class.getCanonicalName()
        });
        webAppContext.setContextPath(systemEnvironment.getWebappContextPath());

        // delegate all logging to parent classloader to avoid initialization of loggers in multiple classloaders
        webAppContext.addSystemClass("org.apache.log4j.");
        webAppContext.addSystemClass("org.slf4j.");
        webAppContext.addSystemClass("org.apache.commons.logging.");

        webAppContext.setWar(getWarFile());
        webAppContext.setParentLoaderPriority(systemEnvironment.getParentLoaderPriority());
        return webAppContext;
    }

    public Server getServer() {
        return server;
    }
}
