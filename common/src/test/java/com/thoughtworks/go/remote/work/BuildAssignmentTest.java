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

import com.google.gson.Gson;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.SecretParams;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.thoughtworks.go.helper.MaterialsMother.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@EnableRuleMigrationSupport
public class BuildAssignmentTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String JOB_NAME = "one";
    private static final String STAGE_NAME = "first";
    private static final String PIPELINE_NAME = "cruise";
    private static final String TRIGGERED_BY_USER = "approver";
    private SvnCommand command;
    private HgTestRepo hgTestRepo;
    private HgMaterial hgMaterial;
    private SvnMaterial svnMaterial;
    private DependencyMaterial dependencyMaterial;
    private DependencyMaterial dependencyMaterialWithName;
    private SvnTestRepo svnRepoFixture;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        svnRepoFixture = new SvnTestRepo(tempDir);

        command = new SvnCommand(null, svnRepoFixture.end2endRepositoryUrl());
        svnMaterial = new SvnMaterial(command);
        dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream1"), new CaseInsensitiveString(STAGE_NAME));
        dependencyMaterialWithName = new DependencyMaterial(new CaseInsensitiveString("upstream2"), new CaseInsensitiveString(STAGE_NAME));
        dependencyMaterialWithName.setName(new CaseInsensitiveString("dependency_material_name"));
        setupHgRepo();
    }

    @AfterEach
    void teardown() {
        TestRepo.internalTearDown();
        hgTestRepo.tearDown();
    }

    @Test
    void shouldInitializeEnvironmentContextFromJobPlan() {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");

        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("key1", "value1");
        variables.add("key2", "value2");

        defaultJobPlan.setVariables(variables);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, null, new ArtifactStores());
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size()).isEqualTo(9);
        assertThat(context.getProperty("key1")).isEqualTo("value1");
        assertThat(context.getProperty("key2")).isEqualTo("value2");
    }

    @Test
    void shouldInitializeEnvironmentContextFromJobPlanWithTriggerVariablesOverridingEnvVariablesFromJob() {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");
        EnvironmentVariables triggerVariables = new EnvironmentVariables();
        triggerVariables.add("key1", "override");
        triggerVariables.add("key3", "value3");

        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("key1", "value1");
        variables.add("key2", "value2");

        defaultJobPlan.setTriggerVariables(triggerVariables);
        defaultJobPlan.setVariables(variables);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, null, new ArtifactStores());
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size()).isEqualTo(9);
        assertThat(context.getProperty("key1")).isEqualTo("override");
        assertThat(context.getProperty("key2")).isEqualTo("value2");
    }

    @Test
    void shouldIntializeEnvironmentContextWithJobPlanEnvironmentVariablesOveridingEnvVariablesFromTheEnvironment() {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");

        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("key1", "value_from_job_plan");
        variables.add("key2", "value2");

        defaultJobPlan.setVariables(variables);

        EnvironmentVariableContext contextFromEnvironment = new EnvironmentVariableContext("key1", "value_from_environment");
        contextFromEnvironment.setProperty("key3", "value3", false);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, contextFromEnvironment, new ArtifactStores());
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size()).isEqualTo(10);
        assertThat(context.getProperty("key1")).isEqualTo("value_from_job_plan");
        assertThat(context.getProperty("key2")).isEqualTo("value2");
        assertThat(context.getProperty("key3")).isEqualTo("value3");
    }

    @Test
    void shouldNotHaveReferenceToModifiedFilesSinceLargeCommitsCouldCauseBothServerAndAgentsToRunOutOfMemory_MoreoverThisInformationIsNotRequiredOnAgentSide() {
        List<Modification> modificationsForSvn = ModificationsMother.multipleModificationList();
        List<Modification> modificationsForHg = ModificationsMother.multipleModificationList();
        MaterialRevision svn = new MaterialRevision(svnMaterial(), modificationsForSvn);
        MaterialRevision hg = new MaterialRevision(hgMaterial(), modificationsForHg);
        MaterialRevisions materialRevisions = new MaterialRevisions(svn, hg);
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "user1");

        BuildAssignment buildAssignment = BuildAssignment.create(jobForPipeline("foo"), buildCause, new ArrayList<>(), null, null, new ArtifactStores());

        assertThat(buildAssignment.getBuildApprover()).isEqualTo("user1");
        assertThat(buildAssignment.materialRevisions().getRevisions().size()).isEqualTo(materialRevisions.getRevisions().size());
        assertRevisions(buildAssignment, svn);
        assertRevisions(buildAssignment, hg);
    }

    @Test
    void shouldCopyAdditionalDataToBuildAssignment() {
        MaterialRevision packageMaterialRevision = ModificationsMother.createPackageMaterialRevision("revision");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        String additionalDataAsString = new Gson().toJson(additionalData);
        packageMaterialRevision.getModifications().first().setAdditionalData(additionalDataAsString);
        MaterialRevisions materialRevisions = new MaterialRevisions(packageMaterialRevision);
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "user1");

        BuildAssignment buildAssignment = BuildAssignment.create(jobForPipeline("foo"), buildCause, new ArrayList<>(), null, null, new ArtifactStores());

        assertThat(buildAssignment.getBuildApprover()).isEqualTo("user1");
        assertThat(buildAssignment.materialRevisions().getRevisions().size()).isEqualTo(materialRevisions.getRevisions().size());
        assertRevisions(buildAssignment, packageMaterialRevision);
        Modification actualModification = buildAssignment.materialRevisions().getRevisions().get(0).getModification(0);
        assertThat(actualModification.getAdditionalData()).isEqualTo(additionalDataAsString);
        assertThat(actualModification.getAdditionalDataMap()).isEqualTo(additionalData);
    }

    @Test
    void shouldSetUpGoGeneratedEnvironmentContextCorrectly() throws Exception {
        new SystemEnvironment().setProperty("serviceUrl", "some_random_place");
        BuildAssignment buildAssigment = createAssignment(null);
        EnvironmentVariableContext environmentVariableContext = buildAssigment.initialEnvironmentVariableContext();
        assertThat(environmentVariableContext.getProperty("GO_REVISION")).isEqualTo("3");
        assertThat(environmentVariableContext.getProperty("GO_PIPELINE_NAME")).isEqualTo(PIPELINE_NAME);
        assertThat(environmentVariableContext.getProperty("GO_PIPELINE_LABEL")).isEqualTo("1");
        assertThat(environmentVariableContext.getProperty("GO_STAGE_NAME")).isEqualTo(STAGE_NAME);
        assertThat(environmentVariableContext.getProperty("GO_STAGE_COUNTER")).isEqualTo("1");
        assertThat(environmentVariableContext.getProperty("GO_JOB_NAME")).isEqualTo(JOB_NAME);
        assertThat(environmentVariableContext.getProperty("GO_TRIGGER_USER")).isEqualTo(TRIGGERED_BY_USER);
    }

    @Nested
    class HasSecretParams {

        @Test
        void shouldBeFalseWhenNoneOfTheEnvironmentVariablesIsDefinedAsSecretParam() throws IOException {
            BuildAssignment buildAssigment = createAssignment(null);

            boolean result = buildAssigment.hasSecretParams();

            assertThat(result).isFalse();
        }

        @Test
        void shouldBeTrueWhenOneOfTheEnvironmentVariableIsDefinedAsSecretParam() throws IOException {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Token", "{{SECRET:[secret_config_id][token]}}", false);
            BuildAssignment buildAssigment = createAssignment(environmentVariableContext);

            boolean result = buildAssigment.hasSecretParams();

            assertThat(result).isTrue();
        }
    }

    @Nested
    class GetSecretParams {
        @Test
        void shouldReturnEmptyIfNoneOfTheEnvironmentVariablesIsDefinedAsSecretParam() throws IOException {
            BuildAssignment buildAssigment = createAssignment(null);

            SecretParams secretParams = buildAssigment.getSecretParams();

            assertThat(secretParams).isEmpty();
        }

        @Test
        void shouldReturnSecretParamsIfTheEnvironmentVariablesIsDefinedAsSecretParam() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Token", "{{SECRET:[secret_config_id][token]}}", false);

            ScmMaterial gitMaterial = gitMaterial("https://example.org");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][GIT_PASSWORD]}}");
            MaterialRevision gitRevision = new MaterialRevision(gitMaterial, new Modification());
            BuildCause buildCause = BuildCause.createManualForced(new MaterialRevisions(gitRevision), Username.ANONYMOUS);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext, buildCause);

            SecretParams secretParams = buildAssigment.getSecretParams();

            assertThat(secretParams)
                    .hasSize(2)
                    .contains(new SecretParam("secret_config_id", "token"),
                            new SecretParam("secret_config_id", "GIT_PASSWORD"));
        }

        @Test
        void shouldIgnoreIfMaterialHasNoSecretParam() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Version", "1.0.0", false);

            ScmMaterial gitMaterial = gitMaterial("https://example.org");
            MaterialRevision gitRevision = new MaterialRevision(gitMaterial, new Modification());
            BuildCause buildCause = BuildCause.createManualForced(new MaterialRevisions(gitRevision), Username.ANONYMOUS);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext, buildCause);

            assertThat(buildAssigment.hasSecretParams()).isFalse();
        }

        @Test
        void shouldIgnoreTheSecretParamsInPluggableMaterial() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Token", "{{SECRET:[secret_config_id][token]}}", false);

            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][token]}}");
            k1.getSecretParams().get(0).setValue("resolved-value");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            PluggableSCMMaterial pluggableSCMMaterial = pluggableSCMMaterial("scm-id", "scm-name", k1, k2);
            MaterialRevision gitRevision = new MaterialRevision(pluggableSCMMaterial, new Modification());
            BuildCause buildCause = BuildCause.createManualForced(new MaterialRevisions(gitRevision), Username.ANONYMOUS);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext, buildCause);

            assertThat(buildAssigment.hasSecretParams()).isTrue();
            assertThat(buildAssigment.getSecretParams())
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "token"));
        }

        @Test
        void shouldIgnoreTheSecretParamsInPackageMaterial() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Token", "{{SECRET:[secret_config_id][token]}}", false);

            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][token]}}");
            k1.getSecretParams().get(0).setValue("resolved-value");
            PackageMaterial packageMaterial = packageMaterial();
            MaterialRevision gitRevision = new MaterialRevision(packageMaterial, new Modification());
            BuildCause buildCause = BuildCause.createManualForced(new MaterialRevisions(gitRevision), Username.ANONYMOUS);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext, buildCause);

            assertThat(buildAssigment.hasSecretParams()).isTrue();
            assertThat(buildAssigment.getSecretParams())
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "token"));
        }
    }

    private BuildAssignment createAssignment(EnvironmentVariableContext environmentVariableContext, BuildCause buildCause) {
        JobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(), -1, new JobIdentifier(PIPELINE_NAME, 1, "1", STAGE_NAME, "1", JOB_NAME, 123L), null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        List<Builder> builders = new ArrayList<>();
        builders.add(new CommandBuilder("ls", "", null, new RunIfConfigs(), new NullBuilder(), ""));
        return BuildAssignment.create(plan, buildCause, builders, null, environmentVariableContext, new ArtifactStores());
    }

    private BuildAssignment createAssignment(EnvironmentVariableContext environmentVariableContext) throws IOException {
        MaterialRevisions materialRevisions = materialRevisions();
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, TRIGGERED_BY_USER);
        return createAssignment(environmentVariableContext, buildCause);
    }

    private MaterialRevisions materialRevisions() throws IOException {
        MaterialRevision svnRevision = new MaterialRevision(this.svnMaterial,
                ModificationsMother.oneModifiedFile(svnRepoFixture.end2ndRepositoryLatestRevision()));

        SvnMaterial svnMaterialForExternal = new SvnMaterial(new SvnCommand(null, svnRepoFixture.projectRepositoryUrl()));
        String folder = this.svnMaterial.getFolder() == null ? "external" : this.svnMaterial.getFolder() + "/" + "external";
        svnMaterialForExternal.setFolder(folder);
        MaterialRevision svnExternalRevision = new MaterialRevision(svnMaterialForExternal,
                ModificationsMother.oneModifiedFile(svnRepoFixture.latestRevision()));

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
        assertThat(actualRevision.getMaterial()).isEqualTo(expectedRevision.getMaterial());
        assertThat(actualRevision.getModifications().size()).isEqualTo(expectedRevision.getModifications().size());
        for (int i = 0; i < actualRevision.getModifications().size(); i++) {
            final Modification actualModification = actualRevision.getModifications().get(i);
            final Modification expectedModification = expectedRevision.getModifications().get(i);
            assertThat(actualModification.getRevision()).isEqualTo(expectedModification.getRevision());
            assertThat(actualModification.getModifiedFiles().isEmpty()).isTrue();
        }
    }

    private DefaultJobPlan jobForPipeline(String pipelineName) {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), 1L, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
    }

    private void setupHgRepo() throws IOException {
        hgTestRepo = new HgTestRepo("hgTestRepo1", temporaryFolder);
        hgMaterial = hgMaterial(hgTestRepo.projectRepositoryUrl(), "hg_Dir");
    }

}
