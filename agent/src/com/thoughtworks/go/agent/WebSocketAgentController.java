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
import com.thoughtworks.go.buildsession.ArtifactsRepository;
import com.thoughtworks.go.buildsession.BuildSession;
import com.thoughtworks.go.buildsession.BuildVariables;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.BuildSettings;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.ConsoleOutputTransmitter;
import com.thoughtworks.go.remote.work.RemoteConsoleAppender;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageCallback;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@WebSocket
public class WebSocketAgentController extends AgentController {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketAgentController.class);
    private final AgentAutoRegistrationPropertiesImpl agentAutoRegistrationProperties;
    private final AgentUpgradeService agentUpgradeService;
    private final SslInfrastructureService sslInfrastructureService;
    private final AgentWebSocketService agentWebSocketService;
    private final GoArtifactsManipulator manipulator;
    private HttpService httpService;
    private final Map<String, MessageCallback> callbacks = new ConcurrentHashMap<>();
    private AtomicReference<BuildSession> buildSession = new AtomicReference<>();
    private JobRunner runner;
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private SCMExtension scmExtension;
    private TaskExtension taskExtension;
    private BuildRepositoryRemote server;

    public WebSocketAgentController(AgentUpgradeService agentUpgradeService,
                                    SslInfrastructureService sslInfrastructureService,
                                    SystemEnvironment systemEnvironment,
                                    AgentWebSocketService agentWebSocketService,
                                    AgentRegistry agentRegistry,
                                    SubprocessLogger subprocessLogger,
                                    PackageAsRepositoryExtension packageAsRepositoryExtension,
                                    HttpService httpService,
                                    GoArtifactsManipulator manipulator,
                                    SCMExtension scmExtension,
                                    TaskExtension taskExtension,
                                    BuildRepositoryRemote server) {
        super(sslInfrastructureService, systemEnvironment, agentRegistry, subprocessLogger);
        this.packageAsRepositoryExtension = packageAsRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.server = server;
        this.agentAutoRegistrationProperties = new AgentAutoRegistrationPropertiesImpl(new File("config", "autoregister.properties"));
        this.agentUpgradeService = agentUpgradeService;
        this.sslInfrastructureService = sslInfrastructureService;
        this.agentWebSocketService = agentWebSocketService;
        this.httpService = httpService;
        this.manipulator = manipulator;
    }

    @Override
    public void ping() {
        try {
            agentUpgradeService.checkForUpgrade();
            sslInfrastructureService.registerIfNecessary(agentAutoRegistrationProperties);
            if (sslInfrastructureService.isRegistered()) {
                if (!agentWebSocketService.isRunning()) {
                    callbacks.clear();
                    agentWebSocketService.start(this);
                }
                updateServerAgentRuntimeInfo();
            }
        } catch (Exception e) {
            if (isCausedBySecurity(e)) {
                handleIfSecurityException(e);
            } else {
                LOG.error("Error occurred when agent tried to ping server: ", e);
            }
        }
    }

    @Override
    void execute() {
        // Do nothing
    }

    private void handleIfSecurityException(Exception e) {
        if (!isCausedBySecurity(e)) {
            return;
        }
        sslInfrastructureService.invalidateAgentCertificate();
        LOG.error("There has been a problem with one of Go's SSL certificates." +
                        " This can be caused by a man-in-the-middle attack, or by pointing the agent to a new server, or by" +
                        " deleting and re-installing Go Server. Go will ask for a new certificate. If this" +
                        " fails to solve the problem, try deleting config/trust.jks in Go Agent's home directory.",
                e);
    }

    @Override
    public void loop() {

    }

    public void process(Message message) throws InterruptedException {
        switch (message.getAction()) {
            case cancelBuild:
                cancelJobIfThereIsOneRunning();
                cancelBuild();
                break;
            case setCookie:
                String cookie = MessageEncoding.decodeData(message.getData(), String.class);
                getAgentRuntimeInfo().setCookie(cookie);
                LOG.info("Got cookie: {}", cookie);
                break;
            case assignWork:
                cancelJobIfThereIsOneRunning();
                Work work = MessageEncoding.decodeWork(message.getData());
                LOG.debug("Got work from server: [{}]", work.description());
                getAgentRuntimeInfo().idle();
                runner = new JobRunner();
                try {
                    runner.run(work, getIdentifier(),
                            new BuildRepositoryRemoteAdapter(runner, this),
                            manipulator, getAgentRuntimeInfo(),
                            packageAsRepositoryExtension, scmExtension,
                            taskExtension);
                } finally {
                    getAgentRuntimeInfo().idle();
                    updateServerAgentRuntimeInfo();
                }
                break;
            case build:
                cancelBuild();
                BuildSettings buildSettings = MessageEncoding.decodeData(message.getData(), BuildSettings.class);
                runBuild(buildSettings);
                break;
            case reregister:
                LOG.warn("Reregister: invalidate current agent certificate fingerprint {} and stop websocket client.", getAgentRegistry().uuid());
                agentWebSocketService.stop();
                sslInfrastructureService.invalidateAgentCertificate();
                break;
            case ack:
                callbacks.remove(MessageEncoding.decodeData(message.getData(), String.class)).call();
                break;
            default:
                throw new RuntimeException("Unknown action: " + message.getAction());

        }
    }

    private void runBuild(BuildSettings buildSettings) {
        URLService urlService = new URLService();
        ConsoleOutputTransmitter buildConsole = new ConsoleOutputTransmitter(
                new RemoteConsoleAppender(
                        urlService.prefixPartialUrl(buildSettings.getConsoleUrl()),
                        httpService));
        ArtifactsRepository artifactsRepository = new UrlBasedArtifactsRepository(
                httpService,
                urlService.prefixPartialUrl(buildSettings.getArtifactUploadBaseUrl()),
                urlService.prefixPartialUrl(buildSettings.getPropertyBaseUrl()),
                new ZipUtil());

        DefaultBuildStateReporter buildStateReporter = new DefaultBuildStateReporter(this, getAgentRuntimeInfo());

        TimeProvider clock = new TimeProvider();
        BuildVariables buildVariables = new BuildVariables(getAgentRuntimeInfo(), clock);
        BuildSession build = new BuildSession(
                buildSettings.getBuildId(),
                buildStateReporter,
                buildConsole,
                buildVariables,
                artifactsRepository,
                httpService, clock, new File("."));

        this.buildSession.set(build);

        build.setEnv("GO_SERVER_URL", new SystemEnvironment().getPropertyImpl("serviceUrl"));
        getAgentRuntimeInfo().idle();
        try {
            getAgentRuntimeInfo().busy(new AgentBuildingInfo(buildSettings.getBuildLocatorForDisplay(), buildSettings.getBuildLocator()));
            build.build(buildSettings.getBuildCommand());
        } finally {
            try {
                buildConsole.stop();
            } finally {
                getAgentRuntimeInfo().idle();
            }
        }
        this.buildSession.set(null);
    }

    private void cancelBuild() throws InterruptedException {
        BuildSession build = this.buildSession.get();
        if (build == null) {
            return;
        }
        getAgentRuntimeInfo().cancel();
        if (!build.cancel(30, TimeUnit.SECONDS)) {
            LOG.error("Waited 30 seconds for canceling job finish, but the job is still running. Maybe canceling job does not work as expected, here is buildSession details: " + buildSession.get());
        }
    }

    private void cancelJobIfThereIsOneRunning() throws InterruptedException {
        if (runner == null || !runner.isRunning()) {
            return;
        }
        LOG.info("Cancel running job");
        runner.handleInstruction(new AgentInstruction(true), getAgentRuntimeInfo());
        runner.waitUntilDone(30);
        if (runner.isRunning()) {
            LOG.error("Waited 30 seconds for canceling job finish, but the job is still running. Maybe canceling job does not work as expected, here is running job details: " + runner);
        }
    }

    private void updateServerAgentRuntimeInfo() {
        AgentIdentifier agent = getIdentifier();
        LOG.trace("{} is pinging server [{}]", agent, server);
        getAgentRuntimeInfo().refreshUsableSpace();
        sendAndWaitForAck(new Message(Action.ping, MessageEncoding.encodeData(getAgentRuntimeInfo())));
        LOG.trace("{} pinged server [{}]", agent, server);
    }

    public void sendAndWaitForAck(Message message) {
        final CountDownLatch wait = new CountDownLatch(1);
        sendWithCallback(message, new MessageCallback() {
            @Override
            public void call() {
                wait.countDown();
            }
        });
        try {
            wait.await(getSystemEnvironment().getWebsocketAckMessageTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            bomb(e);
        }
    }

    public void sendWithCallback(Message message, MessageCallback callback) {
        message.generateAckId();
        callbacks.put(message.getAckId(), callback);
        agentWebSocketService.send(message);
    }

    private String sessionName;
    private Executor executor = Executors.newFixedThreadPool(5);

    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOG.info(session + " connected.");
        this.sessionName = "[" + session.getRemoteAddress() + "]";
    }

    @OnWebSocketMessage
    public void onMessage(InputStream raw) {
        final Message msg = MessageEncoding.decodeMessage(raw);
        if (LOG.isDebugEnabled()) {
            LOG.debug(getSessionName() + " message: " + msg);
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    WebSocketAgentController.this.process(msg);
                } catch (InterruptedException e) {
                    LOG.error("Process message[" + msg + "] is interruptted.", e);
                } catch (RuntimeException e) {
                    LOG.error("Unexpected error while processing message[" + msg + "]: " + e.getMessage(), e);
                }
            }
        });
    }

    @OnWebSocketClose
    public void onClose(int closeCode, String closeReason) {
        LOG.debug("{} closed. code: {}, reason: {}", getSessionName(), closeCode, closeReason);
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        LOG.error(getSessionName() + " error", error);
    }

    @OnWebSocketFrame
    public void onFrame(Frame frame) {
        LOG.debug("{} receive frame: {}", getSessionName(), frame.getPayloadLength());
    }

    private String getSessionName() {
        return sessionName;
    }

}
