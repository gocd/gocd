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
package com.thoughtworks.go.server.materials;

import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

/**
 * @understands how to retrieve changes on SCMs given a material
 */
@Component
public class MaterialChecker {
    private MaterialRepository materialRepository;

    @Autowired
    public MaterialChecker(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public MaterialRevision findSpecificRevision(Material material, String revision) {
        if (StringUtils.isEmpty(revision)) { throw new RuntimeException(format("Revision was not specified for material [%s]", material)); }
        Modification modification = materialRepository.findModificationWithRevision(material, revision);
        if (modification == null) { throw new RuntimeException(format("Unable to find revision [%s] for material [%s]", revision, material)); }
        return new MaterialRevision(material, modification);
    }

    public MaterialRevisions findRevisionsSince(MaterialRevisions peggedRevisions, Materials newMaterials, MaterialRevisions previous, MaterialRevisions latestRevisions) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (Material material : newMaterials) {
            MaterialRevision previousRevision = previous.findRevisionFor(material);
            if (previousRevision == null) {
                materialRevisions.addRevision(latestRevisions.findRevisionFor(material));
            } else {
                Material previousMaterial = previousRevision.getMaterial();
                if (ensureHasRevisionForMaterial(peggedRevisions, materialRevisions, previousMaterial)) {
                    continue;
                }
                previousMaterial = previousMaterial instanceof ScmMaterial ? newMaterials.byFolder(previousMaterial.getFolder()) : previousMaterial;
                List<Modification> newModifications = materialRepository.findModificationsSince(previousMaterial, previousRevision);
                MaterialRevision newMaterialRevision = previousRevision.latestChanges(material, previousRevision.getModifications(), newModifications);
                materialRevisions.addRevision(newMaterialRevision);
            }
        }
        return materialRevisions;
    }

    public MaterialRevisions findLatestRevisions(MaterialRevisions peggedRevisions, Materials materials) {
        MaterialRevisions revisions = new MaterialRevisions();

        for (Material material : materials) {
            if (ensureHasRevisionForMaterial(peggedRevisions, revisions, material)) { continue; }

            for (MaterialRevision revision : materialRepository.findLatestModification(material)) {
                revision.markAsChanged();
                revisions.addRevision(revision);
            }
        }
        return revisions;
    }

    private boolean ensureHasRevisionForMaterial(MaterialRevisions alreadyFoundRevisions, MaterialRevisions revisions, Material material) {
        for (MaterialRevision alreadyFoundRevision : alreadyFoundRevisions) {
            if (material.equals(alreadyFoundRevision.getMaterial())) {
                revisions.addRevision(alreadyFoundRevision);
                return true;
            }
        }
        return false;
    }

    public boolean hasPipelineEverRunWith(String pipelineName, MaterialRevisions materialRevisions) {
        return materialRepository.hasPipelineEverRunWith(pipelineName, materialRevisions);
    }

    public void updateChangedRevisions(CaseInsensitiveString pipelineName, BuildCause buildCause) {
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            Long latestModificationRunByPipeline = materialRepository.latestModificationRunByPipeline(pipelineName, materialRevision.getMaterial());
            Modifications revised = new Modifications();
            for (Modification modification : materialRevision.getModifications()) {
                if(modification.getId() > latestModificationRunByPipeline)
                    revised.add(modification);
            }
            if(!revised.isEmpty()) {
                materialRevision.replaceModifications(revised);
                materialRevision.markAsChanged();
            }
            else{
                materialRevision.markAsNotChanged();
            }
        }
    }
}
