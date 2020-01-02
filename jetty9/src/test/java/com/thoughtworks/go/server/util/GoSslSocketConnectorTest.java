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
package com.thoughtworks.go.server.util;

import com.thoughtworks.go.server.Jetty9Server;
import com.thoughtworks.go.server.config.GoSSLConfig;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoSslSocketConnectorTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    private File truststore;
    private File keystore;
    private GoSslSocketConnector sslSocketConnector;
    @Mock
    private GoSSLConfig goSSLConfig;
    @Mock
    private Jetty9Server jettyServer;
    @Mock
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        keystore = folder.newFile("keystore");
        truststore = folder.newFile("truststore");
        String[] cipherSuitesToBeIncluded = {"FOO"};
        when(goSSLConfig.getCipherSuitesToBeIncluded()).thenReturn(cipherSuitesToBeIncluded);
        when(systemEnvironment.getSslServerPort()).thenReturn(1234);
        when(systemEnvironment.keystore()).thenReturn(keystore);
        when(systemEnvironment.truststore()).thenReturn(truststore);
        when(systemEnvironment.get(SystemEnvironment.RESPONSE_BUFFER_SIZE)).thenReturn(100);
        when(systemEnvironment.get(SystemEnvironment.IDLE_TIMEOUT)).thenReturn(200);
        when(systemEnvironment.getListenHost()).thenReturn("foo");
        when(jettyServer.getServer()).thenReturn(new Server());
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_CLEAR_JETTY_DEFAULT_EXCLUSIONS)).thenReturn(false);
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_JETTY_WANT_CLIENT_AUTH)).thenReturn(false);
        sslSocketConnector = new GoSslSocketConnector(jettyServer, "password", systemEnvironment, goSSLConfig);
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
    public void shouldSetupSslContextWithKeystoreAndTruststore() throws IOException {
        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);
        assertThat(sslContextFactory.getKeyStorePath(), is(keystore.getCanonicalFile().toPath().toAbsolutePath().toUri().toString()));
        assertThat(sslContextFactory.getTrustStorePath(), is(truststore.getCanonicalFile().toPath().toAbsolutePath().toUri().toString()));
        assertThat(sslContextFactory.getWantClientAuth(), is(false));
    }

    @Test
    public void shouldSetupCipherSuitesToBeIncluded() {
        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);

        List<String> includedCipherSuites = new ArrayList<>(Arrays.asList(sslContextFactory.getIncludeCipherSuites()));
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

    @Test
    public void shouldLeaveTheDefaultCipherSuiteInclusionAndExclusionListUnTouchedIfNotOverridden() {
        when(goSSLConfig.getCipherSuitesToBeIncluded()).thenReturn(null);
        when(goSSLConfig.getCipherSuitesToBeExcluded()).thenReturn(null);
        sslSocketConnector = new GoSslSocketConnector(jettyServer, "password", systemEnvironment, goSSLConfig);

        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);

        assertThat(sslContextFactory.getExcludeCipherSuites(), is(arrayWithSize(5)));
        assertThat(sslContextFactory.getExcludeCipherSuites(), is(arrayContainingInAnyOrder("^.*_(MD5|SHA|SHA1)$", "^TLS_RSA_.*$", "^SSL_.*$", "^.*_NULL_.*$", "^.*_anon_.*$")));
        assertThat(sslContextFactory.getIncludeCipherSuites(), is(emptyArray()));
    }

    @Test
    public void shouldClearOutDefaultProtocolsAndCipherSetByJettyIfFlagIsSet() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_CLEAR_JETTY_DEFAULT_EXCLUSIONS)).thenReturn(true);

        when(goSSLConfig.getProtocolsToBeExcluded()).thenReturn(null);
        when(goSSLConfig.getProtocolsToBeIncluded()).thenReturn(null);
        when(goSSLConfig.getCipherSuitesToBeIncluded()).thenReturn(null);
        when(goSSLConfig.getCipherSuitesToBeExcluded()).thenReturn(null);
        sslSocketConnector = new GoSslSocketConnector(jettyServer, "password", systemEnvironment, goSSLConfig);

        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);

        assertThat(sslContextFactory.getExcludeProtocols().length, is(0));
        assertThat(sslContextFactory.getIncludeProtocols().length, is(0));
        assertThat(sslContextFactory.getExcludeCipherSuites().length, is(0));
        assertThat(sslContextFactory.getIncludeCipherSuites().length, is(0));
    }

    @Test
    public void shouldOverrideTheDefaultCipherSuiteExclusionListIfConfigured() {
        when(goSSLConfig.getCipherSuitesToBeExcluded()).thenReturn(new String[]{"*MD5*"});
        when(goSSLConfig.getCipherSuitesToBeIncluded()).thenReturn(new String[]{"*ECDHE*"});
        sslSocketConnector = new GoSslSocketConnector(jettyServer, "password", systemEnvironment, goSSLConfig);

        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);

        assertThat(sslContextFactory.getExcludeCipherSuites().length, is(1));
        assertThat(sslContextFactory.getExcludeCipherSuites()[0], is("*MD5*"));
        assertThat(sslContextFactory.getIncludeCipherSuites().length, is(1));
        assertThat(sslContextFactory.getIncludeCipherSuites()[0], is("*ECDHE*"));
    }

    @Test
    public void shouldLeaveTheDefaultProtocolInclusionAndExclusionListUnTouchedIfNotOverridden() {
        when(goSSLConfig.getProtocolsToBeIncluded()).thenReturn(null);
        when(goSSLConfig.getProtocolsToBeExcluded()).thenReturn(null);
        sslSocketConnector = new GoSslSocketConnector(jettyServer, "password", systemEnvironment, goSSLConfig);

        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);

        assertThat(sslContextFactory.getExcludeProtocols().length, is(4));
        assertThat(Arrays.asList(sslContextFactory.getExcludeProtocols()).containsAll(Arrays.asList("SSL", "SSLv2", "SSLv2Hello", "SSLv3")), is(true));
        assertThat(sslContextFactory.getIncludeProtocols().length, is(0));
    }

    @Test
    public void shouldOverrideTheDefaultProtocolExclusionListIfConfigured() {
        when(goSSLConfig.getProtocolsToBeExcluded()).thenReturn(new String[]{"SSL", "TLS1.0", "TLS1.1"});
        when(goSSLConfig.getProtocolsToBeIncluded()).thenReturn(new String[]{"TLS1.2"});
        sslSocketConnector = new GoSslSocketConnector(jettyServer, "password", systemEnvironment, goSSLConfig);

        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        SslContextFactory sslContextFactory = findSslContextFactory(connectionFactories);

        assertThat(sslContextFactory.getExcludeProtocols().length, is(3));
        assertThat(Arrays.asList(sslContextFactory.getExcludeProtocols()).containsAll(Arrays.asList("SSL", "TLS1.0", "TLS1.1")), is(true));
        assertThat(sslContextFactory.getIncludeProtocols().length, is(1));
        assertThat(sslContextFactory.getIncludeProtocols()[0], is("TLS1.2"));
    }


    private HttpConnectionFactory getHttpConnectionFactory(Collection<ConnectionFactory> connectionFactories) {
        return (HttpConnectionFactory) getConnectionFactoryOfType(connectionFactories, HttpConnectionFactory.class);
    }

    private SslContextFactory findSslContextFactory(Collection<ConnectionFactory> connectionFactories) {
        return ((SslConnectionFactory) getConnectionFactoryOfType(connectionFactories, SslConnectionFactory.class)).getSslContextFactory();
    }

    private ConnectionFactory getConnectionFactoryOfType(Collection<ConnectionFactory> connectionFactories, final Class<?> aClass) {
        return connectionFactories.stream().filter(new Predicate<ConnectionFactory>() {
            @Override
            public boolean test(ConnectionFactory item) {
                return aClass.isInstance(item);
            }
        }).findFirst().orElse(null);
    }
}
