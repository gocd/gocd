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

package com.thoughtworks.go.server.valuestreammap;

import java.util.Arrays;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.NodeLevelMap;
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CrossingMinimizationTest {

    private CrossingMinimization crossingMinimization;

    @Before
    public void setup(){
        crossingMinimization = new CrossingMinimization();
    }

    @Test
    public void shouldReorderNodesBasedOnBaryCenterValueForUpstreamGraph(){
        /*
            g1 --> P1--->P3
               \/      /
               /\    /
            g2    P2
         */


        String p3 = "P3";
        String p1 = "P1";
        String p2 = "P2";
        String g1 = "g1";
        String g2 = "g2";
        ValueStreamMap graph = new ValueStreamMap(p3, null);
        graph.addUpstreamNode(new PipelineDependencyNode(p1, p1), null, p3);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1, g1, "git"), null, p1, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g2, g2, "git"), null, p1, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(p2, p2), null, p3);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1, g1, "git"), null, p2, new MaterialRevision(null));

        NodeLevelMap levelToNodesMap = nodeLevelMap(graph);
        assertThat(levelToNodesMap.get(-1), is(Arrays.asList(graph.findNode(p1), graph.findNode(p2))));

        crossingMinimization.apply(levelToNodesMap);

        assertThat(levelToNodesMap.get(0), is(Arrays.asList(graph.findNode(p3))));
        assertThat(levelToNodesMap.get(-1), is(Arrays.asList(graph.findNode(p2), graph.findNode(p1))));
        assertThat(levelToNodesMap.get(-2), is(Arrays.asList(graph.findNode(g1), graph.findNode(g2))));

        assertThat(graph.findNode(g1).getDepth(), is(1));
        assertThat(graph.findNode(g2).getDepth(), is(2));
        assertThat(graph.findNode(p1).getDepth(), is(2));
        assertThat(graph.findNode(p2).getDepth(), is(1));
        assertThat(graph.findNode(p3).getDepth(), is(1));
    }


    @Test
    public void shouldAssignNodesBasedOnMinimumDepthForUpstreamGraph(){
/*
            g1 --> P1 --> P
               \        / ^
                \     /   |
            g2   +->P2    |
               \          |
                \         |
            g3 --> P3-----+
         */

        String p = "P";
        String p1 = "P1";
        String p2 = "P2";
        String p3 = "P3";
        String g1 = "g1";
        String g2 = "g2";
        String g3 = "g3";
        ValueStreamMap graph = new ValueStreamMap(p, null);

        graph.addUpstreamNode(new PipelineDependencyNode(p1, p1), null, p);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1, g1, "git"), null, p1, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(p2, p2), null, p);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1, g1, "git"), null, p2, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(p3, p3), null, p);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g2, g2, "git"), null, p3, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g3, g3, "git"), null, p3, new MaterialRevision(null));

        NodeLevelMap levelToNodesMap = nodeLevelMap(graph);
        crossingMinimization.apply(levelToNodesMap);

        assertThat(levelToNodesMap.get(0), is(Arrays.asList(graph.findNode(p))));
        assertThat(levelToNodesMap.get(-1), is(Arrays.asList(graph.findNode(p1), graph.findNode(p2), graph.findNode(p3))));
        assertThat(levelToNodesMap.get(-2), is(Arrays.asList(graph.findNode(g1), graph.findNode(g2), graph.findNode(g3))));

        assertThat(graph.findNode(g1).getDepth(), is(1));
        assertThat(graph.findNode(g2).getDepth(), is(2));
        assertThat(graph.findNode(g3).getDepth(), is(3));
        assertThat(graph.findNode(p1).getDepth(), is(1));
        assertThat(graph.findNode(p2).getDepth(), is(2));
        assertThat(graph.findNode(p3).getDepth(), is(3));
        assertThat(graph.findNode(p).getDepth(), is(1));
    }

    @Test
    public void shouldAssignNodesBasedOnMinimumDepthForDownstreamGraph(){
/*
            g1 --> P ---> P1  P3
                     \      /
                      +-> P2
         */

        ValueStreamMap graph = new ValueStreamMap("P", null);
        Node g1 = graph.addUpstreamMaterialNode(new SCMDependencyNode("g1","g1", "git"), null, "P", new MaterialRevision(null));
        Node p1 = graph.addDownstreamNode(new PipelineDependencyNode("p1","p1"), "P");
        Node p2 = graph.addDownstreamNode(new PipelineDependencyNode("P2", "P2"), "P");
        Node p3 = graph.addDownstreamNode(new PipelineDependencyNode("P3", "P3"), "P2");

        NodeLevelMap levelToNodesMap = nodeLevelMap(graph);
        crossingMinimization.apply(levelToNodesMap);

        assertThat(levelToNodesMap.get(-1), is(Arrays.asList(g1)));
        assertThat(levelToNodesMap.get(0), is(Arrays.asList(graph.getCurrentPipeline())));
        assertThat(levelToNodesMap.get(1), is(Arrays.asList(p1, p2)));
        assertThat(levelToNodesMap.get(2), is(Arrays.asList(p3)));

        assertThat(p1.getDepth(), is(1));
        assertThat(p2.getDepth(), is(2));
        assertThat(p3.getDepth(), is(2));
    }

    @Test
    public void shouldNotReassignMinimumDepthIfOverallSlopeIncreases() {
        /*

             g1    g4---->  p
               \       /  /
                \    /  /
             g2--> p1 /
                    /
             g3--> p2

        */

        ValueStreamMap graph = new ValueStreamMap("P", null);
        Node g4 = graph.addUpstreamMaterialNode(new SCMDependencyNode("g4", "g4", "git"), null, "P", new MaterialRevision(null));
        Node p1 = graph.addUpstreamNode(new PipelineDependencyNode("P1", "P1"), null, "P");
        Node g1 = graph.addUpstreamMaterialNode(new SCMDependencyNode("g1", "g1", "git"), null, "P1", new MaterialRevision(null));
        Node g2 = graph.addUpstreamMaterialNode(new SCMDependencyNode("g2", "g2", "git"), null, "P1", new MaterialRevision(null));
        Node p2 = graph.addUpstreamNode(new PipelineDependencyNode("P2", "P2"), null, "P");
        Node g3 = graph.addUpstreamMaterialNode(new SCMDependencyNode("g3", "g3", "git"), null, "P2", new MaterialRevision(null));

        NodeLevelMap levelToNodesMap = nodeLevelMap(graph);
        crossingMinimization.apply(levelToNodesMap);

        assertThat(levelToNodesMap.get(0), is(Arrays.asList(graph.getCurrentPipeline())));
        assertThat(levelToNodesMap.get(-1), is(Arrays.asList(g4, p1, p2)));
        assertThat(levelToNodesMap.get(-2), is(Arrays.asList(g1, g2, g3)));

        assertThat(graph.toString(), g1.getDepth(), is(1));
        assertThat(g2.getDepth(), is(2));
        assertThat(g3.getDepth(), is(3));
        assertThat(g4.getDepth(), is(1));
        assertThat(p1.getDepth(), is(2));
        assertThat(p2.getDepth(), is(3));
        assertThat(graph.getCurrentPipeline().getDepth(), is(1));
    }

    @Test
    public void shouldReorderBasedOnBarycenterForDownstreamGraph(){
/*
       g1 -->p ---->p1 -----> p4
             \ \            /
              \ \         /
               \ +->p2 -(---> p5
                \      /
                 \    /
                  >p3
         */

        String g1 = "g1";
        String p = "P";
        String p1 = "P1";
        String p2 = "P2";
        String p3 = "P3";
        String p4 = "P4";
        String p5 = "P5";
        ValueStreamMap graph = new ValueStreamMap(p, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1, g1, "git"), null, p, new MaterialRevision(null));
        graph.addDownstreamNode(new PipelineDependencyNode(p1, p1), p);
        graph.addDownstreamNode(new PipelineDependencyNode(p4, p4), p1);
        graph.addDownstreamNode(new PipelineDependencyNode(p2, p2), p);
        graph.addDownstreamNode(new PipelineDependencyNode(p5, p5), p2);
        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3), p);
        graph.addDownstreamNode(new PipelineDependencyNode(p4, p4), p3);

        NodeLevelMap levelToNodesMap = nodeLevelMap(graph);
        crossingMinimization.apply(levelToNodesMap);

        assertThat(levelToNodesMap.get(0), is(Arrays.asList(graph.findNode(p))));
        assertThat(levelToNodesMap.get(-1), is(Arrays.asList(graph.findNode(g1))));
        assertThat(levelToNodesMap.get(1), is(Arrays.asList(graph.findNode(p1), graph.findNode(p3), graph.findNode(p2))));
        assertThat(levelToNodesMap.get(2), is(Arrays.asList(graph.findNode(p4), graph.findNode(p5))));

        assertThat(graph.findNode(g1).getDepth(), is(1));
        assertThat(graph.findNode(p).getDepth(), is(1));
        assertThat(graph.findNode(p1).getDepth(), is(1));
        assertThat(graph.findNode(p2).getDepth(), is(3));
        assertThat(graph.findNode(p3).getDepth(), is(2));
        assertThat(graph.findNode(p4).getDepth(), is(1));
        assertThat(graph.findNode(p5).getDepth(), is(3));
    }

    @Test
    public void shouldReorderGraphsWithScmMaterialsOccurringInLevelsOtherThanTheFirst(){
/*
        g1   g3 ---> P
          \          ^
           \         |
        g2-->P1+-----+

         */

        String g1 = "g1";
        String g2 = "g2";
        String g3 = "g3";
        String p1 = "P1";
        String p = "P";

        ValueStreamMap graph = new ValueStreamMap(p, null);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g3, g3, "git"), null, p, new MaterialRevision(null));
        graph.addUpstreamNode(new PipelineDependencyNode(p1, p1), null, p);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1, g1, "git"), null, p1, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g2, g2, "git"), null, p1, new MaterialRevision(null));

        NodeLevelMap levelToNodesMap = nodeLevelMap(graph);
        crossingMinimization.apply(levelToNodesMap);

        assertThat(levelToNodesMap.get(0), is(Arrays.asList(graph.findNode(p))));
        assertThat(levelToNodesMap.get(-1), is(Arrays.asList(graph.findNode(g3), graph.findNode(p1))));
        assertThat(levelToNodesMap.get(-2), is(Arrays.asList(graph.findNode(g1), graph.findNode(g2))));

        assertThat(graph.findNode(g1).getDepth(), is(1));
        assertThat(graph.findNode(g2).getDepth(), is(2));
        assertThat(graph.findNode(g3).getDepth(), is(1));
        assertThat(graph.findNode(p1).getDepth(), is(2));
        assertThat(graph.findNode(p).getDepth(), is(1));
    }

    @Test
    public void shouldInitializeNodeDepthsInDfSOrder(){

        /*
        g2   p1 --> P --> p3   p6
          \ /    /          \/
          /\   /           / \
        g1  p2           p4   p5

         */
        String g1 = "g1";
        String g2 = "g2";
        String p1 = "p1";
        String p2 = "p2";
        String p = "p";
        String p3 = "p3";
        String p4 = "p4";
        String p5 = "p5";
        String p6 = "p6";

        ValueStreamMap graph = new ValueStreamMap(p, null);
        graph.addUpstreamNode(new PipelineDependencyNode(p1, p1), null, p);
        graph.addUpstreamNode(new PipelineDependencyNode(p2, p2), null, p);
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g2, g2, "git"), null, p2, new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode(g1, g1, "git"), null, p1, new MaterialRevision(null));

        graph.addDownstreamNode(new PipelineDependencyNode(p3, p3), p);
        graph.addDownstreamNode(new PipelineDependencyNode(p4, p4), p);
        graph.addDownstreamNode(new PipelineDependencyNode(p6, p6), p4);
        graph.addDownstreamNode(new PipelineDependencyNode(p5, p5), p3);

        crossingMinimization.initializeNodeDepths(nodeLevelMap(graph));

        assertThat(graph.findNode(g1).getDepth(), is(1));
        assertThat(graph.findNode(g2).getDepth(), is(2));
        assertThat(graph.findNode(p1).getDepth(), is(1));
        assertThat(graph.findNode(p2).getDepth(), is(2));

        assertThat(graph.findNode(p).getDepth(), is(1));

        assertThat(graph.findNode(p3).getDepth(), is(1));
        assertThat(graph.findNode(p4).getDepth(), is(2));
        assertThat(graph.findNode(p5).getDepth(), is(1));
        assertThat(graph.findNode(p6).getDepth(), is(2));
    }

    private NodeLevelMap nodeLevelMap(ValueStreamMap graph) {
        return new LevelAssignment().apply(graph);
    }
}
