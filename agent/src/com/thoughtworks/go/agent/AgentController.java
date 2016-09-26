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
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;

public abstract class AgentController {
    private AgentRuntimeInfo agentRuntimeInfo;
    private AgentAutoRegistrationProperties agentAutoRegistrationProperties;
    private AgentIdentifier identifier;
    private SslInfrastructureService sslInfrastructureService;

    public SystemEnvironment getSystemEnvironment() {
        return systemEnvironment;
    }

    private SystemEnvironment systemEnvironment;

    private AgentRegistry agentRegistry;

    private SubprocessLogger subprocessLogger;

    abstract void ping();

    abstract void execute();

    abstract void loop();

    public AgentRegistry getAgentRegistry() {
        return agentRegistry;
    }

    public AgentAutoRegistrationProperties getAgentAutoRegistrationProperties() {
        return agentAutoRegistrationProperties;
    }

    public AgentIdentifier getIdentifier() {
        return identifier;
    }

    boolean isCausedBySecurity(Throwable e) {
        if (e == null) {
            return false;
        }
        return (e instanceof GeneralSecurityException) || isCausedBySecurity(e.getCause());
    }

    public AgentController(SslInfrastructureService sslInfrastructureService, SystemEnvironment systemEnvironment, AgentRegistry agentRegistry, SubprocessLogger subprocessLogger) {
        this.sslInfrastructureService = sslInfrastructureService;
        this.systemEnvironment = systemEnvironment;
        this.agentRegistry = agentRegistry;
        this.subprocessLogger = subprocessLogger;
    }

    AgentRuntimeInfo getAgentRuntimeInfo() {
        return agentRuntimeInfo;
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
        identifier = new AgentIdentifier(SystemUtil.getLocalhostNameOrRandomNameIfNotFound(), SystemUtil.getClientIp(systemEnvironment.getServiceUrl()), agentRegistry.uuid());
    }

    private void initRuntimeInfo() {
        Boolean buildCommandProtocolEnabled = systemEnvironment.isBuildCommandProtocolEnabled();
        agentAutoRegistrationProperties = new AgentAutoRegistrationPropertiesImpl(new File("config", "autoregister.properties"));

        if (agentAutoRegistrationProperties.isElastic()) {
            agentRuntimeInfo = ElasticAgentRuntimeInfo.fromAgent(identifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), agentAutoRegistrationProperties.agentAutoRegisterElasticAgentId(), agentAutoRegistrationProperties.agentAutoRegisterElasticPluginId());
        } else {
            agentRuntimeInfo = AgentRuntimeInfo.fromAgent(identifier, AgentStatus.Idle.getRuntimeStatus(), currentWorkingDirectory(), buildCommandProtocolEnabled);
        }
    }

    private void initPipelinesFolder() {
        File pipelines = new File(currentWorkingDirectory(), "pipelines");
        if (!pipelines.exists()) {
            pipelines.mkdirs();
        }
    }

    public static AgentController createInstance(
            BuildRepositoryRemote server,
            GoArtifactsManipulator manipulator,
            AgentUpgradeService agentUpgradeService,
            PluginManager pluginManager,
            SslInfrastructureService sslInfrastructureService,
            SystemEnvironment systemEnvironment,
            AgentRegistry agentRegistry,
            SubprocessLogger subprocessLogger,
            PackageAsRepositoryExtension packageAsRepositoryExtension,
            SCMExtension scmExtension,
            TaskExtension taskExtension
            ) {
        return new HTTPAgentController(
                server,
                manipulator,
                sslInfrastructureService,
                agentRegistry,
                agentUpgradeService,
                subprocessLogger,
                systemEnvironment,
                pluginManager,
                packageAsRepositoryExtension,
                scmExtension,
                taskExtension);
    }
}
