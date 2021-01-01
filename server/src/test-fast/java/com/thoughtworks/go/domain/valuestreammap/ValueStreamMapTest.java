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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ValueStreamMapTest {
    @Test
    public void shouldKeepFirstNodeAtLevelZero() {
        /*
            P1
         */
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("P1");
        ValueStreamMap graph = new ValueStreamMap(pipelineName, null);

        assertThat(graph.getCurrentPipeline().getId(), is(pipelineName));
        assertThat(graph.getCurrentPipeline().getName(), is(pipelineName.toString()));
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
        CaseInsensitiveString dependent = new CaseInsensitiveString("P1");
        ValueStreamMap graph = new ValueStreamMap(dependent, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), null, dependent, new MaterialRevision(null));
        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();

        assertThat(nodesAtEachLevel.size(), is(2));
        assertThat(nodesAtEachLevel.get(0).size(), is(1));

        Node gitScmNode = nodesAtEachLevel.get(0).get(0);
        assertThat(gitScmNode.getId().toString(), is("git_fingerprint"));
        assertThat(gitScmNode.getName(), is("git"));
        VSMTestHelper.assertThatNodeHasChildren(graph, new CaseInsensitiveString("git_fingerprint"), 0, dependent);
    }

    @Test
    public void shouldPopulateAllNamesOfAMaterial() {
        /*
            git_fingerprint -> P1 -> P2
            |_________________________^
         */

        CaseInsensitiveString P1 = new CaseInsensitiveString("P1");
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("P2");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), currentPipeline, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(P1, P1.toString()), null, currentPipeline);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git2"), P1, new MaterialRevision(null));

        SCMDependencyNode node = (SCMDependencyNode) graph.findNode(new CaseInsensitiveString("git_fingerprint"));
        HashSet<String> materialNames = new HashSet<>();
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

        CaseInsensitiveString P1 = new CaseInsensitiveString("P1");
        CaseInsensitiveString P2 = new CaseInsensitiveString("P2");
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("P3");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        MaterialRevision revision1 = new MaterialRevision(MaterialsMother.gitMaterial("test/repo1"), oneModifiedFile("revision1"));
        MaterialRevision revision2 = new MaterialRevision(MaterialsMother.gitMaterial("test/repo2"), oneModifiedFile("revision2"));

        graph.addUpstreamNode(new PipelineDependencyNode(P1, P1.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(P2, P2.toString()), null, currentPipeline);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), P1, revision1);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), P2, revision2);

        SCMDependencyNode node = (SCMDependencyNode) graph.findNode(new CaseInsensitiveString("git_fingerprint"));

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

        CaseInsensitiveString P1 = new CaseInsensitiveString("P1");
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("P2");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), currentPipeline, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(P1, P1.toString()), null, currentPipeline);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "git", "git"), new CaseInsensitiveString("git1"), P1, new MaterialRevision(null));

        SCMDependencyNode node = (SCMDependencyNode) graph.findNode(new CaseInsensitiveString("git_fingerprint"));
        HashSet<String> materialNames = new HashSet<>();
        materialNames.add("git1");

        assertThat(node.getMaterialNames(), is(materialNames));
    }

    @Test
    public void shouldNotPopulateAnyMaterialNamesForAMaterial_WhenNoNamesAreGiven() {
        /*
            git_fingerprint -> P1 -> P2
            |_________________________^
         */

        CaseInsensitiveString P1 = new CaseInsensitiveString("P1");
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("P2");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "http://git.com", "git"), null, currentPipeline, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(P1, P1.toString()), null, currentPipeline);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("git_fingerprint", "http://git.com", "git"), null , P1, new MaterialRevision(null));

        SCMDependencyNode node = (SCMDependencyNode) graph.findNode(new CaseInsensitiveString("git_fingerprint"));

        assertTrue(node.getMaterialNames().isEmpty());
    }

    @Test
    public void shouldNotAddDuplicateDependents() {
        /*
             p4 -> p5
         */
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("p5");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        CaseInsensitiveString p4Name = new CaseInsensitiveString("p4");
        Node p4 = graph.addUpstreamNode(new PipelineDependencyNode(p4Name, p4Name.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(p4Name, p4Name.toString()), null, currentPipeline);

        assertThat(p4.getChildren().size(), is(1));
        VSMTestHelper.assertThatNodeHasChildren(graph, p4Name, 0, new CaseInsensitiveString("p5"));
    }

    @Test
    public void shouldUpdateDependentIfNodeAlreadyPresent() {
        /*
             +---> d1 ---> P1
            d3             ^
             +---> d2 -----+
         */
        CaseInsensitiveString dependent = new CaseInsensitiveString("P1");
        CaseInsensitiveString d1 = new CaseInsensitiveString("d1");
        CaseInsensitiveString d2 = new CaseInsensitiveString("d2");
        CaseInsensitiveString d3 = new CaseInsensitiveString("d3");
        ValueStreamMap graph = new ValueStreamMap(dependent, null);
        graph.addUpstreamNode(new PipelineDependencyNode(d1, d1.toString()), null, dependent);
        graph.addUpstreamNode(new PipelineDependencyNode(d2, d2.toString()), null, dependent);
        graph.addUpstreamNode(new PipelineDependencyNode(d3, d3.toString()), null, d1);
        graph.addUpstreamNode(new PipelineDependencyNode(d3, d3.toString()), null, d2);

        VSMTestHelper.assertThatNodeHasChildren(graph, d3, 0, d1, d2);
    }


    @Test
    public void shouldGetTheLevelsInSortedOrder() {
        /*
             +---> d1 ---> P1
            d3             ^
             +---> d2 -----+
         */
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("P1");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamNode(new PipelineDependencyNode(new CaseInsensitiveString("d1"), "d1"), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(new CaseInsensitiveString("d2"), "d2"), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(new CaseInsensitiveString("d3"), "d3"), null, new CaseInsensitiveString("d1"));
        graph.addUpstreamNode(new PipelineDependencyNode(new CaseInsensitiveString("d3"), "d3"), null, new CaseInsensitiveString("d2"));

        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();

        assertThat(nodesAtEachLevel.size(), is(3));
        assertThat(nodesAtEachLevel.get(0).get(0).getName(), is("d3"));
        assertThat(nodesAtEachLevel.get(1).get(0).getName(), is("d1"));
        assertThat(nodesAtEachLevel.get(1).get(1).getName(), is("d2"));
        assertThat(nodesAtEachLevel.get(2).get(0).getName(), is(currentPipeline.toString()));
    }

    @Test
    public void shouldAddADownstreamNode(){
        CaseInsensitiveString p1 = new CaseInsensitiveString("p1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("p2");
        ValueStreamMap graph = new ValueStreamMap(p1, null);
        graph.addDownstreamNode(new PipelineDependencyNode(p2, p2.toString()), p1);
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

		CaseInsensitiveString acceptance = new CaseInsensitiveString("acceptance");
		CaseInsensitiveString plugins = new CaseInsensitiveString("plugins");
		CaseInsensitiveString gitPlugins = new CaseInsensitiveString("git-plugins");
		CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
		CaseInsensitiveString gitTrunk = new CaseInsensitiveString("git-trunk");
		CaseInsensitiveString hgTrunk = new CaseInsensitiveString("hg-trunk");

		ValueStreamMap graph = new ValueStreamMap(acceptance, null);
		graph.addUpstreamNode(new PipelineDependencyNode(plugins, plugins.toString()), null, acceptance);
		graph.addUpstreamNode(new PipelineDependencyNode(gitPlugins, gitPlugins.toString()), null, plugins);
		graph.addUpstreamNode(new PipelineDependencyNode(cruise, cruise.toString()), null, plugins);
		graph.addUpstreamMaterialNode(new SCMDependencyNode(gitTrunk.toString(), gitTrunk.toString(), "git"), null, cruise, new MaterialRevision(null));
		graph.addUpstreamMaterialNode(new SCMDependencyNode(hgTrunk.toString(), hgTrunk.toString(), "hg"), null, cruise, new MaterialRevision(null));
		graph.addUpstreamNode(new PipelineDependencyNode(cruise, cruise.toString()), null, acceptance);
		graph.addUpstreamMaterialNode(new SCMDependencyNode(hgTrunk.toString(), hgTrunk.toString(), "hg"), null, acceptance, new MaterialRevision(null));

		List<Node> rootNodes = graph.getRootNodes();
		assertThat(rootNodes.size(), is(3));
		assertThat(getNodeIds(rootNodes), contains(gitPlugins, gitTrunk, hgTrunk));
	}

	private List<CaseInsensitiveString> getNodeIds(List<Node> rootNodes) {
		List<CaseInsensitiveString> nodeIds = new LinkedList<>();
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
        CaseInsensitiveString p1 = new CaseInsensitiveString("p1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("p2");
        CaseInsensitiveString p3 = new CaseInsensitiveString("p3");
        ValueStreamMap graph = new ValueStreamMap(p1, null);
        graph.addDownstreamNode(new PipelineDependencyNode(p2, p2.toString()), p1);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3.toString()), p2);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3.toString()), p1);

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
        CaseInsensitiveString p1 = new CaseInsensitiveString("p1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("p2");
        CaseInsensitiveString p3 = new CaseInsensitiveString("p3");
        ValueStreamMap graph = new ValueStreamMap(p1, null);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3.toString()), p1);

        List<List<Node>> nodesAtEachLevel = graph.presentationModel().getNodesAtEachLevel();
        assertThat(nodesAtEachLevel.size(), is(2));
        VSMTestHelper.assertNodeHasChildren(graph, p1, p3);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, p1);
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, p3);

        graph.addDownstreamNode(new PipelineDependencyNode(p2, p2.toString()), p1);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3.toString()), p2);

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

        CaseInsensitiveString acceptance = new CaseInsensitiveString("acceptance");
        CaseInsensitiveString plugins = new CaseInsensitiveString("plugins");
        CaseInsensitiveString gitPlugins = new CaseInsensitiveString("git-plugins");
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        CaseInsensitiveString gitTrunk = new CaseInsensitiveString("git-trunk");
        CaseInsensitiveString hgTrunk = new CaseInsensitiveString("hg-trunk");
        CaseInsensitiveString deployGo03 = new CaseInsensitiveString("deploy-go03");
        CaseInsensitiveString deployGo02 = new CaseInsensitiveString("deploy-go02");
        CaseInsensitiveString deployGo01 = new CaseInsensitiveString("deploy-go01");
        CaseInsensitiveString publish = new CaseInsensitiveString("publish");

        ValueStreamMap graph = new ValueStreamMap(acceptance, null);
        graph.addUpstreamNode(new PipelineDependencyNode(plugins, plugins.toString()), null, acceptance);
        graph.addUpstreamNode(new PipelineDependencyNode(gitPlugins, gitPlugins.toString()), null, plugins);
        graph.addUpstreamNode(new PipelineDependencyNode(cruise, cruise.toString()), null, plugins);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(gitTrunk.toString(), gitTrunk.toString(), "git"), null, cruise, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(hgTrunk.toString(), hgTrunk.toString(), "hg"), null, cruise, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(cruise, cruise.toString()), null, acceptance);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(hgTrunk.toString(), hgTrunk.toString(), "hg"), null, acceptance, new MaterialRevision(null));

        graph.addDownstreamNode(new PipelineDependencyNode(deployGo03, deployGo03.toString()), acceptance);
        graph.addDownstreamNode(new PipelineDependencyNode(publish, publish.toString()), deployGo03);
        graph.addDownstreamNode(new PipelineDependencyNode(deployGo01, deployGo01.toString()), publish);
        graph.addDownstreamNode(new PipelineDependencyNode(deployGo02, deployGo02.toString()), acceptance);
        graph.addDownstreamNode(new PipelineDependencyNode(deployGo01, deployGo01.toString()), deployGo02);

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
        CaseInsensitiveString grandParent = new CaseInsensitiveString("grandParent");
        CaseInsensitiveString parent = new CaseInsensitiveString("parent");
        CaseInsensitiveString child = new CaseInsensitiveString("child");
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("current");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addDownstreamNode(new PipelineDependencyNode(child, child.toString()), currentPipeline);
        graph.addDownstreamNode(new PipelineDependencyNode(grandParent, grandParent.toString()), child);
        graph.addUpstreamNode(new PipelineDependencyNode(parent, parent.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(grandParent, grandParent.toString()), null, parent);
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

        CaseInsensitiveString a = new CaseInsensitiveString("A");
        CaseInsensitiveString b = new CaseInsensitiveString("B");
        CaseInsensitiveString c = new CaseInsensitiveString("C");
        CaseInsensitiveString d = new CaseInsensitiveString("D");
        ValueStreamMap valueStreamMap = new ValueStreamMap(c, null);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(a, a.toString()), null, c);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(b, b.toString()), null, c);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(d, d.toString()), null, b);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(a, a.toString()), null, d);
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

        CaseInsensitiveString a = new CaseInsensitiveString("A");
        CaseInsensitiveString b = new CaseInsensitiveString("B");
        CaseInsensitiveString c = new CaseInsensitiveString("C");
        ValueStreamMap graph = new ValueStreamMap(b, null);
        graph.addUpstreamNode(new PipelineDependencyNode(a, a.toString()), null, b);
        graph.addUpstreamMaterialNode(new SCMDependencyNode("g","g","git"), null, a, new MaterialRevision(null));
        graph.addDownstreamNode(new PipelineDependencyNode(a, a.toString()), b);
        graph.addDownstreamNode(new PipelineDependencyNode(c, c.toString()), a);

        assertThat(graph.hasCycle(), is(true));
    }

    @Test
    public void shouldDetectCycleWhenUpstreamLeavesAreAtDifferentLevels() {
        /*
         *   g1 --> p1 --> p2 --> p3 --> p4 ------------\
         *                                                 --> current
         *          g2 --> p5(1) --> p6(1) --> p5(2) ---/
         */

        CaseInsensitiveString g1 = new CaseInsensitiveString("g1");
        CaseInsensitiveString g2 = new CaseInsensitiveString("g2");
        CaseInsensitiveString p1 = new CaseInsensitiveString("p1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("p2");
        CaseInsensitiveString p3 = new CaseInsensitiveString("p3");
        CaseInsensitiveString p4 = new CaseInsensitiveString("p4");
        CaseInsensitiveString p5 = new CaseInsensitiveString("p5");
        CaseInsensitiveString p6 = new CaseInsensitiveString("p6");
        CaseInsensitiveString current = new CaseInsensitiveString("current");

        ValueStreamMap valueStreamMap = new ValueStreamMap(current, null);

        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p4, p4.toString()), null, current);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p3, p3.toString()), null, p4);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p2, p2.toString()), null, p3);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p1, p1.toString()), null, p2);
        valueStreamMap.addUpstreamMaterialNode(new SCMDependencyNode(g1.toString(), g1.toString(), "git"), null, p1, new MaterialRevision(null));

        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p5, p5.toString()), new PipelineRevision(p5.toString(),2,"2"), current);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p6, p6.toString()), new PipelineRevision(p6.toString(),1,"1"), p5);
        valueStreamMap.addUpstreamNode(new PipelineDependencyNode(p5, p5.toString()), new PipelineRevision(p5.toString(),1,"1"), p6);
        valueStreamMap.addUpstreamMaterialNode(new SCMDependencyNode(g2.toString(),g2.toString(),"git"), null, p5, new MaterialRevision(null));

        assertThat(valueStreamMap.hasCycle(), is(true));
    }

    @Test
    public void currentPipelineShouldHaveWarningsIfBuiltFromIncompatibleRevisions() {
        CaseInsensitiveString current = new CaseInsensitiveString("current");

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
        CaseInsensitiveString current = new CaseInsensitiveString("current");

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

