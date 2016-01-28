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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.service.AgentUpgradeService;
import com.thoughtworks.go.agent.service.AgentWebsocketService;
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.work.SleepWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import static com.thoughtworks.go.util.SystemUtil.getFirstLocalNonLoopbackIpAddress;
import static com.thoughtworks.go.util.SystemUtil.getLocalhostName;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;


public class AgentControllerTest {
    public static final int MAX_WAIT_IN_TEST = 10000;
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
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;
    @Mock
    private AgentWebsocketService agentWebsocketService;

    private String agentUuid = "uuid";
    private AgentIdentifier agentIdentifier;
    private AgentController agentController;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        agentIdentifier = new AgentIdentifier(getLocalhostName(), getFirstLocalNonLoopbackIpAddress(), agentUuid);
    }

    @After
    public void tearDown() {
        GuidService.deleteGuid();
    }


    @Test
    public void shouldSetPluginManagerReference() throws Exception {
        agentController = createAgentController();
        assertThat(PluginManagerReference.reference().getPluginManager(),is(pluginManager));
    }

    @Test
    public void shouldRetrieveWorkFromServerAndDoIt() throws Exception {
        when(loopServer.getWork(any(AgentRuntimeInfo.class))).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        agentController.retrieveWork();
        verify(work).doWork(eq(agentIdentifier), eq(loopServer), eq(artifactsManipulator), any(EnvironmentVariableContext.class), any(AgentRuntimeInfo.class), eq(packageAsRepositoryExtension), eq(scmExtension), eq(taskExtension));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    public void shouldRetriveCookieIfNotPresent() throws Exception {
        AgentRuntimeInfo infoWithCookie = AgentRuntimeInfo.fromAgent(new AgentIdentifier(SystemUtil.getLocalhostName(), SystemUtil.getFirstLocalNonLoopbackIpAddress(), agentUuid), "cookie",
                null);
        when(loopServer.getCookie(any(AgentIdentifier.class), eq(infoWithCookie.getLocation()))).thenReturn("cookie");
        when(sslInfrastructureService.isRegistered()).thenReturn(true);
        when(loopServer.getWork(infoWithCookie)).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        agentController.loop();
        verify(work).doWork(eq(agentIdentifier), eq(loopServer), eq(artifactsManipulator), any(EnvironmentVariableContext.class), eq(infoWithCookie), eq(packageAsRepositoryExtension), eq(scmExtension), eq(taskExtension));
    }

    @Test
    public void shouldNotTellServerWorkIsCompletedWhenThereIsNoWork() throws Exception {
        when(loopServer.getWork(any(AgentRuntimeInfo.class))).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        agentController.retrieveWork();
        verify(work).doWork(eq(agentIdentifier), eq(loopServer), eq(artifactsManipulator), any(EnvironmentVariableContext.class), any(AgentRuntimeInfo.class), eq(packageAsRepositoryExtension), eq(scmExtension), eq(taskExtension));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    public void shouldReturnTrueIfCausedBySecurity() throws Exception {
        Exception exception = new Exception(new RuntimeException(new GeneralSecurityException()));
        when(agentRegistry.uuid()).thenReturn(agentUuid);

        agentController = createAgentController();
        agentController.init();
        assertTrue(agentController.isCausedBySecurity(exception));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    public void shouldReturnFalseIfNotCausedBySecurity() throws Exception {
        Exception exception = new Exception(new IOException());
        when(agentRegistry.uuid()).thenReturn(agentUuid);


        agentController = createAgentController();
        agentController.init();
        assertFalse(agentController.isCausedBySecurity(exception));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    public void shouldRegisterSubprocessLoggerAtExit() throws Exception {
        SslInfrastructureService sslInfrastructureService = mock(SslInfrastructureService.class);
        AgentRegistry agentRegistry = mock(AgentRegistry.class);
        agentController = new AgentController(loopServer, artifactsManipulator, sslInfrastructureService, agentRegistry, agentUpgradeService, subprocessLogger, systemEnvironment,pluginManager, packageAsRepositoryExtension, scmExtension, taskExtension, agentWebsocketService);
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

    @Test
    public void shouldUpgradeAgentBeforeAgentRegistration() throws Exception {
        agentController = createAgentController();
        InOrder inOrder = inOrder(agentUpgradeService, sslInfrastructureService);
        agentController.loop();
        inOrder.verify(agentUpgradeService).checkForUpgrade();
        inOrder.verify(sslInfrastructureService).registerIfNecessary();
    }

    @Test
    public void websocketPing() throws Exception {
        when(systemEnvironment.isWebsocketEnabled()).thenReturn(true);
        when(agentWebsocketService.isRunning()).thenReturn(false);
        when(sslInfrastructureService.isRegistered()).thenReturn(true);

        agentController = createAgentController();
        agentController.init();

        agentController.loop();

        verify(agentUpgradeService).checkForUpgrade();
        verify(sslInfrastructureService).registerIfNecessary();
        verify(agentWebsocketService).start();
        verify(agentWebsocketService).send(new Message(Action.ping, agentController.getAgentRuntimeInfo()));
    }

    @Test
    public void shouldHandleSecurityErrorWhenOpenningWebsocketFailed() throws Exception {
        when(systemEnvironment.isWebsocketEnabled()).thenReturn(true);
        when(sslInfrastructureService.isRegistered()).thenReturn(true);

        when(agentWebsocketService.isRunning()).thenReturn(false);
        doThrow(new GeneralSecurityException()).when(agentWebsocketService).start();

        agentController = createAgentController();
        agentController.init();

        agentController.loop();

        verify(agentUpgradeService).checkForUpgrade();
        verify(sslInfrastructureService).registerIfNecessary();
        verify(sslInfrastructureService).invalidateAgentCertificate();
    }

    @Test
    public void processSetCookieAction() throws IOException, InterruptedException {
        agentController = createAgentController();
        agentController.init();

        agentController.process(new Message(Action.setCookie, "cookie"));

        assertThat(agentController.getAgentRuntimeInfo().getCookie(), is("cookie"));
    }

    @Test
    public void processAssignWorkAction() throws IOException, InterruptedException {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                AgentRuntimeInfo info = (AgentRuntimeInfo) invocationOnMock.getArguments()[4];
                info.busy(new AgentBuildingInfo("locator for display", "build locator"));
                return null;
            }
        }).when(work).doWork(any(AgentIdentifier.class), any(BuildRepositoryRemote.class), any(GoArtifactsManipulator.class), any(EnvironmentVariableContext.class), any(AgentRuntimeInfo.class), any(PackageAsRepositoryExtension.class), any(SCMExtension.class), any(TaskExtension.class));

        agentController = createAgentController();
        agentController.init();
        agentController.process(new Message(Action.assignWork, work));

        verify(work).doWork(eq(agentIdentifier), any(BuildRepositoryRemote.class), eq(artifactsManipulator), any(EnvironmentVariableContext.class), eq(agentController.getAgentRuntimeInfo()), eq(packageAsRepositoryExtension), eq(scmExtension), eq(taskExtension));
        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
        verify(agentWebsocketService).send(new Message(Action.ping, agentController.getAgentRuntimeInfo()));
    }

    @Test
    public void processCancelJobAction() throws IOException, InterruptedException {
        agentController = createAgentController();
        agentController.init();
        final SleepWork sleep1secWork = new SleepWork();

        Thread buildingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    agentController.process(new Message(Action.assignWork, sleep1secWork));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        buildingThread.start();
        sleep1secWork.started.await(MAX_WAIT_IN_TEST, TimeUnit.MILLISECONDS);

        agentController.process(new Message(Action.cancelJob));
        buildingThread.join(MAX_WAIT_IN_TEST);

        assertThat(sleep1secWork.done.get(), is(true));
        assertThat(sleep1secWork.canceled.get(), is(true));
    }

    @Test
    public void processReregisterAction() throws IOException, InterruptedException {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        agentController.process(new Message(Action.reregister));

        verify(sslInfrastructureService).invalidateAgentCertificate();
        verify(agentWebsocketService).stop();
    }

    @Test
    public void shouldCancelPreviousRunningJobIfANewAssignWorkMessageIsReceived() throws IOException, InterruptedException {
        agentController = createAgentController();
        agentController.init();
        final SleepWork work1 = new SleepWork();
        Thread work1Thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    agentController.process(new Message(Action.assignWork, work1));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        work1Thread.start();
        work1.started.await(MAX_WAIT_IN_TEST, TimeUnit.MILLISECONDS);
        SleepWork work2 = new SleepWork(0);
        agentController.process(new Message(Action.assignWork, work2));
        work1Thread.join(MAX_WAIT_IN_TEST);

        assertThat(work1.done.get(), is(true));
        assertThat(work1.canceled.get(), is(true));
        assertThat(work2.done.get(), is(true));
        assertThat(work2.canceled.get(), is(false));
    }

    private AgentController createAgentController() {
        return new AgentController(loopServer, artifactsManipulator, sslInfrastructureService, agentRegistry, agentUpgradeService, subprocessLogger, systemEnvironment,pluginManager, packageAsRepositoryExtension, scmExtension, taskExtension, agentWebsocketService);
    }
}
