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
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;
import com.thoughtworks.go.server.valuestreammap.CrossingMinimization;
import com.thoughtworks.go.server.valuestreammap.DummyNodeCreation;
import com.thoughtworks.go.server.valuestreammap.LevelAssignment;

import java.util.*;

public class ValueStreamMap {
    private Node currentPipeline;
    private Node currentMaterial;
	private MaterialInstance currentMaterialInstance;
    private LinkedHashMap<String, Node> nodeIdToNodeMap = new LinkedHashMap<String, Node>();
	private List<Node> rootNodes = new ArrayList<Node>();

	private LevelAssignment levelAssignment = new LevelAssignment();
    private DummyNodeCreation dummyNodeCreation = new DummyNodeCreation();
    private CrossingMinimization crossingMinimization = new CrossingMinimization();

    public ValueStreamMap(String pipeline, PipelineRevision pipelineRevision) {
        currentPipeline = new PipelineDependencyNode(pipeline, pipeline);
        nodeIdToNodeMap.put(currentPipeline.getId(), currentPipeline);
        currentPipeline.addRevision(pipelineRevision);
    }

	public ValueStreamMap(Material material, MaterialInstance materialInstance, Modification modification) {
		currentMaterial = new SCMDependencyNode(material.getFingerprint(), material.getUriForDisplay(), material.getTypeForDisplay());
		currentMaterialInstance = materialInstance;
		nodeIdToNodeMap.put(currentMaterial.getId(), currentMaterial);
		currentMaterial.addRevision(new SCMRevision(modification));
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

	public Node addUpstreamNode(Node node, PipelineRevision revision, String dependentNodeId) {
        node = addUpstreamNode(node, dependentNodeId);
        node.addRevision(revision);
        return node;
    }

    public Node addUpstreamMaterialNode(Node node, CaseInsensitiveString materialName, Modifications modifications, String dependentNodeId) {
        List<Revision> revisions = new ArrayList<Revision>();
        for (Modification modification : modifications) {
            revisions.add(new SCMRevision(modification));
        }
        SCMDependencyNode scmNode = (SCMDependencyNode) addUpstreamNode(node, dependentNodeId);
        scmNode.addRevisions(revisions);
        if (materialName != null) {
            scmNode.addMaterialName(materialName.toString());
        }
        return scmNode;
    }

    public Node addDownstreamNode(Node node, String parentNodeId) {
        Node parentNode = findNode(parentNodeId);
        if (hasNode(node.getId())) {
            node = findNode(node.getId());
        } else {
            nodeIdToNodeMap.put(node.getId(), node);
        }
        parentNode.addEdge(node);
        return node;
    }

    private Node addUpstreamNode(Node node, String dependentNodeId) {
        Node dependentNode = findNode(dependentNodeId);
        if (hasNode(node.getId())) {
            node = findNode(node.getId());
        } else {
            nodeIdToNodeMap.put(node.getId(), node);
        }
        node.addEdge(dependentNode);
        return node;
    }

    public Node findNode(String nodeId) {
        return nodeIdToNodeMap.get(nodeId);
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
		rootNodes = new ArrayList<Node>();
		for (Node currentNode : allNodes()) {
			if (currentNode.getParents().isEmpty()) {
				rootNodes.add(currentNode);
			}
		}
	}

    private boolean hasNode(String nodeId) {
        return nodeIdToNodeMap.containsKey(nodeId);
    }

    public ValueStreamMapPresentationModel presentationModel() {
        NodeLevelMap nodeLevelMap = levelAssignment.apply(this);
        dummyNodeCreation.apply(this, nodeLevelMap);
        crossingMinimization.apply(nodeLevelMap);
        return new ValueStreamMapPresentationModel(currentPipeline, currentMaterial, nodeLevelMap.nodesAtEachLevel());
    }

    public boolean hasCycle() {
        Set<Node> verifiedNodes = new HashSet<Node>();
        Set<String> nodesInPath = new HashSet<String>();

        for (Node node : getRootNodes()) {
            if (node.hasCycleInSubGraph(nodesInPath, verifiedNodes)) {
                return true;
            }
        }
        return false;
    }

	@Override
	public String toString() {
		String s = "graph:\n";
		for (Node currentNode : allNodes()) {
			s += currentNode + "\n";
		}
		return s;
	}
}
