/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.exception.UnregisteredAgentException;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.monitor.PluginJarLocationMonitor;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.work.AgentWorkContext;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.remote.AgentInstruction.NONE;

@Component
public class AgentHTTPClientController extends AgentController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentHTTPClientController.class);

    private final RemotingClient client;
    private final GoArtifactsManipulator manipulator;
    private final SslInfrastructureService sslInfrastructureService;
    private final ArtifactExtension artifactExtension;
    private final PluginRequestProcessorRegistry pluginRequestProcessorRegistry;

    private final PackageRepositoryExtension packageRepositoryExtension;
    private final SCMExtension scmExtension;
    private final TaskExtension taskExtension;
    private volatile JobRunner runner;
    private volatile AgentInstruction agentInstruction = NONE;

    @Autowired
    public AgentHTTPClientController(RemotingClient client,
                                     GoArtifactsManipulator manipulator,
                                     SslInfrastructureService sslInfrastructureService,
                                     AgentRegistry agentRegistry,
                                     AgentUpgradeService agentUpgradeService,
                                     SubprocessLogger subprocessLogger,
                                     SystemEnvironment systemEnvironment,
                                     PluginManager pluginManager,
                                     PackageRepositoryExtension packageRepositoryExtension,
                                     SCMExtension scmExtension,
                                     TaskExtension taskExtension,
                                     ArtifactExtension artifactExtension,
                                     PluginRequestProcessorRegistry pluginRequestProcessorRegistry,
                                     AgentHealthHolder agentHealthHolder,
                                     PluginJarLocationMonitor pluginJarLocationMonitor) {
        super(sslInfrastructureService, systemEnvironment, agentRegistry, pluginManager, subprocessLogger, agentUpgradeService, agentHealthHolder, pluginJarLocationMonitor);
        this.client = client;
        this.packageRepositoryExtension = packageRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.manipulator = manipulator;
        this.sslInfrastructureService = sslInfrastructureService;
        this.artifactExtension = artifactExtension;
        this.pluginRequestProcessorRegistry = pluginRequestProcessorRegistry;
    }

    @Override
    public void ping() {
        try {
            if (sslInfrastructureService.isRegistered()) {
                AgentIdentifier agent = agentIdentifier();
                LOG.trace("{} is pinging server [{}]", agent, client);

                getAgentRuntimeInfo().refreshUsableSpace();

                agentInstruction = client.ping(getAgentRuntimeInfo());
                pingSuccess();
                LOG.trace("{} pinged server [{}]", agent, client);
            }
        } catch (Throwable e) {
            LOG.error("Error occurred when agent tried to ping server: ", e);
        }
    }

    @Override
    public void execute() {
        if (runner != null) {
            runner.handleInstruction(agentInstruction, getAgentRuntimeInfo());
        }
    }

    @Override
    public WorkAttempt tryDoWork() {
        retrieveCookieIfNecessary();
        return doWork();
    }

    private void retrieveCookieIfNecessary() {
        if (!getAgentRuntimeInfo().hasCookie() && sslInfrastructureService.isRegistered()) {
            LOG.info("[Agent Loop] Registered, but need new agent cookie - about to get cookie from the server.");
            String cookie = client.getCookie(getAgentRuntimeInfo());
            getAgentRuntimeInfo().setCookie(cookie);
            LOG.info("[Agent Loop] Got new cookie - ready to retrieve work.");
        }
    }

    private WorkAttempt doWork() {
        AgentIdentifier agentIdentifier = agentIdentifier();
        LOG.debug("[Agent Loop] {} is checking for work from Go", agentIdentifier);
        try {
            getAgentRuntimeInfo().idle();
            Work work = client.getWork(getAgentRuntimeInfo());
            LOG.debug("[Agent Loop] Got work from server: [{}]", work.description());
            runner = new JobRunner();
            final AgentWorkContext agentWorkContext = new AgentWorkContext(agentIdentifier, client, manipulator, getAgentRuntimeInfo(), packageRepositoryExtension, scmExtension, taskExtension, artifactExtension, pluginRequestProcessorRegistry);
            runner.run(work, agentWorkContext);
            LOG.debug("[Agent Loop] Successfully executed work.");
            return WorkAttempt.fromWork(work);
        } catch (UnregisteredAgentException e) {
            LOG.warn("[Agent Loop] Agent is not registered. [{}] Registering with server on next iteration.", e.getMessage());
            sslInfrastructureService.createSslInfrastructure();
            return WorkAttempt.FAILED;
        } finally {
            getAgentRuntimeInfo().idle();
        }
    }
}
