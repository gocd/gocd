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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMProperty;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.materials.StaleMaterialsOnBuildCause;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(ClearSingleton.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ScheduledPipelineLoaderIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private ScheduledPipelineLoader loader;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private JobInstanceService jobInstanceService;
    @Autowired
    private MaterialExpansionService materialExpansionService;
    @Autowired
    private ConsoleService consoleService;
    @Autowired
    private InstanceFactory instanceFactory;
    @Autowired
    private StageService stageService;

    GoConfigFileHelper configHelper;
    private SvnTestRepoWithExternal svnRepo;

    @BeforeEach
    public void setup(@TempDir Path tempDir) throws Exception {
        configHelper = new GoConfigFileHelper(goConfigDao);
        dbHelper.onSetUp();
        goCache.clear();
        configHelper.onSetUp();
        svnRepo = new SvnTestRepoWithExternal(tempDir);
        cleanupTempFolders();
    }

    private void cleanupTempFolders() {
        FileUtils.deleteQuietly(new File("data/console"));
        FileUtils.deleteQuietly(new File("logs"));
        FileUtils.deleteQuietly(new File("pipelines"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
                cleanupTempFolders();
    }

    @Test
    public void shouldLoadPipelineAlongWithBuildCauseHavingMaterialPasswordsPopulated() {
        JobConfig jobConfig = new JobConfig("job-one");
        jobConfig.addTask(new AntTask());
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("last", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig)));
        pipelineConfig.materialConfigs().clear();
        SvnMaterial onDirOne = MaterialsMother.svnMaterial("google.com", "dirOne", "loser", "boozer", false, "**/*.html");
        P4Material onDirTwo = MaterialsMother.p4Material("host:987654321", "zoozer", "secret", "through-the-window", true);
        onDirTwo.setFolder("dirTwo");
        pipelineConfig.addMaterialConfig(onDirOne.config());
        pipelineConfig.addMaterialConfig(onDirTwo.config());

        configHelper.addPipeline(pipelineConfig);

        Pipeline building = PipelineMother.building(pipelineConfig);
        Pipeline pipeline = dbHelper.savePipelineWithStagesAndMaterials(building);

        final long jobId = pipeline.getStages().get(0).getJobInstances().get(0).getId();
        Pipeline loadedPipeline = loader.pipelineWithPasswordAwareBuildCauseByBuildId(jobId);

        MaterialRevisions revisions = loadedPipeline.getBuildCause().getMaterialRevisions();
        assertThat(((SvnMaterial) revisions.findRevisionFor(onDirOne).getMaterial()).getPassword()).isEqualTo("boozer");
        assertThat(((P4Material) revisions.findRevisionFor(onDirTwo).getMaterial()).getPassword()).isEqualTo("secret");
    }

    @Test
    public void shouldUpdateScmConfigurationOfPluggableScmMaterialsOnPipeline() {
        String jobName = "job-one";
        PipelineConfig pipelineConfig = setupPipelineWithScmMaterial("pipeline_with_pluggable_scm_mat", "stage", jobName);
        final Pipeline previousSuccessfulBuildWithOlderScmConfig = simulateSuccessfulPipelineRun(pipelineConfig);
        PipelineConfig updatedPipelineConfig = configHelper.updatePipeline(pipelineConfig.name(), config -> {
            PluggableSCMMaterialConfig materialConfig = (PluggableSCMMaterialConfig) config.materialConfigs().first();
            materialConfig.getSCMConfig().getConfiguration().getProperty("password").setConfigurationValue(new ConfigurationValue("new_value"));
        });

        final long jobId = rerunJob(jobName, pipelineConfig, previousSuccessfulBuildWithOlderScmConfig);

        Pipeline loadedPipeline = loader.pipelineWithPasswordAwareBuildCauseByBuildId(jobId);

        MaterialRevisions revisions = loadedPipeline.getBuildCause().getMaterialRevisions();
        Configuration updatedConfiguration = ((PluggableSCMMaterial) revisions.findRevisionFor(updatedPipelineConfig.materialConfigs().first()).getMaterial()).getScmConfig().getConfiguration();
        assertThat(updatedConfiguration.size()).isEqualTo(2);
        assertThat(updatedConfiguration.getProperty("password").getConfigurationValue()).isEqualTo(new ConfigurationValue("new_value"));
    }

    @Test
    public void shouldUpdatePackageMaterialConfigurationOfMaterialsOnPipeline() {
        String jobName = "job-one";
        PipelineConfig pipelineConfig = setupPipelineWithPackageMaterial("pipeline_with_pluggable_scm_mat", "stage", jobName);
        final Pipeline previousSuccessfulBuildWithOlderPackageConfig = simulateSuccessfulPipelineRun(pipelineConfig);
        PipelineConfig updatedPipelineConfig = configHelper.updatePipeline(pipelineConfig.name(), config -> {
            PackageMaterialConfig materialConfig = (PackageMaterialConfig) config.materialConfigs().first();
            materialConfig.getPackageDefinition().getConfiguration().getProperty("package-key2").setConfigurationValue(new ConfigurationValue("package-updated-value"));
            materialConfig.getPackageDefinition().getRepository().getConfiguration().getProperty("repo-key2").setConfigurationValue(new ConfigurationValue("repo-updated-value"));
        });
        final long jobId = rerunJob(jobName, pipelineConfig, previousSuccessfulBuildWithOlderPackageConfig);
        Pipeline loadedPipeline = loader.pipelineWithPasswordAwareBuildCauseByBuildId(jobId);

        MaterialRevisions revisions = loadedPipeline.getBuildCause().getMaterialRevisions();
        PackageMaterial updatedMaterial = (PackageMaterial) revisions.findRevisionFor(updatedPipelineConfig.materialConfigs().first()).getMaterial();
        Configuration updatedConfiguration = updatedMaterial.getPackageDefinition().getConfiguration();
        assertThat(updatedConfiguration.size()).isEqualTo(2);
        assertThat(updatedConfiguration.getProperty("package-key2").getConfigurationValue()).isEqualTo(new ConfigurationValue("package-updated-value"));
        assertThat(updatedMaterial.getPackageDefinition().getRepository().getConfiguration().size()).isEqualTo(2);
        assertThat(updatedMaterial.getPackageDefinition().getRepository().getConfiguration().getProperty("repo-key2").getConfigurationValue()).isEqualTo(new ConfigurationValue("repo-updated-value"));
    }

    private long rerunJob(String jobName, PipelineConfig pipelineConfig, Pipeline previousSuccessfulBuildWithOlderPackageConfig) {
        Stage stage = instanceFactory.createStageForRerunOfJobs(previousSuccessfulBuildWithOlderPackageConfig.getFirstStage(), List.of(jobName), new DefaultSchedulingContext(), pipelineConfig.getFirstStageConfig(), new TimeProvider(), configHelper.currentConfig().getMd5());
        stage = stageService.save(previousSuccessfulBuildWithOlderPackageConfig, stage);
        return stage.getFirstJob().getId();
    }

    private PipelineConfig setupPipelineWithScmMaterial(String pipelineName, String stageName, String jobName) {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = MaterialConfigsMother.pluggableSCMMaterialConfigWithConfigProperties("url", "password");
        SCMPropertyConfiguration configuration = new SCMPropertyConfiguration();
        configuration.add(new SCMProperty("url", null).with(PackageConfiguration.PART_OF_IDENTITY, true));
        configuration.add(new SCMProperty("password", null).with(PackageConfiguration.PART_OF_IDENTITY, false));
        SCMMetadataStore.getInstance().addMetadataFor(pluggableSCMMaterialConfig.getPluginId(), new SCMConfigurations(configuration), null);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName, stageName, new MaterialConfigs(pluggableSCMMaterialConfig), jobName);
        configHelper.addSCMConfig(pluggableSCMMaterialConfig.getSCMConfig());
        configHelper.addPipeline(pipelineConfig);
        return pipelineConfig;
    }

    private Pipeline simulateSuccessfulPipelineRun(PipelineConfig pipelineConfig) {
        final Pipeline previousSuccessfulBuildWithOlderPackageConfig = PipelineMother.completed(pipelineConfig);
        dbHelper.savePipelineWithStagesAndMaterials(previousSuccessfulBuildWithOlderPackageConfig);
        return previousSuccessfulBuildWithOlderPackageConfig;
    }

    private PipelineConfig setupPipelineWithPackageMaterial(String pipelineName, String stageName, String jobName) {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("p-id");
        Configuration repoConfig = new Configuration(ConfigurationPropertyMother.create("repo-key1", false, "repo-k1-value"), ConfigurationPropertyMother.create("repo-key2", false, "repo-k2-value"));
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", repoConfig);
        Configuration packageConfig = new Configuration(ConfigurationPropertyMother.create("package-key1", false, "package-key1-value"), ConfigurationPropertyMother.create("package-key2", false, "package-key2-value"));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "package-name", packageConfig, repository);
        packageMaterialConfig.setPackageDefinition(packageDefinition);
        repository.getPackages().add(packageDefinition);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName, stageName, new MaterialConfigs(packageMaterialConfig), jobName);
        configHelper.addPackageDefinition(packageMaterialConfig);
        configHelper.addPipeline(pipelineConfig);
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(new PackageConfiguration("package-key1").with(PackageConfiguration.PART_OF_IDENTITY, true));
        packageConfigurations.add(new PackageConfiguration("package-key2").with(PackageConfiguration.PART_OF_IDENTITY, false));
        PackageMetadataStore.getInstance().addMetadataFor(packageMaterialConfig.getPluginId(), packageConfigurations);
        PackageConfigurations configuration = new PackageConfigurations();
        configuration.add(new PackageConfiguration("repo-key1").with(PackageConfiguration.PART_OF_IDENTITY, true));
        configuration.add(new PackageConfiguration("repo-key2").with(PackageConfiguration.PART_OF_IDENTITY, false));
        RepositoryMetadataStore.getInstance().addMetadataFor(packageMaterialConfig.getPluginId(), configuration);
        return pipelineConfig;
    }

    @Test
    public void shouldSetAServerHealthMessageWhenMaterialForPipelineWithBuildCauseIsNotFound() throws IllegalArtifactLocationException, IOException {
        JobConfig jobConfig = new JobConfig("job-one");
        jobConfig.addTask(new AntTask());
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("last", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig)));
        pipelineConfig.materialConfigs().clear();
        SvnMaterialConfig onDirOne = MaterialConfigsMother.svnMaterialConfig("google.com", "dirOne", "loser", "boozer", false, "**/*.html");
        final P4MaterialConfig onDirTwo = MaterialConfigsMother.p4MaterialConfig("host:987654321", "zoozer", "secret", "through-the-window", true);
        onDirTwo.setConfigAttributes(Map.of(ScmMaterialConfig.FOLDER, "dirTwo"));
        pipelineConfig.addMaterialConfig(onDirOne);
        pipelineConfig.addMaterialConfig(onDirTwo);

        configHelper.addPipeline(pipelineConfig);

        Pipeline building = PipelineMother.building(pipelineConfig);
        final Pipeline pipeline = dbHelper.savePipelineWithStagesAndMaterials(building);

        CruiseConfig cruiseConfig = configHelper.currentConfig();
        PipelineConfig cfg = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("last"));
        cfg.removeMaterialConfig(cfg.materialConfigs().get(1));
        configHelper.writeConfigFile(cruiseConfig);

        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPipeline("last")).size()).isEqualTo(0);

        final long jobId = pipeline.getStages().get(0).getJobInstances().get(0).getId();

        Instant currentTime = Instant.now();

        Pipeline loadedPipeline = null;
        try {
            loadedPipeline = loader.pipelineWithPasswordAwareBuildCauseByBuildId(jobId);
            fail("should not have loaded pipeline with build-cause as one of the necessary materials was not found");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(StaleMaterialsOnBuildCause.class);
            assertThat(e.getMessage()).isEqualTo("Cannot load job 'last/" + pipeline.getCounter() + "/stage/1/job-one' because material " + onDirTwo + " was not found in config.");
        }

        assertThat(loadedPipeline).isNull();

        JobInstance reloadedJobInstance = jobInstanceService.buildById(jobId);
        assertThat(reloadedJobInstance.getState()).isEqualTo(JobState.Completed);
        assertThat(reloadedJobInstance.getResult()).isEqualTo(JobResult.Failed);

        HealthStateScope scope = HealthStateScope.forJob("last", "stage", "job-one");
        assertThat(serverHealthService.logsSortedForScope(scope).size()).isEqualTo(1);
        ServerHealthState error = serverHealthService.logsSortedForScope(HealthStateScope.forJob("last", "stage", "job-one")).get(0);
        assertThat(error).isEqualTo(ServerHealthState.error("Cannot load job 'last/" + pipeline.getCounter() + "/stage/1/job-one' because material " + onDirTwo + " was not found in config.", "Job for pipeline 'last/" + pipeline.getCounter() + "/stage/1/job-one' has been failed as one or more material configurations were either changed or removed.", HealthStateType.general(HealthStateScope.forJob("last", "stage", "job-one"))));
        Instant expiryTime = ReflectionUtil.getField(error, "expiryTime");
        assertThat(expiryTime.isAfter(currentTime)).isTrue();
        assertThat(expiryTime.isBefore(currentTime.plus(5, HOURS))).isTrue();

        String logText = Files.readString(consoleService.consoleLogFile(reloadedJobInstance.getIdentifier()).toPath(), UTF_8);
        assertThat(logText).contains("Cannot load job 'last/" + pipeline.getCounter() + "/stage/1/job-one' because material " + onDirTwo + " was not found in config.");
        assertThat(logText).contains("Job for pipeline 'last/" + pipeline.getCounter() + "/stage/1/job-one' has been failed as one or more material configurations were either changed or removed.");
    }

    @Test//if other materials have expansion concept at some point, add more tests here
    public void shouldSetPasswordForExpandedSvnMaterial() {
        JobConfig jobConfig = new JobConfig("job-one");
        jobConfig.addTask(new AntTask());
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("last", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig)));
        pipelineConfig.materialConfigs().clear();
        SvnMaterialConfig materialConfig = svnRepo.materialConfig();
        materialConfig.setConfigAttributes(Map.of(SvnMaterialConfig.CHECK_EXTERNALS, String.valueOf(true)));
        materialConfig.setPassword("boozer");
        pipelineConfig.addMaterialConfig(materialConfig);
        configHelper.addPipeline(pipelineConfig);

        Pipeline loadedPipeline = createAndLoadModifyOneFilePipeline(pipelineConfig);

        MaterialRevisions revisions = loadedPipeline.getBuildCause().getMaterialRevisions();
        assertThat(revisions.getRevisions().size()).isEqualTo(2);
        assertThat(((SvnMaterial) revisions.getRevisions().get(0).getMaterial()).getPassword()).isEqualTo("boozer");
        assertThat(((SvnMaterial) revisions.getRevisions().get(1).getMaterial()).getPassword()).isEqualTo("boozer");
    }

    @Test
    public void shouldLoadShallowCloneFlagForGitMaterialsBaseOnTheirOwnPipelineConfig(@TempDir Path tempDir) throws IOException {
        GitTestRepo testRepo = new GitTestRepo(tempDir);

        JobConfig jobConfig = new JobConfig("job-one");
        jobConfig.addTask(new AntTask());

        PipelineConfig shallowPipeline = PipelineConfigMother.pipelineConfig("shallowPipeline", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig)));
        shallowPipeline.materialConfigs().clear();
        shallowPipeline.addMaterialConfig(git(testRepo.projectRepositoryUrl(), true));
        configHelper.addPipeline(shallowPipeline);

        PipelineConfig fullPipeline = PipelineConfigMother.pipelineConfig("fullPipeline", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig)));
        fullPipeline.materialConfigs().clear();
        fullPipeline.addMaterialConfig(git(testRepo.projectRepositoryUrl(), false));
        configHelper.addPipeline(fullPipeline);

        Pipeline shallowPipelineInstance = createAndLoadModifyOneFilePipeline(shallowPipeline);
        MaterialRevisions shallowRevisions = shallowPipelineInstance.getBuildCause().getMaterialRevisions();
        assertThat(((GitMaterial) shallowRevisions.getRevisions().get(0).getMaterial()).isShallowClone()).isTrue();

        Pipeline fullPipelineInstance = createAndLoadModifyOneFilePipeline(fullPipeline);
        MaterialRevisions fullRevisions = fullPipelineInstance.getBuildCause().getMaterialRevisions();
        assertThat(((GitMaterial) fullRevisions.getRevisions().get(0).getMaterial()).isShallowClone()).isFalse();
    }

    private Pipeline createAndLoadModifyOneFilePipeline(PipelineConfig pipelineConfig) {
        MaterialConfigs expandedConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());
        MaterialRevisions materialRevisions = ModificationsMother.modifyOneFile(new MaterialConfigConverter().toMaterials(expandedConfigs));
        Pipeline building = PipelineMother.buildingWithRevisions(pipelineConfig, materialRevisions);
        Pipeline pipeline = dbHelper.savePipelineWithStagesAndMaterials(building);
        final long jobId = pipeline.getStages().get(0).getJobInstances().get(0).getId();
        return loader.pipelineWithPasswordAwareBuildCauseByBuildId(jobId);
    }
}
