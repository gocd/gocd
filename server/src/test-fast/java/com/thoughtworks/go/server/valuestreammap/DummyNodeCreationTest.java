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
package com.thoughtworks.go.server.valuestreammap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.valuestreammap.DependencyNodeType;
import com.thoughtworks.go.domain.valuestreammap.NodeLevelMap;
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.VSMTestHelper;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DummyNodeCreationTest {
    private DummyNodeCreation dummyNodeCreation;

    @Before
    public void setup(){
        dummyNodeCreation = new DummyNodeCreation();
    }

    @Test
    public void shouldHandleDeepTriangle() {
        /*
             +---> d1 ---> d2 ---> d3
             g                     ^
             +---- x ----- x ------+
         */
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("d3");
        CaseInsensitiveString d1 = new CaseInsensitiveString("d1");
        CaseInsensitiveString d2 = new CaseInsensitiveString("d2");
        CaseInsensitiveString g = new CaseInsensitiveString("g");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamNode(new PipelineDependencyNode(d2, d2.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d1, d1.toString()), null, d2);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g.toString(), g.toString(), "git"), null, d1, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g.toString(), g.toString(), "git"), null, currentPipeline, new MaterialRevision(null));

        NodeLevelMap nodeLevelMap = new LevelAssignment().apply(graph);
        dummyNodeCreation.apply(graph, nodeLevelMap);

        VSMTestHelper.assertNumberOfLevelsInGraph(nodeLevelMap, 4);
        VSMTestHelper.assertThatLevelHasNodes(nodeLevelMap.get(0), 0, currentPipeline);
        VSMTestHelper.assertThatLevelHasNodes(nodeLevelMap.get(-1), 1, d2);
        VSMTestHelper.assertThatLevelHasNodes(nodeLevelMap.get(-2), 1, d1);
        VSMTestHelper.assertThatLevelHasNodes(nodeLevelMap.get(-3), 0, g);
    }

    @Test
    public void shouldCreateDummyNodeAtRightIndexToHelpInMinimizingCrossings() {
        /*
             +---> d1 ---> d4---->d5
            d3             |
             +---> X ------+
             |             |
             ------d2-----+
         */
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("d5");
        CaseInsensitiveString d4 = new CaseInsensitiveString("d4");
        CaseInsensitiveString d1 = new CaseInsensitiveString("d1");
        CaseInsensitiveString d3 = new CaseInsensitiveString("d3");
        CaseInsensitiveString d2 = new CaseInsensitiveString("d2");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamNode(new PipelineDependencyNode(d4, d4.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d1, d1.toString()), null, d4);
        graph.addUpstreamNode(new PipelineDependencyNode(d3, d3.toString()), null, d1);
        graph.addUpstreamNode(new PipelineDependencyNode(d3, d3.toString()), null, d4);
        graph.addUpstreamNode(new PipelineDependencyNode(d2, d2.toString()), null, d4);
        graph.addUpstreamNode(new PipelineDependencyNode(d3, d3.toString()), null, d2);

        NodeLevelMap nodeLevelMap = new LevelAssignment().apply(graph);
        dummyNodeCreation.apply(graph, nodeLevelMap);

        VSMTestHelper.assertNumberOfLevelsInGraph(nodeLevelMap, 4);
        VSMTestHelper.assertThatNodeHasChildren(graph, d3, 1, d1, d2);
        Node secondChildOfD3 = graph.findNode(d3).getChildren().get(1);
        assertThat(secondChildOfD3.getType(), is(DependencyNodeType.DUMMY));
    }

    @Test
    public void shouldMoveNodeAndIntroduceDummyNodesCorrectly_shouldHandleDoubleTriangle() {
        /*
        * +----- X ------+
        * |              v
        * g---->d1----->d2 ---> d3
        *        |              ^
        *        ------ X -----+
        */
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("d3");
        CaseInsensitiveString d2 = new CaseInsensitiveString("d2");
        CaseInsensitiveString d1 = new CaseInsensitiveString("d1");
        CaseInsensitiveString g = new CaseInsensitiveString("g");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamNode(new PipelineDependencyNode(d2, d2.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d1, d1.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d1, d1.toString()), null, d2);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g.toString(), g.toString(), "git"), null, d1, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g.toString(), g.toString(), "git"), null, d2, new MaterialRevision(null));

        NodeLevelMap nodeLevelMap = new LevelAssignment().apply(graph);
        dummyNodeCreation.apply(graph, nodeLevelMap);

        VSMTestHelper.assertNumberOfLevelsInGraph(nodeLevelMap, 4);
        assertThat(nodeLevelMap.get(0).size(), is(1));
        assertThat(nodeLevelMap.get(-1).size(), is(2));
        assertThat(nodeLevelMap.get(-2).size(), is(2));
        assertThat(nodeLevelMap.get(-3).size(), is(1));

        VSMTestHelper.assertThatNodeHasChildren(graph, d1, 1, d2);
        VSMTestHelper.assertThatNodeHasChildren(graph, g, 1, d1);

        VSMTestHelper.assertThatNodeHasParents(graph, new CaseInsensitiveString("d3"), 1, d2);
        VSMTestHelper.assertThatNodeHasParents(graph, d2, 1, d1);
        VSMTestHelper.assertThatNodeHasParents(graph, d1, 0, g);
    }
    @Test
    public void shouldMoveNodeAndIntroduceDummyNodesToCorrectLayer() {
        /*
           d2 -- X -----+
            |           V
             ==> d4 --> d1
            |           ^
           d3 -- X -----+
         */
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("d1");
        CaseInsensitiveString d2 = new CaseInsensitiveString("d2");
        CaseInsensitiveString d3 = new CaseInsensitiveString("d3");
        CaseInsensitiveString d4 = new CaseInsensitiveString("d4");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamNode(new PipelineDependencyNode(d2, d2.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d3, d3.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d4, d4.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d2, d2.toString()), null, d4);
        graph.addUpstreamNode(new PipelineDependencyNode(d3, d3.toString()), null, d4);

        NodeLevelMap nodeLevelMap = new LevelAssignment().apply(graph);
        dummyNodeCreation.apply(graph, nodeLevelMap);

        VSMTestHelper.assertNumberOfLevelsInGraph(nodeLevelMap, 3);
        assertThat(nodeLevelMap.get(0).size(), is(1));
        assertThat(nodeLevelMap.get(-1).size(), is(3));
        assertThat(nodeLevelMap.get(-2).size(), is(2));
    }

    @Test
    public void shouldMoveNodeAndIntroduceDummyNodesToCorrectLayer_crossMaterialPipelineDependency() {
        /*
           g1 --- X -----+
            |           V
            +---> d1 --> d2 -->
                               d5
           g2 --> d3 --> d4 -->
            |           ^
            +---- X -----+
         */
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("d5");
        CaseInsensitiveString d2 = new CaseInsensitiveString("d2");
        CaseInsensitiveString d1 = new CaseInsensitiveString("d1");
        CaseInsensitiveString g1 = new CaseInsensitiveString("g1");
        CaseInsensitiveString d4 = new CaseInsensitiveString("d4");
        CaseInsensitiveString d3 = new CaseInsensitiveString("d3");
        CaseInsensitiveString g2 = new CaseInsensitiveString("g2");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamNode(new PipelineDependencyNode(d2, d2.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d1, d1.toString()), null, d2);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1.toString(), g1.toString(), "git"), null, d1, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1.toString(), g1.toString(), "git"), null, d2, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(d4, d4.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(d3, d3.toString()), null, d4);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g2.toString(), g2.toString(), "git"), null, d3, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g2.toString(), g2.toString(), "git"), null, d4, new MaterialRevision(null));

        NodeLevelMap nodeLevelMap = new LevelAssignment().apply(graph);
        dummyNodeCreation.apply(graph, nodeLevelMap);

        VSMTestHelper.assertNumberOfLevelsInGraph(nodeLevelMap, 4);
        VSMTestHelper.assertThatLevelHasNodes(nodeLevelMap.get(0), 0, new CaseInsensitiveString("d5"));
        VSMTestHelper.assertThatLevelHasNodes(nodeLevelMap.get(-1), 0, d2, d4);
        VSMTestHelper.assertThatLevelHasNodes(nodeLevelMap.get(-2), 2, d1, d3);
        VSMTestHelper.assertThatLevelHasNodes(nodeLevelMap.get(-3), 0, g1, g2);
    }

    @Test
    public void shouldCreateDummyNodesAtCorrectLevelsWhenNodesAreAddedUsingDFS() {
        /*                +-------X------+
                          |              v
             +---> p1 --> p4 --> p5 ---> P6
            |             ^      ^
           git --> p2 ----+      |
            |                    |
            +---> X -----p3------+
         */
        CaseInsensitiveString currentPipeline = new CaseInsensitiveString("p6");
        CaseInsensitiveString p4 = new CaseInsensitiveString("p4");
        CaseInsensitiveString p1 = new CaseInsensitiveString("p1");
        CaseInsensitiveString git = new CaseInsensitiveString("git");
        CaseInsensitiveString p2 = new CaseInsensitiveString("p2");
        CaseInsensitiveString p5 = new CaseInsensitiveString("p5");
        CaseInsensitiveString p3 = new CaseInsensitiveString("p3");
        ValueStreamMap graph = new ValueStreamMap(currentPipeline, null);
        graph.addUpstreamNode(new PipelineDependencyNode(p4, p4.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(p1, p1.toString()), null, p4);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(git.toString(), git.toString(), git.toString()), null, p1, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(p2, p2.toString()), null, p4);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(git.toString(), git.toString(), git.toString()), null, p2, new MaterialRevision(null));

        graph.addUpstreamNode(new PipelineDependencyNode(p5, p5.toString()), null, currentPipeline);
        graph.addUpstreamNode(new PipelineDependencyNode(p3, p3.toString()), null, p5);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(git.toString(), git.toString(), git.toString()), null, p3, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(p4, p4.toString()), null, p5);

        graph.addUpstreamNode(new PipelineDependencyNode(p1, p1.toString()), null, p4);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(git.toString(), git.toString(), git.toString()), null, p1, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(p2, p2.toString()), null, p4);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(git.toString(), git.toString(), git.toString()), null, p2, new MaterialRevision(null));

        NodeLevelMap nodeLevelMap = new LevelAssignment().apply(graph);
        dummyNodeCreation.apply(graph, nodeLevelMap);

        VSMTestHelper.assertNumberOfLevelsInGraph(nodeLevelMap, 5);
        assertThat(nodeLevelMap.get(0).size(), is(1));
        assertThat(nodeLevelMap.get(-1).size(), is(2));
        assertThat(nodeLevelMap.get(-2).size(), is(2));
        assertThat(nodeLevelMap.get(-3).size(), is(3));
        assertThat(nodeLevelMap.get(-4).size(), is(1));
    }
}
