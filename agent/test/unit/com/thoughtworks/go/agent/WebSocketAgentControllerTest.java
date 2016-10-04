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
import com.thoughtworks.go.agent.service.AgentWebSocketService;
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.websocket.*;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketAgentControllerTest {
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
    private AgentWebSocketService agentWebSocketService;
    @Mock
    private HttpService httpService;
    @Mock
    private HttpClient httpClient;
    private String agentUuid = "uuid";
    private AgentIdentifier agentIdentifier;
    private WebSocketAgentController agentController;

    @Before
    public void setUp() throws Exception {
        agentIdentifier = new AgentIdentifier(getLocalhostName(), getFirstLocalNonLoopbackIpAddress(), agentUuid);
    }

    @After
    public void tearDown() {
        GuidService.deleteGuid();
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
    public void shouldNotPingIfNotRegisteredYet() throws Exception {
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        when(sslInfrastructureService.isRegistered()).thenReturn(false);

        agentController = createAgentController();
        agentController.init();
        agentController.ping();
        verify(sslInfrastructureService).createSslInfrastructure();
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
        when(agentWebSocketService.isRunning()).thenReturn(false);
        when(sslInfrastructureService.isRegistered()).thenReturn(true);
        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        doNothing().when(agentWebSocketService).send(argumentCaptor.capture());


        agentController = createAgentController();
        agentController.init();

        agentController.loop();

        verify(agentUpgradeService).checkForUpgrade();
        verify(sslInfrastructureService).registerIfNecessary(agentController.getAgentAutoRegistrationProperties());
        verify(agentWebSocketService).send(any(Message.class));

        Message message = argumentCaptor.getValue();
        assertThat(message.getAckId(), notNullValue());
        assertThat(message.getAction(), is(Action.ping));
        assertThat(message.getData(), is(MessageEncoding.encodeData(agentController.getAgentRuntimeInfo())));
    }

    @Test
    public void shouldHandleSecurityErrorWhenOpenningWebsocketFailed() throws Exception {
        when(systemEnvironment.isWebsocketEnabled()).thenReturn(true);
        when(sslInfrastructureService.isRegistered()).thenReturn(true);

        agentController = createAgentController();
        when(agentWebSocketService.isRunning()).thenReturn(false);
        doThrow(new GeneralSecurityException()).when(agentWebSocketService).start(agentController);

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
        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        doNothing().when(agentWebSocketService).send(argumentCaptor.capture());

        when(agentRegistry.uuid()).thenReturn(agentUuid);
        agentController = createAgentController();
        agentController.init();
        agentController.process(new Message(Action.assignWork, MessageEncoding.encodeWork(new SleepWork("work1", 0))));
        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));

        verify(agentWebSocketService).send(any(Message.class));
        verify(artifactsManipulator).setProperty(null, new Property("work1_result", "done"));

        Message message = argumentCaptor.getValue();
        assertThat(message.getAckId(), notNullValue());
        assertThat(message.getAction(), is(Action.ping));
        assertThat(message.getData(), is(MessageEncoding.encodeData(agentController.getAgentRuntimeInfo())));
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
//        verify(agentWebSocketService).send(new Message(Action.reportCurrentStatus, MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", JobState.Building, null))));
//        verify(agentWebSocketService).sendAndWaitForAck(new Message(Action.reportCompleted, MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", null, JobResult.Passed))));


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
        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        doNothing().when(agentWebSocketService).send(argumentCaptor.capture());

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

        verify(agentWebSocketService).send(any(Message.class));

        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));

        Message message = argumentCaptor.getValue();
        assertThat(message.getAckId(), notNullValue());
        assertThat(message.getAction(), is(Action.reportCompleted));
        assertThat(message.getData(), is(MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", null, JobResult.Cancelled))));
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
        verify(agentWebSocketService).stop();
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

    private WebSocketAgentController createAgentController() {
        return new WebSocketAgentController(
                loopServer,
                artifactsManipulator,
                sslInfrastructureService,
                agentRegistry,
                agentUpgradeService,
                subprocessLogger,
                systemEnvironment,
                pluginManager,
                packageAsRepositoryExtension,
                scmExtension,
                taskExtension,
                agentWebSocketService,
                httpService);
    }
}
