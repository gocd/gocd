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

import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.NodeLevelMap;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;

import java.util.*;

public class LevelAssignment {

    public NodeLevelMap apply(ValueStreamMap valueStreamMap) {
        Node rootNode = valueStreamMap.getCurrentPipeline() != null ? valueStreamMap.getCurrentPipeline() : valueStreamMap.getCurrentMaterial();
        rootNode.setLevel(0);

		if(!rootNode.getParents().isEmpty()) {
			assignLevelsToDependencies(rootNode, new Upstream());
		}

		if(!rootNode.getChildren().isEmpty()) {
			assignLevelsToDependencies(rootNode, new Downstream());
		}

        return levelToNodesMap(valueStreamMap);
    }

	private void assignLevelsToDependencies(Node rootNode, LevelAssignmentDirection direction) {
		List<Node> topologicalOrder = new ArrayList<>();
		getTopologicalOrder(rootNode, direction, new HashSet<Node>(), topologicalOrder);
		Collections.reverse(topologicalOrder);
		assignLevels(topologicalOrder, direction);
	}

	private void getTopologicalOrder(Node rootNode, LevelAssignmentDirection direction, Set<Node> visitedNodes, List<Node> topologicalOrder) {
        if (visitedNodes.contains(rootNode)) {
            return;
        }
        visitedNodes.add(rootNode);
        List<Node> relatedNodes = direction.getRelatedNodes(rootNode);
        if (!relatedNodes.isEmpty()) {
            for (Node relatedNode : relatedNodes) {
                getTopologicalOrder(relatedNode, direction, visitedNodes, topologicalOrder);
            }
        }
        topologicalOrder.add(rootNode);
    }

    private void assignLevels(List<Node> topologicalOrder, LevelAssignmentDirection direction) {
        for (Node currentNode : topologicalOrder) {
            int nextLevel = direction.getNextLevel(currentNode);
            List<Node> relatedNodes = direction.getRelatedNodes(currentNode);
            if (!relatedNodes.isEmpty()) {
                for (Node relatedNode : relatedNodes) {
                    if (direction.canResetLevel(relatedNode, nextLevel)) {
                        relatedNode.setLevel(nextLevel);
                    }
                }
            }
        }
    }

    private NodeLevelMap levelToNodesMap(ValueStreamMap valueStreamMap) {
        NodeLevelMap nodeLevelMap = new NodeLevelMap();
        Collection<Node> nodes = valueStreamMap.allNodes();
        for (Node node : nodes) {
            nodeLevelMap.add(node);
        }
        return nodeLevelMap;
    }

    private interface LevelAssignmentDirection {
        List<Node> getRelatedNodes(Node node);

        int getNextLevel(Node node);

        boolean canResetLevel(Node node, int nextLevel);
    }

    private class Upstream implements LevelAssignmentDirection {
        @Override
        public List<Node> getRelatedNodes(Node node) {
            return node.getParents();
        }

        @Override
        public int getNextLevel(Node node) {
            return node.getLevel() - 1;
        }

        @Override
        public boolean canResetLevel(Node node, int nextLevel) {
            return nextLevel < node.getLevel();
        }
    }

    private class Downstream implements LevelAssignmentDirection {
        @Override
        public List<Node> getRelatedNodes(Node node) {
            return node.getChildren();
        }

        @Override
        public int getNextLevel(Node node) {
            return node.getLevel() + 1;
        }

        @Override
        public boolean canResetLevel(Node node, int nextLevel) {
            return nextLevel > node.getLevel();
        }
    }
}
