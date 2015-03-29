package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.config.ArtifactCleanupStrategy;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.artifactcleanup.ArtifactCleanupExtension;
import com.thoughtworks.go.plugin.access.artifactcleanup.ArtifactExtensionStageConfiguration;
import com.thoughtworks.go.plugin.access.artifactcleanup.ArtifactExtensionStageInstance;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ArtifactCleanupExtensionInvokerTest {

    private ArtifactCleanupExtension artifactCleanupExtension;
    private GoConfigService goConfigService;
    private ArtifactsService artifactsService;
    private ArtifactCleanupExtensionInvoker artifactCleanupExtensionInvoker;
    private PluginConfiguration globalPlugin;
    private PluginConfiguration pluginOne;
    private PluginConfiguration pluginTwo;
    private CruiseConfig cruiseConfig;

    @Before
    public void setUp() throws Exception {
        artifactCleanupExtension = mock(ArtifactCleanupExtension.class);
        goConfigService = mock(GoConfigService.class);
        artifactsService = mock(ArtifactsService.class);
        cruiseConfig = new CruiseConfig();
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

        globalPlugin = new PluginConfiguration("global", "1.0");
        pluginOne = new PluginConfiguration("local-one", "1.0");
        pluginTwo = new PluginConfiguration("local-two", "1.0");

        artifactCleanupExtensionInvoker = new ArtifactCleanupExtensionInvoker(artifactCleanupExtension, goConfigService, artifactsService);
    }

    @Test
    public void shouldBuildArtifactCleanupPluginMapFromConfig() throws Exception {
        cruiseConfig.server().setArtifactCleanupStrategy(new ArtifactCleanupStrategy(globalPlugin));
        cruiseConfig.addPipeline("gOne", pipelineOne());
        cruiseConfig.addPipeline("gTwo", pipelineTwo());


        Map<PluginConfiguration, List<StageConfigIdentifier>> result = artifactCleanupExtensionInvoker.updatedPluginMap();

        assertThat(result.get(globalPlugin), hasItems(new StageConfigIdentifier("p1", "s-one"), new StageConfigIdentifier("p2", "s-one")));
        assertThat(result.get(pluginOne), hasItems(new StageConfigIdentifier("p1", "s-two")));
        assertThat(result.get(pluginTwo), hasItems(new StageConfigIdentifier("p2", "s-two")));
    }

    @Test
    public void shouldInvokeExtensionForStageLevelPlugins() throws Exception {
        cruiseConfig.server().setArtifactCleanupStrategy(new ArtifactCleanupStrategy(globalPlugin));

        cruiseConfig.addPipeline("gOne", pipelineOne());

        cruiseConfig.addPipeline("gTwo", pipelineTwo());

        ArtifactExtensionStageConfiguration stageOne = new ArtifactExtensionStageConfiguration("p1", "s-two");
        List<ArtifactExtensionStageInstance> stageOneInstanceList = asList(new ArtifactExtensionStageInstance(1, "p1", 1, "s-two", "1"));
        ArtifactExtensionStageConfiguration stageTwo = new ArtifactExtensionStageConfiguration("p2", "s-two");
        List<ArtifactExtensionStageInstance> stageTwoInstanceList = asList(new ArtifactExtensionStageInstance(1, "p2", 1, "s-two", "1"));


        doReturn(stageOneInstanceList).when(artifactCleanupExtension).getStageInstancesForArtifactCleanup(pluginOne.getId(), asList(stageOne));
        doReturn(stageTwoInstanceList).when(artifactCleanupExtension).getStageInstancesForArtifactCleanup(pluginTwo.getId(), asList(stageTwo));

        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        artifactCleanupExtensionInvoker.invokeStageLevelArtifactCleanupPlugins();

        verify(artifactCleanupExtensionInvoker).cleanupArtifacts(pluginOne, stageOneInstanceList);
        verify(artifactCleanupExtensionInvoker).cleanupArtifacts(pluginTwo, stageTwoInstanceList);
    }


    @Test
    public void shouldInvokeExtensionForGlobalPlugin() throws Exception {
        cruiseConfig.server().setArtifactCleanupStrategy(new ArtifactCleanupStrategy(globalPlugin));

        cruiseConfig.addPipeline("gOne", pipelineOne());

        cruiseConfig.addPipeline("gTwo", pipelineTwo());

        ArtifactExtensionStageConfiguration stageOne = new ArtifactExtensionStageConfiguration("p1", "s-one");
        ArtifactExtensionStageConfiguration stageTwo = new ArtifactExtensionStageConfiguration("p2", "s-one");
        List<ArtifactExtensionStageInstance> stageInstanceList = asList(new ArtifactExtensionStageInstance(1, "p1", 1, "s-two", "1"));


        doReturn(stageInstanceList).when(artifactCleanupExtension).getStageInstancesForArtifactCleanup(globalPlugin.getId(), asList(stageTwo, stageOne));


        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        artifactCleanupExtensionInvoker.invokeGlobalArtifactCleanupPlugin();

        verify(artifactCleanupExtensionInvoker).cleanupArtifacts(globalPlugin, stageInstanceList);
    }

    @Test
    public void shouldBuildArtifactCleanupPluginMap() throws Exception {

        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        Map<PluginConfiguration, List<StageConfigIdentifier>> expected = new HashMap<PluginConfiguration, List<StageConfigIdentifier>>();
        doReturn(expected).when(artifactCleanupExtensionInvoker).updatedPluginMap();

        Map<PluginConfiguration, List<StageConfigIdentifier>> actual = artifactCleanupExtensionInvoker.buildArtifactCleanupPluginMap();

        assertThat(actual, is(expected));
        verify(artifactCleanupExtensionInvoker).updatedPluginMap();
    }

    @Test
    public void shouldNotBuildArtifactCleanupPluginMapWhenAvailable() throws Exception {
        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        Map<PluginConfiguration, List<StageConfigIdentifier>> expected = new HashMap<PluginConfiguration, List<StageConfigIdentifier>>();
        doReturn(expected).when(artifactCleanupExtensionInvoker).updatedPluginMap();

        artifactCleanupExtensionInvoker.buildArtifactCleanupPluginMap();
        artifactCleanupExtensionInvoker.buildArtifactCleanupPluginMap();

        verify(artifactCleanupExtensionInvoker, times(1)).updatedPluginMap();
    }

    @Test
    public void shouldNotCleanupArtifactsWhenPluginNotConfiguredToHandleStage() throws Exception {
        ArtifactExtensionStageInstance instance = new ArtifactExtensionStageInstance(1, "pipeline", 1, "stage", "1");

        Map<PluginConfiguration,List<StageConfigIdentifier>> pluginConfigurationMap = new HashMap<PluginConfiguration, List<StageConfigIdentifier>>();
        pluginConfigurationMap.put(pluginTwo,asList(new StageConfigIdentifier("pipeline","stage")));
        pluginConfigurationMap.put(pluginOne,new ArrayList<StageConfigIdentifier>());

        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        doReturn(pluginConfigurationMap).when(artifactCleanupExtensionInvoker).buildArtifactCleanupPluginMap();

        artifactCleanupExtensionInvoker.cleanupArtifacts(pluginOne, asList(instance));

        verifyZeroInteractions(artifactsService);
    }

    @Test
    public void shouldCleanupArtifacts() throws Exception {

        ArtifactExtensionStageInstance instance = new ArtifactExtensionStageInstance(1, "pipeline", 1, "stage", "1");
        List<ArtifactExtensionStageInstance> stageInstanceList = asList(instance);

        Map<PluginConfiguration,List<StageConfigIdentifier>> pluginConfigurationMap = new HashMap<PluginConfiguration, List<StageConfigIdentifier>>();
        pluginConfigurationMap.put(pluginOne,asList(new StageConfigIdentifier("pipeline","stage")));

        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        doReturn(pluginConfigurationMap).when(artifactCleanupExtensionInvoker).buildArtifactCleanupPluginMap();

        artifactCleanupExtensionInvoker.cleanupArtifacts(pluginOne, stageInstanceList);

        ArgumentCaptor<Stage> stageArgumentCaptor = ArgumentCaptor.forClass(Stage.class);

        verify(artifactsService).purgeArtifactsForStage(stageArgumentCaptor.capture());
        assertStageInstance(stageArgumentCaptor.getValue(), 1L, "pipeline", 1, "stage", "1");

    }

    @Test
    public void shouldCleanupArtifactsWithIncludePaths() throws Exception {

        ArtifactExtensionStageInstance instance = new ArtifactExtensionStageInstance(1, "pipeline", 1, "stage", "1");
        instance.getIncludePaths().add("/include/this/path");
        List<ArtifactExtensionStageInstance> stageInstanceList = asList(instance);

        Map<PluginConfiguration,List<StageConfigIdentifier>> pluginConfigurationMap = new HashMap<PluginConfiguration, List<StageConfigIdentifier>>();
        pluginConfigurationMap.put(pluginOne,asList(new StageConfigIdentifier("pipeline","stage")));

        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        doReturn(pluginConfigurationMap).when(artifactCleanupExtensionInvoker).buildArtifactCleanupPluginMap();

        artifactCleanupExtensionInvoker.cleanupArtifacts(pluginOne, stageInstanceList);

        ArgumentCaptor<Stage> stageArgumentCaptor = ArgumentCaptor.forClass(Stage.class);

        verify(artifactsService).purgeArtifactsForStage(stageArgumentCaptor.capture(),eq(asList("/include/this/path")));
        assertStageInstance(stageArgumentCaptor.getValue(), 1L, "pipeline", 1, "stage", "1");

    }

    @Test
    public void shouldCleanupArtifactsWithExcludePaths() throws Exception {

        ArtifactExtensionStageInstance instance = new ArtifactExtensionStageInstance(1, "pipeline", 1, "stage", "1");
        instance.getExcludePaths().add("/exclude/this/path");
        List<ArtifactExtensionStageInstance> stageInstanceList = asList(instance);

        Map<PluginConfiguration,List<StageConfigIdentifier>> pluginConfigurationMap = new HashMap<PluginConfiguration, List<StageConfigIdentifier>>();
        pluginConfigurationMap.put(pluginOne,asList(new StageConfigIdentifier("pipeline","stage")));

        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        doReturn(pluginConfigurationMap).when(artifactCleanupExtensionInvoker).buildArtifactCleanupPluginMap();

        artifactCleanupExtensionInvoker.cleanupArtifacts(pluginOne, stageInstanceList);

        ArgumentCaptor<Stage> stageArgumentCaptor = ArgumentCaptor.forClass(Stage.class);

        verify(artifactsService).purgeArtifactsForStageExcept(stageArgumentCaptor.capture(),eq(asList("/exclude/this/path")));
        assertStageInstance(stageArgumentCaptor.getValue(), 1L, "pipeline", 1, "stage", "1");
    }

    @Test
    public void shouldCleanupArtifactsWithIncludePathsOverExcludePaths() throws Exception {

        ArtifactExtensionStageInstance instance = new ArtifactExtensionStageInstance(1, "pipeline", 1, "stage", "1");
        instance.getIncludePaths().add("/include/this/path");
        instance.getExcludePaths().add("/exclude/this/path");
        List<ArtifactExtensionStageInstance> stageInstanceList = asList(instance);

        Map<PluginConfiguration,List<StageConfigIdentifier>> pluginConfigurationMap = new HashMap<PluginConfiguration, List<StageConfigIdentifier>>();
        pluginConfigurationMap.put(pluginOne,asList(new StageConfigIdentifier("pipeline","stage")));

        artifactCleanupExtensionInvoker = spy(artifactCleanupExtensionInvoker);
        doReturn(pluginConfigurationMap).when(artifactCleanupExtensionInvoker).buildArtifactCleanupPluginMap();

        artifactCleanupExtensionInvoker.cleanupArtifacts(pluginOne, stageInstanceList);


        verify(artifactsService).purgeArtifactsForStage(any(Stage.class),eq(asList("/include/this/path")));
    }

    private void assertStageInstance(Stage stage, long expectedId, String expectedPipeline, int expectedPipelineCounter, String expectedStage, String expectedStageCounter) {
        assertThat(stage.getId(), is(expectedId));
        assertThat(stage.getIdentifier().getPipelineName(), is(expectedPipeline));
        assertThat(stage.getIdentifier().getPipelineCounter(), is(expectedPipelineCounter));
        assertThat(stage.getIdentifier().getStageName(), is(expectedStage));
        assertThat(stage.getIdentifier().getStageCounter(), is(expectedStageCounter));
    }

    private PipelineConfig pipelineOne() {
        return new PipelineConfig(
                new CaseInsensitiveString("p1"),
                new MaterialConfigs(),
                addStageWith("s-one", null),
                addStageWith("s-two", pluginOne));
    }


    private PipelineConfig pipelineTwo() {
        return new PipelineConfig(
                new CaseInsensitiveString("p2"),
                new MaterialConfigs(),
                addStageWith("s-one", null),
                addStageWith("s-two", pluginTwo));
    }

    private StageConfig addStageWith(String name, PluginConfiguration pluginConfiguration) {
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString(name), new JobConfigs());
        if (pluginConfiguration != null) {
            stageConfig.setArtifactCleanupStrategy(new ArtifactCleanupStrategy(pluginConfiguration));
        }
        return stageConfig;
    }
}