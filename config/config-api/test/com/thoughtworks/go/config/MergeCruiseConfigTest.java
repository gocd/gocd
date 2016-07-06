package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.*;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.helper.PartialConfigMother.createRepoOrigin;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static com.thoughtworks.go.util.ArrayUtil.asList;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class MergeCruiseConfigTest extends CruiseConfigTestBase {

    @Before
    public void setup() throws Exception {
        pipelines = new BasicPipelineConfigs("existing_group", new Authorization());
        cruiseConfig = new BasicCruiseConfig(new BasicCruiseConfig(pipelines), createPartial());
        goConfigMother = new GoConfigMother();
    }

    @Override
    protected BasicCruiseConfig createCruiseConfig(BasicPipelineConfigs pipelineConfigs) {
        return new BasicCruiseConfig(new BasicCruiseConfig(pipelineConfigs),
                // we append one more, remote pipeline in the same group as requested local ones to make test use MergePipelineConfigs
                PartialConfigMother.withPipelineInGroup("remote-pipe-1", pipelineConfigs.getGroup()));
    }

    @Override
    protected BasicCruiseConfig createCruiseConfig() {
        return new BasicCruiseConfig(new BasicCruiseConfig(), new PartialConfig());
    }

    @Test
    public void shouldMergeWhenSameEnvironmentExistsInManyPartials(){
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2");
        ConfigRepoConfig repoConfig1 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url1"), "plugin");
        ConfigRepoConfig repoConfig2 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url2"), "plugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig1, repoConfig2));
        PartialConfig partialConfigInRepo1 = PartialConfigMother.withEnvironment("environment", new RepoConfigOrigin(repoConfig1, "repo1_r1"));
        RepoConfigOrigin configOrigin = new RepoConfigOrigin(repoConfig2, "repo2_r1");
        PartialConfig partialConfigInRepo2 = PartialConfigMother.withEnvironment("environment", configOrigin);
        BasicEnvironmentConfig environment2InRepo2 = EnvironmentConfigMother.environment("environment2_in_repo2");
        environment2InRepo2.setOrigins(configOrigin);
        partialConfigInRepo2.getEnvironments().add(environment2InRepo2);
        cruiseConfig.merge(asList(partialConfigInRepo2, partialConfigInRepo1), false);
        assertThat(cruiseConfig.getEnvironments().hasEnvironmentNamed(new CaseInsensitiveString("environment")),is(true));
        assertThat(cruiseConfig.getEnvironments().hasEnvironmentNamed(new CaseInsensitiveString("environment2_in_repo2")),is(true));
    }

    @Test
    public void mergeShouldThrowWhenCalledSecondTime() {
        cruiseConfig = new BasicCruiseConfig(new BasicCruiseConfig(pipelines), PartialConfigMother.withEnvironment("remote-env"));
        assertThat(cruiseConfig.getEnvironments().size(),is(1));
        try {
            cruiseConfig.merge(Arrays.asList(PartialConfigMother.withEnvironment("remote-env")), false);
        }
        catch (RuntimeException ex)
        {
            //ok
            assertThat(cruiseConfig.getEnvironments().size(),is(1));
            cruiseConfig.validateAfterPreprocess();
            return;
        }
        fail("should have thrown");
    }

    @Test
    public void shouldReturnRemoteOriginOfTheGroup()
    {
        assertThat(cruiseConfig.findGroup("remote_group").getOrigin(), Is.<ConfigOrigin>is(createRepoOrigin()));
    }

    @Test
    public void getAllLocalPipelineConfigs_shouldReturnOnlyLocalPipelinesWhenRemoteExist()
    {
        PipelineConfig pipeline1 = createPipelineConfig("local-pipe-1", "stage1");
        cruiseConfig.getGroups().addPipeline("existing_group", pipeline1);

        List<PipelineConfig> localPipelines = cruiseConfig.getAllLocalPipelineConfigs(false);
        assertThat(localPipelines.size(),is(1));
        assertThat(localPipelines,hasItem(pipeline1));
    }

    @Test
    public void getAllLocalPipelineConfigs_shouldReturnEmptyListWhenNoLocalPipelines()
    {
        List<PipelineConfig> localPipelines = cruiseConfig.getAllLocalPipelineConfigs(false);
        assertThat(localPipelines.size(),is(0));
    }

    @Test
    public void getAllLocalPipelineConfigs_shouldExcludePipelinesReferencedByRemoteEnvironmentWhenRequested()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("local-pipeline-1"));
        cruiseConfig = new BasicCruiseConfig(pipelines);
        ConfigReposConfig reposConfig = new ConfigReposConfig();
        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(new GitMaterialConfig("http://git"), "myplug");
        reposConfig.add(configRepoConfig);
        cruiseConfig.setConfigRepos(reposConfig);
        PartialConfig partialConfig = PartialConfigMother.withPipelineInGroup("remote-pipeline-1", "g2");

        BasicEnvironmentConfig remoteEnvironment = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        remoteEnvironment.setOrigins(new RepoConfigOrigin());
        // remote environment declares a local pipeline as member
        remoteEnvironment.addPipeline(new CaseInsensitiveString("local-pipeline-1"));
        partialConfig.getEnvironments().add(remoteEnvironment);
        partialConfig.setOrigins(new RepoConfigOrigin(configRepoConfig,"123"));

        cruiseConfig.merge(Arrays.asList(partialConfig),true);
        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("local-pipeline-1")),is(true));

        List<PipelineConfig> localPipelines = cruiseConfig.getAllLocalPipelineConfigs(true);
        assertThat(localPipelines.size(),is(0));
    }

    @Test
    public void shouldGetPipelinesWithGroupName() throws Exception {
        PipelineConfig pipeline1 = createPipelineConfig("pipeline1", "stage1");
        cruiseConfig.getGroups().addPipeline("existing_group", pipeline1);

        assertThat(cruiseConfig.pipelines("existing_group"), hasItem(pipeline1));
        assertThat(cruiseConfig.pipelines("remote_group").hasPipeline(new CaseInsensitiveString("remote-pipe-1")), is(true));
    }

    @Test
    public void shouldReturnTrueForPipelineThatInFirstGroup_WhenFirstGroupIsLocal() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        CruiseConfig config = new BasicCruiseConfig(new BasicCruiseConfig(group1), new PartialConfig());
        assertThat("shouldReturnTrueForPipelineThatInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline1")), is(true));
    }

    @Test
    public void shouldReturnTrueForPipelineThatInFirstGroup_WhenFirstGroupIsRemote() {
        CruiseConfig config = new BasicCruiseConfig(new BasicCruiseConfig(),
                PartialConfigMother.withPipelineInGroup("remote-pipe-1", "remote_group"));
        assertThat("shouldReturnTrueForPipelineThatInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("remote-pipe-1")), is(true));
    }

    @Test
    public void shouldReturnFalseForPipelineThatNotInFirstGroup_WhenSecondGroupIsLocal() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        PipelineConfigs group2 = createGroup("group2", createPipelineConfig("pipeline2", "stage2"));
        CruiseConfig config = new BasicCruiseConfig(new BasicCruiseConfig(group1, group2), new PartialConfig());
        assertThat("shouldReturnFalseForPipelineThatNotInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline2")), is(false));
    }

    @Test
    public void shouldReturnFalseForPipelineThatNotInFirstGroup_WhenSecondGroupIsRemote() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        CruiseConfig config = new BasicCruiseConfig(new BasicCruiseConfig(group1),
                PartialConfigMother.withPipelineInGroup("remote-pipe-1", "remote_group"));
        assertThat("shouldReturnFalseForPipelineThatNotInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline2")), is(false));
    }

    @Test
    public void shouldGenerateAMapOfAllPipelinesAndTheirParentDependencies_WhenAllPipelinesInMapAreLocal() {
        /*
        *    -----+ p2 --> p4
        *  p1
        *    -----+ p3
        *
        * */
        PipelineConfig p1 = createPipelineConfig("p1", "s1", "j1");
        PipelineConfig p2 = createPipelineConfig("p2", "s2", "j1");
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p3 = createPipelineConfig("p3", "s3", "j1");
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p4 = createPipelineConfig("p4", "s4", "j1");
        p4.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"), new CaseInsensitiveString("s2")));
        pipelines.addAll(Arrays.asList(p4, p2, p1, p3));
        cruiseConfig = new BasicCruiseConfig(new BasicCruiseConfig(pipelines),
                PartialConfigMother.withPipelineInGroup("remote-pipe-1", "remote_group"));
        Map<String, List<PipelineConfig>> expectedPipelines = cruiseConfig.generatePipelineVsDownstreamMap();
        assertThat(expectedPipelines.size(), is(5));
        assertThat(expectedPipelines.get("p1"), hasItems(p2, p3));
        assertThat(expectedPipelines.get("p2"), hasItems(p4));
        assertThat(expectedPipelines.get("p3").isEmpty(), is(true));
        assertThat(expectedPipelines.get("p4").isEmpty(), is(true));
        assertThat(expectedPipelines.get("remote-pipe-1").isEmpty(), is(true));
    }

    @Test
    public void shouldGenerateAMapOfAllPipelinesAndTheirParentDependencies_WhenThereAreRemotePipelinesInMap() {
        /*
        *    -----+ p2 --> p4
        *  p1
        *    -----+ p3 --> remote-pipe-1
        *
        * */
        PipelineConfig p1 = createPipelineConfig("p1", "s1", "j1");
        PipelineConfig p2 = createPipelineConfig("p2", "s2", "j1");
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p3 = createPipelineConfig("p3", "s3", "j1");
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p4 = createPipelineConfig("p4", "s4", "j1");
        p4.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"), new CaseInsensitiveString("s2")));
        pipelines.addAll(Arrays.asList(p4, p2, p1, p3));

        PipelineConfig remotePipe1 = createPipelineConfig("remote-pipe-1", "s5", "j1");
        remotePipe1.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p3"), new CaseInsensitiveString("s3")));
        PartialConfig part = new PartialConfig();
        part.getGroups().addPipeline("remoteGroup", remotePipe1);
        cruiseConfig = new BasicCruiseConfig(new BasicCruiseConfig(pipelines), part);
        Map<String, List<PipelineConfig>> expectedPipelines = cruiseConfig.generatePipelineVsDownstreamMap();
        assertThat(expectedPipelines.size(), is(5));
        assertThat(expectedPipelines.get("p1"), hasItems(p2, p3));
        assertThat(expectedPipelines.get("p2"), hasItems(p4));
        assertThat(expectedPipelines.get("p3"), hasItems(remotePipe1));
        assertThat(expectedPipelines.get("remote-pipe-1").isEmpty(), is(true));
        assertThat(expectedPipelines.get("p4").isEmpty(), is(true));
    }


    @Test
    public void shouldCollectOriginErrorsFromEnvironments_InMergedConfig() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        PartialConfig partialConfig = PartialConfigMother.withPipelineInGroup("pipe2", "g2");
        partialConfig.getGroups().get(0).get(0).setOrigin(new RepoConfigOrigin());
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig, partialConfig);
        BasicEnvironmentConfig uat = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uat.addPipeline(new CaseInsensitiveString("pipe2"));
        cruiseConfig.addEnvironment(uat);

        List<ConfigErrors> allErrors = cruiseConfig.validateAfterPreprocess();
        assertThat(allErrors.size(), is(1));
        assertNotNull(allErrors.get(0).on("origin"));
    }

    @Test
    public void shouldCollectOriginErrorsFromMaterialConfigs_InMergedConfig() {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        PartialConfig partialConfig = PartialConfigMother.withPipelineInGroup("pipe2", "g2");
        partialConfig.getGroups().get(0).get(0).setOrigin(new RepoConfigOrigin());
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig, partialConfig);
        PipelineConfig pipeline1 = goConfigMother.addPipeline(cruiseConfig, "pipeline1", "stage", "build");
        PipelineConfig pipeline2 = PipelineConfigMother.createPipelineConfigWithStage("pipeline2", "stage");
        pipeline2.setOrigin(new RepoConfigOrigin());
        partialConfig.getGroups().addPipeline("g2", pipeline2);

        goConfigMother.setDependencyOn(cruiseConfig, pipeline1, "pipeline2", "stage");

        List<ConfigErrors> allErrors = cruiseConfig.validateAfterPreprocess();
        assertThat(allErrors.size(), is(1));
        assertNotNull(allErrors.get(0).on("origin"));
    }

    @Test
    public void shouldCollectAllTheErrorsInTheChildren_InMergedConfig() {
        BasicCruiseConfig mainCruiseConfig = GoConfigMother.configWithPipelines("pipeline-1");
        PartialConfig partialConfig = PartialConfigMother.withPipelineInGroup("pipe2", "g2");
        partialConfig.getGroups().get(0).get(0).setOrigin(new RepoConfigOrigin());
        CruiseConfig config = new BasicCruiseConfig(mainCruiseConfig, partialConfig);

        shouldCollectAllTheErrorsInTheChilderHelper(config);
    }

    @Test
    public void shouldCollectPipelineNameConflictErrorsInTheChildren_InMergedConfig_WhenPipelinesIn2Groups() {
        BasicCruiseConfig mainCruiseConfig = GoConfigMother.configWithPipelines("pipeline-1");
        PartialConfig partialConfig = PartialConfigMother.withPipelineInGroup("pipeline-1", "g2");
        partialConfig.setOrigin(new RepoConfigOrigin());
        CruiseConfig config = new BasicCruiseConfig(mainCruiseConfig, partialConfig);

        List<ConfigErrors> allErrors = config.validateAfterPreprocess();
        assertThat(allErrors.size(), is(2));
        assertThat(allErrors.get(0).on("name"), is("You have defined multiple pipelines named 'pipeline-1'. Pipeline names must be unique. Source(s): [http://some.git at 1234fed, cruise-config.xml]"));
        assertThat(allErrors.get(1).on("name"), is("You have defined multiple pipelines named 'pipeline-1'. Pipeline names must be unique. Source(s): [http://some.git at 1234fed, cruise-config.xml]"));
    }

    @Test
    public void shouldCollectPipelineNameConflictErrorsInTheChildren_InMergedConfig2_WhenPipelinesInDefaultGroup() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipeline1");
        // pipeline1 is in xml and in config repo - this is an error at merged scope
        PartialConfig remotePart = PartialConfigMother.withPipelineInGroup("pipeline1", "defaultGroup");
        remotePart.setOrigin(new RepoConfigOrigin());
        BasicCruiseConfig merged = new BasicCruiseConfig((BasicCruiseConfig) cruiseConfig, remotePart);
        List<ConfigErrors> allErrors = merged.validateAfterPreprocess();
        assertThat(remotePart.getGroups().get(0).getPipelines().get(0).errors().size(), is(1));
        assertThat(allErrors.size(), is(2));
        assertThat(allErrors.get(0).on("name"), is("You have defined multiple pipelines named 'pipeline1'. Pipeline names must be unique. Source(s): [http://some.git at 1234fed, cruise-config.xml]"));
        assertThat(allErrors.get(1).on("name"), is("You have defined multiple pipelines named 'pipeline1'. Pipeline names must be unique. Source(s): [http://some.git at 1234fed, cruise-config.xml]"));
    }

    @Test
    public void shouldCollectPipelineNameConflictErrorsInTheChildren_InMergedConfig_WhenCloned() {
        //we need this case because cloning has proven to be problematic with complex object graph in merged config
        BasicCruiseConfig mainCruiseConfig = GoConfigMother.configWithPipelines("pipeline-1");
        PartialConfig partialConfig = PartialConfigMother.withPipelineInGroup("pipeline-1", "g2");
        partialConfig.setOrigin(new RepoConfigOrigin());
        CruiseConfig config = new BasicCruiseConfig(mainCruiseConfig, partialConfig);
        Cloner CLONER = new Cloner();
        CruiseConfig cloned = CLONER.deepClone(config);

        List<ConfigErrors> allErrors = cloned.validateAfterPreprocess();
        assertThat(allErrors.size(), is(2));
        assertThat(allErrors.get(0).on("name"), is("You have defined multiple pipelines named 'pipeline-1'. Pipeline names must be unique. Source(s): [http://some.git at 1234fed, cruise-config.xml]"));
        assertThat(allErrors.get(1).on("name"), is("You have defined multiple pipelines named 'pipeline-1'. Pipeline names must be unique. Source(s): [http://some.git at 1234fed, cruise-config.xml]"));
    }

    @Test
    public void shouldReturnGroupsOtherThanMain_WhenMerged() {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));
        assertNotSame(mainCruiseConfig.getGroups(), cruiseConfig.getGroups());
    }

    @Test
    public void shouldReturnTrueHasPipelinesFrom2Parts() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));

        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe2")), is(true));
    }

    @Test
    public void shouldReturnFalseWhenHasNotPipelinesFrom2Parts() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));

        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")), is(false));
    }

    @Test
    public void shouldReturnGroupsFrom2Parts() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "g2"));

        assertThat(cruiseConfig.hasPipelineGroup("g2"), is(true));
    }

    @Test
    public void addPipeline_shouldAddPipelineToMain() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        pipelines.setOrigin(new FileConfigOrigin());
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));
        cruiseConfig.addPipeline("group_main", PipelineConfigMother.pipelineConfig("pipe3"));

        assertThat(mainCruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")), is(true));
        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")), is(true));

    }

    @Test
    public void addPipelineWithoutValidation_shouldAddPipelineToMain() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        pipelines.setOrigin(new FileConfigOrigin());
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));
        cruiseConfig.addPipelineWithoutValidation("group_main", PipelineConfigMother.pipelineConfig("pipe3"));

        assertThat(mainCruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")), is(true));
        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")), is(true));

    }

    @Test
    public void shouldgetAllPipelineNamesFromAllParts() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "g2"), PartialConfigMother.withPipelineInGroup("pipe3", "g3"));

        assertThat(cruiseConfig.getAllPipelineNames(), contains(
                new CaseInsensitiveString("pipe1"),
                new CaseInsensitiveString("pipe2"),
                new CaseInsensitiveString("pipe3")));
    }

    @Test
    public void createsMergePipelineConfigsOnlyWhenManyParts() {
        assertThat(cruiseConfig.getGroups().get(0) instanceof MergePipelineConfigs, is(false));

        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "existing_group"));
        assertThat(cruiseConfig.getGroups().get(0) instanceof MergePipelineConfigs, is(true));

    }

    @Test
    public void shouldGetUniqueMaterialsWithConfigRepos() {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        ConfigReposConfig reposConfig = new ConfigReposConfig();
        GitMaterialConfig configRepo = new GitMaterialConfig("http://git");
        reposConfig.add(new ConfigRepoConfig(configRepo, "myplug"));
        mainCruiseConfig.setConfigRepos(reposConfig);

        PartialConfig partialConfig = PartialConfigMother.withPipeline("pipe2");
        MaterialConfig pipeRepo = partialConfig.getGroups().get(0).get(0).materialConfigs().get(0);

        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig, partialConfig);

        Set<MaterialConfig> materials = cruiseConfig.getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos();
        assertThat(materials, hasItem(configRepo));
        assertThat(materials, hasItem(pipeRepo));
        assertThat(materials.size(), is(2));
    }

    @Test
    public void shouldGetUniqueMaterialsWithoutConfigRepos() {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        ConfigReposConfig reposConfig = new ConfigReposConfig();
        GitMaterialConfig configRepo = new GitMaterialConfig("http://git");
        reposConfig.add(new ConfigRepoConfig(configRepo, "myplug"));
        mainCruiseConfig.setConfigRepos(reposConfig);

        PartialConfig partialConfig = PartialConfigMother.withPipeline("pipe2");
        MaterialConfig pipeRepo = partialConfig.getGroups().get(0).get(0).materialConfigs().get(0);

        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig, partialConfig);

        Set<MaterialConfig> materials = cruiseConfig.getAllUniqueMaterialsBelongingToAutoPipelines();
        assertThat(materials, hasItem(pipeRepo));
        assertThat(materials.size(), is(1));
    }

    @Test
    public void shouldUpdatePipelineConfigsListWhenAPartialIsMerged(){
        cruiseConfig = new BasicCruiseConfig(pipelines);
        PartialConfig partial = PartialConfigMother.withPipeline("pipeline3");
        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(new GitMaterialConfig("http://git"), "myplug");
        partial.setOrigins(new RepoConfigOrigin(configRepoConfig,"123"));
        ConfigReposConfig reposConfig = new ConfigReposConfig();
        reposConfig.add(configRepoConfig);
        cruiseConfig.setConfigRepos(reposConfig);

        cruiseConfig.merge(Arrays.asList(partial), false);
        PipelineConfig pipeline3 = partial.getGroups().first().findBy(new CaseInsensitiveString("pipeline3"));
        assertThat(cruiseConfig.getAllPipelineConfigs().contains(pipeline3), is(true));
        assertThat(cruiseConfig.getAllPipelineNames().contains(pipeline3.name()), is(true));
    }
}
