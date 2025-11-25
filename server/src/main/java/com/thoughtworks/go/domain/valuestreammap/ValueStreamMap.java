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
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;
import com.thoughtworks.go.server.valuestreammap.CrossingMinimization;
import com.thoughtworks.go.server.valuestreammap.DummyNodeCreation;
import com.thoughtworks.go.server.valuestreammap.LevelAssignment;

import java.util.*;

public class ValueStreamMap {
    private static final int VALID_LATEST_REVISION_STRING_COUNT = 1;

    private final Map<CaseInsensitiveString, Node> nodeIdToNodeMap = new LinkedHashMap<>();
    private final LevelAssignment levelAssignment = new LevelAssignment();
    private final DummyNodeCreation dummyNodeCreation = new DummyNodeCreation();
    private final CrossingMinimization crossingMinimization = new CrossingMinimization();

    private PipelineDependencyNode currentPipeline;
    private SCMDependencyNode currentMaterial;
    private MaterialInstance currentMaterialInstance;
    private List<Node> rootNodes = new ArrayList<>();

    public ValueStreamMap(CaseInsensitiveString pipeline, PipelineRevision pipelineRevision) {
        currentPipeline = new PipelineDependencyNode(pipeline, pipeline.toString());
        nodeIdToNodeMap.put(currentPipeline.getId(), currentPipeline);
        currentPipeline.addRevision(pipelineRevision);
    }

    public ValueStreamMap(Material material, MaterialInstance materialInstance, Modification modification) {
        currentMaterial = new SCMDependencyNode(material.getFingerprint(), material.getUriForDisplay(), material.getTypeForDisplay());
        currentMaterialInstance = materialInstance;
        nodeIdToNodeMap.put(currentMaterial.getId(), currentMaterial);
        currentMaterial.addMaterialRevision(new MaterialRevision(material, false, modification));
    }

    //used in rails
    public Node getCurrentPipeline() {
        return currentPipeline;
    }

    public Node getCurrentMaterial() {
        return currentMaterial;
    }

    public MaterialInstance getCurrentMaterialInstance() {
        return currentMaterialInstance;
    }

    public PipelineDependencyNode addUpstreamPipelineNode(PipelineDependencyNode node, PipelineRevision revision, CaseInsensitiveString dependentNodeId) {
        PipelineDependencyNode pipelineNode = addUpstreamNode(node, dependentNodeId);
        pipelineNode.addRevision(revision);
        return pipelineNode;
    }

    public SCMDependencyNode addUpstreamMaterialNode(SCMDependencyNode node, CaseInsensitiveString materialName, CaseInsensitiveString dependentNodeId,
                                        MaterialRevision materialRevision) {
        SCMDependencyNode scmNode = addUpstreamNode(node, dependentNodeId);
        scmNode.addMaterialRevision(materialRevision);
        if (materialName != null) {
            scmNode.addMaterialName(materialName.toString());
        }
        return scmNode;
    }

    public Node addDownstreamNode(Node node, CaseInsensitiveString parentNodeId) {
        Node parentNode = findNode(parentNodeId);
        Node resolvedNode = findOrAddNode(node);
        parentNode.addEdge(resolvedNode);
        return resolvedNode;
    }

    private <T extends Node> T addUpstreamNode(T node, CaseInsensitiveString dependentNodeId) {
        Node dependentNode = findNode(dependentNodeId);
        T resolvedNode = findOrAddNode(node);
        resolvedNode.addEdge(dependentNode);
        return resolvedNode;
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> T findNode(CaseInsensitiveString nodeId) {
        return (T) nodeIdToNodeMap.get(nodeId);
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> T findOrAddNode(T node) {
        return (T) nodeIdToNodeMap.computeIfAbsent(node.getId(), id -> node);
    }

    public Collection<Node> allNodes() {
        return nodeIdToNodeMap.values();
    }

    public List<Node> getRootNodes() {
        if (rootNodes.isEmpty()) {
            populateRootNodes();
        }
        return rootNodes;
    }

    void populateRootNodes() {
        rootNodes = new ArrayList<>();
        for (Node currentNode : allNodes()) {
            if (currentNode.getParents().isEmpty()) {
                rootNodes.add(currentNode);
            }
        }
    }

    public ValueStreamMapPresentationModel presentationModel() {
        NodeLevelMap nodeLevelMap = levelAssignment.apply(this);
        dummyNodeCreation.apply(this, nodeLevelMap);
        crossingMinimization.apply(nodeLevelMap);
        return new ValueStreamMapPresentationModel(currentPipeline, currentMaterial, nodeLevelMap.nodesAtEachLevel());
    }

    public boolean hasCycle() {
        Set<Node> verifiedNodes = new HashSet<>();
        Set<CaseInsensitiveString> nodesInPath = new HashSet<>();

        for (Node node : getRootNodes()) {
            if (node.hasCycleInSubGraph(nodesInPath, verifiedNodes)) {
                return true;
            }
        }
        return false;
    }

    public void addWarningIfBuiltFromInCompatibleRevisions() {
        if (anyRootNodeWithInCompatibleRevisions()) {
            addWarning(currentPipeline);
        }
    }

    private boolean anyRootNodeWithInCompatibleRevisions() {
        boolean anyRootNodeWithInCompatibleRevisions = false;

        for (Node rootNode : getRootNodes()) {
            if (hasMultipleLatestRevisionString(((SCMDependencyNode) rootNode).getMaterialRevisions())) {
                addWarning(rootNode);
                anyRootNodeWithInCompatibleRevisions = true;
            }
        }

        return anyRootNodeWithInCompatibleRevisions;
    }

    private boolean hasMultipleLatestRevisionString(List<MaterialRevision> materialRevisions) {
        if (materialRevisions.size() == VALID_LATEST_REVISION_STRING_COUNT) return false;

        Set<String> latestRevisions = new HashSet<>();
        for (MaterialRevision revision : materialRevisions) {
            latestRevisions.add(revision.getLatestRevisionString());
        }

        return latestRevisions.size() > VALID_LATEST_REVISION_STRING_COUNT;
    }

    private void addWarning(Node node) {
        node.setViewType(VSMViewType.WARNING);
        node.setMessage("Built from incompatible revisions.");
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("graph:\n");
        for (Node currentNode : allNodes()) {
            s.append(currentNode).append("\n");
        }
        return s.toString();
    }
}
