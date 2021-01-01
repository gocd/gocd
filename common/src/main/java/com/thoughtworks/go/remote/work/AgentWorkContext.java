/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

public class AgentWorkContext {
    private AgentIdentifier agentIdentifier;
    private BuildRepositoryRemote repositoryRemote;
    private GoArtifactsManipulator artifactsManipulator;
    private AgentRuntimeInfo agentRuntimeInfo;
    private PackageRepositoryExtension packageRepositoryExtension;
    private SCMExtension scmExtension;
    private TaskExtension taskExtension;
    private ArtifactExtension artifactExtension;
    private final PluginRequestProcessorRegistry pluginRequestProcessorRegistry;

    public AgentWorkContext(AgentIdentifier agentIdentifier, BuildRepositoryRemote repositoryRemote, GoArtifactsManipulator artifactsManipulator, AgentRuntimeInfo agentRuntimeInfo, PackageRepositoryExtension packageRepositoryExtension, SCMExtension scmExtension, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry) {
        this.agentIdentifier = agentIdentifier;
        this.repositoryRemote = repositoryRemote;
        this.artifactsManipulator = artifactsManipulator;
        this.agentRuntimeInfo = agentRuntimeInfo;
        this.packageRepositoryExtension = packageRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.artifactExtension = artifactExtension;
        this.pluginRequestProcessorRegistry = pluginRequestProcessorRegistry;
    }

    public AgentIdentifier getAgentIdentifier() {
        return agentIdentifier;
    }

    public BuildRepositoryRemote getRepositoryRemote() {
        return repositoryRemote;
    }

    public GoArtifactsManipulator getArtifactsManipulator() {
        return artifactsManipulator;
    }

    public AgentRuntimeInfo getAgentRuntimeInfo() {
        return agentRuntimeInfo;
    }

    public PackageRepositoryExtension getPackageRepositoryExtension() {
        return packageRepositoryExtension;
    }

    public SCMExtension getScmExtension() {
        return scmExtension;
    }

    public TaskExtension getTaskExtension() {
        return taskExtension;
    }

    public ArtifactExtension getArtifactExtension() {
        return artifactExtension;
    }

    public PluginRequestProcessorRegistry getPluginRequestProcessorRegistry() {
        return pluginRequestProcessorRegistry;
    }
}
