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

import com.thoughtworks.go.server.Jetty9Server;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.*;
import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoPlainSocketConnectorTest {
    @Test
    public void shouldCreateAServerConnectorWithConfiguredPortAndBuffersize() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getServerPort()).thenReturn(1234);
        when(systemEnvironment.get(SystemEnvironment.RESPONSE_BUFFER_SIZE)).thenReturn(100);
        when(systemEnvironment.get(SystemEnvironment.IDLE_TIMEOUT)).thenReturn(200);
        when(systemEnvironment.getListenHost()).thenReturn("foo");
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_ALLOW)).thenReturn(true);
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED)).thenReturn(true);
        Jetty9Server server = new Jetty9Server(systemEnvironment, null, mock(SSLSocketFactory.class));

        ServerConnector connector = (ServerConnector) new GoPlainSocketConnector(server, systemEnvironment).getConnector();

        assertThat(connector.getPort(), is(1234));
        assertThat(connector.getHost(), is("foo"));
        assertThat(connector.getIdleTimeout(), is(200l));
        HttpConnectionFactory connectionFactory = (HttpConnectionFactory) connector.getDefaultConnectionFactory();
        assertThat(connectionFactory.getHttpConfiguration().getOutputBufferSize(), is(100));
    }
}