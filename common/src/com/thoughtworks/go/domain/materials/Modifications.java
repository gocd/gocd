/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;

import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialRevision;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialRevision;
import com.thoughtworks.go.domain.materials.svn.SubversionRevision;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

public class Modifications extends BaseCollection<Modification> {
    private static final Logger LOG = Logger.getLogger(Modifications.class);

    public static final Comparator<Modification> LATEST_MODIFICATION_FIRST = new Comparator<Modification>() {
        public int compare(Modification me, Modification other) {
            return new Long(other.getId()).compareTo(me.getId());
        }
    };

    public Modifications() {
    }

    public Modifications(Modification... modifications) {
        super(modifications);
    }

    public Modifications(List<Modification> modifications) {
        super(modifications);
    }

    public String getUsername() {
        return isEmpty() ? "Unknown" : first().getUserDisplayName();
    }

    public String getRevision() {
        return isEmpty() ? "Unknown" : first().getRevision();
    }

    public static List<Modification> filterOutRevision(List<Modification> modifications,
                                                       Revision withoutThisRevision) {
        List<Modification> filtered = new ArrayList<>();
        for (Modification modification : modifications) {
            if (!modification.getRevision().equals(withoutThisRevision.getRevision())) {
                filtered.add(modification);
            }
        }
        return filtered;
    }

    public Boolean containsRevisionFor(Modification modification) {
        for (Modification curModification : this) {
            if(curModification.isSameRevision(modification)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasModfication(long id) {
        for (Modification modification : this) {
            if (modification.getId() == id) {
                return true;
            }
        }
        return false;
    }

    public Modifications since(long sinceId) {
        Modifications laterRevisions = new Modifications();
        for (Modification modification : this) {
            if (modification.getId() == sinceId) {
                return laterRevisions;
            }
            laterRevisions.add(modification);
        }
        throw new RuntimeException(String.format("Could not find modification %s in %s", sinceId, this));
    }

    public Revision latestRevision(Material material) {
        if (material instanceof SvnMaterial) {
            String revision = Modification.latestRevision(this).getRevision();
            return new SubversionRevision(revision);
        }
        if (material instanceof DependencyMaterial) {
            Modification latestModification = this.get(0);
            String revision = latestModification.getRevision();
            return DependencyMaterialRevision.create(revision, latestModification.getPipelineLabel());
        }
        if(material instanceof PackageMaterial) {
            Modification latestModification = this.get(0);
            return new PackageMaterialRevision(latestModification.getRevision(),latestModification.getModifiedTime(), latestModification.getAdditionalDataMap());
        }
        if (material instanceof PluggableSCMMaterial) {
            Modification latestModification = this.get(0);
            return new PluggableSCMMaterialRevision(latestModification.getRevision(), latestModification.getModifiedTime(), latestModification.getAdditionalDataMap());
        }
        return Modification.latestRevision(this);
    }

    public boolean shouldBeIgnoredByFilterIn(MaterialConfig materialConfig) {
        if (materialConfig.filter().shouldNeverIgnore()) {
            return false;
        }
        Set<ModifiedFile> allFiles = getAllFiles(this);
        Set<ModifiedFile> ignoredFiles = new HashSet<>();

        for (ModifiedFile file : allFiles) {
            appyIgnoreFilter(materialConfig, file, ignoredFiles);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking ignore filters for " + materialConfig);
            LOG.debug("Ignored files: " + ignoredFiles);
            LOG.debug("Changed files: " + CollectionUtils.subtract(allFiles, ignoredFiles));
        }

        if (materialConfig.isInvertFilter()) {
          // return true (ignore) if we are inverting the filter, and the ignoredFiles and allFiles are disjoint sets
          return Collections.disjoint(allFiles, ignoredFiles);
        } else {
          return ignoredFiles.containsAll(allFiles);
        }
    }

    private void appyIgnoreFilter(MaterialConfig materialConfig, ModifiedFile file, Set<ModifiedFile> ignoredFiles) {
        for (IgnoredFiles ignore : materialConfig.filter()) {
            if (ignore.shouldIgnore(materialConfig, file.getFileName())) {
                ignoredFiles.add(file);
            }
        }
    }

    private Set<ModifiedFile> getAllFiles(List<Modification> modifications) {
        Set<ModifiedFile> allFiles = new HashSet<>();
        for (Modification modification : modifications) {
            for (ModifiedFile modifiedFile : modification.getModifiedFiles()) {
                allFiles.add(modifiedFile);
            }
        }
        return allFiles;
    }
}
