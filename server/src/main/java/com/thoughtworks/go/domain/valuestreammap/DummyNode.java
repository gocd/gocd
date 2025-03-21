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

import java.util.List;

public class DummyNode extends Node {
    public DummyNode(String nodeId, String nodeName) {
        super(DependencyNodeType.DUMMY, new CaseInsensitiveString(nodeId), nodeName);
    }

    @Override
    public void addRevision(Revision revision) {
    }

    @Override
    public List<Revision> revisions() {
        return null;
    }

    @Override
    public void addRevisions(List<Revision> revisions) {
    }
}
