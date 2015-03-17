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

import com.thoughtworks.go.server.util.GoJetty6CipherSuite;
import com.thoughtworks.go.util.ArrayUtil;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mortbay.component.Container;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.SslSelectChannelConnector;
import org.mortbay.jetty.webapp.JettyWebXmlConfiguration;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebInfConfiguration;
import org.mortbay.jetty.webapp.WebXmlConfiguration;
import org.mortbay.management.MBeanContainer;

import javax.net.ssl.SSLSocketFactory;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class Jetty6ServerTest {

    private Jetty6Server jetty6Server;
    private Server server;
    private SystemEnvironment systemEnvironment;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private SSLSocketFactory sslSocketFactory;
    private Container container;
    private GoJetty6CipherSuite goJetty6CipherSuite;
    private Jetty6GoWebXmlConfiguration configuration;


    @Before
    public void setUp() throws Exception {
        server = mock(Server.class);
        container = mock(Container.class);
        when(server.getContainer()).thenReturn(container);
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getServerPort()).thenReturn(1234);
        when(systemEnvironment.getSslServerPort()).thenReturn(4567);
        when(systemEnvironment.keystore()).thenReturn(temporaryFolder.newFolder());
        when(systemEnvironment.truststore()).thenReturn(temporaryFolder.newFolder());
        when(systemEnvironment.getWebappContextPath()).thenReturn("context");
        when(systemEnvironment.getCruiseWar()).thenReturn("cruise.war");
        when(systemEnvironment.getParentLoaderPriority()).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.useNioSslSocket()).thenReturn(true);
        when(systemEnvironment.getListenHost()).thenReturn("localhost");
        when(systemEnvironment.getParentLoaderPriority()).thenReturn(false);

        sslSocketFactory = mock(SSLSocketFactory.class);
        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(new String[]{});
        goJetty6CipherSuite = mock(GoJetty6CipherSuite.class);
        when(goJetty6CipherSuite.getExcludedCipherSuites()).thenReturn(new String[]{"CS1", "CS2"});
        configuration = mock(Jetty6GoWebXmlConfiguration.class);
        jetty6Server = new Jetty6Server(systemEnvironment, "pwd", sslSocketFactory, server, goJetty6CipherSuite, configuration);
    }

    @After
    public void tearDown() throws Exception {
        verify(configuration).initialize(systemEnvironment.getCruiseWar());
    }

    @Test
    public void shouldAddMBeanContainerAsEventListener() throws Exception {
        ArgumentCaptor<MBeanContainer> captor = ArgumentCaptor.forClass(MBeanContainer.class);
        jetty6Server.configure();

        verify(container).addEventListener(captor.capture());
        MBeanContainer mBeanContainer = captor.getValue();
        assertThat(mBeanContainer.getMBeanServer(), is(not(nullValue())));
    }

    @Test
    public void shouldAddHttpSocketConnector() throws Exception {
        ArgumentCaptor<Connector> captor = ArgumentCaptor.forClass(Connector.class);
        jetty6Server.configure();

        verify(server, times(2)).addConnector(captor.capture());

        List<Connector> connectors = captor.getAllValues();
        Connector plainConnector = connectors.get(0);

        assertThat(plainConnector instanceof SelectChannelConnector, is(true));
        SelectChannelConnector connector = (SelectChannelConnector) plainConnector;
        assertThat(connector.getPort(), is(1234));
        assertThat(connector.getHost(), is("localhost"));
    }

    @Test
    public void shouldAddSSLSocketConnector() throws Exception {
        ArgumentCaptor<Connector> captor = ArgumentCaptor.forClass(Connector.class);
        jetty6Server.configure();

        verify(server, times(2)).addConnector(captor.capture());
        List<Connector> connectors = captor.getAllValues();
        Connector sslConnector = connectors.get(1);

        assertThat(sslConnector instanceof SslSelectChannelConnector, is(true));
        SslSelectChannelConnector connector = (SslSelectChannelConnector) sslConnector;


        assertThat(connector.getPort(), is(4567));
        assertThat(connector.getHost(), is("localhost"));
        assertThat(connector.getMaxIdleTime(), is(30000));
//        assertThat(connector.getKeystore(), is(nullValue()));
//        assertThat(connector.getPassword(), is());
//        assertThat(connector.getKeyPassword(), is());
//        assertThat(connector.getTruststore(), is(nullValue()));
//        assertThat(connector.setTrustPassword(password), is());
        assertThat(connector.getWantClientAuth(), is(true));
        assertThat(connector.getExcludeCipherSuites(), is(new String[]{"CS1", "CS2"}));
    }

    @Test
    public void shouldAddWelcomeRequestHandler() throws Exception {
        ArgumentCaptor<ContextHandler> captor = ArgumentCaptor.forClass(ContextHandler.class);
        jetty6Server.configure();

        verify(server, times(4)).addHandler(captor.capture());
        List<ContextHandler> handlers = captor.getAllValues();
        assertThat(handlers.size(), is(4));
        ContextHandler handler = handlers.get(0);
        assertThat(handler instanceof Jetty6Server.GoServerWelcomeFileHandler, is(true));

        Jetty6Server.GoServerWelcomeFileHandler welcomeFileHandler = (Jetty6Server.GoServerWelcomeFileHandler) handler;
        assertThat(welcomeFileHandler.getContextPath(), is("/"));
    }

    @Test
    public void shouldAddLegacyUrlRequestHandler() throws Exception {
        ArgumentCaptor<ContextHandler> captor = ArgumentCaptor.forClass(ContextHandler.class);
        jetty6Server.configure();

        verify(server, times(4)).addHandler(captor.capture());
        List<ContextHandler> handlers = captor.getAllValues();
        assertThat(handlers.size(), is(4));
        ContextHandler handler = handlers.get(1);
        assertThat(handler instanceof Jetty6Server.LegacyUrlRequestHandler, is(true));
        Jetty6Server.LegacyUrlRequestHandler legacyUrlRequestHandler = (Jetty6Server.LegacyUrlRequestHandler) handler;
        assertThat(legacyUrlRequestHandler.getContextPath(), is("/cruise"));
    }

    @Test
    public void shouldAddResourceHandlerForAssets() throws Exception {
        ArgumentCaptor<ContextHandler> captor = ArgumentCaptor.forClass(ContextHandler.class);
        jetty6Server.configure();

        verify(server, times(4)).addHandler(captor.capture());
        List<ContextHandler> handlers = captor.getAllValues();
        assertThat(handlers.size(), is(4));
        ContextHandler handler = handlers.get(2);
        assertThat(handler instanceof Jetty6AssetsContextHandler, is(true));
        Jetty6AssetsContextHandler assetsContextHandler = (Jetty6AssetsContextHandler) handler;
        assertThat(assetsContextHandler.getContextPath(), is("context/assets"));
    }

    @Test
    public void shouldAddWebAppContextHandler() throws Exception {
        ArgumentCaptor<ContextHandler> captor = ArgumentCaptor.forClass(ContextHandler.class);
        jetty6Server.configure();

        verify(server, times(4)).addHandler(captor.capture());
        List<ContextHandler> handlers = captor.getAllValues();
        assertThat(handlers.size(), is(4));
        ContextHandler handler = handlers.get(3);
        assertThat(handler instanceof WebAppContext, is(true));
        WebAppContext webAppContext = (WebAppContext) handler;
        List<String> configClasses = ArrayUtil.asList(webAppContext.getConfigurationClasses());
        assertThat(configClasses.contains(WebInfConfiguration.class.getCanonicalName()), is(true));
        assertThat(configClasses.contains(WebXmlConfiguration.class.getCanonicalName()), is(true));
        assertThat(configClasses.contains(JettyWebXmlConfiguration.class.getCanonicalName()), is(true));
        assertThat(webAppContext.getContextPath(), is("context"));
        assertThat(webAppContext.getWar(), is("cruise.war"));
        assertThat(webAppContext.isParentLoaderPriority(), is(false));
        assertThat(webAppContext.getDefaultsDescriptor(), is("org/mortbay/jetty/webapp/webdefault.xml"));
    }

    @Test
    public void shouldSetStopAtShutdown() throws Exception {
        jetty6Server.configure();
        verify(server).setStopAtShutdown(true);
    }

    @Test
    public void shouldAddExtraJarsIntoClassPath() throws Exception {
        jetty6Server.configure();
        jetty6Server.addExtraJarsToClasspath("test-addons/some-addon-dir/addon-1.JAR,test-addons/some-addon-dir/addon-2.jar");
        assertThat(getWebAppContext(jetty6Server).getExtraClasspath(), is("test-addons/some-addon-dir/addon-1.JAR,test-addons/some-addon-dir/addon-2.jar"));
    }

    @Test
    public void shouldSetInitParams() throws Exception {
        jetty6Server.configure();
        jetty6Server.setInitParameter("name", "value");

        assertThat(getWebAppContext(jetty6Server).getInitParameter("name"), CoreMatchers.is("value"));
    }

    private WebAppContext getWebAppContext(Jetty6Server server) {
        return (WebAppContext) ReflectionUtil.getField(server, "webAppContext");
    }
}
