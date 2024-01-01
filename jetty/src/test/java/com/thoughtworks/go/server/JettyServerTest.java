/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketConfiguration;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
@ExtendWith(MockitoExtension.class)
public class JettyServerTest {
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

    private JettyServer jettyServer;
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
        jettyServer = new JettyServer(systemEnvironment, server, deploymentManager);
        JettyServer.JETTY_XML_LOCATION_IN_JAR = "config";
    }

    @Test
    public void shouldAddMBeanContainerAsEventListener() throws Exception {
        ArgumentCaptor<MBeanContainer> captor = ArgumentCaptor.forClass(MBeanContainer.class);
        jettyServer.configure();

        verify(server).addEventListener(captor.capture());
        MBeanContainer mBeanContainer = captor.getValue();
        assertThat(mBeanContainer.getMBeanServer()).isNotNull();
    }

    @Test
    public void shouldAddHttpSocketConnector() throws Exception {
        ArgumentCaptor<Connector> captor = ArgumentCaptor.forClass(Connector.class);
        jettyServer.configure();

        verify(server, times(1)).addConnector(captor.capture());

        assertThat(captor.getValue()).asInstanceOf(type(ServerConnector.class))
                .satisfies(connector -> {
                    assertThat(connector.getServer()).isEqualTo(server);
                    assertThat(connector.getConnectionFactories()).singleElement().isInstanceOf(HttpConnectionFactory.class);
                    assertThat(connector.getIdleTimeout()).isEqualTo(2000L);
                });
    }

    @Test
    public void shouldAddRootRequestHandler() throws Exception {
        jettyServer.configure();
        jettyServer.startHandlers();

        ContextHandler rootRequestHandler = getLoadedHandlers().get(GoServerLoadingIndicationHandler.class);
        assertThat(rootRequestHandler.getContextPath()).isEqualTo("/");
    }

    @Test
    public void shouldAddDefaultHeadersForRootContext() throws Exception {
        jettyServer.configure();
        jettyServer.startHandlers();

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
    public void shouldAddAndInitializeResourceHandlerForAssets(@TempDir Path temporaryFolder) throws Exception {
        jettyServer.configure();
        jettyServer.startHandlers();

        ContextHandler assetsContextHandler = getLoadedHandlers().get(AssetsContextHandler.class);
        assertThat(assetsContextHandler.getContextPath()).isEqualTo("context/assets");

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);

        // Initialize the webapp
        webAppContext.setInitParameter("rails.root", "/WEB-INF/somelocation");
        Files.createDirectory(temporaryFolder.resolve("WEB-INF"));
        webAppContext.setResourceBase(temporaryFolder.toString());
        webAppContext.start();

        // Ensure it was initialized
        assertThat(assetsContextHandler).asInstanceOf(type(AssetsContextHandler.class))
                .satisfies(handler -> assertThat(handler.getAssetsHandler().getResourceHandler()).satisfies(resourceHandler -> {
                    assertThat(resourceHandler.getCacheControl()).isEqualTo("max-age=31536000,public");
                    assertThat(resourceHandler.isEtags()).isFalse();
                    assertThat(resourceHandler.isDirAllowed()).isFalse();
                    assertThat(resourceHandler.isDirectoriesListed()).isFalse();
                    assertThat(resourceHandler.getResourceBase())
                            .isEqualTo(temporaryFolder.resolve("WEB-INF/somelocation/public/assets/").toUri().toString());
                }));
    }

    @Test
    public void shouldAddWebAppContextHandler() throws Exception {
        jettyServer.configure();
        jettyServer.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);

        assertThat(webAppContext).isInstanceOf(WebAppContext.class);
        assertThat(webAppContext.getConfigurationClasses()).containsExactly(
                WebInfConfiguration.class.getCanonicalName(),
                WebXmlConfiguration.class.getCanonicalName(),
                WebAppConfiguration.class.getCanonicalName(),
                JettyWebSocketConfiguration.class.getCanonicalName()
        );
        assertThat(webAppContext.getContextPath()).isEqualTo("context");
        assertThat(webAppContext.getWar()).isEqualTo("cruise.war");
        assertThat(webAppContext.isParentLoaderPriority()).isTrue();
        assertThat(webAppContext.getDefaultsDescriptor()).isEqualTo("jar:file:cruise.war!/WEB-INF/webdefault.xml");
    }

    @Test
    public void shouldSetStopAtShutdown() throws Exception {
        jettyServer.configure();
        verify(server).setStopAtShutdown(true);
    }

    @Test
    public void shouldSetSessionMaxInactiveInterval() throws Exception {
        jettyServer.configure();
        jettyServer.setSessionConfig();
        jettyServer.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getSessionHandler().getMaxInactiveInterval()).isEqualTo(1234);
    }

    @Test
    public void shouldSetSessionCookieConfig() throws Exception {
        when(systemEnvironment.isSessionCookieSecure()).thenReturn(true);
        jettyServer.configure();
        jettyServer.setSessionConfig();
        jettyServer.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        SessionCookieConfig sessionCookieConfig = webAppContext.getSessionHandler().getSessionCookieConfig();
        assertThat(sessionCookieConfig.isHttpOnly()).isTrue();
        assertThat(sessionCookieConfig.isSecure()).isTrue();
        assertThat(sessionCookieConfig.getMaxAge()).isEqualTo(5678);

        when(systemEnvironment.isSessionCookieSecure()).thenReturn(false);
        jettyServer.setSessionConfig();
        assertThat(sessionCookieConfig.isSecure()).isFalse();
    }

    @Test
    public void shouldSetInitParams() throws Exception {
        jettyServer.configure();
        jettyServer.setInitParameter("name", "value");
        jettyServer.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getInitParameter("name")).isEqualTo("value");
    }

    @Test
    public void shouldReplaceJettyXmlIfItDoesNotContainCorrespondingJettyVersionNumber(@TempDir Path temporaryFolder) throws IOException {
        Path jettyXml = Files.createFile(temporaryFolder.resolve("jetty.xml"));
        when(systemEnvironment.getJettyConfigFile()).thenReturn(jettyXml.toFile());

        String originalContent = "jetty-v6.2.3\nsome other local changes";
        Files.writeString(jettyXml, originalContent, UTF_8);
        jettyServer.replaceJettyXmlIfItBelongsToADifferentVersion(systemEnvironment.getJettyConfigFile());
        try (InputStream configStream = Objects.requireNonNull(getClass().getResourceAsStream("config/jetty.xml"))) {
            assertThat(Files.readString(jettyXml, UTF_8)).isEqualTo(new String(configStream.readAllBytes(), UTF_8));
        }
    }

    @Test
    public void shouldNotReplaceJettyXmlIfItAlreadyContainsCorrespondingVersionNumber(@TempDir Path temporaryFolder) throws IOException {
        Path jettyXml = Files.createFile(temporaryFolder.resolve("jetty.xml"));
        when(systemEnvironment.getJettyConfigFile()).thenReturn(jettyXml.toFile());

        String originalContent = JettyServer.JETTY_CONFIG_VERSION + "\nsome other local changes";
        Files.writeString(jettyXml, originalContent, UTF_8);
        jettyServer.replaceJettyXmlIfItBelongsToADifferentVersion(systemEnvironment.getJettyConfigFile());
        assertThat(Files.readString(jettyXml, UTF_8)).isEqualTo(originalContent);
    }

    @Test
    public void shouldSetErrorHandlerForServer() throws Exception {
        jettyServer.configure();
        verify(server).addBean(any(JettyCustomErrorPageHandler.class));
    }

    @Test
    public void shouldSetErrorHandlerForWebAppContext() throws Exception {
        jettyServer.configure();
        jettyServer.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getErrorHandler()).isInstanceOf(JettyCustomErrorPageHandler.class);
    }

    @Test
    public void shouldAddGzipHandlerAtWebAppContextLevel() throws Exception {
        jettyServer.configure();
        jettyServer.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getHandlers()).anySatisfy(handler -> assertThat(handler).isInstanceOf(GzipHandler.class));
    }

    @Test
    public void shouldHaveAHandlerCollectionAtServerLevel_ToAllowRequestLoggingHandlerToBeAdded() throws Exception {
        jettyServer.configure();
        jettyServer.startHandlers();

        assertThat(serverLevelHandler)
                .isInstanceOf(HandlerCollection.class)
                .isNotInstanceOf(ContextHandlerCollection.class);

        Handler[] contentsOfServerLevelHandler = ((HandlerCollection) serverLevelHandler).getHandlers();
        assertThat(contentsOfServerLevelHandler)
                .singleElement()
                .isInstanceOf(ContextHandlerCollection.class);
    }

    private Map<Class<? extends ContextHandler>, ContextHandler> getLoadedHandlers() throws Exception {
        Map<Class<? extends ContextHandler>, ContextHandler> handlerTypeToHandler = new HashMap<>();
        for (App app : appCaptor.getAllValues()) {
            handlerTypeToHandler.put(app.getContextHandler().getClass(), app.getContextHandler());
        }
        return handlerTypeToHandler;
    }
}
