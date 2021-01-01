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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialAgent;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;

import java.io.File;

public class MaterialAgentFactory {
    private ConsoleOutputStreamConsumer consumer;
    private File workingDirectory;
    private final AgentIdentifier agentIdentifier;
    private SCMExtension scmExtension;

    public MaterialAgentFactory(ConsoleOutputStreamConsumer consumer,
                                File workingDirectory,
                                AgentIdentifier agentIdentifier,
                                SCMExtension scmExtension) {
        this.consumer = consumer;
        this.workingDirectory = workingDirectory;
        this.agentIdentifier = agentIdentifier;
        this.scmExtension = scmExtension;
    }

    public MaterialAgent createAgent(MaterialRevision revision) {
        Material material = revision.getMaterial();
        if (material instanceof DependencyMaterial) {
            return MaterialAgent.NO_OP;
        } else if (material instanceof PackageMaterial) {
            return MaterialAgent.NO_OP;
        } else if (material instanceof PluggableSCMMaterial) {
            return new PluggableSCMMaterialAgent(scmExtension, revision, workingDirectory, consumer);
        } else if (material instanceof ScmMaterial) {
            String destFolderPath = ((ScmMaterial) material).workingdir(workingDirectory).getAbsolutePath();
            return new AbstractMaterialAgent(revision, consumer, workingDirectory, new AgentSubprocessExecutionContext(agentIdentifier, destFolderPath));
        }
        throw new RuntimeException("Could not find MaterialChecker for material = " + material);
    }

}
