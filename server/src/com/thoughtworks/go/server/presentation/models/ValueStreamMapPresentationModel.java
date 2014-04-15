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

package com.thoughtworks.go.server.presentation.models;

import java.util.List;

import com.thoughtworks.go.domain.valuestreammap.Node;

public class ValueStreamMapPresentationModel {
    private Node currentPipeline;
    private List<List<Node>> nodeLevels;

    public ValueStreamMapPresentationModel(Node currentPipeline, List<List<Node>> nodeLevels) {

        this.currentPipeline = currentPipeline;
        this.nodeLevels = nodeLevels;
    }


    public Node getCurrentPipeline() {
        return currentPipeline;
    }

    public List<List<Node>> getNodesAtEachLevel() {
        return nodeLevels;
    }

    @Deprecated //Used only in tests
    public Node findNode(String nodeId) {
        for (List<Node> nodeLevel : nodeLevels) {
            for (Node node : nodeLevel) {
                if (node.getId().equals(nodeId)) {
                    return node;
                }
            }
        }
        return null;
    }
}
