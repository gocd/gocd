/*
 * Copyright 2023 Thoughtworks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.DispatcherType;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
@ExtendWith(MockitoExtension.class)
public class Jetty9ServerTest {
    @Mock(strictness = Mock.Strictness.LENIENT)
    private Server server;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private SystemEnvironment systemEnvironment;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private SSLSocketFactory sslSocketFactory;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private DeploymentManager deploymentManager;

    @SystemStub
    private SystemProperties systemProperties;

    private Jetty9Server jetty9Server;
    private Handler serverLevelHandler;
    private ArgumentCaptor<App> appCaptor;

    @TempDir
    File configDir;

    @BeforeEach
    public void setUp() throws Exception {
        when(server.getThreadPool()).thenReturn(new QueuedThreadPool(1));
        Answer<Void> setHandlerMock = invocation -> {
            serverLevelHandler = (Handler) invocation.getArguments()[0];
            serverLevelHandler.setServer((Server) invocation.getMock());
            return null;
        };
        lenient().doAnswer(setHandlerMock).when(server).setHandler(any(Handler.class));

        appCaptor = ArgumentCaptor.forClass(App.class);
        doNothing().when(deploymentManager).addApp(appCaptor.capture());

        when(systemEnvironment.getServerPort()).thenReturn(1234);
        when(systemEnvironment.getWebappContextPath()).thenReturn("context");
        when(systemEnvironment.getCruiseWar()).thenReturn("cruise.war");
        when(systemEnvironment.getParentLoaderPriority()).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.get(SystemEnvironment.RESPONSE_BUFFER_SIZE)).thenReturn(1000);
        when(systemEnvironment.get(SystemEnvironment.GO_SERVER_CONNECTION_IDLE_TIMEOUT_IN_MILLIS)).thenReturn(2000L);
        when(systemEnvironment.configDir()).thenReturn(configDir);
        when(systemEnvironment.getJettyConfigFile()).thenReturn(new File("foo"));
        when(systemEnvironment.isSessionCookieSecure()).thenReturn(false);
        when(systemEnvironment.sessionTimeoutInSeconds()).thenReturn(1234);
        when(systemEnvironment.sessionCookieMaxAgeInSeconds()).thenReturn(5678);

        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(new String[]{});
        jetty9Server = new Jetty9Server(systemEnvironment, server, deploymentManager);
        Jetty9Server.JETTY_XML_LOCATION_IN_JAR = "config";
    }

    @Test
    public void shouldAddMBeanContainerAsEventListener() throws Exception {
        ArgumentCaptor<MBeanContainer> captor = ArgumentCaptor.forClass(MBeanContainer.class);
        jetty9Server.configure();

        verify(server).addEventListener(captor.capture());
        MBeanContainer mBeanContainer = captor.getValue();
        assertThat(mBeanContainer.getMBeanServer(), is(not(nullValue())));
    }

    @Test
    public void shouldAddHttpSocketConnector() throws Exception {
        ArgumentCaptor<Connector> captor = ArgumentCaptor.forClass(Connector.class);
        jetty9Server.configure();

        verify(server, times(1)).addConnector(captor.capture());

        List<Connector> connectors = captor.getAllValues();
        Connector plainConnector = connectors.get(0);

        assertThat(plainConnector instanceof ServerConnector, is(true));
        ServerConnector connector = (ServerConnector) plainConnector;
        assertThat(connector.getServer(), is(server));
        assertThat(connector.getConnectionFactories().size(), is(1));
        ConnectionFactory connectionFactory = connector.getConnectionFactories().iterator().next();
        assertThat(connectionFactory instanceof HttpConnectionFactory, is(true));
    }

    @Test
    public void shouldAddRootRequestHandler() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        ContextHandler rootRequestHandler = getLoadedHandlers().get(GoServerLoadingIndicationHandler.class);
        assertThat(rootRequestHandler.getContextPath(), is("/"));
    }

    @Test
    public void shouldAddDefaultHeadersForRootContext() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        HttpServletRequest request = mock(HttpServletRequest.class);

        Request baseRequest = mock(Request.class);
        when(baseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(baseRequest.getHttpFields()).thenReturn(mock(HttpFields.class));

        ContextHandler rootPathHandler = getLoadedHandlers().get(GoServerLoadingIndicationHandler.class);
        rootPathHandler.setServer(server);
        rootPathHandler.start();
        rootPathHandler.handle("/something", baseRequest, request, response);

        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response).setHeader("X-UA-Compatible", "chrome=1");
    }

    @Test
    public void shouldAddResourceHandlerForAssets() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        ContextHandler assetsContextHandler = getLoadedHandlers().get(AssetsContextHandler.class);
        assertThat(assetsContextHandler.getContextPath(), is("context/assets"));
    }

    @Test
    public void shouldAddWebAppContextHandler() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);

        assertThat(webAppContext, instanceOf(WebAppContext.class));
        List<String> configClasses = new ArrayList<>(List.of(webAppContext.getConfigurationClasses()));
        assertThat(configClasses.contains(WebInfConfiguration.class.getCanonicalName()), is(true));
        assertThat(configClasses.contains(WebXmlConfiguration.class.getCanonicalName()), is(true));
        assertThat(configClasses.contains(JettyWebXmlConfiguration.class.getCanonicalName()), is(true));
        assertThat(webAppContext.getContextPath(), is("context"));
        assertThat(webAppContext.getWar(), is("cruise.war"));
        assertThat(webAppContext.isParentLoaderPriority(), is(true));
        assertThat(webAppContext.getDefaultsDescriptor(), is("jar:file:cruise.war!/WEB-INF/webdefault.xml"));
    }

    @Test
    public void shouldSetStopAtShutdown() throws Exception {
        jetty9Server.configure();
        verify(server).setStopAtShutdown(true);
    }

    @Test
    public void shouldSetSessionMaxInactiveInterval() throws Exception {
        jetty9Server.configure();
        jetty9Server.setSessionConfig();
        jetty9Server.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getSessionHandler().getMaxInactiveInterval(), is(1234));
    }

    @Test
    public void shouldSetSessionCookieConfig() throws Exception {
        when(systemEnvironment.isSessionCookieSecure()).thenReturn(true);
        jetty9Server.configure();
        jetty9Server.setSessionConfig();
        jetty9Server.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        SessionCookieConfig sessionCookieConfig = webAppContext.getSessionHandler().getSessionCookieConfig();
        assertThat(sessionCookieConfig.isHttpOnly(), is(true));
        assertThat(sessionCookieConfig.isSecure(), is(true));
        assertThat(sessionCookieConfig.getMaxAge(), is(5678));

        when(systemEnvironment.isSessionCookieSecure()).thenReturn(false);
        jetty9Server.setSessionConfig();
        assertThat(sessionCookieConfig.isSecure(), is(false));
    }

    @Test
    public void shouldSetInitParams() throws Exception {
        jetty9Server.configure();
        jetty9Server.setInitParameter("name", "value");
        jetty9Server.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getInitParameter("name"), is("value"));
    }

    @Test
    public void shouldReplaceJettyXmlIfItDoesNotContainCorrespondingJettyVersionNumber(@TempDir Path temporaryFolder) throws IOException {
        Path jettyXml = Files.createFile(temporaryFolder.resolve("jetty.xml"));
        when(systemEnvironment.getJettyConfigFile()).thenReturn(jettyXml.toFile());

        String originalContent = "jetty-v6.2.3\nsome other local changes";
        Files.writeString(jettyXml, originalContent, UTF_8);
        jetty9Server.replaceJettyXmlIfItBelongsToADifferentVersion(systemEnvironment.getJettyConfigFile());
        try (InputStream configStream = Objects.requireNonNull(getClass().getResourceAsStream("config/jetty.xml"))) {
            assertThat(Files.readString(jettyXml, UTF_8), is(new String(configStream.readAllBytes(), UTF_8)));
        }
    }

    @Test
    public void shouldNotReplaceJettyXmlIfItAlreadyContainsCorrespondingVersionNumber(@TempDir Path temporaryFolder) throws IOException {
        Path jettyXml = Files.createFile(temporaryFolder.resolve("jetty.xml"));
        when(systemEnvironment.getJettyConfigFile()).thenReturn(jettyXml.toFile());

        String originalContent = Jetty9Server.JETTY_CONFIG_VERSION + "\nsome other local changes";
        Files.writeString(jettyXml, originalContent, UTF_8);
        jetty9Server.replaceJettyXmlIfItBelongsToADifferentVersion(systemEnvironment.getJettyConfigFile());
        assertThat(Files.readString(jettyXml, UTF_8), is(originalContent));
    }

    @Test
    public void shouldSetErrorHandlerForServer() throws Exception {
        jetty9Server.configure();
        verify(server).addBean(any(JettyCustomErrorPageHandler.class));
    }

    @Test
    public void shouldSetErrorHandlerForWebAppContext() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getErrorHandler() instanceof JettyCustomErrorPageHandler, is(true));
    }

    @Test
    public void shouldAddGzipHandlerAtWebAppContextLevel() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getGzipHandler(), is(not(nullValue())));
    }

    @Test
    public void shouldHaveAHandlerCollectionAtServerLevel_ToAllowRequestLoggingHandlerToBeAdded() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        assertThat(serverLevelHandler, instanceOf(HandlerCollection.class));
        assertThat(serverLevelHandler, not(instanceOf(ContextHandlerCollection.class)));

        Handler[] contentsOfServerLevelHandler = ((HandlerCollection) serverLevelHandler).getHandlers();
        assertThat(contentsOfServerLevelHandler.length, is(1));
        assertThat(contentsOfServerLevelHandler[0], instanceOf(ContextHandlerCollection.class));
    }

    private Map<Class<? extends ContextHandler>, ContextHandler> getLoadedHandlers() throws Exception {
        Map<Class<? extends ContextHandler>, ContextHandler> handlerTypeToHandler = new HashMap<>();
        for (App app : appCaptor.getAllValues()) {
            handlerTypeToHandler.put(app.getContextHandler().getClass(), app.getContextHandler());
        }
        return handlerTypeToHandler;
    }
}
