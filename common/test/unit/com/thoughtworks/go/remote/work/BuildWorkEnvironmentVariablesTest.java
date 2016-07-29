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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.utils.SvnRepoFixture;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.matchers.ConsoleOutMatcher.printedEnvVariable;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

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
    private SvnRepoFixture svnRepoFixture;
    @Mock
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        dir = new File("someFolder");
        environmentVariableContext = new EnvironmentVariableContext();
        svnRepoFixture = new SvnRepoFixture("../common/test-resources/unit/data/svnrepo");
        svnRepoFixture.createRepository();
        command = new SvnCommand(null, svnRepoFixture.getEnd2EndRepoUrl());

        pipelineConfig = PipelineConfigMother.createPipelineConfig(PIPELINE_NAME, STAGE_NAME, JOB_NAME);
        svnMaterial = SvnMaterial.createSvnMaterialWithMock(command);
        dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream1"), new CaseInsensitiveString(STAGE_NAME));
        dependencyMaterialWithName = new DependencyMaterial(new CaseInsensitiveString("upstream2"), new CaseInsensitiveString(STAGE_NAME));
        dependencyMaterialWithName.setName(new CaseInsensitiveString("dependency_material_name"));
        setupHgRepo();
    }

    @After
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
        hgTestRepo.tearDown();
        FileUtil.deleteFolder(dir);
    }

    @Test
    public void shouldSetUpEnvironmentContextCorrectly() throws Exception {
        new SystemEnvironment().setProperty("serviceUrl", "some_random_place");
        Materials materials = new Materials(svnMaterial);
        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(materials);
        assertThat(environmentVariableContext.getProperty("GO_REVISION"), is("3"));
        assertThat(environmentVariableContext.getProperty("GO_SERVER_URL"), is("some_random_place"));
        assertThat(environmentVariableContext.getProperty("GO_PIPELINE_NAME"), is(PIPELINE_NAME));
        assertThat(environmentVariableContext.getProperty("GO_PIPELINE_LABEL"), is("1"));
        assertThat(environmentVariableContext.getProperty("GO_STAGE_NAME"), is(STAGE_NAME));
        assertThat(environmentVariableContext.getProperty("GO_STAGE_COUNTER"), is("1"));
        assertThat(environmentVariableContext.getProperty("GO_JOB_NAME"), is(JOB_NAME));
        assertThat(environmentVariableContext.getProperty("GO_TRIGGER_USER"), is(TRIGGERED_BY_USER));
    }

    @Test public void shouldMergeEnvironmentVariablesFromInitialContext() throws Exception {
        pipelineConfig.setMaterialConfigs(new MaterialConfigs());

        BuildAssignment buildAssigment = createAssignment();
        buildAssigment.enhanceEnvironmentVariables(new EnvironmentVariableContext("foo", "bar"));
        BuildWork work = new BuildWork(buildAssigment);
        EnvironmentVariableContext environmentContext = new EnvironmentVariableContext();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(agentIdentifier, new FakeBuildRepositoryRemote(), new GoArtifactsManipulatorStub(),
                environmentContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertEnvironmentContext(environmentContext, "foo", is("bar"));
    }

    private void assertEnvironmentContext(EnvironmentVariableContext environmentVariableContext, String key, Matcher<String> matcher) {
        assertThat("Properties: \n" + environmentVariableContext.getProperties(), environmentVariableContext.getProperty(key), matcher);
    }

    @Test
    public void shouldSetupEnvironmentVariableForDependencyMaterial() {
        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(new Materials());

        assertThat("Properties: \n" + environmentVariableContext.getProperties(),
                environmentVariableContext.getProperty("GO_DEPENDENCY_LOCATOR_UPSTREAM1"), is("upstream1/0/first/1"));
        assertThat("Properties: \n" + environmentVariableContext.getProperties(),
                environmentVariableContext.getProperty("GO_DEPENDENCY_LABEL_UPSTREAM1"), is("upstream1-label"));
    }

    @Test
    public void shouldSetupEnvironmentVariableUsingDependencyMaterialName() {
        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(new Materials());

        assertThat("Properties: \n" + environmentVariableContext.getProperties(),
                environmentVariableContext.getProperty("GO_DEPENDENCY_LOCATOR_DEPENDENCY_MATERIAL_NAME"), is("upstream2/0/first/1"));
        assertThat("Properties: \n" + environmentVariableContext.getProperties(),
                environmentVariableContext.getProperty("GO_DEPENDENCY_LABEL_DEPENDENCY_MATERIAL_NAME"), is("upstream2-label"));
    }

    @Test
    public void shouldUseSvnMaterialNameIfPresent() throws Exception {
        svnMaterial.setName(new CaseInsensitiveString("Cruise"));
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(svnMaterial.config()));

        BuildAssignment buildAssigment = createAssignment();
        BuildWork work = new BuildWork(buildAssigment);
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        new SystemEnvironment().setProperty("serviceUrl", "some_random_place");

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(agentIdentifier, new FakeBuildRepositoryRemote(),
                new GoArtifactsManipulatorStub(),
                environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(environmentVariableContext.getProperty("GO_REVISION_CRUISE"), is("3"));
    }

    @Test
    public void shouldSetUpRevisionIntoEnvironmentContextCorrectlyForMutipleMaterial() throws Exception {
        svnMaterial.setFolder("svn-Dir");

        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(new Materials(svnMaterial, hgMaterial));

        assertThat(environmentVariableContext.getProperty("GO_REVISION_SVN_DIR"), is("3"));
        assertThat(environmentVariableContext.getProperty("GO_REVISION_HG_DIR"), is("ca3ebb67f527c0ad7ed26b789056823d8b9af23f"));
    }

    @Test
    public void shouldOutputEnvironmentVariablesIntoConsoleOut() throws Exception {
        BuildAssignment buildAssigment = createAssignment();
        BuildWork work = new BuildWork(buildAssigment);
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();
        new SystemEnvironment().setProperty("serviceUrl", "some_random_place");

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(agentIdentifier, new FakeBuildRepositoryRemote(),
                manipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_SERVER_URL", "some_random_place"));
        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_PIPELINE_NAME", PIPELINE_NAME));
        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_PIPELINE_COUNTER", 1));
        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_PIPELINE_LABEL", 1));
        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_STAGE_NAME", STAGE_NAME));
        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_STAGE_COUNTER", 1));
        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_JOB_NAME", JOB_NAME));
        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_REVISION", 3));
        assertThat(manipulator.consoleOut(), printedEnvVariable("GO_TRIGGER_USER", TRIGGERED_BY_USER));
    }

    @Test
    public void shouldSetEnvironmentVariableForSvnExternal() throws Exception {
        svnRepoFixture.createExternals(svnRepoFixture.getEnd2EndRepoUrl());

        command = new SvnCommand(null, svnRepoFixture.getEnd2EndRepoUrl(), null, null, true);
        svnMaterial = SvnMaterial.createSvnMaterialWithMock(command);
        svnMaterial.setFolder("svn-Dir");

        EnvironmentVariableContext environmentVariableContext = doWorkWithMaterials(new Materials(svnMaterial));

        assertThat(environmentVariableContext.getProperty("GO_REVISION_SVN_DIR"), is("4"));
        assertThat(environmentVariableContext.getProperty("GO_REVISION_SVN_DIR_EXTERNAL"), is("4"));
    }

    private BuildAssignment createAssignment() {
        JobPlan plan = new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), -1, new JobIdentifier(PIPELINE_NAME, 1, "1", STAGE_NAME, "1", JOB_NAME, 123L), null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        MaterialRevisions materialRevisions = materialRevisions();
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, TRIGGERED_BY_USER);
        List<Builder> builders = new ArrayList<Builder>();
        builders.add(new CommandBuilder("ant", "", dir, new RunIfConfigs(), new NullBuilder(), ""));
        return BuildAssignment.create(plan, buildCause, builders, dir);
    }

    private void setupHgRepo() throws IOException {
        hgTestRepo = new HgTestRepo("hgTestRepo1");
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl(), "hg_Dir");
    }

    private MaterialRevisions materialRevisions() {
        MaterialRevision svnRevision = new MaterialRevision(this.svnMaterial,
                ModificationsMother.oneModifiedFile(
                        svnRepoFixture.getHeadRevision(svnRepoFixture.getEnd2EndRepoUrl())));

        SvnMaterial svnMaterialForExternal = SvnMaterial.createSvnMaterialWithMock(new SvnCommand(null, svnRepoFixture.getExternalRepoUrl()));
        String folder = this.svnMaterial.getFolder() == null ? "external" : this.svnMaterial.getFolder() + "/" + "external";
        svnMaterialForExternal.setFolder(folder);
        MaterialRevision svnExternalRevision = new MaterialRevision(svnMaterialForExternal,
                ModificationsMother.oneModifiedFile(
                        svnRepoFixture.getHeadRevision(svnRepoFixture.getExternalRepoUrl())));

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

    private EnvironmentVariableContext doWorkWithMaterials(Materials materials) {
        pipelineConfig.setMaterialConfigs(materials.convertToConfigs());

        BuildAssignment buildAssigment = createAssignment();
        BuildWork work = new BuildWork(buildAssigment);
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(agentIdentifier, new FakeBuildRepositoryRemote(),
                new GoArtifactsManipulatorStub(),
                environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
        return environmentVariableContext;
    }
}
