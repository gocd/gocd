package com.thoughtworks.go.server.util;

import com.thoughtworks.go.server.JettyServer;
import com.thoughtworks.go.util.ArrayUtil;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jgit.util.TemporaryBuffer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.contains;
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
        GoCipherSuite cipherSuite = mock(GoCipherSuite.class);
        String[] cipherSuitesToBeIncluded = {"FOO"};
        when(cipherSuite.getCipherSuitsToBeIncluded()).thenReturn(cipherSuitesToBeIncluded);
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getSslServerPort()).thenReturn(1234);
        when(systemEnvironment.keystore()).thenReturn(keystore);
        when(systemEnvironment.truststore()).thenReturn(truststore);

        JettyServer jettyServer = mock(JettyServer.class);
        when(jettyServer.getServer()).thenReturn(new Server());
        sslSocketConnector = new GoSslSocketConnector(jettyServer, "password", systemEnvironment, cipherSuite);
    }

    @Test
    public void shouldCreateSslConnectorWithRelevantPortAndTimeout() {
        assertThat(sslSocketConnector.getConnector() instanceof ServerConnector, is(true));
        ServerConnector connector = (ServerConnector) sslSocketConnector.getConnector();
        assertThat(connector.getPort(), is(1234));
        assertThat(connector.getIdleTimeout(), is(GoSslSocketConnector.MAX_IDLE_TIME));
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

        HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) ListUtil.find(connectionFactories, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                ConnectionFactory factory = (ConnectionFactory) item;
                return factory instanceof HttpConnectionFactory;
            }
        });
        assertThat(httpConnectionFactory.getHttpConfiguration().getOutputBufferSize(), is(GoSslSocketConnector.RESPONSE_BUFFER_SIZE));
        assertThat(httpConnectionFactory.getHttpConfiguration().getCustomizers().size(), is(1));
        assertThat(httpConnectionFactory.getHttpConfiguration().getCustomizers().get(0) instanceof SecureRequestCustomizer, is(true));
    }

    private SslContextFactory findSslContextFactory(Collection<ConnectionFactory> connectionFactories) {
        return ((SslConnectionFactory) ListUtil.find(connectionFactories, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                ConnectionFactory factory = (ConnectionFactory) item;
                return factory instanceof SslConnectionFactory;
            }
        })).getSslContextFactory();
    }


}