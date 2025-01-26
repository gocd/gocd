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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.perforce.P4Client;
import com.thoughtworks.go.domain.materials.perforce.P4Fixture;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class BuildWorkEnvironmentVariablesTest {
    private static final String JOB_NAME = "one";
    private static final String STAGE_NAME = "first";
    private static final String PIPELINE_NAME = "cruise";
    private static final String AGENT_UUID = "uuid";
    private static final String TRIGGERED_BY_USER = "approver";
    private File dir;
    private PipelineConfig pipelineConfig;
    private EnvironmentVariableContext environmentVariableContext;
    private SvnCommand command;
    private HgTestRepo hgTestRepo;
    private HgMaterial hgMaterial;
    private SvnMaterial svnMaterial;
    private DependencyMaterial dependencyMaterial;
    private DependencyMaterial dependencyMaterialWithName;
    private SvnTestRepo repo;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        dir = TempDirUtils.createTempDirectoryIn(tempDir, "someFolder").toFile();
        environmentVariableContext = new EnvironmentVariableContext();
        repo = new SvnTestRepo(tempDir);
        command = new SvnCommand(null, repo.end2endRepositoryUrl());

        pipelineConfig = PipelineConfigMother.createPipelineConfig(PIPELINE_NAME, STAGE_NAME, JOB_NAME);
        svnMaterial = new SvnMaterial(command);
        dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream1"), new CaseInsensitiveString(STAGE_NAME));
        dependencyMaterialWithName = new DependencyMaterial(new CaseInsensitiveString("upstream2"), new CaseInsensitiveString(STAGE_NAME));
        dependencyMaterialWithName.setName(new CaseInsensitiveString("dependency_material_name"));
        setupHgRepo(tempDir);

    }

    @Test
    public void shouldSetUpEnvironmentContextCorrectly() throws Exception {
        new SystemEnvironment().setProperty(SystemEnvironment.SERVICE_URL, "some_random_place");
        Materials materials = new Materials(svnMaterial);
        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(materials);
        assertThat(environmentVariableContext.getProperty("GO_REVISION")).isEqualTo("3");
        assertThat(environmentVariableContext.getProperty("GO_SERVER_URL")).isEqualTo("some_random_place");
        assertThat(environmentVariableContext.getProperty("GO_PIPELINE_NAME")).isEqualTo(PIPELINE_NAME);
        assertThat(environmentVariableContext.getProperty("GO_PIPELINE_LABEL")).isEqualTo("1");
        assertThat(environmentVariableContext.getProperty("GO_STAGE_NAME")).isEqualTo(STAGE_NAME);
        assertThat(environmentVariableContext.getProperty("GO_STAGE_COUNTER")).isEqualTo("1");
        assertThat(environmentVariableContext.getProperty("GO_JOB_NAME")).isEqualTo(JOB_NAME);
        assertThat(environmentVariableContext.getProperty("GO_TRIGGER_USER")).isEqualTo(TRIGGERED_BY_USER);
    }

    @Nested
    public class P4 {
        private P4Material p4Material;
        private P4Fixture p4Fixture;
        private P4Client p4Client;

        @BeforeEach
        public void setUp(@TempDir Path tempDir) throws Exception {
            p4Fixture = new P4Fixture();
            p4Material = getP4Material(tempDir);
        }

        @AfterEach
        public void teardown() {
            p4Fixture.stop(p4Client);
        }

        @Test
        public void shouldSetUpP4ClientEnvironmentVariableEnvironmentContextCorrectly() {
            new SystemEnvironment().setProperty(SystemEnvironment.SERVICE_URL, "some_random_place");
            BuildWork work = getBuildWorkWithP4MaterialRevision(p4Material);
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

            AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);

            work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                    new GoArtifactsManipulatorStub(), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), scmExtension, taskExtension, null, null));


            assertThat(environmentVariableContext.getProperty("GO_REVISION")).isEqualTo("10");
            assertThat(environmentVariableContext.getProperty("GO_SERVER_URL")).isEqualTo("some_random_place");
            assertThat(environmentVariableContext.getProperty("GO_TRIGGER_USER")).isEqualTo(TRIGGERED_BY_USER);
            assertThat(environmentVariableContext.getProperty("GO_P4_CLIENT")).isEqualTo(p4Material.clientName(dir));
        }

        private BuildWork getBuildWorkWithP4MaterialRevision(P4Material p4Material) {
            pipelineConfig.setMaterialConfigs(new Materials(p4Material).convertToConfigs());
            JobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(), -1, new JobIdentifier(PIPELINE_NAME, 1, "1", STAGE_NAME, "1", JOB_NAME, 123L), null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
            MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(p4Material, new Modification("user", "comment", "a@b.com", new Date(), "10")));
            BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, TRIGGERED_BY_USER);
            List<Builder> builders = new ArrayList<>();
            builders.add(new CommandBuilder("ant", "", dir, new RunIfConfigs(), new NullBuilder(), ""));
            BuildAssignment assignment = BuildAssignment.create(plan, buildCause, builders, dir, environmentVariableContext, new ArtifactStores());
            return new BuildWork(assignment, StandardCharsets.UTF_8);
        }

        private P4Material getP4Material(Path tempDir) throws Exception {
            String view = "//depot/... //something/...";
            P4TestRepo repo = P4TestRepo.createP4TestRepo(tempDir, TempDirUtils.createTempDirectoryIn(tempDir, "p4client").toFile());
            repo.onSetup();
            p4Fixture.setRepo(repo);
            p4Client = p4Fixture.createClient();
            return p4Fixture.material(view);
        }
    }

    @Test
    public void shouldMergeEnvironmentVariablesFromInitialContext() throws IOException {
        pipelineConfig.setMaterialConfigs(new MaterialConfigs());

        BuildAssignment buildAssignment = createAssignment(new EnvironmentVariableContext("foo", "bar"));
        BuildWork work = new BuildWork(buildAssignment, StandardCharsets.UTF_8);
        EnvironmentVariableContext environmentContext = new EnvironmentVariableContext();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(), new GoArtifactsManipulatorStub(), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), scmExtension, taskExtension, null, null));

        assertEnvironmentContext(environmentContext, "foo").isEqualTo("bar");
    }

    private AbstractStringAssert<?> assertEnvironmentContext(EnvironmentVariableContext environmentVariableContext, String key) {
        return assertThat(environmentVariableContext.getProperty(key)).describedAs("Properties: \n" + environmentVariableContext.getProperties());
    }

    @Test
    public void shouldSetupEnvironmentVariableForDependencyMaterial() throws IOException {
        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(new Materials());

        assertEnvironmentContext(environmentVariableContext, "GO_DEPENDENCY_LOCATOR_UPSTREAM1").isEqualTo("upstream1/0/first/1");
        assertEnvironmentContext(environmentVariableContext, "GO_DEPENDENCY_LABEL_UPSTREAM1").isEqualTo("upstream1-label");
    }

    @Test
    public void shouldSetupEnvironmentVariableUsingDependencyMaterialName() throws IOException {
        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(new Materials());

        assertEnvironmentContext(environmentVariableContext, "GO_DEPENDENCY_LOCATOR_DEPENDENCY_MATERIAL_NAME").isEqualTo("upstream2/0/first/1");
        assertEnvironmentContext(environmentVariableContext, "GO_DEPENDENCY_LABEL_DEPENDENCY_MATERIAL_NAME").isEqualTo("upstream2-label");
    }

    @Test
    public void shouldUseSvnMaterialNameIfPresent() throws IOException {
        svnMaterial.setName(new CaseInsensitiveString("Cruise"));
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(svnMaterial.config()));

        BuildAssignment buildAssigment = createAssignment(null);
        BuildWork work = new BuildWork(buildAssigment, StandardCharsets.UTF_8);
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                new GoArtifactsManipulatorStub(), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), scmExtension, taskExtension, null, null));

        assertThat(environmentVariableContext.getProperty("GO_REVISION_CRUISE")).isEqualTo("3");
    }

    @Test
    public void shouldSetUpRevisionIntoEnvironmentContextCorrectlyForMultipleMaterial() throws IOException {
        svnMaterial.setFolder("svn-Dir");

        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(new Materials(svnMaterial, hgMaterial));

        assertThat(environmentVariableContext.getProperty("GO_REVISION_SVN_DIR")).isEqualTo("3");
        assertThat(environmentVariableContext.getProperty("GO_REVISION_HG_DIR")).isEqualTo("ca3ebb67f527c0ad7ed26b789056823d8b9af23f");
    }

    @Test
    public void shouldOutputEnvironmentVariablesIntoConsoleOut() throws IOException {
        BuildAssignment buildAssigment = createAssignment(null);
        BuildWork work = new BuildWork(buildAssigment, StandardCharsets.UTF_8);
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();
        new SystemEnvironment().setProperty(SystemEnvironment.SERVICE_URL, "some_random_place");

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), scmExtension, taskExtension, null, null));

        assertThat(manipulator.consoleOut())
            .satisfies(hasPrintedEnvVariable("GO_SERVER_URL", "some_random_place"))
            .satisfies(hasPrintedEnvVariable("GO_PIPELINE_NAME", PIPELINE_NAME))
            .satisfies(hasPrintedEnvVariable("GO_PIPELINE_COUNTER", 1))
            .satisfies(hasPrintedEnvVariable("GO_PIPELINE_LABEL", 1))
            .satisfies(hasPrintedEnvVariable("GO_STAGE_NAME", STAGE_NAME))
            .satisfies(hasPrintedEnvVariable("GO_STAGE_COUNTER", 1))
            .satisfies(hasPrintedEnvVariable("GO_JOB_NAME", JOB_NAME))
            .satisfies(hasPrintedEnvVariable("GO_REVISION", 3))
            .satisfies(hasPrintedEnvVariable("GO_TRIGGER_USER", TRIGGERED_BY_USER));
    }

    private Consumer<? super String> hasPrintedEnvVariable(String key, Object value) {
        return console -> assertThat(console).containsAnyOf(
            format("environment variable '%s' to value '%s'", key, value),
            format("environment variable '%s' with value '%s'", key, value)
        );
    }

    @Test
    public void shouldSetEnvironmentVariableForSvnExternal() throws IOException {
        SvnTestRepo repoExternals = new SvnTestRepoWithExternal(tempDir);

        command = new SvnCommand(null, repoExternals.projectRepositoryUrl(), null, null, true);
        svnMaterial = new SvnMaterial(command);
        svnMaterial.setFolder("svn-Dir");

        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(new Materials(svnMaterial));

        assertThat(environmentVariableContext.getProperty("GO_REVISION_SVN_DIR")).isEqualTo("3");
        assertThat(environmentVariableContext.getProperty("GO_REVISION_SVN_DIR_EXTERNAL")).isEqualTo("4");
    }

    private BuildAssignment createAssignment(EnvironmentVariableContext environmentVariableContext) throws IOException {
        JobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(), -1, new JobIdentifier(PIPELINE_NAME, 1, "1", STAGE_NAME, "1", JOB_NAME, 123L), null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        MaterialRevisions materialRevisions = materialRevisions();
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, TRIGGERED_BY_USER);
        List<Builder> builders = new ArrayList<>();
        builders.add(new CommandBuilder("ant", "", dir, new RunIfConfigs(), new NullBuilder(), ""));
        return BuildAssignment.create(plan, buildCause, builders, dir, environmentVariableContext, new ArtifactStores());
    }

    private void setupHgRepo(Path tempDir) throws IOException {
        hgTestRepo = new HgTestRepo("hgTestRepo1", tempDir);
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl(), "hg_Dir");
    }

    private MaterialRevisions materialRevisions() throws IOException {
        MaterialRevision svnRevision = new MaterialRevision(this.svnMaterial,
                ModificationsMother.oneModifiedFile(repo.end2ndRepositoryLatestRevision()));

        SvnMaterial svnMaterialForExternal = new SvnMaterial(new SvnCommand(null, repo.projectRepositoryUrl()));
        String folder = this.svnMaterial.getFolder() == null ? "external" : this.svnMaterial.getFolder() + "/" + "external";
        svnMaterialForExternal.setFolder(folder);
        MaterialRevision svnExternalRevision = new MaterialRevision(svnMaterialForExternal,
                ModificationsMother.oneModifiedFile(repo.latestRevision()));

        MaterialRevision hgRevision = new MaterialRevision(hgMaterial,
                ModificationsMother.oneModifiedFile(hgTestRepo.latestModifications().get(0).getRevision()));

        MaterialRevision dependencyRevision1 = ModificationsMother.dependencyMaterialRevision(0,
                dependencyMaterial.getPipelineName() + "-label", 1,
                dependencyMaterial, new Date());
        MaterialRevision dependencyRevisionWithName = ModificationsMother.dependencyMaterialRevision(0,
                dependencyMaterialWithName.getPipelineName() + "-label", 1,
                dependencyMaterialWithName, new Date());

        return new MaterialRevisions(svnRevision, svnExternalRevision, hgRevision, dependencyRevision1,
                dependencyRevisionWithName);
    }

    private EnvironmentVariableContext doWorkWithMaterials(Materials materials) throws IOException {
        pipelineConfig.setMaterialConfigs(materials.convertToConfigs());

        BuildAssignment buildAssigment = createAssignment(null);
        BuildWork work = new BuildWork(buildAssigment, StandardCharsets.UTF_8);
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                new GoArtifactsManipulatorStub(), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), scmExtension, taskExtension, null, null));
        return environmentVariableContext;
    }
}
