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
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.exception.UnregisteredAgentException;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;

public class HTTPAgentController extends AgentController {
    private static final Logger LOG = LoggerFactory.getLogger(HTTPAgentController.class);

    private BuildRepositoryRemote server;
    private GoArtifactsManipulator manipulator;
    private SslInfrastructureService sslInfrastructureService;

    private JobRunner runner;
    private AgentUpgradeService agentUpgradeService;
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private SCMExtension scmExtension;
    private TaskExtension taskExtension;
    private AgentInstruction agentInstruction = new AgentInstruction(false);

    public HTTPAgentController(BuildRepositoryRemote server,
                               GoArtifactsManipulator manipulator,
                               SslInfrastructureService sslInfrastructureService,
                               AgentRegistry agentRegistry,
                               AgentUpgradeService agentUpgradeService,
                               SubprocessLogger subprocessLogger,
                               SystemEnvironment systemEnvironment,
                               PluginManager pluginManager,
                               PackageAsRepositoryExtension packageAsRepositoryExtension,
                               SCMExtension scmExtension,
                               TaskExtension taskExtension) {
        super(sslInfrastructureService, systemEnvironment, agentRegistry, pluginManager, subprocessLogger);
        this.agentUpgradeService = agentUpgradeService;
        this.packageAsRepositoryExtension = packageAsRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.server = server;
        this.manipulator = manipulator;
        this.sslInfrastructureService = sslInfrastructureService;
    }

    @Override
    public void ping() {
        try {
            if (sslInfrastructureService.isRegistered()) {
                AgentIdentifier agent = agentIdentifier();
                LOG.trace("{} is pinging server [{}]", agent, server);

                getAgentRuntimeInfo().refreshUsableSpace();

                agentInstruction = server.ping(getAgentRuntimeInfo());
                LOG.trace("{} pinged server [{}]", agent, server);
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
    public void loop() {
        try {
            LOG.debug("[Agent Loop] Trying to retrieve work.");
            agentUpgradeService.checkForUpgrade();
            sslInfrastructureService.registerIfNecessary(getAgentAutoRegistrationProperties());
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
        if (!getAgentRuntimeInfo().hasCookie() && sslInfrastructureService.isRegistered()) {
            LOG.info("About to get cookie from the server.");
            String cookie = server.getCookie(agentIdentifier(), getAgentRuntimeInfo().getLocation());
            getAgentRuntimeInfo().setCookie(cookie);
            LOG.info("Got cookie: {}", cookie);
        }
    }

    private void handleIfSecurityException(Exception e) {
        if (!isCausedBySecurity(e)) {
            return;
        }
        sslInfrastructureService.invalidateAgentCertificate();
        LOG.error("There has been a problem with one of Go's SSL certificates." +
                        " This can be caused by a man-in-the-middle attack, or by pointing the agent to a new e, or by" +
                        " deleting and re-installing Go Server. Go will ask for a new certificate. If this" +
                        " fails to solve the problem, try deleting config/trust.jks in Go Agent's home directory.",
                e);
    }

    void retrieveWork() {
        AgentIdentifier agentIdentifier = agentIdentifier();
        LOG.debug("[Agent Loop] {} is checking for work from Go", agentIdentifier);
        Work work;
        try {
            getAgentRuntimeInfo().idle();
            work = server.getWork(getAgentRuntimeInfo());
            if (!(work instanceof NoWork)) {
                LOG.debug("[Agent Loop] Got work from server: [{}]", work.description());
            }
            runner = new JobRunner();
            runner.run(work, agentIdentifier, server, manipulator, getAgentRuntimeInfo(), packageAsRepositoryExtension, scmExtension, taskExtension);
        } catch (UnregisteredAgentException e) {
            LOG.warn("[Agent Loop] Invalid agent certificate with fingerprint {}. Registering with server on next iteration.", e.getUuid());
            sslInfrastructureService.invalidateAgentCertificate();
        } finally {
            getAgentRuntimeInfo().idle();
        }
    }


}
