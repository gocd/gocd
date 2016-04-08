/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.util.*;


public class SCMDependencyNode extends Node {
    private Set<Revision> revisions = new HashSet<>();
    private String materialType;
    private HashSet<String> materialNames = new LinkedHashSet<>();

    public SCMDependencyNode(String nodeId, String nodeName, String materialType) {
        super(DependencyNodeType.MATERIAL, nodeId, nodeName);
        this.materialType = materialType;
    }

    @Override
    public void addRevision(Revision revision) {
        this.revisions.add(revision);
    }

    @Override
    public List<Revision> revisions() {
        ArrayList<Revision> revisions = new ArrayList<>(this.revisions);
        Collections.sort(revisions, new Comparator<Revision>() {
            @Override
            public int compare(Revision o1, Revision o2) {
                return ((SCMRevision) o1).compareTo((SCMRevision) o2);
            }
        });
        return revisions;
    }

    @Override
    public void addRevisions(List<Revision> revisions) {
        this.revisions.addAll(revisions);
    }

    public String getMaterialType() {
        return materialType;
    }

    public void addMaterialName(String name) {
        this.materialNames.add(name);
    }

    public HashSet<String> getMaterialNames() {
        return materialNames;
    }
}
