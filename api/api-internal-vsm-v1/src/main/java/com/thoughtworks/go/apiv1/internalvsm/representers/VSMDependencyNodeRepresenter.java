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

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.valuestreammap.Node;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class VSMDependencyNodeRepresenter {
    public static void toJSON(OutputWriter outputWriter, Node node) {
        List<String> dependents = node.getChildren().stream().map((c) -> c.getId().toString()).collect(toList());
        List<String> parents = node.getParents().stream().map((c) -> c.getId().toString()).collect(toList());

        String name = node.getName();
        outputWriter.add("id", node.getId())
                .add("name", name)
                .addChildList("dependents", dependents)
                .addChildList("parents", parents)
                .add("depth", node.getDepth())
                .addIfNotNull("message", node.getMessageString());
        if (node.getViewType() != null) {
            outputWriter.add("view_type", node.getViewType());
        }
    }
}
