/*
 * Copyright 2020 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.internalvsm.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.valuestreammap.DependencyNodeType;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode;

import java.util.List;

public class VSMNodesRepresenter {
    public static void toJSON(OutputWriter childWriter, List<Node> nodes) {
        childWriter.addChildList("nodes", nodeWriter -> {
            nodes.forEach((node) -> toJSON(nodeWriter, node));
        });
    }

    private static void toJSON(OutputListWriter listWriter, Node node) {
        DependencyNodeType nodeType = node.getType();
        switch (nodeType) {
            case PIPELINE:
                PipelineDependencyNode pipelineDependencyNode = (PipelineDependencyNode) node;
                listWriter.addChild(outputWriter -> PipelineDependencyNodeRepresenter.toJSON(outputWriter, pipelineDependencyNode));
                break;
            case MATERIAL:
                SCMDependencyNode scmDependencyNode = (SCMDependencyNode) node;
                listWriter.addChild(outputWriter -> SCMDependencyNodeRepresenter.toJSON(outputWriter, scmDependencyNode));
                break;
            case DUMMY:
                listWriter.addChild(outputWriter -> VSMDependencyNodeRepresenter.toJSON(outputWriter, node));
                break;
        }
    }
}

