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

import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.DispatcherType;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class Jetty9ServerTest {
    @Mock
    private Server server;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Mock
    private SSLSocketFactory sslSocketFactory;
    @Mock
    private DeploymentManager deploymentManager;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    private Jetty9Server jetty9Server;
    private Handler serverLevelHandler;
    private File configDir;
    private ArgumentCaptor<App> appCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(server.getThreadPool()).thenReturn(new QueuedThreadPool(1));
        Answer<Void> setHandlerMock = invocation -> {
            serverLevelHandler = (Handler) invocation.getArguments()[0];
            serverLevelHandler.setServer((Server) invocation.getMock());
            return null;
        };
        Mockito.doAnswer(setHandlerMock).when(server).setHandler(any(Handler.class));

        appCaptor = ArgumentCaptor.forClass(App.class);
        Mockito.doNothing().when(deploymentManager).addApp(appCaptor.capture());

        when(systemEnvironment.getServerPort()).thenReturn(1234);
        when(systemEnvironment.getWebappContextPath()).thenReturn("context");
        when(systemEnvironment.getCruiseWar()).thenReturn("cruise.war");
        when(systemEnvironment.getParentLoaderPriority()).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.get(SystemEnvironment.RESPONSE_BUFFER_SIZE)).thenReturn(1000);
        when(systemEnvironment.get(SystemEnvironment.IDLE_TIMEOUT)).thenReturn(2000);
        when(systemEnvironment.configDir()).thenReturn(configDir = temporaryFolder.newFolder());
        when(systemEnvironment.getJettyConfigFile()).thenReturn(new File("foo"));
        when(systemEnvironment.isSessionCookieSecure()).thenReturn(false);
        when(systemEnvironment.sessionTimeoutInSeconds()).thenReturn(1234);
        when(systemEnvironment.sessionCookieMaxAgeInSeconds()).thenReturn(5678);

        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(new String[]{});
        jetty9Server = new Jetty9Server(systemEnvironment, "pwd", server, deploymentManager);
        ReflectionUtil.setStaticField(Jetty9Server.class, "JETTY_XML_LOCATION_IN_JAR", "config");
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
        List<String> configClasses = new ArrayList<>(Arrays.asList(webAppContext.getConfigurationClasses()));
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
    public void shouldAddExtraJarsIntoClassPath() throws Exception {
        jetty9Server.configure();
        jetty9Server.addExtraJarsToClasspath("test-addons/some-addon-dir/addon-1.JAR,test-addons/some-addon-dir/addon-2.jar");
        jetty9Server.startHandlers();

        WebAppContext webAppContext = (WebAppContext) getLoadedHandlers().get(WebAppContext.class);
        assertThat(webAppContext.getExtraClasspath(), is("test-addons/some-addon-dir/addon-1.JAR,test-addons/some-addon-dir/addon-2.jar," + configDir));
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
    public void shouldReplaceJettyXmlIfItDoesNotContainCorrespondingJettyVersionNumber() throws IOException {
        File jettyXml = temporaryFolder.newFile("jetty.xml");
        when(systemEnvironment.getJettyConfigFile()).thenReturn(jettyXml);

        String originalContent = "jetty-v6.2.3\nsome other local changes";
        FileUtils.writeStringToFile(jettyXml, originalContent, UTF_8);
        jetty9Server.replaceJettyXmlIfItBelongsToADifferentVersion(systemEnvironment.getJettyConfigFile());
        assertThat(FileUtils.readFileToString(systemEnvironment.getJettyConfigFile(), UTF_8), is(FileUtils.readFileToString(new File(getClass().getResource("config/jetty.xml").getPath()), UTF_8)));
    }

    @Test
    public void shouldNotReplaceJettyXmlIfItAlreadyContainsCorrespondingVersionNumber() throws IOException {
        File jettyXml = temporaryFolder.newFile("jetty.xml");
        when(systemEnvironment.getJettyConfigFile()).thenReturn(jettyXml);

        String originalContent = "jetty-v9.4.8.v20171121\nsome other local changes";
        FileUtils.writeStringToFile(jettyXml, originalContent, UTF_8);
        jetty9Server.replaceJettyXmlIfItBelongsToADifferentVersion(systemEnvironment.getJettyConfigFile());
        assertThat(FileUtils.readFileToString(systemEnvironment.getJettyConfigFile(), UTF_8), is(originalContent));
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
