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
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.AgentWorkContext;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.remote.AgentInstruction.NONE;
import static java.util.Objects.requireNonNullElse;

@Component
public class AgentHTTPClientController extends AgentController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentHTTPClientController.class);

    private final RemotingClient client;
    private final BuildRepositoryRemote legacyRmi;
    private final GoArtifactsManipulator manipulator;
    private final SslInfrastructureService sslInfrastructureService;
    private final ArtifactExtension artifactExtension;
    private final PluginRequestProcessorRegistry pluginRequestProcessorRegistry;
    private final PluginJarLocationMonitor pluginJarLocationMonitor;

    private JobRunner runner;
    private final PackageRepositoryExtension packageRepositoryExtension;
    private final SCMExtension scmExtension;
    private final TaskExtension taskExtension;
    private AgentInstruction agentInstruction = NONE;

    @Autowired
    public AgentHTTPClientController(RemotingClient client,
                                     @Qualifier("buildLoopServer")
                                     BuildRepositoryRemote legacyRmi,
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
        super(sslInfrastructureService, systemEnvironment, agentRegistry, pluginManager, subprocessLogger, agentUpgradeService, agentHealthHolder);
        this.client = client;
        this.packageRepositoryExtension = packageRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.legacyRmi = legacyRmi;
        this.manipulator = manipulator;
        this.sslInfrastructureService = sslInfrastructureService;
        this.artifactExtension = artifactExtension;
        this.pluginRequestProcessorRegistry = pluginRequestProcessorRegistry;
        this.pluginJarLocationMonitor = pluginJarLocationMonitor;

        LOG.info("Configured remoting type: {}", remote().getClass().getSimpleName());
    }

    @Override
    public void ping() {
        final BuildRepositoryRemote client = remote();
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
    public void work() {
        LOG.debug("[Agent Loop] Trying to retrieve work.");
        if (pluginJarLocationMonitor.hasRunAtLeastOnce()) {
            retrieveCookieIfNecessary();
            retrieveWork();
            LOG.debug("[Agent Loop] Successfully retrieved work.");
        } else {
            LOG.debug("[Agent Loop] PluginLocationMonitor has not yet run. Not retrieving work since plugins may not be initialized.");
        }
    }

    private void retrieveCookieIfNecessary() {
        if (!getAgentRuntimeInfo().hasCookie() && sslInfrastructureService.isRegistered()) {
            LOG.info("About to get cookie from the server.");
            String cookie = remote().getCookie(getAgentRuntimeInfo());
            getAgentRuntimeInfo().setCookie(cookie);
            LOG.info("Got cookie: {}", cookie);
        }
    }

    void retrieveWork() {
        final BuildRepositoryRemote client = remote();
        AgentIdentifier agentIdentifier = agentIdentifier();
        LOG.debug("[Agent Loop] {} is checking for work from Go", agentIdentifier);
        Work work;
        try {
            getAgentRuntimeInfo().idle();
            work = client.getWork(getAgentRuntimeInfo());
            if (!(work instanceof NoWork)) {
                LOG.debug("[Agent Loop] Got work from server: [{}]", work.description());
            }
            runner = new JobRunner();
            final AgentWorkContext agentWorkContext = new AgentWorkContext(agentIdentifier, client, manipulator, getAgentRuntimeInfo(), packageRepositoryExtension, scmExtension, taskExtension, artifactExtension, pluginRequestProcessorRegistry);
            runner.run(work, agentWorkContext);
        } catch (UnregisteredAgentException e) {
            LOG.warn("[Agent Loop] Invalid agent certificate with fingerprint {}. Registering with server on next iteration.", e.getUuid());
            sslInfrastructureService.invalidateAgentCertificate();
        } finally {
            getAgentRuntimeInfo().idle();
        }
    }

    private BuildRepositoryRemote remote() {
        boolean useLegacy = useLegacy();

        LOG.debug("Remoting type used: {}", (useLegacy ? "RMI" : "JSON"));

        return useLegacy ? legacyRmi : client;
    }

    private boolean useLegacy() {
        return "true".equalsIgnoreCase(
                requireNonNullElse(
                        System.getProperty("gocd.agent.remoting.legacy"),
                        "false"
                ).trim()
        );
    }
}
