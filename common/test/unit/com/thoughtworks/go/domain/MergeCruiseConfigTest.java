package com.thoughtworks.go.domain;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.merge.MergeConfigOrigin;
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;

public class MergeCruiseConfigTest extends CruiseConfigTestBase {

    @Before
    public void setup() throws Exception {
        pipelines = new BasicPipelineConfigs("existing_group", new Authorization());
        cruiseConfig = new BasicCruiseConfig(new BasicCruiseConfig(pipelines),
                PartialConfigMother.withPipelineInGroup("remote-pipe-1", "remote_group"));
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
        return new BasicCruiseConfig(new BasicCruiseConfig(),new PartialConfig());
    }

    @Test
    public void shouldGetPipelinesWithGroupName() throws Exception {
        PipelineConfig pipeline1 = createPipelineConfig("pipeline1", "stage1");
        cruiseConfig.getGroups().addPipeline("existing_group", pipeline1);

        assertThat(cruiseConfig.pipelines("existing_group"), hasItem(pipeline1));
        assertThat(cruiseConfig.pipelines("remote_group").hasPipeline(new CaseInsensitiveString("remote-pipe-1")),is(true));
    }

    @Test
    public void shouldReturnTrueForPipelineThatInFirstGroup_WhenFirstGroupIsLocal() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        CruiseConfig config = new BasicCruiseConfig(new BasicCruiseConfig(group1),new PartialConfig());
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
        CruiseConfig config = new BasicCruiseConfig(new BasicCruiseConfig(group1, group2),new PartialConfig());
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
        part.getGroups().addPipeline("remoteGroup",remotePipe1);
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
        partialConfig.getGroups().addPipeline("g2",pipeline2);

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
        assertThat(allErrors.get(0).on("name"), is("You have defined multiple pipelines named 'pipeline-1'. Pipeline names must be unique."));
        assertThat(allErrors.get(1).on("name"), is("You have defined multiple pipelines named 'pipeline-1'. Pipeline names must be unique."));
    }

    @Test
    public void shouldCollectPipelineNameConflictErrorsInTheChildren_InMergedConfig2_WhenPipelinesInDefaultGroup() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipeline1");
        // pipeline1 is in xml and in config repo - this is an error at merged scope
        PartialConfig remotePart = PartialConfigMother.withPipelineInGroup("pipeline1", "defaultGroup");
        remotePart.setOrigin(new RepoConfigOrigin());
        BasicCruiseConfig merged = new BasicCruiseConfig((BasicCruiseConfig) cruiseConfig, remotePart);
        List<ConfigErrors> allErrors = merged.validateAfterPreprocess();
        assertThat(remotePart.getGroups().get(0).getPipelines().get(0).errors().size(),is(1));
        assertThat(allErrors.size(), is(2));
        assertThat(allErrors.get(0).on("name"), is("You have defined multiple pipelines named 'pipeline1'. Pipeline names must be unique."));
        assertThat(allErrors.get(1).on("name"), is("You have defined multiple pipelines named 'pipeline1'. Pipeline names must be unique."));
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
        assertThat(allErrors.get(0).on("name"), is("You have defined multiple pipelines named 'pipeline-1'. Pipeline names must be unique."));
        assertThat(allErrors.get(1).on("name"), is("You have defined multiple pipelines named 'pipeline-1'. Pipeline names must be unique."));
    }

    @Test
    public void shouldReturnGroupsOtherThanMain_WhenMerged()
    {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));
        assertNotSame(mainCruiseConfig.getGroups(), cruiseConfig.getGroups());
    }

    @Test
    public void shouldReturnTrueHasPipelinesFrom2Parts()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));

        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe1")),is(true));
        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe2")),is(true));
    }
    @Test
    public void shouldReturnFalseWhenHasNotPipelinesFrom2Parts()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));

        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")), is(false));
    }
    @Test
    public void shouldReturnGroupsFrom2Parts()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "g2"));

        assertThat(cruiseConfig.hasPipelineGroup("g2"),is(true));
    }
    @Test
    public void addPipeline_shouldAddPipelineToMain()
    {
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
    public void addPipelineWithoutValidation_shouldAddPipelineToMain()
    {
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
    public void addPipelineWithoutValidation_shouldFailToAddPipelineWhenItExistsInPartialConfig()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        pipelines.setOrigin(new FileConfigOrigin());
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));
        try {
            cruiseConfig.addPipelineWithoutValidation("group_main", PipelineConfigMother.pipelineConfig("pipe2"));
            fail("should have thrown when trying to add pipe2 when it already exists in partial config");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(),containsString("Pipeline called 'pipe2' is already defined in configuration repository"));
        }
    }
    @Test
    public void addPipeline_shouldFailToAddPipelineToMainWhenItExistsInPartialConfig()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        pipelines.setOrigin(new FileConfigOrigin());
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipeline("pipe2"));
        try {
            cruiseConfig.addPipeline("group_main", PipelineConfigMother.pipelineConfig("pipe2"));
            fail("should have thrown when trying to add pipe2 when it already exists in partial config");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(),containsString("Pipeline called 'pipe2' is already defined in configuration repository"));
        }
    }
    @Test
    public void shouldgetAllPipelineNamesFromAllParts()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "g2"),PartialConfigMother.withPipelineInGroup("pipe3", "g3"));

        assertThat(cruiseConfig.getAllPipelineNames(), contains(
                new CaseInsensitiveString("pipe2"),
                new CaseInsensitiveString("pipe1"),
                new CaseInsensitiveString("pipe3")));
    }
    @Test
    public void createsMergePipelineConfigsOnlyWhenManyParts()
    {
        assertThat(cruiseConfig.getGroups().get(0) instanceof MergePipelineConfigs, is(false));

        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "existing_group"));
        assertThat(cruiseConfig.getGroups().get(0) instanceof MergePipelineConfigs, is(true));

    }
    @Test
    public void shouldReturnOriginAsASumOfAllOrigins()
    {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        FileConfigOrigin fileOrigin = new FileConfigOrigin();
        mainCruiseConfig.setOrigins(fileOrigin);

        PartialConfig part = PartialConfigMother.withPipeline("pipe2");
        RepoConfigOrigin repoOrigin = new RepoConfigOrigin();
        part.setOrigin(repoOrigin);
        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,part);

        ConfigOrigin allOrigins = cruiseConfig.getOrigin();
        assertThat(allOrigins instanceof MergeConfigOrigin,is(true));

        MergeConfigOrigin mergeConfigOrigin = (MergeConfigOrigin)allOrigins;
        assertThat(mergeConfigOrigin.size(),is(2));
        assertThat(mergeConfigOrigin.contains(fileOrigin),is(true));
        assertThat(mergeConfigOrigin.contains(repoOrigin),is(true));
    }
    @Test
    public void shouldAddPipelineToNewGroup_InMergeAndLocalScope()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig localCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(localCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "remote_group"));

        PipelineConfig pipe3 = PipelineConfigMother.pipelineConfig("pipe3");
        cruiseConfig.addPipeline("newGroup", pipe3);

        assertThat(cruiseConfig.allPipelines().size(),is(3));
        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")),is(true));

        assertThat(localCruiseConfig.allPipelines().size(),is(2));
        assertThat(localCruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe1")),is(true));
        assertThat(localCruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")),is(true));
        assertThat(localCruiseConfig.pipelines("newGroup").contains(pipe3),is(true));
    }
    @Test
    public void shouldAddPipelineToExistingGroup_InMergeAndLocalScope()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig localCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(localCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "remote_group"));

        PipelineConfig pipe3 = PipelineConfigMother.pipelineConfig("pipe3");
        cruiseConfig.addPipeline("remote_group", pipe3);

        assertThat(cruiseConfig.allPipelines().size(),is(3));
        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")),is(true));

        assertThat(localCruiseConfig.allPipelines().size(),is(2));
        assertThat(localCruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe1")),is(true));
        assertThat(localCruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe3")),is(true));
        assertThat(localCruiseConfig.pipelines("remote_group").contains(pipe3),is(true));
    }
    @Test
    public void shouldFailToAddDuplicatePipelineAlreadyDefinedInConfigRepo()
    {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig localCruiseConfig = new BasicCruiseConfig(pipelines);
        cruiseConfig = new BasicCruiseConfig(localCruiseConfig,
                PartialConfigMother.withPipelineInGroup("pipe2", "remote_group"));

        PipelineConfig pipe2Dup = PipelineConfigMother.pipelineConfig("pipe2");
        try {
            cruiseConfig.addPipeline("doesNotMatterWhichGroup", pipe2Dup);
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(),is("Pipeline called 'pipe2' is already defined in configuration repository http://some.git at 1234fed"));
        }

        assertThat(cruiseConfig.allPipelines().size(),is(2));
        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe2")),is(true));

        assertThat(localCruiseConfig.allPipelines().size(),is(1));
        assertThat(localCruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe1")),is(true));
        assertThat(localCruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe2")),is(false));
    }

    @Test
    public void shouldGetUniqueMaterialsWithConfigRepos()
    {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        ConfigReposConfig reposConfig = new ConfigReposConfig();
        GitMaterialConfig configRepo = new GitMaterialConfig("http://git");
        reposConfig.add(new ConfigRepoConfig(configRepo,"myplug"));
        mainCruiseConfig.setConfigRepos(reposConfig);

        PartialConfig partialConfig = PartialConfigMother.withPipeline("pipe2");
        MaterialConfig pipeRepo = partialConfig.getGroups().get(0).get(0).materialConfigs().get(0);

        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,  partialConfig);

        Set<MaterialConfig> materials = cruiseConfig.getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos();
        assertThat(materials,hasItem(configRepo));
        assertThat(materials,hasItem(pipeRepo));
        assertThat(materials.size(),is(2));
    }
    @Test
    public void shouldGetUniqueMaterialsWithoutConfigRepos()
    {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        ConfigReposConfig reposConfig = new ConfigReposConfig();
        GitMaterialConfig configRepo = new GitMaterialConfig("http://git");
        reposConfig.add(new ConfigRepoConfig(configRepo,"myplug"));
        mainCruiseConfig.setConfigRepos(reposConfig);

        PartialConfig partialConfig = PartialConfigMother.withPipeline("pipe2");
        MaterialConfig pipeRepo = partialConfig.getGroups().get(0).get(0).materialConfigs().get(0);

        cruiseConfig = new BasicCruiseConfig(mainCruiseConfig,  partialConfig);

        Set<MaterialConfig> materials = cruiseConfig.getAllUniqueMaterialsBelongingToAutoPipelines();
        assertThat(materials,hasItem(pipeRepo));
        assertThat(materials.size(),is(1));
    }
}
