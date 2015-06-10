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
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.config.AgentRegistry;
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
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;

@Component
public class AgentController {
    private static final Log LOG = LogFactory.getLog(AgentController.class);

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

    @Autowired
    public AgentController(BuildRepositoryRemote server, BuildRepositoryHttpRemote remoteServer, GoArtifactsManipulator manipulator, SslInfrastructureService sslInfrastructureService, AgentRegistry agentRegistry,
                           AgentUpgradeService agentUpgradeService, SubprocessLogger subprocessLogger, SystemEnvironment systemEnvironment,
                           PluginManager pluginManager, PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension, TaskExtension taskExtension) {
        this.agentUpgradeService = agentUpgradeService;
        this.packageAsRepositoryExtension = packageAsRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        hostName = SystemUtil.getLocalhostNameOrRandomNameIfNotFound();
        this.server = server;
        this.manipulator = manipulator;
        this.sslInfrastructureService = sslInfrastructureService;
        this.agentRegistry = agentRegistry;
        this.subprocessLogger = subprocessLogger;
        this.systemEnvironment = systemEnvironment;
        PluginManagerReference.reference().setPluginManager(pluginManager);
    }

    void init() throws IOException {
        createPipelinesFolderIfNotExist();
        sslInfrastructureService.createSslInfrastructure();
        AgentIdentifier identifier = agentIdentifier();
        agentRuntimeInfo = AgentRuntimeInfo.fromAgent(identifier, systemEnvironment.getAgentLauncherVersion());
        subprocessLogger.registerAsExitHook("Following processes were alive at shutdown: ");
    }

    private void createPipelinesFolderIfNotExist() {
        File pipelines = new File(currentWorkingDirectory(), "pipelines");
        if (!pipelines.exists()) {
            pipelines.mkdirs();
        }
    }

    public void ping() {
        try {
            if (sslInfrastructureService.isRegistered()) {
                AgentIdentifier agent = agentIdentifier();
                LOG.trace(agent + " is pinging server [" + server.toString() + "]");

                agentRuntimeInfo.refreshUsableSpace();
                instruction = server.ping(agentRuntimeInfo);
                LOG.trace(agent + " pinged server [" + server.toString() + "]");
            }
        } catch (Throwable e) {
            LOG.error("Error occurred when agent tried to ping server: ", e);
        }
    }

    private AgentIdentifier agentIdentifier() {
        return new AgentIdentifier(hostName, ipAddress, agentRegistry.uuid());
    }

    public void loop() {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[Agent Loop] Trying to retrieve work.");
            }
            agentUpgradeService.checkForUpgrade();
            sslInfrastructureService.registerIfNecessary();
            retrieveCookieIfNecessary();
            retrieveWork();
            if (LOG.isDebugEnabled()) {
                LOG.debug("[Agent Loop] Successfully retrieved work.");
            }

        } catch (Exception e) {
            if (isCausedBySecurity(e)) {
                handleIfSecurityException(e);
            } else if (e instanceof DataRetrievalFailureException) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[Agent Loop] Error occurred during loop: ", e);
                }
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
            LOG.info(String.format("Got cookie: %s ", cookie));
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
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("[Agent Loop] %s is checking for work from Go", agentIdentifier));
        }
        Work work;
        try {
            agentRuntimeInfo.idle();
            work = server.getWork(agentRuntimeInfo);
            if (!(work instanceof NoWork)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("[Agent Loop] Got work from server: [%s]", work.description()));
                }
            }
            runner = new JobRunner();
            runner.run(work, agentIdentifier, server, manipulator, agentRuntimeInfo, packageAsRepositoryExtension, scmExtension, taskExtension);
        } catch (UnregisteredAgentException e) {
            LOG.warn(String.format("[Agent Loop] Invalid agent certificate with fingerprint %s. Registering with server on next iteration.", e.getUuid()));
            sslInfrastructureService.invalidateAgentCertificate();
        } finally {
            agentRuntimeInfo.idle();
        }
    }

    public void executeAgentInstruction() {
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

}
