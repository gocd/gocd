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

package com.thoughtworks.go.remote.work;

import com.google.gson.Gson;
import com.thoughtworks.go.config.ArtifactPropertiesGenerators;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.utils.SvnRepoFixture;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.thoughtworks.go.config.materials.svn.SvnMaterial.createSvnMaterialWithMock;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class BuildAssignmentTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String JOB_NAME = "one";
    private static final String STAGE_NAME = "first";
    private static final String PIPELINE_NAME = "cruise";
    private static final String TRIGGERED_BY_USER = "approver";
    private File dir;
    private SvnCommand command;
    private HgTestRepo hgTestRepo;
    private HgMaterial hgMaterial;
    private SvnMaterial svnMaterial;
    private DependencyMaterial dependencyMaterial;
    private DependencyMaterial dependencyMaterialWithName;
    private SvnRepoFixture svnRepoFixture;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        dir = temporaryFolder.newFolder("someFolder");
        svnRepoFixture = new SvnRepoFixture("../common/test-resources/unit/data/svnrepo");
        svnRepoFixture.createRepository();
        command = new SvnCommand(null, svnRepoFixture.getEnd2EndRepoUrl());
        svnMaterial = createSvnMaterialWithMock(command);
        dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream1"), new CaseInsensitiveString(STAGE_NAME));
        dependencyMaterialWithName = new DependencyMaterial(new CaseInsensitiveString("upstream2"), new CaseInsensitiveString(STAGE_NAME));
        dependencyMaterialWithName.setName(new CaseInsensitiveString("dependency_material_name"));
        setupHgRepo();
    }

    @After
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
        hgTestRepo.tearDown();
    }

    @Test
    public void shouldInitializeEnvironmentContextFromJobPlan() throws Exception {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");

        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        variables.add("key1", "value1");
        variables.add("key2", "value2");

        defaultJobPlan.setVariables(variables);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, null);
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size(), is(9));
        assertThat(context.getProperty("key1"), is("value1"));
        assertThat(context.getProperty("key2"), is("value2"));
    }

    @Test
    public void shouldInitializeEnvironmentContextFromJobPlanWithTriggerVariablesOverridingEnvVariablesFromJob() throws Exception {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");
        EnvironmentVariablesConfig triggerVariables = new EnvironmentVariablesConfig();
        triggerVariables.add("key1", "override");
        triggerVariables.add("key3", "value3");

        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        variables.add("key1", "value1");
        variables.add("key2", "value2");

        defaultJobPlan.setTriggerVariables(triggerVariables);
        defaultJobPlan.setVariables(variables);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, null);
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size(), is(9));
        assertThat(context.getProperty("key1"), is("override"));
        assertThat(context.getProperty("key2"), is("value2"));
    }

    @Test
    public void shouldIntializeEnvironmentContextWithJobPlanEnvironmentVariablesOveridingEnvVariablesFromTheEnvironment() throws Exception {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");

        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        variables.add("key1", "value_from_job_plan");
        variables.add("key2", "value2");

        defaultJobPlan.setVariables(variables);

        EnvironmentVariableContext contextFromEnvironment = new EnvironmentVariableContext("key1", "value_from_environment");
        contextFromEnvironment.setProperty("key3", "value3", false);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, contextFromEnvironment);
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size(), is(10));
        assertThat(context.getProperty("key1"), is("value_from_job_plan"));
        assertThat(context.getProperty("key2"), is("value2"));
        assertThat(context.getProperty("key3"), is("value3"));
    }

    @Test
    public void shouldNotHaveReferenceToModifiedFilesSinceLargeCommitsCouldCauseBothServerAndAgentsToRunOutOfMemory_MoreoverThisInformationIsNotRequiredOnAgentSide() {
        List<Modification> modificationsForSvn = ModificationsMother.multipleModificationList();
        List<Modification> modificationsForHg = ModificationsMother.multipleModificationList();
        MaterialRevision svn = new MaterialRevision(MaterialsMother.svnMaterial(), modificationsForSvn);
        MaterialRevision hg = new MaterialRevision(MaterialsMother.hgMaterial(), modificationsForHg);
        MaterialRevisions materialRevisions = new MaterialRevisions(svn, hg);
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "user1");

        BuildAssignment buildAssignment = BuildAssignment.create(jobForPipeline("foo"), buildCause, new ArrayList<>(), null, null);

        assertThat(buildAssignment.getBuildApprover(), is("user1"));
        assertThat(buildAssignment.materialRevisions().getRevisions().size(), is(materialRevisions.getRevisions().size()));
        assertRevisions(buildAssignment, svn);
        assertRevisions(buildAssignment, hg);
    }

    @Test
    public void shouldCopyAdditionalDataToBuildAssignment() {
        MaterialRevision packageMaterialRevision = ModificationsMother.createPackageMaterialRevision("revision");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        String additionalDataAsString = new Gson().toJson(additionalData);
        packageMaterialRevision.getModifications().first().setAdditionalData(additionalDataAsString);
        MaterialRevisions materialRevisions = new MaterialRevisions(packageMaterialRevision);
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "user1");

        BuildAssignment buildAssignment = BuildAssignment.create(jobForPipeline("foo"), buildCause, new ArrayList<>(), null, null);

        assertThat(buildAssignment.getBuildApprover(), is("user1"));
        assertThat(buildAssignment.materialRevisions().getRevisions().size(), is(materialRevisions.getRevisions().size()));
        assertRevisions(buildAssignment, packageMaterialRevision);
        Modification actualModification = buildAssignment.materialRevisions().getRevisions().get(0).getModification(0);
        assertThat(actualModification.getAdditionalData(), is(additionalDataAsString));
        assertThat(actualModification.getAdditionalDataMap(), is(additionalData));
    }

    @Test
    public void shouldSetUpGoGeneratedEnvironmentContextCorrectly() throws Exception {
        new SystemEnvironment().setProperty("serviceUrl", "some_random_place");
        BuildAssignment buildAssigment = createAssignment(null);
        EnvironmentVariableContext environmentVariableContext = buildAssigment.initialEnvironmentVariableContext();
        assertThat(environmentVariableContext.getProperty("GO_REVISION"), Matchers.is("3"));
        assertThat(environmentVariableContext.getProperty("GO_PIPELINE_NAME"), Matchers.is(PIPELINE_NAME));
        assertThat(environmentVariableContext.getProperty("GO_PIPELINE_LABEL"), Matchers.is("1"));
        assertThat(environmentVariableContext.getProperty("GO_STAGE_NAME"), Matchers.is(STAGE_NAME));
        assertThat(environmentVariableContext.getProperty("GO_STAGE_COUNTER"), Matchers.is("1"));
        assertThat(environmentVariableContext.getProperty("GO_JOB_NAME"), Matchers.is(JOB_NAME));
        assertThat(environmentVariableContext.getProperty("GO_TRIGGER_USER"), Matchers.is(TRIGGERED_BY_USER));
    }

    private BuildAssignment createAssignment(EnvironmentVariableContext environmentVariableContext) {
        JobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArtifactPropertiesGenerators(), -1, new JobIdentifier(PIPELINE_NAME, 1, "1", STAGE_NAME, "1", JOB_NAME, 123L), null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        MaterialRevisions materialRevisions = materialRevisions();
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, TRIGGERED_BY_USER);
        List<Builder> builders = new ArrayList<>();
        builders.add(new CommandBuilder("ls", "", dir, new RunIfConfigs(), new NullBuilder(), ""));
        return BuildAssignment.create(plan, buildCause, builders, dir, environmentVariableContext);
    }

    private MaterialRevisions materialRevisions() {
        MaterialRevision svnRevision = new MaterialRevision(this.svnMaterial,
                ModificationsMother.oneModifiedFile(
                        svnRepoFixture.getHeadRevision(svnRepoFixture.getEnd2EndRepoUrl())));

        SvnMaterial svnMaterialForExternal = createSvnMaterialWithMock(new SvnCommand(null, svnRepoFixture.getExternalRepoUrl()));
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


    private void assertRevisions(BuildAssignment buildAssignment, MaterialRevision expectedRevision) {
        MaterialRevision actualRevision = buildAssignment.materialRevisions().findRevisionFor(expectedRevision.getMaterial());
        assertThat(actualRevision.getMaterial(), is(expectedRevision.getMaterial()));
        assertThat(actualRevision.getModifications().size(), is(expectedRevision.getModifications().size()));
        for (int i = 0; i < actualRevision.getModifications().size(); i++) {
            final Modification actualModification = actualRevision.getModifications().get(i);
            final Modification expectedModification = expectedRevision.getModifications().get(i);
            assertThat(actualModification.getRevision(), is(expectedModification.getRevision()));
            assertThat(actualModification.getModifiedFiles().isEmpty(), is(true));
        }
    }

    private DefaultJobPlan jobForPipeline(String pipelineName) {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArtifactPropertiesGenerators(), 1L, jobIdentifier, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
    }

    private void setupHgRepo() throws IOException {
        hgTestRepo = new HgTestRepo("hgTestRepo1");
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl(), "hg_Dir");
    }

}
