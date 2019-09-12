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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.List;


@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "pipelineMaterialRevisions")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PipelineMaterialRevision extends HibernatePersistedObject {
    private String name;
    private String folder;
    private Long materialId;
    private Long actualFromRevisionId;
    private long pipelineId;
    private boolean changed;

    @ManyToOne
    @JoinColumn(name = "fromRevisionId", nullable = false)
    private Modification fromModification;
    @ManyToOne
    @JoinColumn(name = "toRevisionId", nullable = false)
    private Modification toModification;

    public PipelineMaterialRevision(long pipelineId, MaterialRevision revision, Long actualFromModificationId) {
        this(CaseInsensitiveString.str(revision.getMaterial().getName()), revision.getMaterial().getFolder(), pipelineId, revision.getOldestModification(), revision.getLatestModification(), revision.isChanged(), actualFromModificationId);
        recomputeFromModification(revision.getMaterial());
    }

    private PipelineMaterialRevision(String name, String folder, long pipelineId, Modification from, Modification to, boolean changed, Long actualFromModificationId) {
        this();
        this.pipelineId = pipelineId;
        this.fromModification = from;
        this.toModification = to;
        this.name = name;
        this.folder = folder;
        this.changed = changed;
        this.actualFromRevisionId = actualFromModificationId;
        this.materialId = to.getMaterialInstance().getId();
    }

    private void recomputeFromModification(Material material) {
        if (material instanceof DependencyMaterial) {
            this.fromModification = this.toModification;
        }
    }

    public Material getMaterial() {
        return toModification.getMaterialInstance().toOldMaterial(name, folder, null);
    }

    public MaterialInstance getMaterialInstance() {
        return toModification.getMaterialInstance();
    }

    public List<Modification> getModifications() {
        return new Modifications(fromModification, toModification);
    }

    public void useMaterialRevision(MaterialRevision materialRevision) {
        setFromModification(materialRevision.getOldestModification());
        setToModification(materialRevision.getLatestModification());
        recomputeFromModification(materialRevision.getMaterial());
    }

    public boolean getChanged() {
        return changed;
    }

    public Long getActualFromRevisionId() {
        return actualFromRevisionId;
    }
}
