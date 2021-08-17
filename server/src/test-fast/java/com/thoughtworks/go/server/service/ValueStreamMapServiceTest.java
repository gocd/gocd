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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.domain.valuestreammap.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.valuestreammap.DownstreamInstancePopulator;
import com.thoughtworks.go.server.valuestreammap.RunStagesPopulator;
import com.thoughtworks.go.server.valuestreammap.UnrunStagesPopulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.thoughtworks.go.domain.valuestreammap.VSMTestHelper.assertDepth;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.ModificationsMother.checkinWithComment;
import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ValueStreamMapServiceTest {
    @Mock
    private PipelineService pipelineService;
    @Mock
    private MaterialRepository materialRepository;
    @Mock(lenient = true)
    private GoConfigService goConfigService;
    @Mock
    private RunStagesPopulator runStagesPopulator;
    @Mock
    private UnrunStagesPopulator unrunStagesPopulator;
    @Mock
    private DownstreamInstancePopulator downstreaminstancepopulator;
    @Mock(lenient = true)
    private SecurityService securityService;

    private Username user;
    private ValueStreamMapService valueStreamMapService;
    private HttpLocalizedOperationResult result;

    @BeforeEach
    public void setUp() throws Exception {
        user = new Username(new CaseInsensitiveString("poovan"));

        setupExistenceOfPipelines("p1", "p2", "p3", "MYPIPELINE");

        setupViewPermissionForPipelines("C", "A", "B", "P1", "P2", "P3", "p1", "p2", "p3", "mypipeline", "MYPIPELINE");

        setupViewPermissionForGroups("g1");

        valueStreamMapService = new ValueStreamMapService(pipelineService, materialRepository, goConfigService, downstreaminstancepopulator, runStagesPopulator, unrunStagesPopulator, securityService);
        result = new HttpLocalizedOperationResult();

        when(goConfigService.findPipelineByName(any())).thenReturn(PipelineConfigMother.pipelineConfig("found-pipeline"));
    }

    private void setupExistenceOfPipelines(String... pipelineNames) {
        for (String pipelineName : pipelineNames) {
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
        }
    }

    private void setupViewPermissionForPipelines(String... pipelineNames) {
        for (String pipelineName : pipelineNames) {
            when(securityService.hasViewPermissionForPipeline(user, pipelineName)).thenReturn(true);
        }
    }

    private void setupViewPermissionForGroups(String... groups) {
        for (String group : groups) {
            when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(user.getUsername()), group)).thenReturn(true);
        }
    }

    @Test
    public void shouldBeCaseInsensitiveWhenGettingPipelineDependencyGraphForAPipeline() {
        /*
         * svn => P1
         * */

        String pipelineName = "myPipeline";
        int counter = 1;

        BuildCause buildCause = PipelineMother.pipeline(pipelineName, new Stage()).getBuildCause();
        MaterialConfig materialConfig = buildCause.getMaterialRevisions().getMaterialRevision(0).getMaterial().config();
        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(pipelineName, new MaterialConfigs(materialConfig));

        when(pipelineService.buildCauseFor(pipelineName, counter)).thenReturn(buildCause);
        when(goConfigService.currentCruiseConfig()).thenReturn(new BasicCruiseConfig(new BasicPipelineConfigs(p1Config)));
        when(pipelineService.findPipelineByNameAndCounter(pipelineName, 1)).thenReturn(new Pipeline("MYPIPELINE", "p1-label", buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("MYPIPELINE"), counter, user, result);

        assertThat(graph.getCurrentPipeline().getName().toString(), is(pipelineName));
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(2));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(-1, firstLevel.get(0), materialConfig.getDisplayName(), materialConfig.getFingerprint(), 0, new CaseInsensitiveString(pipelineName));

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(1));
        assertNode(0, secondLevel.get(0), pipelineName, pipelineName, 0);
    }

    @Test
    public void shouldGetAllDownstreamPipelinesForVSMOfUpstreamPipelineIfDownstreamPipelineRefersToUpstreamDependencyUsingADifferentCase() {
        /*
         * +-------------------------+
         * |                         v
         * g---->myPipeline---->downstream(MYPIPELINE)
         *
         * */
        String pipelineName = "myPipeline";
        String downstreamPipelineName = "downstream";
        String uppercasePipelineName = pipelineName.toUpperCase();
        int counter = 1;

        BuildCause buildCause = PipelineMother.pipeline(pipelineName, new Stage()).getBuildCause();
        MaterialConfig materialConfig1 = buildCause.getMaterialRevisions().getMaterialRevision(0).getMaterial().config();
        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(pipelineName, new MaterialConfigs(materialConfig1));
        PipelineConfig downstreamConfig = PipelineConfigMother.pipelineConfig(downstreamPipelineName, new MaterialConfigs(
                materialConfig1, new DependencyMaterialConfig(
                new CaseInsensitiveString(p1Config.name().toUpper()),
                new CaseInsensitiveString(p1Config.first().name().toUpper()))));

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs("default", new Authorization(), p1Config, downstreamConfig));

        when(pipelineService.buildCauseFor(pipelineName, counter)).thenReturn(buildCause);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByNameAndCounter(pipelineName, 1)).thenReturn(new Pipeline(uppercasePipelineName, "p1-label", buildCause, new EnvironmentVariables()));
        MaterialConfig materialConfig = cruiseConfig.getAllUniqueMaterials().iterator().next();
        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString(pipelineName.toLowerCase()), 1, user, result);

        assertThat(graph.getCurrentPipeline().getName(), is(pipelineName));
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(3));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(-1, firstLevel.get(0), materialConfig.getDisplayName(), materialConfig.getFingerprint(), 0, new CaseInsensitiveString(pipelineName));

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(1));
        assertNode(0, secondLevel.get(0), pipelineName, pipelineName, 0, new CaseInsensitiveString(downstreamPipelineName));

        List<Node> thirdLevel = nodesAtEachLevel.get(2);
        assertThat(thirdLevel.size(), is(1));
        assertNode(1, thirdLevel.get(0), downstreamPipelineName, downstreamPipelineName, 0);
    }

    @Test
    public void shouldGetAllDownstreamPipelinesForVSMOfUpstreamScmMaterialIfDownstreamPipelineRefersToUpstreamDependenciesUsingADifferentCase() {
        String pipelineName = "myPipeline";
        String downstreamPipelineName = "downstream";
        String uppercasePipelineName = pipelineName.toUpperCase();
        int counter = 1;

        BuildCause buildCause = PipelineMother.pipeline(pipelineName, new Stage()).getBuildCause();
        MaterialConfig materialConfig1 = buildCause.getMaterialRevisions().getMaterialRevision(0).getMaterial().config();
        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(pipelineName, new MaterialConfigs(materialConfig1));
        PipelineConfig downstreamConfig = PipelineConfigMother.pipelineConfig(downstreamPipelineName, new MaterialConfigs(
                materialConfig1, new DependencyMaterialConfig(
                new CaseInsensitiveString(p1Config.name().toUpper()),
                new CaseInsensitiveString(p1Config.first().name().toUpper()))));

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs("default", new Authorization(), p1Config, downstreamConfig));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        MaterialConfig materialConfig = cruiseConfig.getAllUniqueMaterials().iterator().next();
        MaterialRevision materialRevision = buildCause.getMaterialRevisions().findRevisionFor(materialConfig);
        Material material = materialRevision.getMaterial();
        String revision = materialRevision.getRevision().getRevision();
        when(materialRepository.findModificationWithRevision(material, revision)).thenReturn(materialRevision.getLatestModification());
        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(materialRevision.getMaterial().createMaterialInstance());
        when(goConfigService.groups()).thenReturn(cruiseConfig.getGroups());
        when(securityService.hasViewPermissionForGroup(user.getUsername().toString(), cruiseConfig.getGroups().first().getGroup())).thenReturn(true);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(materialConfig.getFingerprint(), revision, user, result);
        assertThat(graph.getCurrentMaterial().getName(), is(materialConfig.getDisplayName()));
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(3));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(0, firstLevel.get(0), materialConfig.getDisplayName(), materialConfig.getFingerprint(), 1, new CaseInsensitiveString(pipelineName));

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(2));
        assertNode(1, secondLevel.get(0), pipelineName, pipelineName, 0, new CaseInsensitiveString(downstreamPipelineName));
        Node dummyNode = secondLevel.get(1);
        assertThat(dummyNode.getType(), is(DependencyNodeType.DUMMY));
        assertThat(dummyNode.getLevel(), is(1));
        assertThat(dummyNode.getChildren().size(), is(1));
        VSMTestHelper.assertNodeHasChildren(dummyNode, new CaseInsensitiveString(downstreamPipelineName));

        List<Node> thirdLevel = nodesAtEachLevel.get(2);
        assertThat(thirdLevel.size(), is(1));
        assertNode(2, thirdLevel.get(0), downstreamPipelineName, downstreamPipelineName, 0);
    }

    @Test
    public void shouldGetPipelineDependencyGraphForAPipelineWithNoCrossLevelDependencies() {
        /*
         * svn => P1
         * */

        String pipeline = "P1";
        int counter = 1;
        BuildCause buildCause = PipelineMother.pipeline(pipeline, new Stage()).getBuildCause();
        MaterialConfig material = buildCause.getMaterialRevisions().getMaterialRevision(0).getMaterial().config();


        when(pipelineService.buildCauseFor(pipeline, counter)).thenReturn(buildCause);
        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(pipeline, new MaterialConfigs(material));
        when(goConfigService.currentCruiseConfig()).thenReturn(new BasicCruiseConfig(new BasicPipelineConfigs(p1Config)));
        when(pipelineService.findPipelineByNameAndCounter(pipeline, 1)).thenReturn(new Pipeline(pipeline, "p1-label", buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString(pipeline), counter, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is(pipeline));
        assertThat(nodesAtEachLevel.size(), is(2));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(-1, firstLevel.get(0), material.getDisplayName(), material.getFingerprint(), 0, new CaseInsensitiveString(pipeline));

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(1));
        assertNode(0, secondLevel.get(0), pipeline, pipeline, 0);
    }

    @Test
    public void shouldGetPipelineDependencyGraphForAPipelineWithDiamondDependency() {
        /*
         * |----> P1----->
         * g             |_> p3
         * |             |
         * ---- > P2----->
         *
         * */

        GitMaterial git = new GitMaterial("git");
        BuildCause p3buildCause = createBuildCause(asList("p1", "p2"), new ArrayList<>());
        BuildCause p2buildCause = createBuildCause(new ArrayList<>(), asList(git));
        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));


        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(git.config()));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(git.config()));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByNameAndCounter("p3", 1)).thenReturn(new Pipeline("p3", "p3-label", p3buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p3"), 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is("p3"));
        assertThat(nodesAtEachLevel.size(), is(3));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(-2, firstLevel.get(0), git.getDisplayName(), git.getFingerprint(), 0, new CaseInsensitiveString("p1"), new CaseInsensitiveString("p2"));

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(2));
        assertNode(-1, secondLevel.get(0), "p1", "p1", 0, new CaseInsensitiveString("p3"));
        assertNode(-1, secondLevel.get(1), "p2", "p2", 0, new CaseInsensitiveString("p3"));

        List<Node> thirdLevel = nodesAtEachLevel.get(2);
        assertThat(thirdLevel.size(), is(1));
        assertNode(0, thirdLevel.get(0), "p3", "p3", 0);
    }

    @Test
    public void shouldGetPipelineDependencyGraphForAPipelineWithDiamondDependency_VSMForMaterial() {
        /*
         * |----> P1----->
         * g             |_> p3
         * |             |
         * ---- > P2----->
         *
         * */

        GitMaterial gitMaterial = new GitMaterial("git");
        MaterialConfig gitConfig = gitMaterial.config();
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("git", null, "master", "submodule", "flyweight");
        BuildCause p2buildCause = createBuildCause(new ArrayList<>(), asList(gitMaterial));
        Modification gitModification = p2buildCause.getMaterialRevisions().getRevisions().get(0).getModifications().get(0);
        String gitRevision = gitModification.getRevision();

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs("g1", new Authorization(), p1Config, p2Config, p3Config);
        CruiseConfig cruiseConfig = new BasicCruiseConfig(pipelineConfigs);

        when(goConfigService.groups()).thenReturn(new PipelineGroups(pipelineConfigs));
        when(materialRepository.findMaterialInstance(gitConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.findModificationWithRevision(gitMaterial, gitRevision)).thenReturn(gitModification);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), gitRevision, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline(), is(nullValue()));
        assertThat(graph.getCurrentMaterial().getId().toString(), is(gitMaterial.getFingerprint()));
        assertThat(nodesAtEachLevel.size(), is(3));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(0, firstLevel.get(0), gitMaterial.getDisplayName(), gitMaterial.getFingerprint(), 0, new CaseInsensitiveString("p1"), new CaseInsensitiveString("p2"));
        assertDepth(graph, firstLevel.get(0).getId(), 1);

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(2));
        assertNode(1, secondLevel.get(0), "p1", "p1", 0, new CaseInsensitiveString("p3"));
        assertDepth(graph, secondLevel.get(0).getId(), 1);
        assertNode(1, secondLevel.get(1), "p2", "p2", 0, new CaseInsensitiveString("p3"));
        assertDepth(graph, secondLevel.get(1).getId(), 2);

        List<Node> thirdLevel = nodesAtEachLevel.get(2);
        assertThat(thirdLevel.size(), is(1));
        assertNode(2, thirdLevel.get(0), "p3", "p3", 0);
        assertDepth(graph, thirdLevel.get(0).getId(), 1);
    }

    @Test
    public void shouldMoveNodeAndIntroduceDummyNodesWhenCurrentLevelIsDeeperThanExistingNodeLevel() throws Exception {
        /*
         * +-------------+
         * |             v
         * g---->p1---->p2 ---> p3
         *        |             ^
         *        -------------+
         *
         * */

        GitMaterial git = new GitMaterial("git");
        MaterialConfig gitConfig = git.config();
        String p1 = "p1";
        String p2 = "p2";
        String p3 = "p3";
        BuildCause p3buildCause = createBuildCause(asList(p1, p2), new ArrayList<>());
        BuildCause p2buildCause = createBuildCause(asList(p1), asList(git));
        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));

        when(pipelineService.buildCauseFor(p3, 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor(p2, 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor(p1, 1)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(p1, new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig(p2, new MaterialConfigs(gitConfig, new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name())));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig(p3,
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByNameAndCounter("p3", 1)).thenReturn(new Pipeline("p3", "p3-label", p3buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString(p3), 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is(p3));
        assertThat(nodesAtEachLevel.size(), is(4));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertLayerHasNode(firstLevel, git.getDisplayName(), git.getFingerprint(), new CaseInsensitiveString(p1));

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(2));
        assertLayerHasNode(secondLevel, p1, p1, new CaseInsensitiveString(p2));
        assertLayerHasDummyNodeWithDependents(secondLevel, new CaseInsensitiveString(p2));

        List<Node> thirdLevel = nodesAtEachLevel.get(2);
        assertThat(thirdLevel.size(), is(2));
        assertLayerHasNode(thirdLevel, p2, p2, new CaseInsensitiveString(p3));
        assertLayerHasDummyNodeWithDependents(thirdLevel, new CaseInsensitiveString(p3));

        List<Node> fourthLevel = nodesAtEachLevel.get(3);
        assertThat(fourthLevel.size(), is(1));
        assertLayerHasNode(fourthLevel, p3, p3);
    }

    @Test
    public void shouldDrawDependenciesIncludingDownstreamBasedOnConfig() {
        /*
         * These are all the pipelines in the config.
         *
         * |----> p1----->
         * g             |_> p3
         * |             |
         * ---- > p2----->
         *
         * We are drawing a graph for 'p1' : g -> p1 -> p3
         * */

        CruiseConfig cruiseConfig = GoConfigMother.simpleDiamond();
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        String currentPipeline = "p1";
        String p3 = "p3";
        GitMaterial git = new GitMaterial("git");
        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));

        when(pipelineService.buildCauseFor(currentPipeline, 1)).thenReturn(p1buildCause);
        when(pipelineService.findPipelineByNameAndCounter(currentPipeline, 1)).thenReturn(new Pipeline(currentPipeline, "p1-label", p1buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString(currentPipeline), 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is(currentPipeline));
        assertThat(nodesAtEachLevel.size(), is(3));

        assertLayerHasNode(nodesAtEachLevel.get(0), git.getDisplayName(), git.getFingerprint(), new CaseInsensitiveString(currentPipeline));
        assertLayerHasNode(nodesAtEachLevel.get(1), currentPipeline, currentPipeline, new CaseInsensitiveString(p3));
        assertLayerHasNode(nodesAtEachLevel.get(2), p3, p3);
    }

    @Test
    public void shouldAddDummyNodesUpstreamAndDownstreamInDependencyGraph() {
        /*
         * +------------+
         * |             v
         * g---->p1---->p2 ---> p3
         *        |             ^
         *        --------------+
         *
         * Drawing graph for p2, expected graph:
         *
         * +------X1------+
         * |             v
         * g---->p1---->p2 ---> p3
         * */

        String p1 = "p1";
        String p2 = "p2";
        String p3 = "p3";
        GitMaterial git = new GitMaterial("git");
        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(p1, new MaterialConfigs(git.config()));
        DependencyMaterial dependencyMaterialP1 = new DependencyMaterial(new CaseInsensitiveString(p1), p1Config.getFirstStageConfig().name());
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig(p2, new MaterialConfigs(git.config(), dependencyMaterialP1.config()));
        DependencyMaterial dependencyMaterialP2 = new DependencyMaterial(new CaseInsensitiveString(p2), p2Config.getFirstStageConfig().name());
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig(p3, new MaterialConfigs(dependencyMaterialP1.config(), dependencyMaterialP2.config()));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));

        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));
        BuildCause p2buildCause = createBuildCause(asList(p1), asList(git));

        when(pipelineService.buildCauseFor(p1, 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor(p2, 1)).thenReturn(p2buildCause);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByNameAndCounter(p2, 1)).thenReturn(new Pipeline(p2, "label-p2", p2buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString(p2), 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is(p2));
        assertThat(nodesAtEachLevel.size(), is(4));

        assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, new CaseInsensitiveString(git.getFingerprint()));
        Node gitNode = nodesAtEachLevel.get(0).get(0);
        assertNode(-2, gitNode, git.getDisplayName(), git.getFingerprint(), 1, new CaseInsensitiveString(p1));
        VSMTestHelper.assertThatNodeHasParents(gitNode, 0);

        assertThatLevelHasNodes(nodesAtEachLevel.get(1), 1, new CaseInsensitiveString(p1));
        assertLayerHasDummyNodeWithDependents(nodesAtEachLevel.get(1), new CaseInsensitiveString(p2));
        Node p1Node = nodesAtEachLevel.get(1).get(0);
        assertNode(-1, p1Node, p1, p1, 0, new CaseInsensitiveString(p2));
        VSMTestHelper.assertThatNodeHasParents(p1Node, 0, new CaseInsensitiveString(git.getFingerprint()));

        assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, new CaseInsensitiveString(p2));
        Node p2Node = nodesAtEachLevel.get(2).get(0);
        assertNode(0, p2Node, p2, p2, 0, new CaseInsensitiveString(p3));
        VSMTestHelper.assertThatNodeHasParents(p2Node, 1, new CaseInsensitiveString(p1));

        assertThatLevelHasNodes(nodesAtEachLevel.get(3), 0, new CaseInsensitiveString(p3));
        Node p3Node = nodesAtEachLevel.get(3).get(0);
        assertNode(1, p3Node, p3, p3, 0);
        VSMTestHelper.assertThatNodeHasParents(p3Node, 0, new CaseInsensitiveString(p2));
    }

    @Test
    public void shouldPushAllAncestorsLeftByOneWhenMovingImmediateParentToLeftOfANode() {
        /*
         * g1 -> P1--->X------>P2
         *        \             ^
         *          \           |
         *            V         |
         *       g2-->P3-------+
         *
         */

        CaseInsensitiveString p1 = new CaseInsensitiveString("p1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("p2");
        CaseInsensitiveString p3 = new CaseInsensitiveString("p3");
        GitMaterial g1 = new GitMaterial("g1");
        GitMaterial g2 = new GitMaterial("g2");

        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(g1));
        BuildCause p3buildCause = createBuildCause(asList(p1.toString()), asList(g2));
        BuildCause p2buildCause = createBuildCause(asList(p1.toString(), p3.toString()), Arrays.<GitMaterial>asList());

        when(pipelineService.buildCauseFor(p1.toString(), 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor(p2.toString(), 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor(p3.toString(), 1)).thenReturn(p3buildCause);
        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(p1.toString(), new MaterialConfigs(g1.config()));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig(p3.toString(), new MaterialConfigs(g2.config(), new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name())));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig(p2.toString(),
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p3Config.name(), p3Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByNameAndCounter(p2.toString(), 1)).thenReturn(new Pipeline(p2.toString(), "p2-label", p2buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(p2, 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(nodesAtEachLevel.size(), is(4));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, new CaseInsensitiveString(g1.getFingerprint()));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, p1, new CaseInsensitiveString(g2.getFingerprint()));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 1, p3);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(3), 0, p2);
    }

    @Test
    public void shouldPopulateRevisionsForUpstreamPipelines() {
        /*
         * git---> p1 ---> p3
         * |      v      ^
         * +---> p2 -----+
         * **/


        GitMaterial git = new GitMaterial("git");
        MaterialConfig gitConfig = git.config();
        BuildCause p3buildCause = createBuildCause(asList("p1", "p2"), new ArrayList<>());
        BuildCause p2buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 2)), asList(git), ModificationsMother.multipleModificationList(0));
        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));
        Modifications modifications = p1buildCause.getMaterialRevisions().getMaterialRevision(0).getModifications();

        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor("p1", 2)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));
        when(pipelineService.findPipelineByNameAndCounter("p3", 1)).thenReturn(new Pipeline("p3", "LABEL-P3", p3buildCause, new EnvironmentVariables()));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p3"), 1, user, result);
        VSMTestHelper.assertNodeHasRevisions(graph, new CaseInsensitiveString("p1"), new PipelineRevision("p1", 1, "LABEL-p1-1"), new PipelineRevision("p1", 2, "LABEL-p1-2"));
        VSMTestHelper.assertNodeHasRevisions(graph, new CaseInsensitiveString("p2"), new PipelineRevision("p2", 1, "LABEL-p2-1"));
        VSMTestHelper.assertNodeHasRevisions(graph, new CaseInsensitiveString("p3"), new PipelineRevision("p3", 1, "LABEL-P3"));
        VSMTestHelper.assertSCMNodeHasMaterialRevisions(graph, new CaseInsensitiveString(git.getFingerprint()), new MaterialRevision(git, false, modifications));

        verify(runStagesPopulator).apply(ArgumentMatchers.any(ValueStreamMap.class));
    }

    @Test
    public void shouldPopulateAllMaterialRevisionsThatCausedPipelineRun() {
        /*
         * git---> p1 --->p2
         *  |             ^
         *  +-------------+
         * **/


        GitMaterial git = new GitMaterial("git");
        MaterialConfig gitConfig = git.config();
        BuildCause p2buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 1)), asList(git), ModificationsMother.multipleModificationList(0));
        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));
        Modifications gitModifications = p1buildCause.getMaterialRevisions().getMaterialRevision(0).getModifications();

        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));

        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config));

        when(pipelineService.findPipelineByNameAndCounter("p2", 1)).thenReturn(new Pipeline("p2", "LABEL-P2", p2buildCause, new EnvironmentVariables()));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p2"), 1, user, result);
        VSMTestHelper.assertNodeHasRevisions(graph, new CaseInsensitiveString("p1"), new PipelineRevision("p1", 1, "LABEL-p1-1"));
        VSMTestHelper.assertNodeHasRevisions(graph, new CaseInsensitiveString("p2"), new PipelineRevision("p2", 1, "LABEL-P2"));

        VSMTestHelper.assertSCMNodeHasMaterialRevisions(graph, new CaseInsensitiveString(git.getFingerprint()), new MaterialRevision(git, false, gitModifications));

        verify(runStagesPopulator).apply(ArgumentMatchers.any(ValueStreamMap.class));
    }

    @Test
    public void shouldPopulateAllSCMMaterialRevisionsThatCausedPipelineRun_WhenFaninIsNotObeyed() {
        /*
         * git---> p1 --->p2
         *  |             ^
         *  +-------------+
         * **/


        GitMaterial git = new GitMaterial("git");
        MaterialConfig gitConfig = git.config();
        Modification modification1 = checkinWithComment("rev1", "comment1", new Date());
        Modification modification2 = checkinWithComment("rev2", "comment2", new Date());
        Modification modification3 = checkinWithComment("rev3", "comment3", new Date());
        BuildCause p1buildCause = createBuildCauseForRevisions(new ArrayList<>(), asList(git), new Modifications(
                modification1, modification2));
        BuildCause p2buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 1)), asList(git), new Modifications(modification3));

        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));

        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config));

        when(pipelineService.findPipelineByNameAndCounter("p2", 1)).thenReturn(new Pipeline("p2", "LABEL-P2", p2buildCause, new EnvironmentVariables()));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p2"), 1, user, result);
        VSMTestHelper.assertNodeHasRevisions(graph, new CaseInsensitiveString("p1"), new PipelineRevision("p1", 1, "LABEL-p1-1"));
        VSMTestHelper.assertNodeHasRevisions(graph, new CaseInsensitiveString("p2"), new PipelineRevision("p2", 1, "LABEL-P2"));

        VSMTestHelper.assertSCMNodeHasMaterialRevisions(graph, new CaseInsensitiveString(git.getFingerprint()),
                new MaterialRevision(git, false, modification1, modification2),
                new MaterialRevision(git, false, modification3));

        verify(runStagesPopulator).apply(ArgumentMatchers.any(ValueStreamMap.class));
    }

    @Test
    public void currentPipelineShouldHaveWarningsIfBuiltFromIncompatibleRevisions() {
        /*
                            /-> P1 -- \
                        git            -> p3
                            \-> P2 -- /
         */


        GitMaterial git = new GitMaterial("git");
        MaterialConfig gitConfig = git.config();
        BuildCause p1buildCause = createBuildCauseForRevisions(new ArrayList<>(), asList(git), Arrays.asList(ModificationsMother.oneModifiedFile("rev1")));
        BuildCause p2buildCause = createBuildCauseForRevisions(new ArrayList<>(), asList(git), Arrays.asList(ModificationsMother.oneModifiedFile("rev2")));

        BuildCause p3buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 1), dependencyMaterial("p2", 1)), new ArrayList<>(), new ArrayList<>());


        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));
        when(pipelineService.findPipelineByNameAndCounter("p3", 1)).thenReturn(new Pipeline("p3", "LABEL-P3", p3buildCause, new EnvironmentVariables()));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);


        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p3"), 1, user, result);

        assertThat(graph.getCurrentPipeline().getViewType(), is(VSMViewType.WARNING));
    }

    @Test
    public void currentPipelineShouldNotHaveWarningsIfBuiltFromMultipleRevisionsWithSameLatestRevision() {
        /*
                            /-> P1 -- \
                        git            -> p3
                            \-> P2 -- /
         */


        GitMaterial git = new GitMaterial("git");
        MaterialConfig gitConfig = git.config();
        Modification rev1 = ModificationsMother.oneModifiedFile("rev1");
        Modification rev2 = ModificationsMother.oneModifiedFile("rev2");
        Modification rev3 = ModificationsMother.oneModifiedFile("rev3");

        BuildCause p1buildCause = createBuildCauseForRevisions(new ArrayList<>(), asList(git), Arrays.asList(rev3, rev2, rev1));
        BuildCause p2buildCause = createBuildCauseForRevisions(new ArrayList<>(), asList(git), Arrays.asList(rev3));

        BuildCause p3buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 1), dependencyMaterial("p2", 1)), new ArrayList<>(), new ArrayList<>());


        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));
        when(pipelineService.findPipelineByNameAndCounter("p3", 1)).thenReturn(new Pipeline("p3", "LABEL-P3", p3buildCause, new EnvironmentVariables()));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);


        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p3"), 1, user, result);

        assertNull(graph.getCurrentPipeline().getViewType());
    }

    @Test
    public void shouldPopulateLabelForCurrentPipeline() throws Exception {
        /*
                git --> p1
         */

        GitMaterial git = new GitMaterial("git");
        String pipelineName = "p1";
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(git.config()))));
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);
        when(pipelineService.findPipelineByNameAndCounter(pipelineName, 1)).thenReturn(new Pipeline("p1", "label-1", p1buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString(pipelineName), 1, user, result);

        PipelineRevision revision = (PipelineRevision) graph.getCurrentPipeline().revisions().get(0);
        assertThat(revision.getLabel(), is("label-1"));
    }

    @Test
    public void shouldPopulateErrorWhenUserDoesNotHaveViewPermissionForCurrentPipeline() throws Exception {
        /*
                git --> p1
         */

        GitMaterial git = new GitMaterial("git");
        String pipelineName = "p1";
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(git.config()))));
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        Username newUser = new Username(new CaseInsensitiveString("looser"));
        when(securityService.hasViewPermissionForPipeline(newUser, pipelineName)).thenReturn(false);

        valueStreamMapService.getValueStreamMap(new CaseInsensitiveString(pipelineName), 1, newUser, result);

        assertResult(SC_FORBIDDEN, "You do not have view permissions for pipeline 'p1'.");
    }

    @Test
    public void shouldPopulateErrorWhenUpstreamPipelineDoesNotExistInCurrentConfig() throws Exception {
        /*
         * g --> p1 --> p3
         */

        GitMaterial git = new GitMaterial("git");
        BuildCause p3buildCause = createBuildCause(asList("p1"), new ArrayList<>());
        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));


        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);


        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3", new MaterialConfigs(git("test")));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p3Config));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("p1"))).thenReturn(false);
        when(pipelineService.findPipelineByNameAndCounter("p3", 1)).thenReturn(new Pipeline("p3", "p3-label", p3buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p3"), 1, user, result);

        PipelineDependencyNode node = (PipelineDependencyNode) graph.getNodesAtEachLevel().get(1).get(0);
        assertThat(node.revisions().toString(), node.revisions().isEmpty(), is(true));
        assertThat(node.getViewType(), is(VSMViewType.DELETED));
        assertThat(node.getMessage(), is("Pipeline has been deleted."));
    }

    @Test
    public void shouldPopulateErrorWhenPipelineNameAndCounterAreMultiple() {

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("MYPIPELINE", new MaterialConfigs(git("sampleGit")));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig));

        when(pipelineService.findPipelineByNameAndCounter("MYPIPELINE", 1)).thenThrow(RuntimeException.class);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("MYPIPELINE"), 1, user, result);

        assertThat(graph, is(nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Value Stream Map of pipeline 'MYPIPELINE' with counter '1' can not be rendered. Please check the server log for details."));

    }

    @Test
    public void shouldPopulateEditPermissionsForPipelineDependencyNode() throws Exception {
        /*
         * g --> p1 --> p2
         */

        GitMaterial git = new GitMaterial("git");
        BuildCause p2buildCause = createBuildCause(asList("p1"), new ArrayList<>());
        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));


        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);


        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(git("test")));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p2Config));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.hasPipelineNamed(any())).thenReturn(true);
        when(goConfigService.canEditPipeline("p1", user, result)).thenReturn(true);
        when(goConfigService.canEditPipeline("p2", user, result)).thenReturn(false);
        when(pipelineService.findPipelineByNameAndCounter("p2", 1)).thenReturn(new Pipeline("p2", "p2-label", p2buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p2"), 1, user, result);

        PipelineDependencyNode p1 = (PipelineDependencyNode) graph.getNodesAtEachLevel().get(1).get(0);
        PipelineDependencyNode p2 = (PipelineDependencyNode) graph.getNodesAtEachLevel().get(2).get(0);
        assertTrue(p1.canEdit());
        assertFalse(p2.canEdit());
    }

    @Test
    public void shouldNotModifyResultObjectWhenUserDoesNotHaveEditPermissionsForPipelineDependencyNode() throws Exception {
        /*
         * g --> p1 --> p2
         */

        GitMaterial git = new GitMaterial("git");
        BuildCause p2buildCause = createBuildCause(asList("p1"), new ArrayList<>());
        BuildCause p1buildCause = createBuildCause(new ArrayList<>(), asList(git));

        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);


        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(git("test")));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p2Config));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.hasPipelineNamed(any())).thenReturn(true);
        doAnswer(invocation -> true).when(goConfigService).canEditPipeline("p1", user, result);
        doAnswer(invocation -> {
            invocation.<LocalizedOperationResult>getArgument(2).unprocessableEntity("Boom!!");
            return false;
        }).when(goConfigService).canEditPipeline("p2", user, result);

        when(pipelineService.findPipelineByNameAndCounter("p2", 1)).thenReturn(new Pipeline("p2", "p2-label", p2buildCause, new EnvironmentVariables()));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString("p2"), 1, user, result);

        PipelineDependencyNode p1 = (PipelineDependencyNode) graph.getNodesAtEachLevel().get(1).get(0);
        PipelineDependencyNode p2 = (PipelineDependencyNode) graph.getNodesAtEachLevel().get(2).get(0);
        assertTrue(p1.canEdit());
        assertFalse(p2.canEdit());

        assertTrue(result.isSuccessful());
        assertFalse(result.hasMessage());
    }

    @Test
    public void shouldPopulateErrorCorrectly_VSMForMaterial() throws Exception {
		/*
				git --> p1
		 */

        String groupName = "g1";
        String pipelineName = "p1";
        String userName = "looser";
        GitMaterial gitMaterial = new GitMaterial("git");
        MaterialConfig gitConfig = gitMaterial.config();
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("url", null, "branch", "submodule", "flyweight");
        PipelineConfigs groups = new BasicPipelineConfigs(groupName, new Authorization(), PipelineConfigMother.pipelineConfig(pipelineName, new MaterialConfigs(gitConfig)));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(groups);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.groups()).thenReturn(new PipelineGroups(groups));

        when(securityService.hasViewPermissionForGroup(userName, groupName)).thenReturn(false);

        // unknown material
        valueStreamMapService.getValueStreamMap("unknown-material", "r1", new Username(new CaseInsensitiveString(userName)), result);

        assertResult(SC_NOT_FOUND, "Material with fingerprint 'unknown-material' not found.");

        // unauthorized
        valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), "r1", new Username(new CaseInsensitiveString(userName)), result);

        assertResult(SC_FORBIDDEN, "You do not have view permissions for material with fingerprint '" + gitConfig.getFingerprint() + "'.");

        // material config exists but no material instance
        when(securityService.hasViewPermissionForGroup(userName, groupName)).thenReturn(true);
        when(materialRepository.findMaterialInstance(gitConfig)).thenReturn(null);

        valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), "r1", new Username(new CaseInsensitiveString(userName)), result);

        assertResult(SC_NOT_FOUND, "Material Instance with fingerprint '" + gitConfig.getFingerprint() + "' not found.");

        // modification (revision) doesn't exist
        when(materialRepository.findMaterialInstance(gitConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.findModificationWithRevision(gitMaterial, "r1")).thenReturn(null);

        valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), "r1", new Username(new CaseInsensitiveString(userName)), result);

        assertResult(SC_NOT_FOUND, "Modification 'r1' for material with fingerprint '" + gitMaterial.getFingerprint() + "' not found.");

        // internal error
        when(goConfigService.groups()).thenThrow(new RuntimeException("just for fun"));

        valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), "r1", new Username(new CaseInsensitiveString(userName)), result);

        assertResult(SC_INTERNAL_SERVER_ERROR, "Value Stream Map of material with fingerprint '" + gitMaterial.getFingerprint() + "' with revision 'r1' can not be rendered. Please check the server log for details.");
    }

    private void assertResult(int httpCode, String msgKey) {
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(httpCode));
        assertThat(result.message(), is(msgKey));
    }

    private void assertLayerHasDummyNodeWithDependents(List<Node> nodesOfLevel, CaseInsensitiveString... dependents) {
        for (Node currentNode : nodesOfLevel) {
            if (currentNode.getType() == DependencyNodeType.DUMMY) {
                assertThat(currentNode.getChildren().size(), is(dependents.length));
                VSMTestHelper.assertNodeHasChildren(currentNode, dependents);
            }
        }
    }

    private BuildCause createBuildCauseForRevisions(List<DependencyMaterialDetail> dependencyMaterials, List<GitMaterial> gitMaterials, List<Modification> modifications) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (DependencyMaterialDetail dependencyMaterial : dependencyMaterials) {
            String label = String.format("LABEL-%s-%d", dependencyMaterial.pipelineName, dependencyMaterial.pipelineCounter);
            materialRevisions.addRevision(ModificationsMother.dependencyMaterialRevision(dependencyMaterial.pipelineName, dependencyMaterial.pipelineCounter, label, "s1", 1, null));
        }
        for (GitMaterial gitMaterial : gitMaterials) {
            materialRevisions.addRevision(new MaterialRevision(gitMaterial, modifications));
        }
        return BuildCause.createWithModifications(materialRevisions, "");
    }

    private BuildCause createBuildCause(List<String> dependencyMaterials, List<GitMaterial> gitMaterials, int counter) {
        List<DependencyMaterialDetail> dependencyMaterialDetails = new ArrayList<>();
        for (String dependencyMaterial : dependencyMaterials) {
            dependencyMaterialDetails.add(dependencyMaterial(dependencyMaterial, counter));
        }
        return createBuildCauseForRevisions(dependencyMaterialDetails, gitMaterials, ModificationsMother.multipleModificationList(0));
    }

    private BuildCause createBuildCause(List<String> dependencyMaterials, List<GitMaterial> gitMaterials) {
        return createBuildCause(dependencyMaterials, gitMaterials, 1);
    }

    private void assertNode(int level, final Node node, final String expectedNodeName, final String expectedNodeId,
                            int expectedDummyDependentsCount, CaseInsensitiveString... dependents) {
        assertThat(node.getLevel(), is(level));
        assertThat(node.getName(), is(expectedNodeName));
        assertThat(node.getId().toString(), is(expectedNodeId));
        assertThat(node.getChildren().size(), is(dependents.length + expectedDummyDependentsCount));
        VSMTestHelper.assertNodeHasChildren(node, dependents);
        int dummyDependentsCount = 0;
        for (Node child : node.getChildren()) {
            if (isUUID(child.getId().toString())) {
                dummyDependentsCount++;
            }
        }
        assertThat(dummyDependentsCount, is(expectedDummyDependentsCount));
    }

    private boolean isUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void assertLayerHasNode(final List<Node> nodesOfLevel, final String expectedNodeName, final String expectedNodeId, CaseInsensitiveString... dependents) {
        for (Node currentNode : nodesOfLevel) {
            if (currentNode.getId().toString().equals(expectedNodeId)) {
                assertThat(currentNode.getName(), is(expectedNodeName));
                assertThat(currentNode.getId().toString(), is(expectedNodeId));
                VSMTestHelper.assertNodeHasChildren(currentNode, dependents);
                return;
            }
        }
        fail("was expecting to see node " + expectedNodeId);
    }

    private void assertThatLevelHasNodes(List<Node> nodesAtLevel, int numberOfDummyNodes, CaseInsensitiveString... nodeIds) {
        assertThat(nodesAtLevel.size(), is(numberOfDummyNodes + nodeIds.length));
        List<CaseInsensitiveString> nodeIdsAtLevel = new ArrayList<>();
        for (Node node : nodesAtLevel) {
            if (!node.getType().equals(DependencyNodeType.DUMMY)) {
                nodeIdsAtLevel.add(node.getId());
            }
        }
        assertThat(nodeIdsAtLevel, hasItems(nodeIds));
    }

    private DependencyMaterialDetail dependencyMaterial(String pipelineName, int pipelineCounter) {
        return new DependencyMaterialDetail(pipelineName, pipelineCounter);
    }

    private class DependencyMaterialDetail {
        private final String pipelineName;
        private final int pipelineCounter;

        public DependencyMaterialDetail(String pipelineName, int pipelineCounter) {
            this.pipelineName = pipelineName;
            this.pipelineCounter = pipelineCounter;
        }
    }
}
