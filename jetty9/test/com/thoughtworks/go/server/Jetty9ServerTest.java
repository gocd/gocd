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

import com.thoughtworks.go.util.ArrayUtil;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class Jetty9ServerTest {

    private Jetty9Server jetty9Server;
    private Server server;
    private SystemEnvironment systemEnvironment;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File configDir;
    private DeploymentManager deploymentManager;
    private ArgumentCaptor<App> appCaptor;

    @Before
    public void setUp() throws Exception {
        server = mock(Server.class);
        deploymentManager = mock(DeploymentManager.class);

        Answer<Void> setHandlerMock = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Handler handler = (Handler) invocation.getArguments()[0];
                handler.setServer((Server) invocation.getMock());
                return null;
            }
        };
        Mockito.doAnswer(setHandlerMock).when(server).setHandler(any(Handler.class));

        appCaptor = ArgumentCaptor.forClass(App.class);
        Mockito.doNothing().when(deploymentManager).addApp(appCaptor.capture());

        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getServerPort()).thenReturn(1234);
        when(systemEnvironment.keystore()).thenReturn(temporaryFolder.newFolder());
        when(systemEnvironment.truststore()).thenReturn(temporaryFolder.newFolder());
        when(systemEnvironment.getWebappContextPath()).thenReturn("context");
        when(systemEnvironment.getCruiseWar()).thenReturn("cruise.war");
        when(systemEnvironment.getParentLoaderPriority()).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.get(SystemEnvironment.RESPONSE_BUFFER_SIZE)).thenReturn(1000);
        when(systemEnvironment.get(SystemEnvironment.IDLE_TIMEOUT)).thenReturn(2000);
        when(systemEnvironment.configDir()).thenReturn(configDir = temporaryFolder.newFile());
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_ALLOW)).thenReturn(true);
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED)).thenReturn(true);
        when(systemEnvironment.getJettyConfigFile()).thenReturn(new File("foo"));

        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(new String[]{});
        jetty9Server = new Jetty9Server(systemEnvironment, "pwd", sslSocketFactory, server, deploymentManager);
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

        verify(server, times(2)).addConnector(captor.capture());

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
    public void shouldAddSSLSocketConnector() throws Exception {
        ArgumentCaptor<Connector> captor = ArgumentCaptor.forClass(Connector.class);
        jetty9Server.configure();

        verify(server, times(2)).addConnector(captor.capture());
        List<Connector> connectors = captor.getAllValues();
        Connector sslConnector = connectors.get(1);

        assertThat(sslConnector instanceof ServerConnector, is(true));
        ServerConnector connector = (ServerConnector) sslConnector;
        assertThat(connector.getServer(), is(server));
        assertThat(connector.getConnectionFactories().size(), is(2));
        Iterator<ConnectionFactory> iterator = connector.getConnectionFactories().iterator();
        ConnectionFactory first = iterator.next();
        ConnectionFactory second = iterator.next();
        assertThat(first instanceof SslConnectionFactory, is(true));
        SslConnectionFactory sslConnectionFactory = (SslConnectionFactory) first;
        assertThat(sslConnectionFactory.getProtocol(), is("SSL-HTTP/1.1"));
        assertThat(second instanceof HttpConnectionFactory, is(true));
    }

    @Test
    public void shouldAddWelcomeRequestHandler() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        ContextHandler welcomeFileHandler = getLoadedHandlers().get(Jetty9Server.GoServerWelcomeFileHandler.class);
        assertThat(welcomeFileHandler.getContextPath(), is("/"));
    }

    @Test
    public void shouldAddDefaultHeadersForRootContext() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getPathInfo()).thenReturn("/");

        ContextHandler rootPathHandler = getLoadedHandlers().get(Jetty9Server.GoServerWelcomeFileHandler.class);
        rootPathHandler.setServer(server);
        rootPathHandler.start();

        rootPathHandler.handle("/", mock(Request.class), request, response);

        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response).setHeader("X-UA-Compatible", "chrome=1");
    }

    @Test
    public void shouldSkipDefaultHeadersIfContextPathIsGoRootPath() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getPathInfo()).thenReturn("/go");

        ContextHandler rootPathHandler = getLoadedHandlers().get(Jetty9Server.GoServerWelcomeFileHandler.class);
        rootPathHandler.setServer(server);
        rootPathHandler.start();

        rootPathHandler.handle("/go", mock(Request.class), request, response);

        verify(response, never()).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response, never()).setHeader("X-Content-Type-Options", "nosniff");
        verify(response, never()).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response, never()).setHeader("X-UA-Compatible", "chrome=1");
    }

    @Test
    public void shouldSkipDefaultHeadersIfContextPathIsAnyOtherUrlWithinGo() throws Exception {
        jetty9Server.configure();
        jetty9Server.startHandlers();

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getPathInfo()).thenReturn("/go/pipelines");

        ContextHandler rootPathHandler = getLoadedHandlers().get(Jetty9Server.GoServerWelcomeFileHandler.class);
        rootPathHandler.setServer(server);
        rootPathHandler.start();

        rootPathHandler.handle("/go/pipelines", mock(Request.class), request, response);

        verify(response, never()).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response, never()).setHeader("X-Content-Type-Options", "nosniff");
        verify(response, never()).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response, never()).setHeader("X-UA-Compatible", "chrome=1");
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

        List<String> configClasses = ArrayUtil.asList(webAppContext.getConfigurationClasses());
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
        assertThat(webAppContext.getInitParameter("name"), CoreMatchers.is("value"));
    }

    @Test
    public void shouldReplaceJettyXmlIfItDoesNotContainCorrespondingJettyVersionNumber() throws IOException {
        File jettyXml = temporaryFolder.newFile("jetty.xml");
        when(systemEnvironment.getJettyConfigFile()).thenReturn(jettyXml);

        String originalContent = "jetty-v6.2.3\nsome other local changes";
        FileUtil.writeContentToFile(originalContent, jettyXml);
        jetty9Server.replaceJettyXmlIfItBelongsToADifferentVersion(systemEnvironment.getJettyConfigFile());
        assertThat(FileUtil.readContentFromFile(systemEnvironment.getJettyConfigFile()), is(FileUtil.readContentFromFile(new File(getClass().getResource("config/jetty.xml").getPath()))));
    }

    @Test
    public void shouldNotReplaceJettyXmlIfItAlreadyContainsCorrespondingVersionNumber() throws IOException {
        File jettyXml = temporaryFolder.newFile("jetty.xml");
        when(systemEnvironment.getJettyConfigFile()).thenReturn(jettyXml);

        String originalContent = "jetty-v9.2.3\nsome other local changes";
        FileUtil.writeContentToFile(originalContent, jettyXml);
        jetty9Server.replaceJettyXmlIfItBelongsToADifferentVersion(systemEnvironment.getJettyConfigFile());
        assertThat(FileUtil.readContentFromFile(systemEnvironment.getJettyConfigFile()), is(originalContent));
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

        ContextHandler webAppContext = getLoadedHandlers().get(WebAppContext.class);

        assertThat(webAppContext.getErrorHandler() instanceof JettyCustomErrorPageHandler, is(true));
    }

    private Map<Class<? extends ContextHandler>, ContextHandler> getLoadedHandlers() throws Exception {
        Map<Class<? extends ContextHandler>, ContextHandler> handlerTypeToHandler = new HashMap<>();
        for (App app : appCaptor.getAllValues()) {
            handlerTypeToHandler.put(app.getContextHandler().getClass(), app.getContextHandler());
        }
        return handlerTypeToHandler;
    }
}
