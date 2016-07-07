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

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;

import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;


public class SCMDependencyNode extends Node {
    private Set<Revision> revisions = new HashSet<>();
    private String materialType;
    private HashSet<String> materialNames = new LinkedHashSet<>();
    private Set<MaterialRevisionWrapper> materialRevisionWrappers = new HashSet<>();
    private List<MaterialRevision> materialRevisions = new ArrayList<>();

    public SCMDependencyNode(String nodeId, String nodeName, String materialType) {
        super(DependencyNodeType.MATERIAL, nodeId, nodeName);
        this.materialType = materialType;
    }

    @Override
    public void addRevision(Revision revision) {
        bomb("SCMDependencyNode can have only MaterialRevisions, revisions are derived from material revisions.");
    }

    @Override
    public List<Revision> revisions() {
        ArrayList<Revision> revisions = new ArrayList<>(this.revisions);
        for(MaterialRevision revision : materialRevisions) {
            for(Modification modification : revision.getModifications()) {
                revisions.add(new SCMRevision(modification));
            }
        }
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
        bomb("SCMDependencyNode can have only MaterialRevisions, revisions are derived from material revisions.");
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

    public void addMaterialRevision(MaterialRevision materialRevision) {
        if (materialRevisionWrappers.add(new MaterialRevisionWrapper(materialRevision))) {
            materialRevisions.add(materialRevision);
        }
    }

    public List<MaterialRevision> getMaterialRevisions() {
        return materialRevisions;
    }

    private class MaterialRevisionWrapper {
        private MaterialRevision materialRevision;

        public MaterialRevisionWrapper(MaterialRevision materialRevision) {
            bombIfNull(materialRevision, "Material Revision cannot be null");
            this.materialRevision = materialRevision;
        }

        public MaterialRevision getMaterialRevision() {
            return materialRevision;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MaterialRevisionWrapper that = (MaterialRevisionWrapper) o;

            if(this.materialRevision == that.materialRevision) return true;

            return sameMaterial(that.materialRevision.getMaterial()) &&
                    sameModifications(that.materialRevision.getModifications());
        }

        @Override
        public int hashCode() {
            int result;
            result = (materialRevision.getMaterial() != null ? materialRevision.getMaterial().getFingerprint().hashCode() : 0);
            result = 31 * result + (materialRevision.getModifications() != null ? materialRevision.getModifications().hashCode() : 0);
            return result;
        }

        private boolean sameMaterial(Material thatMaterial) {
            Material material = this.materialRevision.getMaterial();

            if(material == thatMaterial) return true;
            return material != null ? (thatMaterial != null && material.getFingerprint().equals(thatMaterial.getFingerprint())) : thatMaterial == null;
        }

        private boolean sameModifications(Modifications thatModifications) {
            Modifications modifications = this.materialRevision.getModifications();

            return modifications != null ? modifications.equals(thatModifications) : thatModifications == null;
        }
    }
}