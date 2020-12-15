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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.service.AgentUpgradeService;
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.plugin.infra.monitor.PluginJarLocationMonitor;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.work.AgentWorkContext;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
public class AgentHTTPClientControllerTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    @Mock
    private RemotingClient loopServer;
    @Mock
    private GoArtifactsManipulator artifactsManipulator;
    @Mock
    private SslInfrastructureService sslInfrastructureService;
    @Mock
    private Work work;
    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private SubprocessLogger subprocessLogger;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private AgentUpgradeService agentUpgradeService;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;
    @Mock
    private ArtifactExtension artifactExtension;
    @Mock
    private PluginJarLocationMonitor pluginJarLocationMonitor;
    private final String agentUuid = "uuid";
    private AgentHTTPClientController agentController;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @AfterEach
    void tearDown() {
        new GuidService().delete();
    }

    @Test
    void shouldSetPluginManagerReference() {
        agentController = createAgentController();
        assertThat(PluginManagerReference.reference().getPluginManager()).isEqualTo(pluginManager);
    }

    @Test
    void shouldRetrieveWorkFromServerAndDoIt() throws Exception {
        when(loopServer.getWork(any(AgentRuntimeInfo.class))).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(pluginJarLocationMonitor.hasRunAtLeastOnce()).thenReturn(true);
        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        agentController.work();
        verify(work).doWork(any(EnvironmentVariableContext.class), any(AgentWorkContext.class));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    void shouldNotRetrieveWorkIfPluginMonitorHasNotRun() throws IOException {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(pluginJarLocationMonitor.hasRunAtLeastOnce()).thenReturn(false);
        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        agentController.work();
        verifyZeroInteractions(work);
    }

    @Test
    void shouldRetrieveCookieIfNotPresent() throws Exception {
        agentController = createAgentController();
        agentController.init();

        when(loopServer.getCookie(eq(agentController.getAgentRuntimeInfo()))).thenReturn("cookie");
        when(sslInfrastructureService.isRegistered()).thenReturn(true);
        when(loopServer.getWork(agentController.getAgentRuntimeInfo())).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(pluginJarLocationMonitor.hasRunAtLeastOnce()).thenReturn(true);
        agentController.loop();
        verify(work).doWork(any(EnvironmentVariableContext.class), any(AgentWorkContext.class));
    }

    @Test
    void shouldNotTellServerWorkIsCompletedWhenThereIsNoWork() throws Exception {
        when(loopServer.getWork(any(AgentRuntimeInfo.class))).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        agentController.retrieveWork();
        verify(work).doWork(any(EnvironmentVariableContext.class), any(AgentWorkContext.class));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    void shouldRegisterSubprocessLoggerAtExit() throws Exception {
        SslInfrastructureService sslInfrastructureService = mock(SslInfrastructureService.class);
        AgentRegistry agentRegistry = mock(AgentRegistry.class);
        agentController = new AgentHTTPClientController(loopServer, loopServer, artifactsManipulator, sslInfrastructureService,
                agentRegistry, agentUpgradeService, subprocessLogger, systemEnvironment, pluginManager,
                packageRepositoryExtension, scmExtension, taskExtension, artifactExtension, null, null, pluginJarLocationMonitor);
        agentController.init();
        verify(subprocessLogger).registerAsExitHook("Following processes were alive at shutdown: ");
    }

    @Test
    void shouldNotPingIfNotRegisteredYet() throws Exception {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(sslInfrastructureService.isRegistered()).thenReturn(false);

        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    void shouldPingIfAfterRegistered() throws Exception {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(sslInfrastructureService.isRegistered()).thenReturn(true);
        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        verify(sslInfrastructureService).createSslInfrastructure();
        verify(loopServer).ping(any(AgentRuntimeInfo.class));
    }

    private AgentHTTPClientController createAgentController() {

        return new AgentHTTPClientController(
                loopServer, loopServer,
                artifactsManipulator,
                sslInfrastructureService,
                agentRegistry,
                agentUpgradeService,
                subprocessLogger,
                systemEnvironment,
                pluginManager,
                packageRepositoryExtension,
                scmExtension,
                taskExtension,
                artifactExtension, null, null, pluginJarLocationMonitor);
    }
}
