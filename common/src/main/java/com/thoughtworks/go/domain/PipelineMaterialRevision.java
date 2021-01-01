/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;

/**
 * @understands pipeline instance's reference to modifications
 */
public class PipelineMaterialRevision extends PersistentObject {
    private String name;
    private String folder;
    private long pipelineId;
    private Modification fromRevision;
    private Modification toRevision;
    private boolean changed;
    private Long materialId;
    private Long actualFromRevisionId;

    protected PipelineMaterialRevision() {}

    public PipelineMaterialRevision(long pipelineId, MaterialRevision revision, Long actualFromModificationId) {
        this(CaseInsensitiveString.str(revision.getMaterial().getName()), revision.getMaterial().getFolder(), pipelineId, revision.getOldestModification(), revision.getLatestModification(), revision.isChanged(), actualFromModificationId);
        recomputeFromModification(revision.getMaterial());
    }

    private PipelineMaterialRevision(String name, String folder, long pipelineId, Modification from, Modification to, boolean changed, Long actualFromModificationId) {
        this();
        this.pipelineId = pipelineId;
        this.fromRevision = from;
        this.toRevision = to;
        this.name = name;
        this.folder = folder;
        this.changed = changed;
        this.actualFromRevisionId = actualFromModificationId;
        this.materialId = to.getMaterialInstance().getId();
    }

    private void recomputeFromModification(Material material) {
        if (material instanceof DependencyMaterial) {
            this.fromRevision = this.toRevision;
        }
    }

    public Material getMaterial() {
        return toRevision.getMaterialInstance().toOldMaterial(name, folder, null);
    }

    public MaterialInstance getMaterialInstance() {
        return toRevision.getMaterialInstance();
    }

    public List<Modification> getModifications() {
        return new Modifications(fromRevision, toRevision);
    }

    public Modification getFromModification() {
        return fromRevision;
    }

    public Modification getToModification() {
        return toRevision;
    }

    public String getMaterialName() {
        return name;
    }

    public String getFolder() {
        return folder;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public long getPipelineId() {
        return pipelineId;
    }

    public void useMaterialRevision(MaterialRevision materialRevision) {
        setFromModification(materialRevision.getOldestModification());
        setToModification(materialRevision.getLatestModification());
        recomputeFromModification(materialRevision.getMaterial());
    }

    private void setFromModification(Modification modification) {
        this.fromRevision = modification;
    }

    private void setToModification(Modification modification) {
        this.toRevision = modification;
    }

    public boolean getChanged() {
        return changed;
    }

    public Long getActualFromRevisionId() {
        return actualFromRevisionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineMaterialRevision that = (PipelineMaterialRevision) o;

        if (changed != that.changed) {
            return false;
        }
        if (pipelineId != that.pipelineId) {
            return false;
        }
        if (actualFromRevisionId != null ? !actualFromRevisionId.equals(that.actualFromRevisionId) : that.actualFromRevisionId != null) {
            return false;
        }
        if (folder != null ? !folder.equals(that.folder) : that.folder != null) {
            return false;
        }
        if (fromRevision != null ? !fromRevision.equals(that.fromRevision) : that.fromRevision != null) {
            return false;
        }
        if (materialId != null ? !materialId.equals(that.materialId) : that.materialId != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (toRevision != null ? !toRevision.equals(that.toRevision) : that.toRevision != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        result = 31 * result + (int) (pipelineId ^ (pipelineId >>> 32));
        result = 31 * result + (fromRevision != null ? fromRevision.hashCode() : 0);
        result = 31 * result + (toRevision != null ? toRevision.hashCode() : 0);
        result = 31 * result + (changed ? 1 : 0);
        result = 31 * result + (materialId != null ? materialId.hashCode() : 0);
        result = 31 * result + (actualFromRevisionId != null ? actualFromRevisionId.hashCode() : 0);
        return result;
    }
}
