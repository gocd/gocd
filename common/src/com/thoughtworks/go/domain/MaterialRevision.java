/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

public class MaterialRevision implements Serializable {
    private Material material;
    private boolean changed;
    private Modifications modifications;

    public MaterialRevision(Material material, boolean changed, List<Modification> modifications) {
        bombIfNull(modifications, "modifications cannot be null");
        bombIf(modifications.contains(null), "modifications cannot be null");
        this.material = material;
        this.changed = changed;
        this.modifications = new Modifications(modifications);
    }

    public MaterialRevision(Material material, List<Modification> modifications) {
        this(material, false, modifications);
    }

    public MaterialRevision(Material material, Modification... modifications) {
        this(material, false, modifications);
    }

    public MaterialRevision(Material material, boolean changed, Modification... modifications) {
        this(material, changed, Arrays.asList(modifications));
    }

    public int numberOfModifications() {
        return modifications.size();
    }

    public Date getDateOfLatestModification() {
        if (modifications.size() > 0) {
            return modifications.get(0).getModifiedTime();
        } else {
            return null;
        }
    }

    public Material getMaterial() {
        return material;
    }

    public Revision getRevision() {
        if (material == null) {
            return new NullRevision();
        }
        return modifications.latestRevision(material);
    }

    public Revision getOldestRevision() {
        return material == null ? new NullRevision() : material.oldestRevision(modifications);
    }

    public String getLatestRevisionString() {
        return getRevision().getRevision();
    }

    public String getLatestShortRevision() {
        return material.getShortRevision(getLatestRevisionString());
    }


    public boolean hasModifications() {
        return modifications.size() > 0;
    }

    public Modification getModification(int i) {
        return modifications.get(i);
    }

    public void accept(ModificationVisitor visitor) {
        visitor.visit(this);
        visitor.visit(material, getRevision());
        for (Modification modification : modifications) {
            modification.accept(visitor);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MaterialRevision that = (MaterialRevision) o;

        if (material != null ? !material.equals(that.material) : that.material != null) {
            return false;
        }

        if (modifications != null ? !modifications.equals(that.modifications) : that.modifications != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (material != null ? material.hashCode() : 0);
        result = 31 * result + (modifications != null ? modifications.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "[" + material + ", " + getRevision() + ";" + modifications.toString() + "]";
    }

    public void updateTo(File baseDir, ProcessOutputStreamConsumer consumer, final SubprocessExecutionContext execCtx) {
        material.updateTo(consumer, baseDir, toRevisionContext(), execCtx);
    }

    public RevisionContext toRevisionContext() {
        return new RevisionContext(getRevision(), getOldestRevision(), numberOfModifications());
    }

    public Boolean hasChangedSince(MaterialRevision original) {
        return !(sameRevision(original) && sameMaterial(original));
    }

    private boolean sameMaterial(MaterialRevision original) {
        return (material.equals(original.material));
    }

    private boolean sameRevision(MaterialRevision original) {
        return getRevision().equals(original.getRevision());
    }

    public MaterialRevision latestChanges(Material newMaterial, List<Modification> oldModifications, List<Modification> newModifications) {
        if (newModifications.isEmpty()) {
            List<Modification> result = new ArrayList<>();
            if (!oldModifications.isEmpty()) {
                result.add(new Modification(oldModifications.get(0)));
            }
            MaterialRevision materialRevision = new MaterialRevision(newMaterial, result);
            materialRevision.markAsNotChanged();
            return materialRevision;
        } else {
            MaterialRevision materialRevision = new MaterialRevision(newMaterial, newModifications);
            materialRevision.markAsChanged();
            return materialRevision;
        }
    }

    public void markAsChanged() {
        this.changed = true;
    }

    public void markAsNotChanged() {
        this.changed = false;
    }

    public String buildCausedBy() {
        //TODO: #2363 May move this logic to material object later
        if (material instanceof DependencyMaterial) {
            return getRevision().getRevision();
        } else {
            return modifications.getUsername();
        }
    }

    public String buildCauseMessage() {
        StringBuilder builder = new StringBuilder();
        //TODO: #2363 May move this logic to material object later
        if (material instanceof DependencyMaterial) {
            return builder.append("triggered by ").append(getRevision().getRevision()).toString();
        } else {
            return builder.append("modified by ").append(buildCausedBy()).toString();
        }
    }

    public Modifications getModifications() {
        return modifications;
    }

    public boolean hasSameHeadAs(MaterialRevision peer) {
        return this.getRevision().equals(peer.getRevision());
    }

    public MaterialRevision filter(MaterialRevision previous) {
        if (modifications.shouldBeIgnoredByFilterIn(material.config())) {
            return previous;
        } else {
            return this;
        }
    }

    public boolean isChanged() {
        return changed;
    }

    public Modification getLatestModification() {
        assertHasModifications();
        return modifications.get(0);
    }

    public Modification getOldestModification() {
        assertHasModifications();
        return modifications.get(modifications.size() - 1);
    }

    private void assertHasModifications() {
        if (modifications.size() == 0) {
            bomb(String.format("There are no modifications on material %s.", material));
        }
    }

    public void populateEnvironmentVariables(EnvironmentVariableContext context, File workingDir) {
        material.populateEnvironmentContext(context, this, workingDir);
    }

    public String getMaterialName() {
        return material.getDisplayName();
    }

    public String getTruncatedMaterialName() {
        return material.getTruncatedDisplayName();
    }

    public String getMaterialType() {
        return material.getTypeForDisplay();
    }

    public String getLatestComment() {
        return getLatestModification().getComment();
    }

    public String getLatestUser() {
        return getLatestModification().getUserDisplayName();
    }

    public MaterialRevision subtract(MaterialRevision other) {
        List<Modification> newModifications = new ArrayList<>();
        for (Modification modification : modifications) {
            if (!other.hasModification(modification)) {
                newModifications.add(modification);
            }
        }
        return new MaterialRevision(material, newModifications);
    }

    private boolean hasModification(Modification modification) {
        return modifications.contains(modification);
    }

    public void addModifications(List<Modification> modifications) {
        this.modifications.addAll(modifications);
    }

    public void replaceModifications(List<Modification> modifications) {
        this.modifications.clear();
        this.modifications.addAll(modifications);
    }

    @Deprecated //used only in triangle dependency case of fan-in off - Srini
    public void updateRevisionChangedStatus(MaterialRevision revisionFor) {
        if (revisionFor.isChanged() && revisionFor.hasModification(getLatestModification())) {
            markAsChanged();
        } else {
            markAsNotChanged();
        }
    }

    public boolean isDependencyMaterialRevision() {
        return material instanceof DependencyMaterial;
    }

    public boolean isPackageMaterialRevision() {
        return material instanceof PackageMaterial;
    }

    public Set<String> getCardNumbersFromComments() {
        TreeSet<String> cardNumbers = new TreeSet<>();
        for (Modification modification : modifications) {
            cardNumbers.addAll(modification.getCardNumbersFromComment());
        }
        return cardNumbers;
    }
}
