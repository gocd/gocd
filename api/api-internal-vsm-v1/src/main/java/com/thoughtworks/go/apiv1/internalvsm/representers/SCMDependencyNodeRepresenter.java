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
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode;

import java.util.Collections;

public class SCMDependencyNodeRepresenter {
    public static void toJSON(OutputWriter outputWriter, SCMDependencyNode node) {
        VSMDependencyNodeRepresenter.toJSON(outputWriter, node);
        outputWriter.add("locator", "")
                .add("node_type", node.getMaterialType().toUpperCase())
                .addChildList("instances", Collections.emptyList())
                .addChildList("material_revisions", revisionWriter -> node.getMaterialRevisions()
                        .forEach((rev) -> revisionWriter.addChild(writer -> writer.addChildList("modifications", modWriter -> ModificationsRepresenter.toJSON(modWriter, node.getId(), rev.getModifications())))));
        if (!node.getMaterialNames().isEmpty()) {
            outputWriter.addChildList("material_names", node.getMaterialNames());
        }
    }
}
