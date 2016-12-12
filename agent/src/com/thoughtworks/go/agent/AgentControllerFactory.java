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
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentControllerFactory {
    private final BuildRepositoryRemote server;
    private final GoArtifactsManipulator manipulator;
    private final PluginManager pluginManager;
    private final SslInfrastructureService sslInfrastructureService;
    private final SystemEnvironment systemEnvironment;
    private final AgentRegistry agentRegistry;
    private final SubprocessLogger subprocessLogger;
    private final AgentUpgradeService agentUpgradeService;
    private final PackageAsRepositoryExtension packageAsRepositoryExtension;
    private final SCMExtension scmExtension;
    private final TaskExtension taskExtension;
    private final HttpService httpService;
    private final WebSocketClientHandler webSocketClientHandler;
    private final WebSocketSessionHandler sessionHandler;

    @Autowired
    public AgentControllerFactory(
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
            TaskExtension taskExtension,
            HttpService httpService,
            WebSocketClientHandler webSocketClientHandler, WebSocketSessionHandler sessionHandler) {
        this.server = server;
        this.manipulator = manipulator;
        this.pluginManager = pluginManager;
        this.sslInfrastructureService = sslInfrastructureService;
        this.systemEnvironment = systemEnvironment;
        this.agentRegistry = agentRegistry;
        this.subprocessLogger = subprocessLogger;
        this.agentUpgradeService = agentUpgradeService;
        this.packageAsRepositoryExtension = packageAsRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.httpService = httpService;
        this.webSocketClientHandler = webSocketClientHandler;
        this.sessionHandler = sessionHandler;
    }

    public AgentController createInstance() {
        if (systemEnvironment.isWebsocketEnabled()) {
            return new AgentWebSocketClientController(
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
                    taskExtension,
                    httpService, webSocketClientHandler, sessionHandler);
        } else {
            return new AgentHTTPClientController(
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
}
