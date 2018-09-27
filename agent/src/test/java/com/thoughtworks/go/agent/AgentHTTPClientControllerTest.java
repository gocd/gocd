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
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.AgentWorkContext;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static com.thoughtworks.go.util.SystemUtil.getFirstLocalNonLoopbackIpAddress;
import static com.thoughtworks.go.util.SystemUtil.getLocalhostName;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentHTTPClientControllerTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    @Mock
    private BuildRepositoryRemote loopServer;
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
    private String agentUuid = "uuid";
    private AgentIdentifier agentIdentifier;
    private AgentHTTPClientController agentController;

    @Before
    public void setUp() throws Exception {
        agentIdentifier = new AgentIdentifier(getLocalhostName(), getFirstLocalNonLoopbackIpAddress(), agentUuid);
    }

    @After
    public void tearDown() {
        new GuidService().delete();
    }

    @Test
    public void shouldSetPluginManagerReference() throws Exception {
        agentController = createAgentController();
        assertThat(PluginManagerReference.reference().getPluginManager(), is(pluginManager));
    }

    @Test
    public void shouldRetrieveWorkFromServerAndDoIt() throws Exception {
        when(loopServer.getWork(any(AgentRuntimeInfo.class))).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(pluginJarLocationMonitor.getLastRun()).thenReturn(System.currentTimeMillis());
        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        agentController.work();
        verify(work).doWork(any(EnvironmentVariableContext.class), any(AgentWorkContext.class));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    public void shouldNotRetrieveWorkIfPluginMonitorHasNotRun() throws IOException {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(pluginJarLocationMonitor.getLastRun()).thenReturn(0L);
        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        agentController.work();
        verifyZeroInteractions(work);
    }

    @Test
    public void shouldRetrieveCookieIfNotPresent() throws Exception {
        agentController = createAgentController();
        agentController.init();

        when(loopServer.getCookie(any(AgentIdentifier.class), eq(agentController.getAgentRuntimeInfo().getLocation()))).thenReturn("cookie");
        when(sslInfrastructureService.isRegistered()).thenReturn(true);
        when(loopServer.getWork(agentController.getAgentRuntimeInfo())).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(pluginJarLocationMonitor.getLastRun()).thenReturn(System.currentTimeMillis());
        agentController.loop();
        verify(work).doWork(any(EnvironmentVariableContext.class), any(AgentWorkContext.class));
    }

    @Test
    public void shouldNotTellServerWorkIsCompletedWhenThereIsNoWork() throws Exception {
        when(loopServer.getWork(any(AgentRuntimeInfo.class))).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        agentController.retrieveWork();
        verify(work).doWork(any(EnvironmentVariableContext.class), any(AgentWorkContext.class));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    public void shouldRegisterSubprocessLoggerAtExit() throws Exception {
        SslInfrastructureService sslInfrastructureService = mock(SslInfrastructureService.class);
        AgentRegistry agentRegistry = mock(AgentRegistry.class);
        agentController = new AgentHTTPClientController(loopServer, artifactsManipulator, sslInfrastructureService,
                agentRegistry, agentUpgradeService, subprocessLogger, systemEnvironment, pluginManager,
                packageRepositoryExtension, scmExtension, taskExtension, artifactExtension, null, null, pluginJarLocationMonitor);
        agentController.init();
        verify(subprocessLogger).registerAsExitHook("Following processes were alive at shutdown: ");
    }

    @Test
    public void shouldNotPingIfNotRegisteredYet() throws Exception {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(sslInfrastructureService.isRegistered()).thenReturn(false);

        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    public void shouldPingIfAfterRegistered() throws Exception {
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
                loopServer,
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
