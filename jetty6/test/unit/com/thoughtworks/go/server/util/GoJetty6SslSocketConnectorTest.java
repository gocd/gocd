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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.security.SslSelectChannelConnector;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoJetty6SslSocketConnectorTest {
    private SystemEnvironment systemEnvironment;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private GoJetty6SslSocketConnector connector;

    @Before
    public void setUp() throws Exception {
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
        GoJetty6CipherSuite goJetty6CipherSuite = mock(GoJetty6CipherSuite.class);
        connector = new GoJetty6SslSocketConnector("pwd", systemEnvironment, goJetty6CipherSuite);
        when(goJetty6CipherSuite.getExcludedCipherSuites()).thenReturn(new String[]{"CS1", "CS2"});
    }

    @Test
    public void shouldCreateSslConnector() throws IOException {
        assertThat(connector.sslConnector() instanceof SslSelectChannelConnector, is(true));
        SslSelectChannelConnector sslConnector = (SslSelectChannelConnector) connector.sslConnector();
        assertThat(sslConnector.getPort(), is(4567));
        assertThat(sslConnector.getHost(), is("localhost"));
        assertThat(sslConnector.getMaxIdleTime(), is(30000));
        assertThat(sslConnector.getWantClientAuth(), is(true));
        assertThat(FileUtil.isChildOf(temporaryFolder.getRoot(), new File(sslConnector.getKeystore())), is(true));
        assertThat(FileUtil.isChildOf(temporaryFolder.getRoot(), new File(sslConnector.getTruststore())), is(true));
        assertThat(sslConnector.getExcludeCipherSuites(), is(new String[]{"CS1", "CS2"}));
    }
}