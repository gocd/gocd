/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
        Jetty9Server server = new Jetty9Server(mock(SystemEnvironment.class), null, mock(SSLSocketFactory.class));
        ServerConnector connector = (ServerConnector) new GoPlainSocketConnector(server, systemEnvironment).getConnector();

        assertThat(connector.getPort(), is(1234));
        assertThat(connector.getIdleTimeout(), is(30000l));
        HttpConnectionFactory connectionFactory = (HttpConnectionFactory) connector.getDefaultConnectionFactory();
        assertThat(connectionFactory.getHttpConfiguration().getOutputBufferSize(), is(32768));
    }
}