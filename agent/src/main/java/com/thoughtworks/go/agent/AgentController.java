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
import com.thoughtworks.go.agent.service.SystemInfo;
import com.thoughtworks.go.agent.statusapi.AgentHealthHolder;
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.plugin.infra.monitor.PluginJarLocationMonitor;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.GeneralSecurityException;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;

public abstract class AgentController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentController.class);

    private AgentRuntimeInfo agentRuntimeInfo;
    private AgentAutoRegistrationProperties agentAutoRegistrationProperties;
    private AgentIdentifier identifier;
    private final SslInfrastructureService sslInfrastructureService;
    private final AgentRegistry agentRegistry;
    private final SubprocessLogger subprocessLogger;
    private final AgentUpgradeService agentUpgradeService;
    private final AgentHealthHolder agentHealthHolder;
    private final PluginJarLocationMonitor pluginJarLocationMonitor;
    private final String hostName;
    private final String ipAddress;

    public AgentController(SslInfrastructureService sslInfrastructureService,
                           SystemEnvironment systemEnvironment,
                           AgentRegistry agentRegistry,
                           PluginManager pluginManager,
                           SubprocessLogger subprocessLogger,
                           AgentUpgradeService agentUpgradeService,
                           AgentHealthHolder agentHealthHolder,
                           PluginJarLocationMonitor pluginJarLocationMonitor) {
        this.sslInfrastructureService = sslInfrastructureService;
        this.agentRegistry = agentRegistry;
        this.subprocessLogger = subprocessLogger;
        this.agentUpgradeService = agentUpgradeService;
        this.agentHealthHolder = agentHealthHolder;
        this.pluginJarLocationMonitor = pluginJarLocationMonitor;
        PluginManagerReference.reference().setPluginManager(pluginManager);
        hostName = SystemUtil.getLocalhostNameOrRandomNameIfNotFound();
        ipAddress = SystemUtil.getClientIp(systemEnvironment.getServiceUrl());
    }

    public abstract void ping();

    public abstract void execute();

    final WorkAttempt performWork() {
        try {
            LOG.debug("[Agent Loop] Trying to retrieve work.");
            agentUpgradeService.checkForUpgradeAndExtraProperties();
            sslInfrastructureService.registerIfNecessary(getAgentAutoRegistrationProperties());

            if (pluginJarLocationMonitor.hasRunAtLeastOnce()) {
                return tryDoWork();
            } else {
                LOG.debug("[Agent Loop] PluginLocationMonitor has not yet run. Not retrieving work since plugins may not be initialized.");
                return WorkAttempt.FAILED;
            }

        } catch (Exception e) {
            if (isCausedBySecurity(e)) {
                handleSecurityException(e);
            } else {
                LOG.error("[Agent Loop] Error occurred during loop: ", e);
            }
            return WorkAttempt.FAILED;
        }
    }

    private void handleSecurityException(Exception e) {
        LOG.error("There has been a problem with one of GoCD's TLS certificates. This can be caused by a man-in-the-middle attack, or a change to the HTTPS certificates of the GoCD Server. Review the agent TLS trust settings and any mutual TLS configuration of the agent.", e);
    }

    protected abstract WorkAttempt tryDoWork();

    protected AgentAutoRegistrationProperties getAgentAutoRegistrationProperties() {
        return agentAutoRegistrationProperties;
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
    void init() {
        initPipelinesFolder();
        initSslInfrastructure();
        initAgentIdentifier();
        initRuntimeInfo();
        initSubProcessLogger();
    }

    private void initSubProcessLogger() {
        subprocessLogger.registerAsExitHook("Following processes were alive at shutdown: ");
    }

    private void initSslInfrastructure() {
        sslInfrastructureService.createSslInfrastructure();
    }

    private void initAgentIdentifier() {
        identifier = agentIdentifier();
    }

    private void initRuntimeInfo() {
        agentAutoRegistrationProperties = new AgentAutoRegistrationPropertiesImpl(new File("config", "autoregister.properties"));
        String bootstrapperVersion = System.getProperty(GoConstants.AGENT_BOOTSTRAPPER_VERSION, "UNKNOWN");
        String agentVersion = getClass().getPackage().getImplementationVersion();

        if (agentAutoRegistrationProperties.isElastic()) {
            agentRuntimeInfo = ElasticAgentRuntimeInfo.fromAgent(
                identifier,
                AgentRuntimeStatus.Idle,
                currentWorkingDirectory(),
                agentAutoRegistrationProperties.agentAutoRegisterElasticAgentId(),
                agentAutoRegistrationProperties.agentAutoRegisterElasticPluginId(),
                bootstrapperVersion,
                agentVersion,
                SystemInfo::getOperatingSystemCompleteName);
        } else {
            agentRuntimeInfo = AgentRuntimeInfo.fromAgent(
                identifier,
                AgentStatus.Idle.getRuntimeStatus(),
                currentWorkingDirectory(),
                bootstrapperVersion,
                agentVersion,
                SystemInfo::getOperatingSystemCompleteName);
        }
    }

    void pingSuccess() {
        agentHealthHolder.pingSuccess();
    }

    private void initPipelinesFolder() {
        File pipelines = new File(currentWorkingDirectory(), "pipelines");
        if (!pipelines.exists()) {
            //noinspection ResultOfMethodCallIgnored
            pipelines.mkdirs();
        }
    }
}
