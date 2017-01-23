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

package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialAgent;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialAgent;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.command.DevNull;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class MaterialAgentFactoryTest {
    private TempFiles tempFiles;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;

    @Before
    public void setUp() {
        initMocks(this);
        tempFiles = new TempFiles();
    }

    @After
    public void tearDown() {
        tempFiles.cleanUp();
    }

    @Test
    public void shouldCreateMaterialAgent_withAgentsUuidAsSubprocessExecutionContextNamespace() {
        String agentUuid = "uuid-01783738";
        File workingDirectory = tempFiles.createUniqueFolder("");
        MaterialAgentFactory factory = new MaterialAgentFactory(new ProcessOutputStreamConsumer(new DevNull(), new DevNull()), workingDirectory,
                new AgentIdentifier("host", "1.1.1.1", agentUuid), packageRepositoryExtension, scmExtension);
        GitMaterial gitMaterial = new GitMaterial("http://foo", "master", "dest_folder");

        MaterialAgent agent = factory.createAgent(new MaterialRevision(gitMaterial));

        assertThat(agent, is(instanceOf(AbstractMaterialAgent.class)));

        SubprocessExecutionContext execCtx = (SubprocessExecutionContext) ReflectionUtil.getField(agent, "execCtx");
        assertThat(execCtx.getProcessNamespace("fingerprint"), is(CachedDigestUtils.sha256Hex(String.format("%s%s%s", "fingerprint", agentUuid, gitMaterial.workingdir(workingDirectory)))));
    }

    @Test
    public void shouldGetPackageMaterialAgent() {
        File workingDirectory = new File("/tmp/workingDirectory");
        MaterialRevision revision = new MaterialRevision(new PackageMaterial(), new Modifications());
        MaterialAgentFactory factory = new MaterialAgentFactory(null, workingDirectory, null, packageRepositoryExtension, scmExtension);
        MaterialAgent agent = factory.createAgent(revision);

        assertThat(agent instanceof PackageMaterialAgent, is(true));
        assertThat(ReflectionUtil.getField(agent, "packageRepositoryExtension"), is(packageRepositoryExtension));
        assertThat(ReflectionUtil.getField(agent, "revision"), is(revision));
        assertThat(ReflectionUtil.getField(agent, "workingDirectory"), is(workingDirectory));
    }

    @Test
    public void shouldGetPluggableSCMMaterialAgent() {
        File workingDirectory = new File("/tmp/workingDirectory");
        MaterialRevision revision = new MaterialRevision(new PluggableSCMMaterial(), new Modifications());
        MaterialAgentFactory factory = new MaterialAgentFactory(null, workingDirectory, null, packageRepositoryExtension, scmExtension);
        MaterialAgent agent = factory.createAgent(revision);

        assertThat(agent instanceof PluggableSCMMaterialAgent, is(true));
        assertThat(ReflectionUtil.getField(agent, "scmExtension"), is(scmExtension));
        assertThat(ReflectionUtil.getField(agent, "revision"), is(revision));
        assertThat(ReflectionUtil.getField(agent, "workingDirectory"), is(workingDirectory));
    }
}
