/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
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
import com.thoughtworks.go.server.valuestreammap.DownstreamInstancePopulator;
import com.thoughtworks.go.server.valuestreammap.RunStagesPopulator;
import com.thoughtworks.go.server.valuestreammap.UnrunStagesPopulator;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;

import static com.thoughtworks.go.domain.valuestreammap.VSMTestHelper.assertDepth;
import static com.thoughtworks.go.helper.ModificationsMother.checkinWithComment;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ValueStreamMapServiceTest {
	@Mock
	private PipelineService pipelineService;
	@Mock
	private MaterialRepository materialRepository;
	@Mock
	private GoConfigService goConfigService;
	@Mock
	private RunStagesPopulator runStagesPopulator;
	@Mock
	private UnrunStagesPopulator unrunStagesPopulator;
	@Mock
	private DownstreamInstancePopulator downstreaminstancepopulator;
	@Mock
	private SecurityService securityService;

	private Username user;
	private ValueStreamMapService valueStreamMapService;
	private HttpLocalizedOperationResult result;

    @Before
    public void setUp() throws Exception {
		initMocks(this);
        user = new Username(new CaseInsensitiveString("poovan"));

		setupExistenceOfPipelines("p1", "p2", "p3", "MYPIPELINE");

		setupViewPermissionForPipelines("C", "A", "B", "P1", "P2", "P3", "p1", "p2", "p3", "MYPIPELINE");

		setupViewPermissionForGroups("g1");

        valueStreamMapService = new ValueStreamMapService(pipelineService, materialRepository, goConfigService, downstreaminstancepopulator, runStagesPopulator, unrunStagesPopulator, securityService);
        result = new HttpLocalizedOperationResult();
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
        when(pipelineService.findPipelineByCounterOrLabel(pipelineName, "1")).thenReturn(new Pipeline("MYPIPELINE", "p1-label", buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("MYPIPELINE", counter, user, result);

        assertThat(graph.getCurrentPipeline().getName(), is(pipelineName));
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(2));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(-1, firstLevel.get(0), materialConfig.getDisplayName(), materialConfig.getFingerprint(), 0, pipelineName);

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(1));
        assertNode(0, secondLevel.get(0), pipelineName, pipelineName, 0);
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
        when(pipelineService.findPipelineByCounterOrLabel(pipeline, "1")).thenReturn(new Pipeline(pipeline, "p1-label", buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(pipeline, counter, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is(pipeline));
        assertThat(nodesAtEachLevel.size(), is(2));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(-1, firstLevel.get(0), material.getDisplayName(), material.getFingerprint(), 0, pipeline);

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
        BuildCause p3buildCause = createBuildCause(asList("p1", "p2"), new ArrayList<GitMaterial>());
        BuildCause p2buildCause = createBuildCause(new ArrayList<String>(), asList(git));
        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));


        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(git.config()));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(git.config()));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByCounterOrLabel("p3", "1")).thenReturn(new Pipeline("p3", "p3-label", p3buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p3", 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is("p3"));
        assertThat(nodesAtEachLevel.size(), is(3));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertNode(-2, firstLevel.get(0), git.getDisplayName(), git.getFingerprint(), 0, "p1", "p2");

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(2));
        assertNode(-1, secondLevel.get(0), "p1", "p1", 0, "p3");
        assertNode(-1, secondLevel.get(1), "p2", "p2", 0, "p3");

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
		GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("git", "master", "submodule", "flyweight");
		BuildCause p3buildCause = createBuildCause(asList("p1", "p2"), new ArrayList<GitMaterial>());
		BuildCause p2buildCause = createBuildCause(new ArrayList<String>(), asList(gitMaterial));
		Modification gitModification = p2buildCause.getMaterialRevisions().getRevisions().get(0).getModifications().get(0);
		String gitRevision = gitModification.getRevision();
		BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(gitMaterial));

		when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
		when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
		when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);

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
		assertThat(graph.getCurrentMaterial().getId(), is(gitMaterial.getFingerprint()));
		assertThat(nodesAtEachLevel.size(), is(3));

		List<Node> firstLevel = nodesAtEachLevel.get(0);
		assertThat(firstLevel.size(), is(1));
		assertNode(0, firstLevel.get(0), gitMaterial.getDisplayName(), gitMaterial.getFingerprint(), 0, "p1", "p2");
		assertDepth(graph, firstLevel.get(0).getId(), 1);

		List<Node> secondLevel = nodesAtEachLevel.get(1);
		assertThat(secondLevel.size(), is(2));
		assertNode(1, secondLevel.get(0), "p1", "p1", 0, "p3");
		assertDepth(graph, secondLevel.get(0).getId(), 1);
		assertNode(1, secondLevel.get(1), "p2", "p2", 0, "p3");
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
        BuildCause p3buildCause = createBuildCause(asList(p1, p2), new ArrayList<GitMaterial>());
        BuildCause p2buildCause = createBuildCause(asList(p1), asList(git));
        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));

        when(pipelineService.buildCauseFor(p3, 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor(p2, 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor(p1, 1)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(p1, new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig(p2, new MaterialConfigs(gitConfig, new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name())));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig(p3,
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByCounterOrLabel("p3", "1")).thenReturn(new Pipeline("p3", "p3-label", p3buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(p3, 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is(p3));
        assertThat(nodesAtEachLevel.size(), is(4));

        List<Node> firstLevel = nodesAtEachLevel.get(0);
        assertThat(firstLevel.size(), is(1));
        assertLayerHasNode(firstLevel, git.getDisplayName(), git.getFingerprint(), p1);

        List<Node> secondLevel = nodesAtEachLevel.get(1);
        assertThat(secondLevel.size(), is(2));
        assertLayerHasNode(secondLevel, p1, p1, p2);
        assertLayerHasDummyNodeWithDependents(secondLevel, p2);

        List<Node> thirdLevel = nodesAtEachLevel.get(2);
        assertThat(thirdLevel.size(), is(2));
        assertLayerHasNode(thirdLevel, p2, p2, p3);
        assertLayerHasDummyNodeWithDependents(thirdLevel, p3);

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
        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));

        when(pipelineService.buildCauseFor(currentPipeline, 1)).thenReturn(p1buildCause);
        when(pipelineService.findPipelineByCounterOrLabel(currentPipeline, "1")).thenReturn(new Pipeline(currentPipeline, "p1-label", p1buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(currentPipeline, 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is(currentPipeline));
        assertThat(nodesAtEachLevel.size(), is(3));

        assertLayerHasNode(nodesAtEachLevel.get(0), git.getDisplayName(), git.getFingerprint(), currentPipeline);
        assertLayerHasNode(nodesAtEachLevel.get(1), currentPipeline, currentPipeline, p3);
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

        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));
        BuildCause p2buildCause = createBuildCause(asList(p1), asList(git));

        when(pipelineService.buildCauseFor(p1, 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor(p2, 1)).thenReturn(p2buildCause);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByCounterOrLabel(p2, "1")).thenReturn(new Pipeline(p2, "label-p2", p2buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(p2, 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(graph.getCurrentPipeline().getName(), is(p2));
        assertThat(nodesAtEachLevel.size(), is(4));

        assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, git.getFingerprint());
        Node gitNode = nodesAtEachLevel.get(0).get(0);
        assertNode(-2, gitNode, git.getDisplayName(), git.getFingerprint(), 1, p1);
        VSMTestHelper.assertThatNodeHasParents(gitNode, 0);

        assertThatLevelHasNodes(nodesAtEachLevel.get(1), 1, p1);
        assertLayerHasDummyNodeWithDependents(nodesAtEachLevel.get(1), p2);
        Node p1Node = nodesAtEachLevel.get(1).get(0);
        assertNode(-1, p1Node, p1, p1, 0, p2);
        VSMTestHelper.assertThatNodeHasParents(p1Node, 0, git.getFingerprint());

        assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, p2);
        Node p2Node = nodesAtEachLevel.get(2).get(0);
        assertNode(0, p2Node, p2, p2, 0, p3);
        VSMTestHelper.assertThatNodeHasParents(p2Node, 1, p1);

        assertThatLevelHasNodes(nodesAtEachLevel.get(3), 0, p3);
        Node p3Node = nodesAtEachLevel.get(3).get(0);
        assertNode(1, p3Node, p3, p3, 0);
        VSMTestHelper.assertThatNodeHasParents(p3Node, 0, p2);
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

        String p1 = "p1";
        String p2 = "p2";
        String p3 = "p3";
        GitMaterial g1 = new GitMaterial("g1");
        GitMaterial g2 = new GitMaterial("g2");

        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(g1));
        BuildCause p3buildCause = createBuildCause(asList(p1), asList(g2));
        BuildCause p2buildCause = createBuildCause(asList(p1, p3), Arrays.<GitMaterial>asList());

        when(pipelineService.buildCauseFor(p1, 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor(p2, 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor(p3, 1)).thenReturn(p3buildCause);
        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig(p1, new MaterialConfigs(g1.config()));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig(p3, new MaterialConfigs(g2.config(), new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name())));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig(p2,
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p3Config.name(), p3Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineService.findPipelineByCounterOrLabel(p2, "1")).thenReturn(new Pipeline(p2, "p2-label", p2buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(p2, 1, user, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        assertThat(nodesAtEachLevel.size(), is(4));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint());
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, p1, g2.getFingerprint());
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
        BuildCause p3buildCause = createBuildCause(asList("p1", "p2"), new ArrayList<GitMaterial>());
        BuildCause p2buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 2)), asList(git), ModificationsMother.multipleModificationList(0));
        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));
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
        when(pipelineService.findPipelineByCounterOrLabel("p3", "1")).thenReturn(new Pipeline("p3", "LABEL-P3", p3buildCause));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p3", 1, user, result);
        VSMTestHelper.assertNodeHasRevisions(graph, "p1", new PipelineRevision("p1", 1, "LABEL-p1-1"), new PipelineRevision("p1", 2, "LABEL-p1-2"));
        VSMTestHelper.assertNodeHasRevisions(graph, "p2", new PipelineRevision("p2", 1, "LABEL-p2-1"));
        VSMTestHelper.assertNodeHasRevisions(graph, "p3", new PipelineRevision("p3", 1, "LABEL-P3"));
        VSMTestHelper.assertSCMNodeHasMaterialRevisions(graph, git.getFingerprint(), new MaterialRevision(git, false, modifications));

        verify(runStagesPopulator).apply(any(ValueStreamMap.class));
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
        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));
        Modifications gitModifications = p1buildCause.getMaterialRevisions().getMaterialRevision(0).getModifications();

        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));

        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config));

        when(pipelineService.findPipelineByCounterOrLabel("p2", "1")).thenReturn(new Pipeline("p2", "LABEL-P2", p2buildCause));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p2", 1, user, result);
        VSMTestHelper.assertNodeHasRevisions(graph, "p1", new PipelineRevision("p1", 1, "LABEL-p1-1"));
        VSMTestHelper.assertNodeHasRevisions(graph, "p2", new PipelineRevision("p2", 1, "LABEL-P2"));

        VSMTestHelper.assertSCMNodeHasMaterialRevisions(graph, git.getFingerprint(), new MaterialRevision(git, false, gitModifications));

        verify(runStagesPopulator).apply(any(ValueStreamMap.class));
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
        BuildCause p1buildCause = createBuildCauseForRevisions(new ArrayList<DependencyMaterialDetail>(), asList(git), new Modifications(
                modification1, modification2));
        BuildCause p2buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 1)),asList(git) , new Modifications(modification3));

        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));

        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config));

        when(pipelineService.findPipelineByCounterOrLabel("p2", "1")).thenReturn(new Pipeline("p2", "LABEL-P2", p2buildCause));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p2", 1, user, result);
        VSMTestHelper.assertNodeHasRevisions(graph, "p1", new PipelineRevision("p1", 1, "LABEL-p1-1"));
        VSMTestHelper.assertNodeHasRevisions(graph, "p2", new PipelineRevision("p2", 1, "LABEL-P2"));

        VSMTestHelper.assertSCMNodeHasMaterialRevisions(graph, git.getFingerprint(),
                new MaterialRevision(git, false, modification1, modification2),
                new MaterialRevision(git, false, modification3));

        verify(runStagesPopulator).apply(any(ValueStreamMap.class));
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
        BuildCause p1buildCause = createBuildCauseForRevisions(new ArrayList<DependencyMaterialDetail>(), asList(git), Arrays.asList(ModificationsMother.oneModifiedFile("rev1")));
        BuildCause p2buildCause = createBuildCauseForRevisions(new ArrayList<DependencyMaterialDetail>(), asList(git), Arrays.asList(ModificationsMother.oneModifiedFile("rev2")));

        BuildCause p3buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 1), dependencyMaterial("p2", 1)), new ArrayList<GitMaterial>(), new ArrayList<Modification>());


        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));
        when(pipelineService.findPipelineByCounterOrLabel("p3", "1")).thenReturn(new Pipeline("p3", "LABEL-P3", p3buildCause));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);


        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p3", 1, user, result);

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

        BuildCause p1buildCause = createBuildCauseForRevisions(new ArrayList<DependencyMaterialDetail>(), asList(git), Arrays.asList(rev3, rev2, rev1));
        BuildCause p2buildCause = createBuildCauseForRevisions(new ArrayList<DependencyMaterialDetail>(), asList(git), Arrays.asList(rev3));

        BuildCause p3buildCause = createBuildCauseForRevisions(asList(dependencyMaterial("p1", 1), dependencyMaterial("p2", 1)), new ArrayList<GitMaterial>(), new ArrayList<Modification>());


        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);
        when(pipelineService.buildCauseFor("p2", 1)).thenReturn(p2buildCause);

        PipelineConfig p1Config = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(gitConfig));
        PipelineConfig p2Config = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(gitConfig));
        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(new DependencyMaterialConfig(p1Config.name(), p1Config.getFirstStageConfig().name()), new DependencyMaterialConfig(p2Config.name(), p2Config.getFirstStageConfig().name())));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1Config, p2Config, p3Config));
        when(pipelineService.findPipelineByCounterOrLabel("p3", "1")).thenReturn(new Pipeline("p3", "LABEL-P3", p3buildCause));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);


        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p3", 1, user, result);

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

        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);
        when(pipelineService.findPipelineByCounterOrLabel(pipelineName, "1")).thenReturn(new Pipeline("p1", "label-1", p1buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(pipelineName, 1, user, result);

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

        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);
        when(pipelineService.findPipelineByCounterOrLabel(pipelineName, "1")).thenReturn(new Pipeline("p1", "label-1", p1buildCause));

        Username newUser = new Username(new CaseInsensitiveString("looser"));
        when(securityService.hasViewPermissionForPipeline(newUser, pipelineName)).thenReturn(false);

        valueStreamMapService.getValueStreamMap(pipelineName, 1, newUser, result);

		assertResult(HttpStatus.SC_UNAUTHORIZED, "PIPELINE_CANNOT_VIEW");
    }

    @Test
    public void shouldPopulateErrorWhenUpstreamPipelineDoesNotExistInCurrentConfig() throws Exception {
        /*
       * g --> p1 --> p3
        */

        GitMaterial git = new GitMaterial("git");
        BuildCause p3buildCause = createBuildCause(asList("p1"), new ArrayList<GitMaterial>());
        BuildCause p1buildCause = createBuildCause(new ArrayList<String>(), asList(git));


        when(pipelineService.buildCauseFor("p3", 1)).thenReturn(p3buildCause);
        when(pipelineService.buildCauseFor("p1", 1)).thenReturn(p1buildCause);


        PipelineConfig p3Config = PipelineConfigMother.pipelineConfig("p3", new MaterialConfigs(new GitMaterialConfig("test")));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p3Config));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("p1"))).thenReturn(false);
        when(pipelineService.findPipelineByCounterOrLabel("p3", "1")).thenReturn(new Pipeline("p3", "p3-label", p3buildCause));

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p3", 1, user, result);

        PipelineDependencyNode node = (PipelineDependencyNode) graph.getNodesAtEachLevel().get(1).get(0);
        assertThat(node.revisions().toString(), node.revisions().isEmpty(), is(true));
        assertThat(node.getViewType(), is(VSMViewType.DELETED));
        assertThat((String) ReflectionUtil.getField((node.getMessage()), "key"), is("VSM_PIPELINE_DELETED"));
    }

    @Test
    public void shouldPopulateErrorWhenPipelineNameAndCounterAreMultiple() {

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("MYPIPELINE", new MaterialConfigs(new GitMaterialConfig("sampleGit")));
        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig));

        when(pipelineService.findPipelineByCounterOrLabel("MYPIPELINE", "1")).thenThrow(Exception.class);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("MYPIPELINE", 1, user, result);

        assertThat(graph, is(IsNull.nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat((String) ReflectionUtil.getField(result.localizable(), "key"), is("VSM_INTERNAL_SERVER_ERROR"));

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
		GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("url", "branch", "submodule", "flyweight");
		PipelineConfigs groups = new BasicPipelineConfigs(groupName, new Authorization(), PipelineConfigMother.pipelineConfig(pipelineName, new MaterialConfigs(gitConfig)));
		CruiseConfig cruiseConfig = new BasicCruiseConfig(groups);
		when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
		when(goConfigService.groups()).thenReturn(new PipelineGroups(groups));

		when(securityService.hasViewPermissionForGroup(userName, groupName)).thenReturn(false);

		// unknown material
		valueStreamMapService.getValueStreamMap("unknown-material", "r1", new Username(new CaseInsensitiveString(userName)), result);

		assertResult(HttpStatus.SC_NOT_FOUND, "MATERIAL_CONFIG_WITH_FINGERPRINT_NOT_FOUND");

		// unauthorized
		valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), "r1", new Username(new CaseInsensitiveString(userName)), result);

		assertResult(HttpStatus.SC_UNAUTHORIZED, "MATERIAL_CANNOT_VIEW");

		// material config exists but no material instance
		when(securityService.hasViewPermissionForGroup(userName, groupName)).thenReturn(true);
		when(materialRepository.findMaterialInstance(gitConfig)).thenReturn(null);

		valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), "r1", new Username(new CaseInsensitiveString(userName)), result);

		assertResult(HttpStatus.SC_NOT_FOUND, "MATERIAL_INSTANCE_WITH_FINGERPRINT_NOT_FOUND");

		// modification (revision) doesn't exist
		when(materialRepository.findMaterialInstance(gitConfig)).thenReturn(gitMaterialInstance);
		when(materialRepository.findModificationWithRevision(gitMaterial, "r1")).thenReturn(null);

		valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), "r1", new Username(new CaseInsensitiveString(userName)), result);

		assertResult(HttpStatus.SC_NOT_FOUND, "MATERIAL_MODIFICATION_NOT_FOUND");

		// internal error
		when(goConfigService.groups()).thenThrow(new RuntimeException("just for fun"));

		valueStreamMapService.getValueStreamMap(gitMaterial.getFingerprint(), "r1", new Username(new CaseInsensitiveString(userName)), result);

		assertResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "VSM_INTERNAL_SERVER_ERROR_FOR_MATERIAL");
	}

	private void assertResult(int httpCode, String msgKey) {
		assertThat(result.isSuccessful(), is(false));
		assertThat(result.httpCode(), is(httpCode));
		assertThat((String) ReflectionUtil.getField((result.localizable()), "key"), is(msgKey));
	}

    private void assertLayerHasDummyNodeWithDependents(List<Node> nodesOfLevel, String... dependents) {
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
        List<DependencyMaterialDetail> dependencyMaterialDetails = new ArrayList<DependencyMaterialDetail>();
        for (String dependencyMaterial : dependencyMaterials) {
            dependencyMaterialDetails.add(dependencyMaterial(dependencyMaterial, counter));
        }
        return createBuildCauseForRevisions(dependencyMaterialDetails, gitMaterials, ModificationsMother.multipleModificationList(0));
    }

    private BuildCause createBuildCause(List<String> dependencyMaterials, List<GitMaterial> gitMaterials) {
        return createBuildCause(dependencyMaterials, gitMaterials, 1);
    }

    private void assertNode(int level, final Node node, final String expectedNodeName, final String expectedNodeId,
                            int expectedDummyDependentsCount, String... dependents) {
        assertThat(node.getLevel(), is(level));
        assertThat(node.getName(), is(expectedNodeName));
        assertThat(node.getId(), is(expectedNodeId));
        assertThat(node.getChildren().size(), is(dependents.length + expectedDummyDependentsCount));
        VSMTestHelper.assertNodeHasChildren(node, dependents);
        int dummyDependentsCount = 0;
        for (Node child : node.getChildren()) {
            if (isUUID(child.getId())) {
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

    private void assertLayerHasNode(final List<Node> nodesOfLevel, final String expectedNodeName, final String expectedNodeId, String... dependents) {
        for (Node currentNode : nodesOfLevel) {
            if (currentNode.getId().equals(expectedNodeId)) {
                assertThat(currentNode.getName(), is(expectedNodeName));
                assertThat(currentNode.getId(), is(expectedNodeId));
                VSMTestHelper.assertNodeHasChildren(currentNode, dependents);
                return;
            }
        }
        fail("was expecting to see node " + expectedNodeId);
    }

    private void assertThatLevelHasNodes(List<Node> nodesAtLevel, int numberOfDummyNodes, String... nodeIds) {
        assertThat(nodesAtLevel.size(), is(numberOfDummyNodes + nodeIds.length));
        List<String> nodeIdsAtLevel = new ArrayList<String>();
        for (Node node : nodesAtLevel) {
            if (!node.getType().equals(DependencyNodeType.DUMMY)) {
                nodeIdsAtLevel.add(node.getId());
            }
        }
        assertThat(nodeIdsAtLevel, CoreMatchers.hasItems(nodeIds));
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
