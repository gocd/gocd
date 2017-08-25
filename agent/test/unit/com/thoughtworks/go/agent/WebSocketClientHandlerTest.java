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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerWebSocketClientBuilder;
import com.thoughtworks.go.agent.service.AgentUpgradeService;
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.util.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.client.io.UpgradeListener;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

import static org.mockito.Mockito.*;

public class WebSocketClientHandlerTest {

    private WebSocketClientHandler webSocketClientHandler;
    private GoAgentServerWebSocketClientBuilder builder;
    private URLService urlService;
    private Future session;

    @Before
    public void setUp() throws Exception {
        builder = mock(GoAgentServerWebSocketClientBuilder.class);
        when(builder.build()).thenReturn(new WebSocketClientStub());

        urlService = mock(URLService.class);
        when(urlService.getAgentRemoteWebSocketUrl()).thenReturn("wss://localhost/websocket");

        webSocketClientHandler = new WebSocketClientHandler(builder, urlService);
        session = mock(Future.class);
    }

    @Test
    public void shouldVerifyThatWebSocketClientIsStarted() throws Exception {
        webSocketClientHandler.connect(createAgentController());
        verify(builder).build();
        verify(session).get();
    }

    @Test
    public void shouldVerifyThatWebSocketClientIsNotStartedIfAlreadyRunning() throws Exception {
        webSocketClientHandler.connect(createAgentController());
        webSocketClientHandler.connect(createAgentController());
        verify(builder, times(1)).build();
        verify(session, times(2)).get();
    }

    private AgentWebSocketClientController createAgentController() {
        return new AgentWebSocketClientController(mock(BuildRepositoryRemote.class),
                mock(GoArtifactsManipulator.class),
                mock(SslInfrastructureService.class),
                mock(AgentRegistry.class),
                mock(AgentUpgradeService.class),
                mock(SubprocessLogger.class),
                mock(SystemEnvironment.class),
                mock(PluginManager.class),
                mock(PackageRepositoryExtension.class),
                mock(SCMExtension.class),
                mock(TaskExtension.class),
                mock(HttpService.class),
                mock(WebSocketClientHandler.class),
                mock(WebSocketSessionHandler.class),
                mock(TimeProvider.class),
                null);
    }

    class WebSocketClientStub extends WebSocketClient {
        @Override
        public Future<Session> connect(Object websocket, URI toUri, ClientUpgradeRequest request, UpgradeListener upgradeListener) throws IOException {
            return session;
        }
    }
}