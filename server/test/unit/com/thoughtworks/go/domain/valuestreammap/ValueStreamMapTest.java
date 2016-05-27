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

package com.thoughtworks.go.domain.valuestreammap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ValueStreamMapTest {
    @Test
    public void shouldKeepFirstNodeAtLevelZero() {
        /*
            P1
         */
        String pipelineName = "P1";
        ValueStreamMap graph = new ValueStreamMap(pipelineName, null);

        assertThat(graph.getCurrentPipeline().getId(), is(pipelineName));
        assertThat(graph.getCurrentPipeline().getName(), is(pipelineName));
        assertThat(graph.getCurrentPipeline().getChildren().isEmpty(), is(true));

        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();
        assertThat(nodesAtEachLevel, hasSize(1));
        assertThat(nodesAtEachLevel.get(0), contains(graph.getCurrentPipeline()));
    }

    @Test
    public void shouldKeepUpstreamNodeAtALevelLessThanDependent() {
        /*
            git_fingerprint -> P1
         */
        String dependent = "P1";
        ValueStreamMap graph = new ValueStreamMap(dependent, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), null, dependent, new MaterialRevision(null));
        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();

        assertThat(nodesAtEachLevel.size(), is(2));
        assertThat(nodesAtEachLevel.get(0).size(), is(1));

        Node gitScmNode = nodesAtEachLevel.get(0).get(0);
        assertThat(gitScmNode.getId(), is("git_fingerprint"));
        assertThat(gitScmNode.getName(), is("git"));
        VSMTestHelper.assertThatNodeHasChildren(graph, "git_fingerprint", 0, dependent);
    }

    @Test
    public void shouldPopulateAllNamesOfAMaterial() {
        /*
            git_fingerprint -> P1 -> P2
            |_________________________^
         */

        String P1 = "P1";
        String currentPipeline = "P2";
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), currentPipeline, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(P1, P1), null, currentPipeline);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git2"), P1, new MaterialRevision(null));

        SCMDependencyNode node = (SCMDependencyNode) graph.findNode("git_fingerprint");
        HashSet<String> materialNames = new HashSet<String>();
        materialNames.add("git1");
        materialNames.add("git2");

        assertThat(node.getMaterialNames(), is(materialNames));
    }

    @Test
    public void shouldPopulateSCMDependencyNodeWithMaterialRevisions() {
        /*
            git_fingerprint -> P1 ---- \
                          \              -> p3
                            -> P2 ---- /
         */

        String P1 = "P1";
        String P2 = "P2";
        String currentPipeline = "P3";
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        MaterialRevision revision1 = new MaterialRevision(MaterialsMother.gitMaterial("test/repo1"), oneModifiedFile("revision1"));
        MaterialRevision revision2 = new MaterialRevision(MaterialsMother.gitMaterial("test/repo2"), oneModifiedFile("revision2"));

        graph.addUpstreamNode(new PipelineDependencyNode(P1, P1), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(P2, P2), null, currentPipeline);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), P1, revision1);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), P2, revision2);

        SCMDependencyNode node = (SCMDependencyNode) graph.findNode("git_fingerprint");

        assertThat(node.getMaterialRevisions().size(), is(2));
        assertTrue(node.getMaterialRevisions().contains(revision1));
        assertTrue(node.getMaterialRevisions().contains(revision2));
    }

    @Test
    public void shouldNotPopulateDuplicateNamesForAMaterial() {
        /*
            git_fingerprint -> P1 -> P2
            |_________________________^
         */

        String P1 = "P1";
        String currentPipeline = "P2";
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), currentPipeline, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(P1, P1), null, currentPipeline);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), P1, new MaterialRevision(null));

        SCMDependencyNode node = (SCMDependencyNode) graph.findNode("git_fingerprint");
        HashSet<String> materialNames = new HashSet<String>();
        materialNames.add("git1");

        assertThat(node.getMaterialNames(), is(materialNames));
    }

    @Test
    public void shouldNotPopulateAnyMaterialNamesForAMaterial_WhenNoNamesAreGiven() {
        /*
            git_fingerprint -> P1 -> P2
            |_________________________^
         */

        String P1 = "P1";
        String currentPipeline = "P2";
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "http://git.com", "git"), null, currentPipeline, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(P1, P1), null, currentPipeline);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "http://git.com", "git"), null , P1, new MaterialRevision(null));

        SCMDependencyNode node = (SCMDependencyNode) graph.findNode("git_fingerprint");

        assertTrue(node.getMaterialNames().isEmpty());
    }

    @Test
    public void shouldNotAddDuplicateDependents() {
        /*
             p4 -> p5
         */
        String currentPipeline = "p5";
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        Node p4 = graph.addUpstreamNode(new PipelineDependencyNode("p4", "p4"), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode("p4", "p4"), null, currentPipeline);

        assertThat(p4.getChildren().size(), is(1));
        VSMTestHelper.assertThatNodeHasChildren(graph, "p4", 0, "p5");
    }

    @Test
    public void shouldUpdateDependentIfNodeAlreadyPresent() {
        /*
             +---> d1 ---> P1
            d3             ^
             +---> d2 -----+
         */
        String dependent = "P1";
        ValueStreamMap graph = new ValueStreamMap(dependent, null);
        graph.addUpstreamNode(new PipelineDependencyNode("d1", "d1"), null, dependent);
        graph.addUpstreamNode(new PipelineDependencyNode("d2", "d2"), null, dependent);
        graph.addUpstreamNode(new PipelineDependencyNode("d3", "d3"), null, "d1");
        graph.addUpstreamNode(new PipelineDependencyNode("d3", "d3"), null, "d2");

        VSMTestHelper.assertThatNodeHasChildren(graph, "d3", 0, "d1", "d2");
    }


    @Test
    public void shouldGetTheLevelsInSortedOrder() {
        /*
             +---> d1 ---> P1
            d3             ^
             +---> d2 -----+
         */
        String currentPipeline = "P1";
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamNode(new PipelineDependencyNode("d1", "d1"), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode("d2", "d2"), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode("d3", "d3"), null, "d1");
        graph.addUpstreamNode(new PipelineDependencyNode("d3", "d3"), null, "d2");

        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();

        assertThat(nodesAtEachLevel.size(), is(3));
        assertThat(nodesAtEachLevel.get(0).get(0).getName(), is("d3"));
        assertThat(nodesAtEachLevel.get(1).get(0).getName(), is("d1"));
        assertThat(nodesAtEachLevel.get(1).get(1).getName(), is("d2"));
        assertThat(nodesAtEachLevel.get(2).get(0).getName(), is(currentPipeline));
    }

    @Test
    public void shouldAddADownstreamNode(){
        String p1 = "p1";
        String p2 = "p2";
        ValueStreamMap graph = new ValueStreamMap(p1, null);
        graph.addDownstreamNode(new PipelineDependencyNode(p2, p2), p1);
        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, p1);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, p2);
        VSMTestHelper.assertNodeHasChildren(graph, p1, p2);
        VSMTestHelper.assertNodeHasParents(graph, p2, p1);
    }

	@Test
	public void shouldGetRootNodesCorrectly() {
		/*
				git-trunk  git-plugins-->plugins ---->acceptance
						\             /                 ^ ^
						 \          /                   | |
						  \       /                     | |
				hg-trunk--->cruise +--------------------+ |
				  +                                       |
				  +---------------------------------------+
		*/

		String acceptance = "acceptance";
		String plugins = "plugins";
		String gitPlugins = "git-plugins";
		String cruise = "cruise";
		String gitTrunk = "git-trunk";
		String hgTrunk = "hg-trunk";

		ValueStreamMap graph = new ValueStreamMap(acceptance, null);
		graph.addUpstreamNode(new PipelineDependencyNode(plugins, plugins), null, acceptance);
		graph.addUpstreamNode(new PipelineDependencyNode(gitPlugins, gitPlugins), null, plugins);
		graph.addUpstreamNode(new PipelineDependencyNode(cruise, cruise), null, plugins);
		graph.addUpstreamMaterialNode(new SCMDependencyNode(gitTrunk, gitTrunk, "git"), null, cruise, new MaterialRevision(null));
		graph.addUpstreamMaterialNode(new SCMDependencyNode(hgTrunk, hgTrunk, "hg"), null, cruise, new MaterialRevision(null));
		graph.addUpstreamNode(new PipelineDependencyNode(cruise, cruise), null, acceptance);
		graph.addUpstreamMaterialNode(new SCMDependencyNode(hgTrunk, hgTrunk, "hg"), null, acceptance, new MaterialRevision(null));

		List<Node> rootNodes = graph.getRootNodes();
		assertThat(rootNodes.size(), is(3));
		assertThat(getNodeIds(rootNodes), contains(gitPlugins, gitTrunk, hgTrunk));
	}

	private List<String> getNodeIds(List<Node> rootNodes) {
		List<String> nodeIds = new LinkedList<String>();
		for (Node rootNode : rootNodes) {
			nodeIds.add(rootNode.getId());
		}
		Collections.sort(nodeIds);
		return nodeIds;
	}

	@Test
    public void shouldUpdateDependentWhileAddingDownstreamIfNodeAlreadyPresent(){
          /*
             +---> p2 ---> p3
             p1             ^
             +----------- +
         */
        String p1 = "p1";
        String p2 = "p2";
        String p3 = "p3";
        ValueStreamMap graph = new ValueStreamMap(p1, null);
        graph.addDownstreamNode(new PipelineDependencyNode(p2, p2), p1);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3), p2);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3), p1);

        VSMTestHelper.assertNodeHasChildren(graph, p1, p2, p3);
        VSMTestHelper.assertNodeHasChildren(graph, p2, p3);
        assertThat(graph.findNode(p3).getChildren().isEmpty(), is(true));
        assertThat(graph.findNode(p1).getParents().isEmpty(), is(true));
        VSMTestHelper.assertNodeHasParents(graph, p2, p1);
        VSMTestHelper.assertNodeHasParents(graph, p3, p1, p2);

        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(3));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, p1);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 1, p2);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, p3);
    }

    @Test
    public void shouldAssignLevelsAndInsertDummyNodes(){
          /*
             +------> p2 ---> p3
             p1                ^
             +-----------------+
         */
        String p1 = "p1";
        String p2 = "p2";
        String p3 = "p3";
        ValueStreamMap graph = new ValueStreamMap(p1, null);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3), p1);

        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(2));
        VSMTestHelper.assertNodeHasChildren(graph, p1, p3);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, p1);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, p3);

        graph.addDownstreamNode(new PipelineDependencyNode(p2, p2), p1);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3), p2);

        VSMTestHelper.assertNodeHasChildren(graph, p1, p2, p3);
        VSMTestHelper.assertNodeHasChildren(graph, p2, p3);
        VSMTestHelper.assertNodeHasParents(graph, p3, p1, p2);
        VSMTestHelper.assertNodeHasParents(graph, p2, p1);
        assertThat(graph.findNode(p3).getChildren().isEmpty(), is(true));
        assertThat(graph.findNode(p1).getParents().isEmpty(), is(true));

        nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(3));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, p1);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 1, p2);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, p3);
    }

    @Test
    public void shouldOptimizeVisualizationOfThePipelineDependencyGraph() {
        /*

                git-trunk  git-plugins-->plugins ---->acceptance---->deploy-go03---> publish --->deploy-go01
                        \             /                 ^ ^   \                                    ^
                         \          /                   | |    \                                  /
                          \       /                     | |     \                               /
                hg-trunk--->cruise +--------------------+ |      +-->deploy-go02---------------+
                  +                                       |
                  +---------------------------------------+
        */

        String acceptance = "acceptance";
        String plugins = "plugins";
        String gitPlugins = "git-plugins";
        String cruise = "cruise";
        String gitTrunk = "git-trunk";
        String hgTrunk = "hg-trunk";
        String deployGo03 = "deploy-go03";
        String deployGo02 = "deploy-go02";
        String deployGo01 = "deploy-go01";
        String publish = "publish";

        ValueStreamMap graph = new ValueStreamMap(acceptance, null);
        graph.addUpstreamNode(new PipelineDependencyNode(plugins, plugins), null, acceptance);
        graph.addUpstreamNode(new PipelineDependencyNode(gitPlugins, gitPlugins), null, plugins);
        graph.addUpstreamNode(new PipelineDependencyNode(cruise, cruise), null, plugins);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(gitTrunk, gitTrunk, "git"), null, cruise, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(hgTrunk, hgTrunk, "hg"), null, cruise, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(cruise, cruise), null, acceptance);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(hgTrunk, hgTrunk, "hg"), null, acceptance, new MaterialRevision(null));

        graph.addDownstreamNode(new PipelineDependencyNode(deployGo03, deployGo03), acceptance);
        graph.addDownstreamNode(new PipelineDependencyNode(publish, publish), deployGo03);
        graph.addDownstreamNode(new PipelineDependencyNode(deployGo01, deployGo01), publish);
        graph.addDownstreamNode(new PipelineDependencyNode(deployGo02, deployGo02), acceptance);
        graph.addDownstreamNode(new PipelineDependencyNode(deployGo01, deployGo01), deployGo02);

        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();

        assertThat(nodesAtEachLevel.size(), is(7));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, gitTrunk, hgTrunk);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 1, gitPlugins, cruise);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 2, plugins);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(3), 0, acceptance);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(4), 0, deployGo03, deployGo02);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(5), 1, publish);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(6), 0, deployGo01);

        MatcherAssert.assertThat(graph.findNode(gitTrunk).getDepth(), is(2));
        MatcherAssert.assertThat(graph.findNode(hgTrunk).getDepth(), is(3));
        MatcherAssert.assertThat(graph.findNode(gitPlugins).getDepth(), is(1));
        MatcherAssert.assertThat(graph.findNode(cruise).getDepth(), is(2));
        MatcherAssert.assertThat(graph.findNode(deployGo03).getDepth(), is(1));
        MatcherAssert.assertThat(graph.findNode(deployGo02).getDepth(), is(2));
        MatcherAssert.assertThat(graph.findNode(publish).getDepth(), is(1));
        MatcherAssert.assertThat(graph.findNode(deployGo01).getDepth(), is(1));
    }

    @Test
    public void shouldThrowCyclicDependencyExceptionIfACycleIsDetected_DownstreamOfCurrentWasUpstreamOfCurrentAtSomePoint() {
        /*
        *  config version 1:
        *  grandParent -> parent -> current -> child
        *  all pipelines run once
        *  config version 2:
        *  parent -> current -> child -> grandParent
        * */
        String grandParent = "grandParent";
        String parent = "parent";
        String child = "child";
        String currentPipeline = "current";
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addDownstreamNode(new PipelineDependencyNode(child, child), currentPipeline);
        graph.addDownstreamNode(new PipelineDependencyNode(grandParent, grandParent), child);
        graph.addUpstreamNode(new PipelineDependencyNode(parent, parent), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(grandParent, grandParent), null, parent);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("g","g","git") , null, grandParent, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode("g","g","git") , null, parent, new MaterialRevision(null));

        assertThat(graph.hasCycle(), is(true));
    }

    @Test
    public void shouldNotConsiderTriangleDependencyAsCyclic(){
        /*
         *    g --> A -> D --------> B ----> C
         *         |________________________^
         */

        String a = "A";
        String b = "B";
        String c = "C";
        String d = "D";
        ValueStreamMap valueStreamMap = new ValueStreamMap(c, null);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(a, a), null, c);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(b, b), null, c);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(d, d), null, b);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(a, a), null, d);
        valueStreamMap.addUpstreamMaterialNode(new SCMDependencyNode("g", "g", "git"), null, a, new MaterialRevision(null));

        assertThat(valueStreamMap.hasCycle(), is(false));
    }

    @Test
    public void shouldThrowCyclicDependencyExceptionIfACycleIsDetected_CycleDetectedInUpstreamNodes() {
        /*
        *  git -> A1 -> B1
        *  all run once
        *  config changes to:
        *  git -> B1 -> A2 -> C1
        *
        */

        String a = "A";
        String b = "B";
        String c = "C";
        ValueStreamMap graph = new ValueStreamMap(b, null);
        graph.addUpstreamNode(new PipelineDependencyNode(a, a), null, b);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("g","g","git"), null, a, new MaterialRevision(null));
        graph.addDownstreamNode(new PipelineDependencyNode(a, a), b);
        graph.addDownstreamNode(new PipelineDependencyNode(c, c), a);

        assertThat(graph.hasCycle(), is(true));
    }

    @Test
    public void shouldDetectCycleWhenUpstreamLeavesAreAtDifferentLevels() {
        /*
         *   g1 --> p1 --> p2 --> p3 --> p4 ------------\
         *                                                 --> current
         *          g2 --> p5(1) --> p6(1) --> p5(2) ---/
         */

        String g1 = "g1";
        String g2 = "g2";
        String p1 = "p1";
        String p2 = "p2";
        String p3 = "p3";
        String p4 = "p4";
        String p5 = "p5";
        String p6 = "p6";
        String current = "current";

        ValueStreamMap valueStreamMap = new ValueStreamMap(current, null);

        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p4, p4), null, current);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p3, p3), null, p4);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p2, p2), null, p3);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p1, p1), null, p2);
        valueStreamMap.addUpstreamMaterialNode(new SCMDependencyNode(g1, g1, "git"), null, p1, new MaterialRevision(null));

        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p5, p5), new PipelineRevision(p5,2,"2"), current);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p6, p6), new PipelineRevision(p6,1,"1"), p5);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p5, p5), new PipelineRevision(p5,1,"1"), p6);
        valueStreamMap.addUpstreamMaterialNode(new SCMDependencyNode(g2,g2,"git"), null, p5, new MaterialRevision(null));

        assertThat(valueStreamMap.hasCycle(), is(true));
    }

    @Test
    public void currentPipelineShouldHaveWarningsIfBuiltFromIncompatibleRevisions() {
        String current = "current";

        ValueStreamMap valueStreamMap = new ValueStreamMap(current, null);
        SCMDependencyNode scmDependencyNode = new SCMDependencyNode("id", "git_node", "git");
        MaterialRevision rev1 = new MaterialRevision(MaterialsMother.gitMaterial("git/repo/url"), ModificationsMother.oneModifiedFile("rev1"));
        MaterialRevision rev2 = new MaterialRevision(MaterialsMother.gitMaterial("git/repo/url"), ModificationsMother.oneModifiedFile("rev2"));

        valueStreamMap.addUpstreamMaterialNode(scmDependencyNode, new CaseInsensitiveString("git"), valueStreamMap.getCurrentPipeline().getId(), rev1);
        valueStreamMap.addUpstreamMaterialNode(scmDependencyNode, new CaseInsensitiveString("git"), valueStreamMap.getCurrentPipeline().getId(), rev2);

        valueStreamMap.addWarningIfBuiltFromInCompatibleRevisions();

        assertThat(valueStreamMap.getCurrentPipeline().getViewType(), is(is(VSMViewType.WARNING)));
    }

    @Test
    public void currentPipelineShouldNotHaveWarningsIfBuiltFromMultipleMaterialRevisionsWithSameLatestRevision() {
        String current = "current";

        ValueStreamMap valueStreamMap = new ValueStreamMap(current, null);
        SCMDependencyNode scmDependencyNode = new SCMDependencyNode("id", "git_node", "git");
        MaterialRevision rev1 = new MaterialRevision(MaterialsMother.gitMaterial("git/repo/url"), ModificationsMother.oneModifiedFile("rev1"));
        MaterialRevision rev2 = new MaterialRevision(MaterialsMother.gitMaterial("git/repo/url"), ModificationsMother.oneModifiedFile("rev1"), ModificationsMother.oneModifiedFile("rev2"));

        valueStreamMap.addUpstreamMaterialNode(scmDependencyNode, new CaseInsensitiveString("git"), valueStreamMap.getCurrentPipeline().getId(), rev1);
        valueStreamMap.addUpstreamMaterialNode(scmDependencyNode, new CaseInsensitiveString("git"), valueStreamMap.getCurrentPipeline().getId(), rev2);

        valueStreamMap.addWarningIfBuiltFromInCompatibleRevisions();

        assertNull(valueStreamMap.getCurrentPipeline().getViewType());
    }
}

