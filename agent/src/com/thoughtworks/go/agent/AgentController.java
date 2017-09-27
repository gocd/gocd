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
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;

public abstract class AgentController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentController.class);

    private AgentRuntimeInfo agentRuntimeInfo;
    private AgentAutoRegistrationProperties agentAutoRegistrationProperties;
    private AgentIdentifier identifier;
    private SslInfrastructureService sslInfrastructureService;
    private SystemEnvironment systemEnvironment;
    private AgentRegistry agentRegistry;
    private SubprocessLogger subprocessLogger;
    private AgentUpgradeService agentUpgradeService;
    private final AgentHealthHolder agentHealthHolder;
    private final TimeProvider timeProvider;
    private final String hostName;
    private final String ipAddress;

    public AgentController(SslInfrastructureService sslInfrastructureService,
                           SystemEnvironment systemEnvironment,
                           AgentRegistry agentRegistry,
                           PluginManager pluginManager,
                           SubprocessLogger subprocessLogger,
                           AgentUpgradeService agentUpgradeService,
                           TimeProvider timeProvider,
                           AgentHealthHolder agentHealthHolder) {
        this.sslInfrastructureService = sslInfrastructureService;
        this.systemEnvironment = systemEnvironment;
        this.agentRegistry = agentRegistry;
        this.subprocessLogger = subprocessLogger;
        this.agentUpgradeService = agentUpgradeService;
        this.timeProvider = timeProvider;
        this.agentHealthHolder = agentHealthHolder;
        PluginManagerReference.reference().setPluginManager(pluginManager);
        hostName = SystemUtil.getLocalhostNameOrRandomNameIfNotFound();
        ipAddress = SystemUtil.getClientIp(systemEnvironment.getServiceUrl());
    }

    public abstract void ping();

    public abstract void execute();

    public final void loop() {
        try {
            LOG.debug("[Agent Loop] Trying to retrieve work.");
            agentUpgradeService.checkForUpgrade();
            sslInfrastructureService.registerIfNecessary(getAgentAutoRegistrationProperties());
            work();
            LOG.debug("[Agent Loop] Successfully retrieved work.");
        } catch (Exception e) {
            if (isCausedBySecurity(e)) {
                handleSecurityException(e);
            } else {
                LOG.error("[Agent Loop] Error occurred during loop: ", e);
            }
        }

    }

    private void handleSecurityException(Exception e) {
        sslInfrastructureService.invalidateAgentCertificate();
        LOG.error("There has been a problem with one of Go's SSL certificates. This can be caused by a man-in-the-middle attack, or by pointing the agent to a new server, or by deleting and re-installing Go Server. Go will ask for a new certificate. If this fails to solve the problem, try deleting config/trust.jks in Go Agent's home directory.", e);
    }

    protected abstract void work() throws Exception;

    protected AgentRegistry getAgentRegistry() {
        return agentRegistry;
    }

    protected AgentAutoRegistrationProperties getAgentAutoRegistrationProperties() {
        return agentAutoRegistrationProperties;
    }

    protected SystemEnvironment getSystemEnvironment() {
        return systemEnvironment;
    }

    protected AgentIdentifier agentIdentifier() {
        return new AgentIdentifier(hostName, ipAddress, agentRegistry.uuid());
    }

    protected AgentRuntimeInfo getAgentRuntimeInfo() {
        return agentRuntimeInfo;
    }

    boolean isCausedBySecurity(Throwable e) {
        if (e == null) {
            return false;
        }
        return (e instanceof GeneralSecurityException) || isCausedBySecurity(e.getCause());
    }

    // Executed when Spring initializes this bean
    void init() throws IOException {
        initPipelinesFolder();
        initSslInfratructure();
        initAgentIdentifier();
        initRuntimeInfo();
        initSubProcessLogger();
    }

    private void initSubProcessLogger() {
        subprocessLogger.registerAsExitHook("Following processes were alive at shutdown: ");
    }

    private void initSslInfratructure() throws IOException {
        sslInfrastructureService.createSslInfrastructure();
    }

    private void initAgentIdentifier() {
        identifier = agentIdentifier();
    }

    private void initRuntimeInfo() {
        Boolean buildCommandProtocolEnabled = systemEnvironment.isBuildCommandProtocolEnabled();
        agentAutoRegistrationProperties = new AgentAutoRegistrationPropertiesImpl(new File("config", "autoregister.properties"));

        if (agentAutoRegistrationProperties.isElastic()) {
            agentRuntimeInfo = ElasticAgentRuntimeInfo.fromAgent(identifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), agentAutoRegistrationProperties.agentAutoRegisterElasticAgentId(), agentAutoRegistrationProperties.agentAutoRegisterElasticPluginId(), timeProvider);
        } else {
            agentRuntimeInfo = AgentRuntimeInfo.fromAgent(identifier, AgentStatus.Idle.getRuntimeStatus(), currentWorkingDirectory(), buildCommandProtocolEnabled, timeProvider);
        }
    }

    void pingSuccess() {
        agentHealthHolder.pingSuccess();
    }

    private void initPipelinesFolder() {
        File pipelines = new File(currentWorkingDirectory(), "pipelines");
        if (!pipelines.exists()) {
            pipelines.mkdirs();
        }
    }
}
