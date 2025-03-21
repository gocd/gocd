/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialAgent;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static com.thoughtworks.go.domain.materials.MaterialAgent.NO_OP;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class MaterialAgentFactoryTest {
    @TempDir
    public File tempWorkingDirectory;

    @Mock
    private SCMExtension scmExtension;

    @Test
    public void shouldCreateMaterialAgent_withAgentsUuidAsSubprocessExecutionContextNamespace() {
        String agentUuid = "uuid-01783738";
        MaterialAgentFactory factory = new MaterialAgentFactory(new InMemoryStreamConsumer(), tempWorkingDirectory,
                new AgentIdentifier("host", "1.1.1.1", agentUuid), scmExtension);
        GitMaterial gitMaterial = new GitMaterial("http://foo", "master", "dest_folder");

        MaterialAgent agent = factory.createAgent(new MaterialRevision(gitMaterial));

        assertThat(agent).isInstanceOf(AbstractMaterialAgent.class);

        SubprocessExecutionContext execCtx = ReflectionUtil.getField(agent, "execCtx");
        assertThat(execCtx.getProcessNamespace("fingerprint")).isEqualTo(DigestUtils.sha256Hex(String.format("%s%s%s", "fingerprint", agentUuid, gitMaterial.workingdir(tempWorkingDirectory))));
    }

    @Test
    public void shouldGetPackageMaterialAgent() {
        File workingDirectory = new File("/tmp/workingDirectory");
        MaterialRevision revision = new MaterialRevision(new PackageMaterial(), new Modifications());
        MaterialAgentFactory factory = new MaterialAgentFactory(null, workingDirectory, null, scmExtension);
        MaterialAgent agent = factory.createAgent(revision);

        assertThat(agent).isEqualTo(NO_OP);
    }

    @Test
    public void shouldGetPluggableSCMMaterialAgent() {
        File workingDirectory = new File("/tmp/workingDirectory");
        MaterialRevision revision = new MaterialRevision(new PluggableSCMMaterial(), new Modifications());
        MaterialAgentFactory factory = new MaterialAgentFactory(null, workingDirectory, null, scmExtension);
        MaterialAgent agent = factory.createAgent(revision);

        assertThat(agent instanceof PluggableSCMMaterialAgent).isTrue();
        assertThat((Object) ReflectionUtil.getField(agent, "scmExtension")).isEqualTo(scmExtension);
        assertThat((Object) ReflectionUtil.getField(agent, "revision")).isEqualTo(revision);
        assertThat((Object) ReflectionUtil.getField(agent, "workingDirectory")).isEqualTo(workingDirectory);
    }
}
