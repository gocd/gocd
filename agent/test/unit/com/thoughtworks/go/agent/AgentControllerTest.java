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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.service.AgentUpgradeService;
import com.thoughtworks.go.agent.service.AgentWebsocketService;
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.BuildSettings;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Property;
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
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageCallback;
import com.thoughtworks.go.websocket.MessageEncoding;
import com.thoughtworks.go.websocket.Report;
import com.thoughtworks.go.work.SleepWork;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.eclipse.jetty.util.IO;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.thoughtworks.go.util.SystemUtil.getFirstLocalNonLoopbackIpAddress;
import static com.thoughtworks.go.util.SystemUtil.getLocalhostName;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AgentControllerTest {
    private static final int MAX_WAIT_IN_TEST = 10000;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
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
    @Mock
    private HttpService httpService;
    @Mock
    private HttpClient httpClient;
    private String agentUuid = "uuid";
    private AgentIdentifier agentIdentifier;
    private AgentController agentController;

    @Before
    public void setUp() throws Exception {
        agentIdentifier = new AgentIdentifier(getLocalhostName(), getFirstLocalNonLoopbackIpAddress(), agentUuid);
    }

    @After
    public void tearDown() {
        GuidService.deleteGuid();
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
        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        agentController.retrieveWork();
        verify(work).doWork(eq(agentIdentifier), eq(loopServer), eq(artifactsManipulator), any(EnvironmentVariableContext.class), any(AgentRuntimeInfo.class), eq(packageAsRepositoryExtension), eq(scmExtension), eq(taskExtension));
        verify(sslInfrastructureService).createSslInfrastructure();
    }

    @Test
    public void shouldRetrieveCookieIfNotPresent() throws Exception {
        agentController = createAgentController();
        agentController.init();

        when(loopServer.getCookie(any(AgentIdentifier.class), eq(agentController.getAgentRuntimeInfo().getLocation()))).thenReturn("cookie");
        when(sslInfrastructureService.isRegistered()).thenReturn(true);
        when(loopServer.getWork(agentController.getAgentRuntimeInfo())).thenReturn(work);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController.loop();
        verify(work).doWork(eq(agentIdentifier), eq(loopServer), eq(artifactsManipulator), any(EnvironmentVariableContext.class), eq(agentController.getAgentRuntimeInfo()), eq(packageAsRepositoryExtension), eq(scmExtension), eq(taskExtension));
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
        agentController = new AgentController(loopServer, artifactsManipulator, sslInfrastructureService, agentRegistry, agentUpgradeService, subprocessLogger, systemEnvironment, pluginManager, packageAsRepositoryExtension, scmExtension, taskExtension, agentWebsocketService, mock(HttpService.class));
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
        inOrder.verify(sslInfrastructureService).registerIfNecessary(agentController.getAgentAutoRegistrationProperties());
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
        verify(sslInfrastructureService).registerIfNecessary(agentController.getAgentAutoRegistrationProperties());
        verify(agentWebsocketService).start();
        verify(agentWebsocketService).sendAndWaitForAck(new Message(Action.ping, MessageEncoding.encodeData(agentController.getAgentRuntimeInfo())));
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
        verify(sslInfrastructureService).registerIfNecessary(agentController.getAgentAutoRegistrationProperties());
        verify(sslInfrastructureService).invalidateAgentCertificate();
    }

    @Test
    public void processSetCookieAction() throws IOException, InterruptedException {
        agentController = createAgentController();
        agentController.init();

        agentController.process(new Message(Action.setCookie, MessageEncoding.encodeData("cookie")));

        assertThat(agentController.getAgentRuntimeInfo().getCookie(), is("cookie"));
    }

    @Test
    public void processAssignWorkAction() throws IOException, InterruptedException {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        agentController.process(new Message(Action.assignWork, MessageEncoding.encodeWork(new SleepWork("work1", 0))));
        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
        verify(agentWebsocketService).sendAndWaitForAck(new Message(Action.ping, MessageEncoding.encodeData(agentController.getAgentRuntimeInfo())));
        verify(artifactsManipulator).setProperty(null, new Property("work1_result", "done"));
    }


    @Test
    public void processBuildCommand() throws Exception {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        BuildSettings build = new BuildSettings();
        build.setBuildId("b001");
        build.setConsoleUrl("http://foo.bar/console");
        build.setArtifactUploadBaseUrl("http://foo.bar/artifacts");
        build.setPropertyBaseUrl("http://foo.bar/properties");
        build.setBuildLocator("build1");
        build.setBuildLocatorForDisplay("build1ForDisplay");
        build.setBuildCommand(BuildCommand.compose(
                BuildCommand.echo("building"),
                BuildCommand.reportCurrentStatus(JobState.Building)));
        agentController.process(new Message(Action.build, MessageEncoding.encodeData(build)));
        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));

        AgentRuntimeInfo agentRuntimeInfo = cloneAgentRuntimeInfo(agentController.getAgentRuntimeInfo());
        agentRuntimeInfo.busy(new AgentBuildingInfo("build1ForDisplay", "build1"));
        verify(agentWebsocketService).sendAndWaitForAck(new Message(Action.reportCurrentStatus, MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", JobState.Building, null))));
        verify(agentWebsocketService).sendAndWaitForAck(new Message(Action.reportCompleted, MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", null, JobResult.Passed))));


        ArgumentCaptor<HttpPut> putMethodArg = ArgumentCaptor.forClass(HttpPut.class);
        verify(httpService).execute(putMethodArg.capture());
        assertThat(putMethodArg.getValue().getURI(), is(new URI("http://foo.bar/console")));
        assertThat(IO.toString(putMethodArg.getValue().getEntity().getContent()), containsString("building"));
    }

    private AgentRuntimeInfo cloneAgentRuntimeInfo(AgentRuntimeInfo agentRuntimeInfo) {
        return MessageEncoding.decodeData(MessageEncoding.encodeData(agentRuntimeInfo), AgentRuntimeInfo.class);
    }

    @Test
    public void processCancelBuildCommandBuild() throws IOException, InterruptedException {
        when(agentRegistry.uuid()).thenReturn(agentUuid);

        agentController = createAgentController();
        agentController.init();
        agentController.getAgentRuntimeInfo().setSupportsBuildCommandProtocol(true);
        final BuildSettings build = new BuildSettings();
        build.setBuildId("b001");
        build.setConsoleUrl("http://foo.bar/console");
        build.setArtifactUploadBaseUrl("http://foo.bar/artifacts");
        build.setPropertyBaseUrl("http://foo.bar/properties");
        build.setBuildLocator("build1");
        build.setBuildLocatorForDisplay("build1ForDisplay");
        build.setBuildCommand(BuildCommand.compose(
                BuildSessionBasedTestCase.execSleepScript(MAX_WAIT_IN_TEST / 1000),
                BuildCommand.reportCurrentStatus(JobState.Building)));


        Thread buildingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    agentController.process(new Message(Action.build, MessageEncoding.encodeData(build)));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        buildingThread.start();

        waitForAgentRuntimeState(agentController.getAgentRuntimeInfo(), AgentRuntimeStatus.Building);

        agentController.process(new Message(Action.cancelBuild));
        buildingThread.join(MAX_WAIT_IN_TEST);

        AgentRuntimeInfo agentRuntimeInfo = cloneAgentRuntimeInfo(agentController.getAgentRuntimeInfo());
        agentRuntimeInfo.busy(new AgentBuildingInfo("build1ForDisplay", "build1"));
        agentRuntimeInfo.cancel();

        verify(agentWebsocketService).sendAndWaitForAck(new Message(Action.reportCompleted, MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", null, JobResult.Cancelled))));

        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
    }


    @Test
    public void processCancelJobAction() throws IOException, InterruptedException {
        agentController = createAgentController();
        agentController.init();
        final SleepWork sleep1secWork = new SleepWork("work1", MAX_WAIT_IN_TEST);

        Thread buildingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    agentController.process(new Message(Action.assignWork, MessageEncoding.encodeWork(sleep1secWork)));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        buildingThread.start();

        waitForAgentRuntimeState(agentController.getAgentRuntimeInfo(), AgentRuntimeStatus.Building);

        agentController.process(new Message(Action.cancelBuild));
        buildingThread.join(MAX_WAIT_IN_TEST);

        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
        verify(artifactsManipulator).setProperty(null, new Property("work1_result", "done_canceled"));
    }

    private void waitForAgentRuntimeState(AgentRuntimeInfo runtimeInfo, AgentRuntimeStatus status) throws InterruptedException {
        int elapsed = 0;
        int waitStep = 100;
        while (elapsed <= MAX_WAIT_IN_TEST) {
            if (runtimeInfo.getRuntimeStatus() == status) {
                return;
            }
            Thread.sleep(waitStep);
            elapsed += waitStep;
        }
        throw new RuntimeException("wait for agent status '" + status.name() + "' timeout, current status is '" + runtimeInfo.getRuntimeStatus().name() + "'");
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
        final SleepWork work1 = new SleepWork("work1", MAX_WAIT_IN_TEST);
        Thread work1Thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    agentController.process(new Message(Action.assignWork, MessageEncoding.encodeWork(work1)));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        work1Thread.start();
        waitForAgentRuntimeState(agentController.getAgentRuntimeInfo(), AgentRuntimeStatus.Building);
        SleepWork work2 = new SleepWork("work2", 1);
        agentController.process(new Message(Action.assignWork, MessageEncoding.encodeWork(work2)));
        work1Thread.join(MAX_WAIT_IN_TEST);

        verify(artifactsManipulator).setProperty(null, new Property("work1_result", "done_canceled"));
        verify(artifactsManipulator).setProperty(null, new Property("work2_result", "done"));
    }

    @Test
    public void sendWithCallback() throws Exception {
        final AtomicBoolean callbackIsCalled = new AtomicBoolean(false);
        agentController = createAgentController();
        agentController.init();
        final Message message = new Message(Action.reportCurrentStatus);
        assertNull(message.getAckId());
        agentController.sendWithCallback(message, new MessageCallback() {
            @Override
            public void call() {
                callbackIsCalled.set(true);
            }
        });
        assertNotNull(message.getAckId());
        agentController.process(new Message(Action.ack, MessageEncoding.encodeData(message.getAckId())));
        assertTrue(callbackIsCalled.get());
    }

    private AgentController createAgentController() {
        return new AgentController(loopServer, artifactsManipulator, sslInfrastructureService, agentRegistry, agentUpgradeService, subprocessLogger, systemEnvironment, pluginManager, packageAsRepositoryExtension, scmExtension, taskExtension, agentWebsocketService, httpService);
    }
}
