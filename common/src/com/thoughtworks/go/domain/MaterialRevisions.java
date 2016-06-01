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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.domain.materials.git.GitMaterialUpdater;
import com.thoughtworks.go.util.ObjectUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.log4j.Logger;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

// Understands multiple materials each with their own revision
public class MaterialRevisions implements Serializable, Iterable<MaterialRevision> {
    private static final Logger LOGGER = Logger.getLogger(MaterialRevisions.class);

    public static final MaterialRevisions EMPTY = new MaterialRevisions();
    private List<MaterialRevision> revisions = new ArrayList<>();

    public MaterialRevisions(MaterialRevision... revisions) {
        this.revisions.addAll(Arrays.asList(revisions));
    }

    public MaterialRevisions(Collection<MaterialRevision> revisions) {
        this(revisions.toArray(new MaterialRevision[]{}));
    }

    public void addAll(MaterialRevisions materialRevisions) {
        this.revisions.addAll(materialRevisions.getRevisions());
    }

    public void addRevision(MaterialRevision materialRevision) {
        revisions.add(materialRevision);
    }

    public void addRevision(Material material, List<Modification> modifications) {
        revisions.add(new MaterialRevision(material, modifications));
    }

    public void addRevision(Material material, Modification... modifications) {
        for (Modification modification : modifications) {
            bombIfNull(modification, "Modification cannot be null.");
        }
        addRevision(material, Arrays.asList(modifications));
    }

    public int totalNumberOfModifications() {
        int count = 0;
        for (MaterialRevision revision : revisions) {
            count += revision.numberOfModifications();
        }
        return count;
    }

    public int numberOfRevisions() {
        return revisions.size();
    }

    public void accept(ModificationVisitor visitor) {
        for (MaterialRevision revision : revisions) {
            revision.accept(visitor);
        }
    }

    public boolean isEmpty() {
        return totalNumberOfModifications() == 0;
    }

    public Date getDateOfLatestModification() {
        return firstModifiedMaterialRevision().getDateOfLatestModification();
    }

    public String buildCausedBy() {
        return firstModifiedMaterialRevision().buildCausedBy();
    }

    public String buildCauseMessage() {
        return firstModifiedMaterialRevision().buildCauseMessage();

    }

    public String latestRevision() {
        return firstModifiedMaterialRevision().getRevision().getRevision();
    }

    public MaterialRevision firstModifiedMaterialRevision() {
        for (MaterialRevision revision : revisions) {
            if (revision.isChanged()) {
                return revision;
            }
        }
        return revisions.isEmpty() ? new NullMaterialRevision() : revisions.get(0);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MaterialRevisions revisions1 = (MaterialRevisions) o;

        if (!revisions.equals(revisions1.revisions)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return revisions.hashCode();
    }

    /**
     * @deprecated Very very evil - TODO: get rid of this as part of #2055
     */
    public Materials getMaterials() {
        Materials materials = new Materials();
        for (MaterialRevision revision : revisions) {
            materials.add(revision.getMaterial());
        }
        return materials;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MaterialRevision[\n");
        for (MaterialRevision revision : revisions) {
            builder.append("\t" + revision);
            builder.append("\n");
        }
        builder.append("]");
        return builder.toString();
    }

    public MaterialRevision getMaterialRevision(int index) {
        return revisions.get(index);
    }

    private boolean internalHasChangedSince(MaterialRevisions original) {
        if (revisions.size() != original.revisions.size()) {
            return true;
        }
        for (MaterialRevision revision : revisions) {
            MaterialRevision originalRevision = original.findRevisionFor(revision.getMaterial());
            if (originalRevision == null || revision.hasChangedSince(originalRevision)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasChangedSince(MaterialRevisions original) {
        return filter(original).internalHasChangedSince(original);
    }


    public MaterialRevision findRevisionFor(Material material) {
        for (MaterialRevision revision : revisions) {
            if (material.getPipelineUniqueFingerprint().equals(revision.getMaterial().getPipelineUniqueFingerprint())) {
                return revision;
            }
        }
        return null;
    }

    public MaterialRevision findRevisionFor(MaterialConfig material) {
        for (MaterialRevision revision : revisions) {
            if (material.getPipelineUniqueFingerprint().equals(revision.getMaterial().getPipelineUniqueFingerprint())) {
                return revision;
            }
        }
        return null;
    }

    public MaterialRevision findRevisionUsingMaterialFingerprintFor(Material material) {
        for (MaterialRevision revision : revisions) {
            if (material.getFingerprint().equals(revision.getMaterial().getFingerprint())) {
                return revision;
            }
        }
        return null;
    }

    public List<MaterialRevision> getRevisions() {
        return revisions;
    }

    public Iterator<MaterialRevision> iterator() {
        return revisions.iterator();
    }

    public boolean containsMyCheckin(Matcher matcher) {
        for (MaterialRevision materialRevision : this) {
            for (Modification modification : materialRevision.getModifications()) {
                String fullComment = String.format("%s %s", modification.getUserName(), modification.getComment());
                if (matcher.matches(fullComment)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSameAs(MaterialRevisions other) {
        if (!getMaterials().equals(other.getMaterials())) {
            return false;
        }
        for (MaterialRevision materialRevision : revisions) {
            MaterialRevision peer = other.findRevisionFor(materialRevision.getMaterial());
            if (peer == null) {
                continue;
            }
            if (!materialRevision.hasSameHeadAs(peer)) {
                return false;
            }
        }
        return true;
    }

    public MaterialRevision getMaterialRevision(String folder) {
        for (MaterialRevision materialRevision : revisions) {
            if (ObjectUtil.nullSafeEquals(folder, materialRevision.getMaterial().getFolder())) {
                return materialRevision;
            }
        }
        return null;
    }

    private MaterialRevisions filter(MaterialRevisions other) {
        MaterialRevisions filtered = new MaterialRevisions();
        for (MaterialRevision myRevision : revisions) {
            MaterialRevision originalRevision = other.findRevisionFor(myRevision.getMaterial());
            filtered.addRevision(myRevision.filter(originalRevision));
        }
        return filtered;
    }

    public Map<CaseInsensitiveString, String> getNamedRevisions() {
        Map<CaseInsensitiveString, String> results = new HashMap<>();
        for (MaterialRevision mr : revisions) {
            CaseInsensitiveString materialName = mr.getMaterial().getName();
            if (!CaseInsensitiveString.isBlank(materialName)) {
                results.put(materialName, getRevisionValueOf(mr.getRevision()));
            }
        }
        return results;
    }

    private String getRevisionValueOf(Revision revision) {
        if (revision instanceof DependencyMaterialRevision) {
            return ((DependencyMaterialRevision) revision).getPipelineLabel();
        }
        return revision.getRevision();
    }

    public DependencyMaterialRevision findDependencyMaterialRevision(String pipelineName) {
        for (MaterialRevision materialRevision : this) {
            Revision revision = materialRevision.getRevision();
            if (revision instanceof DependencyMaterialRevision) {
                DependencyMaterialRevision dependencyMaterialRevision = (DependencyMaterialRevision) revision;
                if (dependencyMaterialRevision.getPipelineName().equalsIgnoreCase(pipelineName)) {
                    return dependencyMaterialRevision;
                }
            }
        }
        return null;
    }

    public boolean isMissingModifications() {
        if (isEmpty()) {
            return true;
        }
        for (MaterialRevision materialRevision : this) {
            if (materialRevision.getModifications().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void populateEnvironmentVariables(EnvironmentVariableContext context, File workingDir) {
        for (MaterialRevision revision : this) {
            revision.populateEnvironmentVariables(context, workingDir);
        }
    }

    public Modifications getModifications(Material material) {
        for (MaterialRevision revision : revisions) {
            if (revision.getMaterial().equals(material)) {
                return revision.getModifications();
            }
        }
        return new Modifications();
    }

    public boolean containsModificationFor(Material material) {
        for (MaterialRevision materialRevision : this) {
            if (material.equals(materialRevision.getMaterial())) {
                return true;
            }
        }
        return false;
    }

    public boolean containsModificationForFingerprint(Material material) {
        String pipelineUniqueFingerprint = material.getPipelineUniqueFingerprint();
        for (MaterialRevision materialRevision : this) {
            if (pipelineUniqueFingerprint.equals(materialRevision.getMaterial().getPipelineUniqueFingerprint())) {
                return true;
            }
        }
        return false;
    }

    public MaterialRevision findRevisionForFingerPrint(String fingerprint) {//TODO: pass material in, it has a method to equate fingerprints(this is bad encapsulation) -jj/shilpa
        for (MaterialRevision revision : revisions) {
            if (fingerprint.equals(revision.getMaterial().getFingerprint())) {
                return revision;
            }
        }
        return null;

    }

    public void updateRevisionChangedStatus(MaterialRevisions original) {
        for (MaterialRevision materialRevision : this) {
            materialRevision.updateRevisionChangedStatus(original.findRevisionFor(materialRevision.getMaterial()));
        }
    }

    public boolean hasDependencyMaterials() {
        for (MaterialRevision materialRevision : this) {
            if (materialRevision.getMaterial() instanceof DependencyMaterial) {
                return true;
            }
        }
        return false;
    }

    public List<String> getCardNumbersFromComments() {
        List<String> cardNumbers = new ArrayList<>();
        for (Modification modification : collectAllModifications()) {
            collectUniqueCardNumbers(modification, cardNumbers);
        }
        return cardNumbers;
    }

    public void collectUniqueCardNumbers(Modification modification, List<String> cardNumbers) {
        for (String number : modification.getCardNumbersFromComment()) {
            if (!cardNumbers.contains(number)) {
                cardNumbers.add(number);
            }
        }
    }

    private Modifications collectAllModifications() {
        Set<Modification> mods = new TreeSet<>(Modifications.LATEST_MODIFICATION_FIRST);
        for (MaterialRevision revision : revisions) {
            mods.addAll(revision.getModifications());
        }
        return new Modifications(new ArrayList<>(mods));
    }

    public BuildCommand updateToCommand(String baseDir) {
        List<BuildCommand> commands = new ArrayList<>();
        for (MaterialRevision revision : revisions) {
            Material material = revision.getMaterial();
            if(material instanceof ScmMaterial) {
                if (material instanceof GitMaterial) {
                    GitMaterialUpdater updater = new GitMaterialUpdater((GitMaterial) material);
                    commands.add(updater.updateTo(baseDir, revision.toRevisionContext()));
                } else {
                    commands.add(BuildCommand.fail("%s Material is not supported for new build command agent", material.getTypeForDisplay()));
                }
            }
        }
        return BuildCommand.compose(commands);
    }

}
