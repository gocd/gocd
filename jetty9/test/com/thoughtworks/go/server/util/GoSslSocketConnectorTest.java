/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.server.Jetty9Server;
import com.thoughtworks.go.server.config.GoSSLConfig;
import com.thoughtworks.go.util.ArrayUtil;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoSslSocketConnectorTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File truststore;
    private File keystore;
    private GoSslSocketConnector sslSocketConnector;

    @Before
    public void setUp() throws Exception {
        keystore = folder.newFile("keystore");
        truststore = folder.newFile("truststore");
        GoSSLConfig cipherSuite = mock(GoSSLConfig.class);
        String[] cipherSuitesToBeIncluded = {"FOO"};
        when(cipherSuite.getCipherSuitesToBeIncluded()).thenReturn(cipherSuitesToBeIncluded);
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getSslServerPort()).thenReturn(1234);
        when(systemEnvironment.keystore()).thenReturn(keystore);
        when(systemEnvironment.truststore()).thenReturn(truststore);
        when(systemEnvironment.get(SystemEnvironment.RESPONSE_BUFFER_SIZE)).thenReturn(100);
        when(systemEnvironment.get(SystemEnvironment.IDLE_TIMEOUT)).thenReturn(200);
        when(systemEnvironment.getListenHost()).thenReturn("foo");

        Jetty9Server jettyServer = mock(Jetty9Server.class);
        when(jettyServer.getServer()).thenReturn(new Server());
        sslSocketConnector = new GoSslSocketConnector(jettyServer, "password", systemEnvironment, cipherSuite);
    }

    @Test
    public void shouldCreateSslConnectorWithRelevantPortAndTimeout() {
        assertThat(sslSocketConnector.getConnector() instanceof ServerConnector, is(true));
        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        assertThat(connector.getPort(), is(1234));
        assertThat(connector.getHost(), is("foo"));
        assertThat(connector.getIdleTimeout(), is(200l));
    }

    @Test
    public void shouldSetupSslContextWithKeystoreAndTruststore() {
        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);
        assertThat(sslContextFactory.getKeyStorePath(), is(keystore.getAbsolutePath()));
        assertThat(sslContextFactory.getTrustStore(), is(truststore.getAbsolutePath()));
        assertThat(sslContextFactory.getWantClientAuth(), is(true));
    }

    @Test
    public void shouldSetupCipherSuitesToBeIncluded() {
        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);

        List<String> includedCipherSuites = ArrayUtil.asList(sslContextFactory.getIncludeCipherSuites());
        assertThat(includedCipherSuites.size(), is(1));
        assertThat(includedCipherSuites.contains("FOO"), is(true));
    }

    @Test
    public void shouldSetupHttpConnectionFactory() {
        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();

        HttpConnectionFactory httpConnectionFactory = getHttpConnectionFactory(connectionFactories);
        assertThat(httpConnectionFactory.getHttpConfiguration().getOutputBufferSize(), is(100));
        assertThat(httpConnectionFactory.getHttpConfiguration().getCustomizers().size(), is(2));
        assertThat(httpConnectionFactory.getHttpConfiguration().getCustomizers().get(0), instanceOf(SecureRequestCustomizer.class));
        assertThat(httpConnectionFactory.getHttpConfiguration().getCustomizers().get(1), instanceOf(ForwardedRequestCustomizer.class));
    }

    @Test
    public void shouldNotSendAServerHeaderForSecurityReasons() throws Exception {
        HttpConnectionFactory httpConnectionFactory = getHttpConnectionFactory(sslSocketConnector.getConnector().getConnectionFactories());
        HttpConfiguration configuration = httpConnectionFactory.getHttpConfiguration();

        assertThat(configuration.getSendServerVersion(), is(false));
    }

    private HttpConnectionFactory getHttpConnectionFactory(Collection<ConnectionFactory> connectionFactories) {
        return (HttpConnectionFactory) getConnectionFactoryOfType(connectionFactories, HttpConnectionFactory.class);
    }

    private SslContextFactory findSslContextFactory(Collection<ConnectionFactory> connectionFactories) {
        return ((SslConnectionFactory) getConnectionFactoryOfType(connectionFactories, SslConnectionFactory.class)).getSslContextFactory();
    }

    private ConnectionFactory getConnectionFactoryOfType(Collection<ConnectionFactory> connectionFactories, final Class<?> aClass) {
        return ListUtil.find(connectionFactories, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return aClass.isInstance(item);
            }
        });
    }
}
