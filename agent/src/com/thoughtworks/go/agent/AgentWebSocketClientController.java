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
import com.thoughtworks.go.agent.statusapi.AgentHealthHolder;
import com.thoughtworks.go.buildsession.ArtifactsRepository;
import com.thoughtworks.go.buildsession.BuildSession;
import com.thoughtworks.go.buildsession.BuildVariables;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.BuildSettings;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.ConsoleOutputTransmitter;
import com.thoughtworks.go.remote.work.RemoteConsoleAppender;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.command.TaggedStreamConsumer;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@WebSocket
public class AgentWebSocketClientController extends AgentController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentWebSocketClientController.class);
    private final SslInfrastructureService sslInfrastructureService;
    private final GoArtifactsManipulator manipulator;
    private HttpService httpService;
    private WebSocketClientHandler webSocketClientHandler;
    private WebSocketSessionHandler webSocketSessionHandler;
    private AtomicReference<BuildSession> buildSession = new AtomicReference<>();
    private JobRunner runner;
    private PackageRepositoryExtension packageRepositoryExtension;
    private SCMExtension scmExtension;
    private TaskExtension taskExtension;
    private BuildRepositoryRemote server;

    public AgentWebSocketClientController(BuildRepositoryRemote server, GoArtifactsManipulator manipulator,
                                          SslInfrastructureService sslInfrastructureService, AgentRegistry agentRegistry,
                                          AgentUpgradeService agentUpgradeService, SubprocessLogger subprocessLogger,
                                          SystemEnvironment systemEnvironment, PluginManager pluginManager,
                                          PackageRepositoryExtension packageRepositoryExtension, SCMExtension scmExtension,
                                          TaskExtension taskExtension, HttpService httpService,
                                          WebSocketClientHandler webSocketClientHandler, WebSocketSessionHandler webSocketSessionHandler,
                                          TimeProvider timeProvider, AgentHealthHolder agentHealthHolder) {
        super(sslInfrastructureService, systemEnvironment, agentRegistry, pluginManager, subprocessLogger, agentUpgradeService, timeProvider, agentHealthHolder);
        this.server = server;
        this.manipulator = manipulator;
        this.packageRepositoryExtension = packageRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.sslInfrastructureService = sslInfrastructureService;
        this.httpService = httpService;
        this.webSocketClientHandler = webSocketClientHandler;
        this.webSocketSessionHandler = webSocketSessionHandler;
    }

    @Override
    public void ping() {
        // Do nothing
    }

    @Override
    public void execute() {
        // Do nothing
    }

    @Override
    public void work() throws Exception {
        if (sslInfrastructureService.isRegistered()) {
            if (webSocketSessionHandler.isNotRunning()) {
                webSocketSessionHandler.clearCallBacks();
                webSocketSessionHandler.setSession(webSocketClientHandler.connect(this));
            }
            updateServerAgentRuntimeInfo();
        }
    }

    void process(Message message) throws InterruptedException {
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
                runner = new JobRunner();
                try {
                    runner.run(work, agentIdentifier(),
                            new BuildRepositoryRemoteAdapter(runner, webSocketSessionHandler),
                            manipulator, getAgentRuntimeInfo(),
                            packageRepositoryExtension, scmExtension,
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
                LOG.warn("Reregister: invalidate current agent certificate fingerprint {} and stop websocket webSocketClient.", getAgentRegistry().uuid());
                webSocketSessionHandler.stop();
                sslInfrastructureService.invalidateAgentCertificate();
                break;
            case acknowledge:
                webSocketSessionHandler.acknowledge(message);
                break;
            default:
                throw new RuntimeException("Unknown action: " + message.getAction());

        }
    }

    private void runBuild(BuildSettings buildSettings) {
        URLService urlService = new URLService();
        TaggedStreamConsumer buildConsole;

        if (getSystemEnvironment().isConsoleLogsThroughWebsocketEnabled()) {
            buildConsole = new ConsoleOutputWebsocketTransmitter(webSocketSessionHandler, buildSettings.getBuildId());
        } else {
            buildConsole = new ConsoleOutputTransmitter(
                new RemoteConsoleAppender(
                    urlService.prefixPartialUrl(buildSettings.getConsoleUrl()),
                    httpService)
            );
        }

        ArtifactsRepository artifactsRepository = new UrlBasedArtifactsRepository(
                httpService,
                urlService.prefixPartialUrl(buildSettings.getArtifactUploadBaseUrl()),
                urlService.prefixPartialUrl(buildSettings.getPropertyBaseUrl()),
                new ZipUtil());

        DefaultBuildStateReporter buildStateReporter = new DefaultBuildStateReporter(webSocketSessionHandler, getAgentRuntimeInfo());

        TimeProvider clock = new TimeProvider();
        BuildVariables buildVariables = new BuildVariables(getAgentRuntimeInfo(), clock);
        BuildSession build = new BuildSession(
                buildSettings.getBuildId(),
                getAgentRuntimeInfo().getIdentifier(),
                buildStateReporter,
                buildConsole,
                buildVariables,
                artifactsRepository,
                httpService, clock, new File("."));

        this.buildSession.set(build);

        build.setEnv("GO_SERVER_URL", getSystemEnvironment().getServiceUrl());
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
            LOG.error("Waited 30 seconds for canceling job finish, but the job is still running. Maybe canceling job does not work as expected, here is buildSession details: {}", buildSession.get());
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
            LOG.error("Waited 30 seconds for canceling job finish, but the job is still running. Maybe canceling job does not work as expected, here is running job details: {}", runner);
        }
    }

    private void updateServerAgentRuntimeInfo() {
        AgentIdentifier agent = agentIdentifier();
        LOG.trace("{} is pinging server [{}]", agent, server);
        getAgentRuntimeInfo().refreshUsableSpace();
        if (webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.ping, MessageEncoding.encodeData(getAgentRuntimeInfo())))) {
            pingSuccess();
        }

        LOG.trace("{} pinged server [{}]", agent, server);
    }

    private Executor executor = Executors.newFixedThreadPool(5);

    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOG.info("{} connected.", session);
    }

    @OnWebSocketMessage
    public void onMessage(InputStream raw) {
        final Message msg = MessageEncoding.decodeMessage(raw);
        LOG.debug("{} message: {}", webSocketSessionHandler.getSessionName(), msg);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.debug("Processing message[{}].", msg);
                    process(msg);
                } catch (InterruptedException e) {
                    LOG.error("Process message[{}] is interruptted.", msg, e);
                } catch (RuntimeException e) {
                    LOG.error("Unexpected error while processing message[{}]: {}", msg, e.getMessage(), e);
                } finally {
                    LOG.debug("Finished trying to process message[{}].", msg);
                }
            }
        });
    }

    @OnWebSocketClose
    public void onClose(int closeCode, String closeReason) {
        LOG.debug("{} closed. code: {}, reason: {}", webSocketSessionHandler.getSessionName(), closeCode, closeReason);
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        LOG.error("{} error", webSocketSessionHandler.getSessionName(), error);
    }

    @OnWebSocketFrame
    public void onFrame(Frame frame) {
        LOG.debug("{} receive frame: {}", webSocketSessionHandler.getSessionName(), frame.getPayloadLength());
    }

}
