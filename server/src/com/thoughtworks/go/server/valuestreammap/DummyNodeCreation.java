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

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;

import com.thoughtworks.go.domain.valuestreammap.DummyNode;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.NodeLevelMap;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;

import static java.lang.Math.abs;

public class DummyNodeCreation {
    public void apply(ValueStreamMap graph, NodeLevelMap nodeLevelMap) {
        List<Node> nodesWithNoParents = graph.getRootNodes();
        Set<Node> visitedNodes = new HashSet<>();
        for (Node current : nodesWithNoParents) {
            addDummyNodesIfRequired(current, nodeLevelMap, visitedNodes);
        }
    }

    private void addDummyNodesIfRequired(Node node, NodeLevelMap nodeLevelMap, Set<Node> visitedNodes) {
        if (visitedNodes.contains(node)) {
            return;
        }
        visitedNodes.add(node);
        for (int i = 0; i < node.getChildren().size(); i++) {
            Node currentChildNode = node.getChildren().get(i);
            if (abs(currentChildNode.getLevel() - node.getLevel()) > 1) {
                DummyNode dummyNode = insertDummyNode(node, currentChildNode, nodeLevelMap);
                addDummyNodesIfRequired(dummyNode, nodeLevelMap, visitedNodes);
            } else {
                addDummyNodesIfRequired(currentChildNode, nodeLevelMap, visitedNodes);
            }
        }
    }

    private DummyNode insertDummyNode(Node node, Node currentChildNode, NodeLevelMap nodeLevelMap) {
        String dummyNodeId = UUID.randomUUID().toString();
        DummyNode dummyNode = new DummyNode(dummyNodeId, "dummy-" + dummyNodeId);
        dummyNode.setLevel(node.getLevel() + 1);
        nodeLevelMap.add(dummyNode);

        dummyNode.addParentIfAbsent(node);
        dummyNode.addChildIfAbsent(currentChildNode);

        node.replaceChildWith(currentChildNode, dummyNode);
        currentChildNode.replaceParentWith(node, dummyNode);
        return dummyNode;
    }
}
