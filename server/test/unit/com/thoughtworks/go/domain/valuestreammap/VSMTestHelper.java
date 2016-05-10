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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class VSMTestHelper {

    public static void assertThatLevelHasNodes(List<Node> nodesAtLevel, int numberOfDummyNodes, String... nodeIds) {
        assertThat(nodesAtLevel.size(), is(numberOfDummyNodes + nodeIds.length));
        List<String> nodeIdsAtLevel = new ArrayList<String>();
        for (Node node : nodesAtLevel) {
            if (!node.getType().equals(DependencyNodeType.DUMMY)) {
                nodeIdsAtLevel.add(node.getId());
            }
        }
        assertThat(nodeIdsAtLevel.size(), is(nodeIds.length));
        assertThat(nodeIdsAtLevel, hasItems(nodeIds));
    }

    public static void assertThatNodeHasChildren(ValueStreamMap graph, String nodeId, int numberOfDummyChildren, String... children) {
        Node node = graph.findNode(nodeId);
        assertThat(node.getChildren().size(), is(numberOfDummyChildren + children.length));
        assertThat(childIdsOf(node), hasItems(children));
    }

    public static void assertThatNodeHasParents(ValueStreamMap graph, String nodeId, int numberOfDummyParents, String... parents) {
        Node node = graph.findNode(nodeId);
        assertThatNodeHasParents(node, numberOfDummyParents, parents);
    }

    public static void assertThatNodeHasParents(Node node, int numberOfDummyParents, String... parents) {
        assertThat(node.getParents().size(), is(numberOfDummyParents + parents.length));
        assertThat(parentIdsOf(node), hasItems(parents));
    }

    public static void assertDepth(ValueStreamMapPresentationModel graph, String nodeId, int expectedDepth){
        assertThat(graph.findNode(nodeId).getDepth(), is(expectedDepth));
    }

    public static void assertNodeHasChildren(ValueStreamMap graph, String actualNode, String... expectedChildren) {
        assertNodeHasChildren(graph.findNode(actualNode), expectedChildren);
    }

    public static void assertNodeHasChildren(Node currentNode, String... expectedChildren) {
        assertThat(childIdsOf(currentNode), hasItems(expectedChildren));
    }

    public static void assertNodeHasParents(ValueStreamMap graph, String actualNode, String... expectedParents) {
        assertThat(parentIdsOf(graph.findNode(actualNode)), hasItems(expectedParents));
    }

    private static List<String> childIdsOf(Node node) {
        List<String> childIds = new ArrayList<String>();
        for (Node child : node.getChildren()) {
            childIds.add(child.getId());
        }
        return childIds;
    }

    private static List<String> parentIdsOf(Node node) {
        List<String> parentIds = new ArrayList<String>();
        for (Node parent : node.getParents()) {
            parentIds.add(parent.getId());
        }
        return parentIds;
    }

    public static void assertNodeHasRevisions(ValueStreamMapPresentationModel graph, String nodeId, Revision... expectedRevisions) {
        Node node = graph.findNode(nodeId);
        List<Revision> revisions = node.revisions();
        assertThat(revisions.toString(), revisions.size(), is(expectedRevisions.length));
        assertThat(revisions, hasItems(expectedRevisions));
    }

    public static void assertSCMNodeHasMaterialRevisions(ValueStreamMapPresentationModel graph, String nodeId, MaterialRevision... expectedMaterialRevisions) {
        SCMDependencyNode node = (SCMDependencyNode)graph.findNode(nodeId);
        List<MaterialRevision> materialRevisions = node.getMaterialRevisions();
        assertThat(materialRevisions.toString(), materialRevisions.size(), is(expectedMaterialRevisions.length));
        assertThat(materialRevisions, hasItems(expectedMaterialRevisions));
    }

    public static void assertStageDetailsOf(ValueStreamMap graph, String nodeId, String counter, Stages expectedStages) {
        Node node = graph.findNode(nodeId);
        PipelineRevision pipelineRevision = findPipelineRevision(node, counter);

        assertThat(pipelineRevision.getStages(), is(expectedStages));
    }

    private static PipelineRevision findPipelineRevision(Node node, String counter) {
        String pipelineName = node.getId();
        List<Revision> revisions = node.revisions();
        for (Revision revision : revisions) {
            if (revision instanceof PipelineRevision && revision.getRevisionString().equals(new PipelineIdentifier(pipelineName, Long.parseLong(counter)).pipelineLocator())) {
                return (PipelineRevision) revision;
            }
        }
        throw new RuntimeException(String.format("Ouch! Cannot find pipeline %s with counter %s. Node: %s", pipelineName, counter, node));
    }

    public static void assertNumberOfLevelsInGraph(NodeLevelMap nodeLevelMap, int expectedNumberOfLevels) {
        assertThat(nodeLevelMap.nodesAtEachLevel().size(), is(expectedNumberOfLevels));
    }

    public static void assertInstances(Node node, String pipelineName, Integer... pipelineCounters) {
        List<Revision> revisions = node.revisions();
        assertThat(revisions.size(), is(pipelineCounters.length));
        for (Integer pipelineCounter : pipelineCounters) {
            assertThat(String.format("Pipeline %s does not have pipeline counter %s",pipelineName, pipelineCounter),revisions.contains(new PipelineRevision(pipelineName, pipelineCounter, pipelineCounter.toString())), is(true));

        }
    }

    public static void assertStageDetails(Node p1_node, int pipelineCounter, String stageName, int stageCounter, StageState stageState) {
        PipelineRevision revision = getPipelineRevision(p1_node, pipelineCounter);
        assertThat(revision.getStages().hasStage(stageName), is(true));
        Stage stage = revision.getStages().byName(stageName);
        assertThat(stage.getCounter(), is(stageCounter));
        assertThat(stage.getState(), is(stageState));
    }

    public static PipelineRevision getPipelineRevision(Node p1_node, int pipelineCounter) {
        for (Revision revision : p1_node.revisions() ) {
            PipelineRevision pipelineRevision = (PipelineRevision) revision;
            if(pipelineCounter == pipelineRevision.getCounter()) {
                return pipelineRevision;
            }
        }
        throw new RuntimeException(String.format("VSM node of %s does not have revision %s", p1_node.getName(), pipelineCounter));
    }

    public static SCMRevision scmRevision(String revision, Date dateTime) {
            return new SCMRevision(new Modification("user","comment","email", dateTime, revision));
        }

}
