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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.NodeLevelMap;

import static java.lang.Math.abs;
import static java.util.Collections.sort;

public class CrossingMinimization {

    private static final int LEVEL_OF_CURRENT_PIPELINE = 0;

    public void apply(NodeLevelMap levelToNodesMap) {
        initializeNodeDepths(levelToNodesMap);

        reorderByBaryCenter(new LeftToRight(levelToNodesMap));
        reorderByBaryCenter(new RightToLeft(levelToNodesMap));
        normalizeUpstream(levelToNodesMap);
        normalizeDownstream(levelToNodesMap);
    }

    private void normalizeUpstream(NodeLevelMap nodeLevelMap) {
        reorderByMinDepth(new RightToLeft(nodeLevelMap, LEVEL_OF_CURRENT_PIPELINE - 2));
    }

    private void normalizeDownstream(NodeLevelMap nodeLevelMap) {
        reorderByMinDepth(new LeftToRight(nodeLevelMap, LEVEL_OF_CURRENT_PIPELINE + 2));
    }

    private void reorderByMinDepth(TraversalDirection traversalDirection) {
        while (traversalDirection.hasNext()) {
            List<Node> nodesAtLevel = traversalDirection.next();
            int depth = 1;
            for (int i = 0; i < nodesAtLevel.size(); i++) {
                Node currentNode = nodesAtLevel.get(i);

                List<Node> relatedNodesAtPreviousLevel = traversalDirection.getRelatedNodesAtPreviousLevel(currentNode);
                int leastDepth = minDepth(relatedNodesAtPreviousLevel);
                if (depth < leastDepth) {
                    List<Node> nodesRemaining = nodesAtLevel.subList(i, nodesAtLevel.size());
                    int initialSlope = calculateSlope(depth - currentNode.getDepth(), nodesRemaining, traversalDirection);
                    int newSlope = calculateSlope(leastDepth - depth, nodesRemaining, traversalDirection);
                    if (newSlope < initialSlope) {
                        depth = leastDepth;
                    }
                }
                currentNode.setDepth(depth++);
            }
        }
    }

    private int calculateSlope(int depthOffset, List<Node> nodes, TraversalDirection traversalDirection) {
        int totalSlope = 0;
        for (Node node : nodes) {
            for (Node relatedNode : traversalDirection.getRelatedNodesAtPreviousLevel(node)) {
                totalSlope += abs(node.getDepth() + depthOffset - relatedNode.getDepth());
            }
        }
        return totalSlope;
    }


    private int minDepth(List<Node> nodes) {
        int min = Integer.MAX_VALUE;
        for (Node node : nodes) {
            int depth = node.getDepth();
            if (min > depth) {
                min = depth;
            }
        }
        return min;
    }

    private void reorderByBaryCenter(TraversalDirection traversalDirection) {
        while (traversalDirection.hasNext()) {
            List<Node> nodesAtLevel = traversalDirection.next();
            ArrayList<NodeBaryCentre> nodeBaryCentres = new ArrayList<>();
            for (Node node : nodesAtLevel) {
                List<Node> relatedNodes = traversalDirection.getRelatedNodesAtPreviousLevel(node);
                nodeBaryCentres.add(getNodeBaryCentre(node, relatedNodes));
            }
            sort(nodeBaryCentres);
            updateNodeDepths(nodeBaryCentres);
            sort(nodesAtLevel);
        }
    }

    private NodeBaryCentre getNodeBaryCentre(Node node, List<Node> relatedNodes) {
        if (relatedNodes.isEmpty()) {
            return new NodeBaryCentre(node, Float.valueOf(node.getDepth()));
        }
        float sum = 0f;
        for (Node relatedNode : relatedNodes) {
            int depth = relatedNode.getDepth();
            sum += depth;
        }
        float averageDepth = sum / relatedNodes.size();
        return new NodeBaryCentre(node, averageDepth);
    }

    private void updateNodeDepths(ArrayList<NodeBaryCentre> nodeBaryCentres) {
        int depth = 1;
        for (NodeBaryCentre nodeBaryCentre : nodeBaryCentres) {
            nodeBaryCentre.node.setDepth(depth++);
        }
    }

    void initializeNodeDepths(NodeLevelMap nodeLevelMap) {
        Node pipeline = nodeLevelMap.get(LEVEL_OF_CURRENT_PIPELINE).get(0);

        HashMap<Integer, Integer> levelToDepthMap = new HashMap<>();

        initializeDepthsFor(pipeline, levelToDepthMap, new RightToLeft(nodeLevelMap, LEVEL_OF_CURRENT_PIPELINE), new HashSet<Node>());
        initializeDepthsFor(pipeline, levelToDepthMap, new LeftToRight(nodeLevelMap, LEVEL_OF_CURRENT_PIPELINE), new HashSet<Node>());
    }

    private void initializeDepthsFor(Node node, HashMap<Integer, Integer> levelToDepthMap, TraversalDirection traversalDirection, Set<Node> visited) {
        if (visited.contains(node)) {
            return;
        }
        visited.add(node);

        int depth = 1;
        if (levelToDepthMap.containsKey(node.getLevel())) {
            depth = levelToDepthMap.get(node.getLevel()) + 1;
        }
        if (node.getDepth() == 0) {
            node.setDepth(depth);
            levelToDepthMap.put(node.getLevel(), depth);
        }
        for (Node relatedNode : traversalDirection.getRelatedNodesAtNextLevel(node)) {
            initializeDepthsFor(relatedNode, levelToDepthMap, traversalDirection, visited);
        }
    }

    private interface TraversalDirection {
        boolean hasNext();

        List<Node> next();

        List<Node> getRelatedNodesAtPreviousLevel(Node node);

        List<Node> getRelatedNodesAtNextLevel(Node node);
    }

    private class LeftToRight implements TraversalDirection {

        private final NodeLevelMap nodeLevelMap;
        private int index;

        public LeftToRight(NodeLevelMap nodeLevelMap) {
            this(nodeLevelMap, nodeLevelMap.lowestLevel());
        }

        public LeftToRight(NodeLevelMap nodeLevelMap, int startIndex) {
            this.nodeLevelMap = nodeLevelMap;
            this.index = startIndex;
        }

        @Override
        public boolean hasNext() {
            return nodeLevelMap.get(index) != null;
        }

        @Override
        public List<Node> next() {
            return nodeLevelMap.get(index++);
        }

        @Override
        public List<Node> getRelatedNodesAtPreviousLevel(Node node) {
            return node.getParents();
        }

        @Override
        public List<Node> getRelatedNodesAtNextLevel(Node node) {
            return node.getChildren();
        }
    }

    private class RightToLeft implements TraversalDirection {

        private final NodeLevelMap nodeLevelMap;
        private int index;

        public RightToLeft(NodeLevelMap nodeLevelMap) {
            this(nodeLevelMap, nodeLevelMap.highestLevel());
        }

        public RightToLeft(NodeLevelMap nodeLevelMap, int startIndex) {
            this.nodeLevelMap = nodeLevelMap;
            this.index = startIndex;
        }

        @Override
        public boolean hasNext() {
            return nodeLevelMap.get(index) != null;
        }

        @Override
        public List<Node> next() {
            return nodeLevelMap.get(index--);
        }

        @Override
        public List<Node> getRelatedNodesAtPreviousLevel(Node node) {
            return node.getChildren();
        }

        @Override
        public List<Node> getRelatedNodesAtNextLevel(Node node) {
            return node.getParents();
        }
    }

    private class NodeBaryCentre implements Comparable {

        private final Node node;
        private final Float averageDepth;

        public NodeBaryCentre(Node node, Float averageDepth) {
            this.node = node;
            this.averageDepth = averageDepth;
        }

        @Override
        public int compareTo(Object other) {
            return this.averageDepth.compareTo(((NodeBaryCentre) other).averageDepth);
        }
    }
}
