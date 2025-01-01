/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("TestOnlyProblems") // Workaround for IntelliJ thinking this place is production rather than test code
public class VSMTestHelper {

    public static void assertThatLevelHasNodes(List<Node> nodesAtLevel, int numberOfDummyNodes, CaseInsensitiveString... nodeIds) {
        assertThat(nodesAtLevel.size()).isEqualTo(numberOfDummyNodes + nodeIds.length);
        List<CaseInsensitiveString> nodeIdsAtLevel = new ArrayList<>();
        for (Node node : nodesAtLevel) {
            if (!node.getType().equals(DependencyNodeType.DUMMY)) {
                nodeIdsAtLevel.add(node.getId());
            }
        }
        assertThat(nodeIdsAtLevel.size()).isEqualTo(nodeIds.length);
        assertThat(nodeIdsAtLevel).contains(nodeIds);
    }

    public static void assertThatNodeHasChildren(ValueStreamMap graph, CaseInsensitiveString nodeId, int numberOfDummyChildren, CaseInsensitiveString... children) {
        Node node = graph.findNode(nodeId);
        assertThat(node.getChildren().size()).isEqualTo(numberOfDummyChildren + children.length);
        assertThat(childIdsOf(node)).contains(children);
    }

    public static void assertThatNodeHasParents(ValueStreamMap graph, CaseInsensitiveString nodeId, int numberOfDummyParents, CaseInsensitiveString... parents) {
        Node node = graph.findNode(nodeId);
        assertThatNodeHasParents(node, numberOfDummyParents, parents);
    }

    public static void assertThatNodeHasParents(Node node, int numberOfDummyParents, CaseInsensitiveString... parents) {
        assertThat(node.getParents().size()).isEqualTo(numberOfDummyParents + parents.length);
        assertThat(parentIdsOf(node)).contains(parents);
    }

    public static void assertDepth(ValueStreamMapPresentationModel graph, CaseInsensitiveString nodeId, int expectedDepth) {
        assertThat(graph.findNode(nodeId).getDepth()).isEqualTo(expectedDepth);
    }

    public static void assertNodeHasChildren(ValueStreamMap graph, CaseInsensitiveString actualNode, CaseInsensitiveString... expectedChildren) {
        assertNodeHasChildren(graph.findNode(actualNode), expectedChildren);
    }

    public static void assertNodeHasChildren(Node currentNode, CaseInsensitiveString... expectedChildren) {
        assertThat(childIdsOf(currentNode)).contains(expectedChildren);
    }

    public static void assertNodeHasParents(ValueStreamMap graph, CaseInsensitiveString actualNode, CaseInsensitiveString... expectedParents) {
        assertThat(parentIdsOf(graph.findNode(actualNode))).contains(expectedParents);
    }

    private static List<CaseInsensitiveString> childIdsOf(Node node) {
        List<CaseInsensitiveString> childIds = new ArrayList<>();
        for (Node child : node.getChildren()) {
            childIds.add(child.getId());
        }
        return childIds;
    }

    private static List<CaseInsensitiveString> parentIdsOf(Node node) {
        List<CaseInsensitiveString> parentIds = new ArrayList<>();
        for (Node parent : node.getParents()) {
            parentIds.add(parent.getId());
        }
        return parentIds;
    }

    public static void assertNodeHasRevisions(ValueStreamMapPresentationModel graph, CaseInsensitiveString nodeId, Revision... expectedRevisions) {
        Node node = graph.findNode(nodeId);
        List<Revision> revisions = node.revisions();
        assertThat(revisions.size()).isEqualTo(expectedRevisions.length);
        assertThat(revisions).contains(expectedRevisions);
    }

    public static void assertSCMNodeHasMaterialRevisions(ValueStreamMapPresentationModel graph, CaseInsensitiveString nodeId, MaterialRevision... expectedMaterialRevisions) {
        SCMDependencyNode node = (SCMDependencyNode) graph.findNode(nodeId);
        List<MaterialRevision> materialRevisions = node.getMaterialRevisions();
        assertThat(materialRevisions.size()).isEqualTo(expectedMaterialRevisions.length);
        assertThat(materialRevisions).contains(expectedMaterialRevisions);
    }

    public static void assertStageDetailsOf(ValueStreamMap graph, CaseInsensitiveString nodeId, String counter, Stages expectedStages) {
        Node node = graph.findNode(nodeId);
        PipelineRevision pipelineRevision = findPipelineRevision(node, counter);

        assertThat(pipelineRevision.getStages()).isEqualTo(expectedStages);
    }

    private static PipelineRevision findPipelineRevision(Node node, String counter) {
        CaseInsensitiveString pipelineName = node.getId();
        List<Revision> revisions = node.revisions();
        for (Revision revision : revisions) {
            if (revision instanceof PipelineRevision && revision.getRevisionString().equals(new PipelineIdentifier(pipelineName.toString(), Long.parseLong(counter)).pipelineLocator())) {
                return (PipelineRevision) revision;
            }
        }
        throw new RuntimeException(String.format("Ouch! Cannot find pipeline %s with counter %s. Node: %s", pipelineName, counter, node));
    }

    public static void assertNumberOfLevelsInGraph(NodeLevelMap nodeLevelMap, int expectedNumberOfLevels) {
        assertThat(nodeLevelMap.nodesAtEachLevel().size()).isEqualTo(expectedNumberOfLevels);
    }

    public static void assertInstances(Node node, String pipelineName, Integer... pipelineCounters) {
        List<Revision> revisions = node.revisions();
        assertThat(revisions.size()).isEqualTo(pipelineCounters.length);
        for (Integer pipelineCounter : pipelineCounters) {
            assertThat(revisions.contains(new PipelineRevision(pipelineName, pipelineCounter, pipelineCounter.toString())))
                .describedAs("Pipeline %s does not have pipeline counter %s", pipelineName, pipelineCounter)
                .isTrue();

        }
    }

    public static void assertStageDetails(Node p1_node, int pipelineCounter, String stageName, int stageCounter, StageState stageState) {
        PipelineRevision revision = getPipelineRevision(p1_node, pipelineCounter);
        assertThat(revision.getStages().hasStage(stageName)).isTrue();
        Stage stage = revision.getStages().byName(stageName);
        assertThat(stage.getCounter()).isEqualTo(stageCounter);
        assertThat(stage.getState()).isEqualTo(stageState);
    }

    public static PipelineRevision getPipelineRevision(Node p1_node, int pipelineCounter) {
        for (Revision revision : p1_node.revisions()) {
            PipelineRevision pipelineRevision = (PipelineRevision) revision;
            if (pipelineCounter == pipelineRevision.getCounter()) {
                return pipelineRevision;
            }
        }
        throw new RuntimeException(String.format("VSM node of %s does not have revision %s", p1_node.getName(), pipelineCounter));
    }
}
