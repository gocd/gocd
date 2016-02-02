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
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.exception.UnregisteredAgentException;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;

@Component
public class AgentController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentController.class);

    private BuildRepositoryRemote server;
    private GoArtifactsManipulator manipulator;
    private SslInfrastructureService sslInfrastructureService;
    private AgentRegistry agentRegistry;

    private final String hostName;
    private final String ipAddress;
    private AgentInstruction instruction = new AgentInstruction(false);
    private JobRunner runner;
    private AgentRuntimeInfo agentRuntimeInfo;
    private SubprocessLogger subprocessLogger;
    private final SystemEnvironment systemEnvironment;
    private AgentUpgradeService agentUpgradeService;
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private SCMExtension scmExtension;
    private TaskExtension taskExtension;
    private AgentWebsocketService websocketService;
    private final AgentAutoRegistrationPropertiesImpl agentAutoRegistrationProperties;

    @Autowired
    public AgentController(BuildRepositoryRemote server, GoArtifactsManipulator manipulator, SslInfrastructureService sslInfrastructureService, AgentRegistry agentRegistry,
                           AgentUpgradeService agentUpgradeService, SubprocessLogger subprocessLogger, SystemEnvironment systemEnvironment,
                           PluginManager pluginManager, PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension, TaskExtension taskExtension,
                           AgentWebsocketService websocketService) {
        this.agentUpgradeService = agentUpgradeService;
        this.packageAsRepositoryExtension = packageAsRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.websocketService = websocketService;
        ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        hostName = SystemUtil.getLocalhostNameOrRandomNameIfNotFound();
        this.server = server;
        this.manipulator = manipulator;
        this.sslInfrastructureService = sslInfrastructureService;
        this.agentRegistry = agentRegistry;
        this.subprocessLogger = subprocessLogger;
        this.systemEnvironment = systemEnvironment;
        PluginManagerReference.reference().setPluginManager(pluginManager);
        this.agentAutoRegistrationProperties = new AgentAutoRegistrationPropertiesImpl(new File("config", "autoregister.properties"));
    }

    void init() throws IOException {
        websocketService.setController(this);
        createPipelinesFolderIfNotExist();
        sslInfrastructureService.createSslInfrastructure();
        AgentIdentifier identifier = agentIdentifier();
        if (agentAutoRegistrationProperties.isElastic()) {
            agentRuntimeInfo = ElasticAgentRuntimeInfo.fromAgent(identifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), systemEnvironment.getAgentLauncherVersion(), agentAutoRegistrationProperties.agentAutoRegisterElasticAgentId(), agentAutoRegistrationProperties.agentAutoRegisterElasticPluginId());
        } else {
            agentRuntimeInfo = AgentRuntimeInfo.fromAgent(identifier, AgentStatus.Idle.getRuntimeStatus(), currentWorkingDirectory(), systemEnvironment.getAgentLauncherVersion());
        }

        subprocessLogger.registerAsExitHook("Following processes were alive at shutdown: ");
    }

    private void createPipelinesFolderIfNotExist() {
        File pipelines = new File(currentWorkingDirectory(), "pipelines");
        if (!pipelines.exists()) {
            pipelines.mkdirs();
        }
    }

    public void ping() {
        if (systemEnvironment.isWebsocketEnabled()) {
            return;
        }
        try {
            if (sslInfrastructureService.isRegistered()) {
                AgentIdentifier agent = agentIdentifier();
                LOG.trace("{} is pinging server [{}]", agent, server);

                agentRuntimeInfo.refreshUsableSpace();

                instruction = server.ping(agentRuntimeInfo);
                LOG.trace("{} pinged server [{}]", agent, server);
            }
        } catch (Throwable e) {
            LOG.error("Error occurred when agent tried to ping server: ", e);
        }
    }

    private AgentIdentifier agentIdentifier() {
        return new AgentIdentifier(hostName, ipAddress, agentRegistry.uuid());
    }

    public void loop() {
        if (systemEnvironment.isWebsocketEnabled()) {
            websocketPing();
        } else {
            rpcLoop();
        }
    }

    private void rpcLoop() {
        try {
            LOG.debug("[Agent Loop] Trying to retrieve work.");
            agentUpgradeService.checkForUpgrade();
            sslInfrastructureService.registerIfNecessary(agentAutoRegistrationProperties);
            retrieveCookieIfNecessary();
            retrieveWork();
            LOG.debug("[Agent Loop] Successfully retrieved work.");

        } catch (Exception e) {
            if (isCausedBySecurity(e)) {
                handleIfSecurityException(e);
            } else if (e instanceof DataRetrievalFailureException) {
                LOG.debug("[Agent Loop] Error occurred during loop: ", e);
            } else {
                LOG.error("[Agent Loop] Error occurred during loop: ", e);
            }
        }
    }

    private void retrieveCookieIfNecessary() {
        if (!agentRuntimeInfo.hasCookie() && sslInfrastructureService.isRegistered()) {
            LOG.info("About to get cookie from the server.");
            String cookie = server.getCookie(agentIdentifier(), agentRuntimeInfo.getLocation());
            agentRuntimeInfo.setCookie(cookie);
            LOG.info("Got cookie: {}", cookie);
        }
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

    void retrieveWork() {
        AgentIdentifier agentIdentifier = agentIdentifier();
        LOG.debug("[Agent Loop] {} is checking for work from Go", agentIdentifier);
        Work work;
        try {
            agentRuntimeInfo.idle();
            work = server.getWork(agentRuntimeInfo);
            if (!(work instanceof NoWork)) {
                LOG.debug("[Agent Loop] Got work from server: [{}]", work.description());
            }
            runner = new JobRunner();
            runner.run(work, agentIdentifier, server, manipulator, agentRuntimeInfo, packageAsRepositoryExtension, scmExtension, taskExtension);
        } catch (UnregisteredAgentException e) {
            LOG.warn("[Agent Loop] Invalid agent certificate with fingerprint {}. Registering with server on next iteration.", e.getUuid());
            sslInfrastructureService.invalidateAgentCertificate();
        } finally {
            agentRuntimeInfo.idle();
        }
    }

    public void executeAgentInstruction() {
        if (systemEnvironment.isWebsocketEnabled()) {
            return;
        }
        if (runner != null) {
            runner.handleInstruction(instruction, agentRuntimeInfo);
        }
    }

    boolean isCausedBySecurity(Throwable e) {
        if (e == null) {
            return false;
        }
        if (e instanceof GeneralSecurityException) {
            return true;
        } else {
            return isCausedBySecurity(e.getCause());
        }
    }


    public void websocketPing() {
        try {
            agentUpgradeService.checkForUpgrade();
            sslInfrastructureService.registerIfNecessary(agentAutoRegistrationProperties);
            if (sslInfrastructureService.isRegistered()) {
                if (!websocketService.isRunning()) {
                    websocketService.start();
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

    public void process(Message message) throws InterruptedException {
        switch (message.getAction()) {
            case cancelJob:
                cancelJobIfThereIsOneRunning();
                break;
            case setCookie:
                String cookie = (String) message.getData();
                agentRuntimeInfo.setCookie(cookie);
                LOG.info("Got cookie: {}", cookie);
                break;
            case assignWork:
                cancelJobIfThereIsOneRunning();
                Work work = (Work) message.getData();
                LOG.debug("Got work from server: [{}]", work.description());
                agentRuntimeInfo.idle();
                runner = new JobRunner();
                try {
                    runner.run(work, agentIdentifier(),
                            new AgentWebsocketService.BuildRepositoryRemoteAdapter(runner, websocketService),
                            manipulator, agentRuntimeInfo,
                            packageAsRepositoryExtension, scmExtension,
                            taskExtension);
                } finally {
                    agentRuntimeInfo.idle();
                    updateServerAgentRuntimeInfo();
                }
                break;
            case reregister:
                LOG.warn("Reregister: invalidate current agent certificate fingerprint {} and stop websocket client.", agentRegistry.uuid());
                websocketService.stop();
                sslInfrastructureService.invalidateAgentCertificate();
                break;
            default:
                throw new RuntimeException("Unknown action: " + message.getAction());

        }
    }

    private void cancelJobIfThereIsOneRunning() throws InterruptedException {
        if (runner == null || !runner.isRunning()) {
            return;
        }
        LOG.info("Cancel running job");
        runner.handleInstruction(new AgentInstruction(true), agentRuntimeInfo);
        runner.waitUntilDone(30);
        if (runner.isRunning()) {
            LOG.error("Waited 30 seconds for canceling job finish, but the job is still running. Maybe canceling job does not work as expected, here is running job details: " + runner);
        }
    }

    private void updateServerAgentRuntimeInfo() {
        AgentIdentifier agent = agentIdentifier();
        LOG.trace("{} is pinging server [{}]", agent, server);
        agentRuntimeInfo.refreshUsableSpace();
        websocketService.send(new Message(Action.ping, agentRuntimeInfo));
        LOG.trace("{} pinged server [{}]", agent, server);
    }

    protected AgentAutoRegistrationProperties getAgentAutoRegistrationProperties() {
        return agentAutoRegistrationProperties;
    }

    protected AgentRuntimeInfo getAgentRuntimeInfo() {
        return agentRuntimeInfo;
    }
}
