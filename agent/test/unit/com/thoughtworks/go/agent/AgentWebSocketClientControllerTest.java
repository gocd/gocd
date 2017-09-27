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
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.matchers.RegexMatcher;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.websocket.*;
import com.thoughtworks.go.work.SleepWork;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentWebSocketClientControllerTest {
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
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;
    @Mock
    private HttpService httpService;
    @Mock
    private HttpClient httpClient;
    private AgentWebSocketClientController agentController;
    @Mock
    private RemoteEndpoint remoteEndpoint;
    @Mock
    private WebSocketClientHandler webSocketClientHandler;
    @Mock
    private WebSocketSessionHandler webSocketSessionHandler;
    private String agentUuid = "uuid";
    @Mock
    private TimeProvider timeProvider;

    @After
    public void tearDown() {
        GuidService.deleteGuid();
    }

    @Test
    public void shouldSendAgentRuntimeInfoWhenWorkIsCalled() throws Exception {
        when(sslInfrastructureService.isRegistered()).thenReturn(true);
        when(webSocketSessionHandler.isNotRunning()).thenReturn(false);
        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

        agentController = createAgentController();
        agentController.init();

        agentController.work();

        verify(webSocketSessionHandler).sendAndWaitForAcknowledgement(argumentCaptor.capture());

        Message message = argumentCaptor.getValue();
        assertThat(message.getAcknowledgementId(), notNullValue());
        assertThat(message.getAction(), is(Action.ping));
        assertThat(message.getData(), is(MessageEncoding.encodeData(agentController.getAgentRuntimeInfo())));
    }

    @Test
    public void shouldHandleSecurityErrorWhenOpeningWebSocketFails() throws Exception {
        when(sslInfrastructureService.isRegistered()).thenReturn(true);

        agentController = createAgentController();
        when(webSocketSessionHandler.isNotRunning()).thenReturn(true);
        doThrow(new GeneralSecurityException()).when(webSocketClientHandler).connect(agentController);

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
        agentController = createAgentController();
        agentController.init();
        agentController.process(new Message(Action.assignWork, MessageEncoding.encodeWork(new SleepWork("work1", 0))));
        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));

        verify(webSocketSessionHandler, times(1)).sendAndWaitForAcknowledgement(argumentCaptor.capture());
        verify(artifactsManipulator).setProperty(null, new Property("work1_result", "done"));

        Message message = argumentCaptor.getAllValues().get(0);
        assertThat(message.getAcknowledgementId(), notNullValue());
        assertThat(message.getAction(), is(Action.ping));
        assertThat(message.getData(), is(MessageEncoding.encodeData(agentController.getAgentRuntimeInfo())));
    }

    @Test
    public void processAssignWorkActionWithConsoleLogsThroughWebSockets() throws IOException, InterruptedException {
        SystemEnvironment env = new SystemEnvironment();
        env.set(SystemEnvironment.WEBSOCKET_ENABLED, true);
        env.set(SystemEnvironment.CONSOLE_LOGS_THROUGH_WEBSOCKET_ENABLED, true);
        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        agentController = createAgentController();
        agentController.init();
        agentController.process(new Message(Action.assignWork, MessageEncoding.encodeWork(new SleepWork("work1", 0))));
        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));

        verify(webSocketSessionHandler, times(2)).sendAndWaitForAcknowledgement(argumentCaptor.capture());
        verify(artifactsManipulator).setProperty(null, new Property("work1_result", "done"));

        Message message = argumentCaptor.getAllValues().get(1);
        assertThat(message.getAcknowledgementId(), notNullValue());
        assertThat(message.getAction(), is(Action.ping));
        assertThat(message.getData(), is(MessageEncoding.encodeData(agentController.getAgentRuntimeInfo())));

        Message message2 = argumentCaptor.getAllValues().get(0);
        assertThat(message2.getAcknowledgementId(), notNullValue());
        assertThat(message2.getAction(), is(Action.consoleOut));
        ConsoleTransmission ct = MessageEncoding.decodeData(message2.getData(), ConsoleTransmission.class);
        assertThat(ct.getLine(), RegexMatcher.matches("Sleeping for 0 milliseconds"));
        env.set(SystemEnvironment.WEBSOCKET_ENABLED, false);
        env.set(SystemEnvironment.CONSOLE_LOGS_THROUGH_WEBSOCKET_ENABLED, false);
    }

    @Test
    public void processBuildCommandWithConsoleLogsThroughWebSockets() throws Exception {
        ArgumentCaptor<Message> currentStatusMessageCaptor = ArgumentCaptor.forClass(Message.class);
        when(systemEnvironment.isConsoleLogsThroughWebsocketEnabled()).thenReturn(true);
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

        verify(webSocketSessionHandler, times(3)).sendAndWaitForAcknowledgement(currentStatusMessageCaptor.capture());

        Message consoleOutMsg = currentStatusMessageCaptor.getAllValues().get(0);
        assertThat(consoleOutMsg.getAcknowledgementId(), notNullValue());
        assertThat(consoleOutMsg.getAction(), is(Action.consoleOut));
        ConsoleTransmission ct = MessageEncoding.decodeData(consoleOutMsg.getData(), ConsoleTransmission.class);
        assertThat(ct.getLine(), RegexMatcher.matches("building"));
        assertEquals(ct.getBuildId(), "b001");

        Message message = currentStatusMessageCaptor.getAllValues().get(1);
        assertThat(message.getAcknowledgementId(), notNullValue());
        assertThat(message.getAction(), is(Action.reportCurrentStatus));
        assertThat(message.getData(), is(MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", JobState.Building, null))));

        Message jobCompletedMessage = currentStatusMessageCaptor.getAllValues().get(2);
        assertThat(jobCompletedMessage.getAcknowledgementId(), notNullValue());
        assertThat(jobCompletedMessage.getAction(), is(Action.reportCompleted));
        assertThat(jobCompletedMessage.getData(), is(MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", null, JobResult.Passed))));
    }

    @Test
    public void processBuildCommand() throws Exception {
        ArgumentCaptor<Message> currentStatusMessageCaptor = ArgumentCaptor.forClass(Message.class);
        when(agentRegistry.uuid()).thenReturn(agentUuid);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpService.execute(any())).thenReturn(httpResponse);

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

        verify(webSocketSessionHandler, times(2)).sendAndWaitForAcknowledgement(currentStatusMessageCaptor.capture());

        Message message = currentStatusMessageCaptor.getAllValues().get(0);
        assertThat(message.getAcknowledgementId(), notNullValue());
        assertThat(message.getAction(), is(Action.reportCurrentStatus));
        assertThat(message.getData(), is(MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", JobState.Building, null))));

        Message jobCompletedMessage = currentStatusMessageCaptor.getAllValues().get(1);
        assertThat(jobCompletedMessage.getAcknowledgementId(), notNullValue());
        assertThat(jobCompletedMessage.getAction(), is(Action.reportCompleted));
        assertThat(jobCompletedMessage.getData(), is(MessageEncoding.encodeData(new Report(agentRuntimeInfo, "b001", null, JobResult.Passed))));
    }

    private AgentRuntimeInfo cloneAgentRuntimeInfo(AgentRuntimeInfo agentRuntimeInfo) {
        return MessageEncoding.decodeData(MessageEncoding.encodeData(agentRuntimeInfo), AgentRuntimeInfo.class);
    }

    @Test
    public void processCancelBuildCommandBuild() throws IOException, InterruptedException {
        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

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

        verify(webSocketSessionHandler).sendAndWaitForAcknowledgement(argumentCaptor.capture());

        assertThat(agentController.getAgentRuntimeInfo().getRuntimeStatus(), is(AgentRuntimeStatus.Idle));

        Message message = argumentCaptor.getValue();
        assertThat(message.getAcknowledgementId(), notNullValue());
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
        verify(webSocketSessionHandler).stop();
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

    private AgentWebSocketClientController createAgentController() {
        AgentWebSocketClientController controller = new AgentWebSocketClientController(
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
                httpService,
                webSocketClientHandler, webSocketSessionHandler, timeProvider, null);
        return controller;
    }
}
