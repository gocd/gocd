/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.ConfigVisitor;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.ObjectUtil;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.domain.BuildCommand.*;

public class Materials extends BaseCollection<Material> {
    private static final int DEFAULT_INTERVAL = 100;
    private int intervalInSeconds = DEFAULT_INTERVAL;

    public Materials() {
    }

    public Materials(Material... materials) {
        super(materials);
    }

    public Materials(List<Material> materials) {
        this(DEFAULT_INTERVAL, materials);
    }

    public Materials(int intervalInSeconds, List<Material> materials) {
        super(materials);
        this.intervalInSeconds = intervalInSeconds;
    }

    public Materials(MaterialConfigs materialConfigs) {
        for (MaterialConfig materialConfig : materialConfigs) {
            add(convertToMaterial(materialConfig));
        }
    }

    public int interval() {
        return intervalInSeconds;
    }

    /**
     * @deprecated Used only in tests
     */
    public MaterialRevisions latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        MaterialRevisions revisions = new MaterialRevisions();
        for (Material material : this) {
            List<Modification> modifications = new ArrayList<>();
            if (material instanceof SvnMaterial) {
                modifications = ((SvnMaterial) material).latestModification(baseDir, execCtx);
            }
            if (material instanceof HgMaterial) {
                modifications = ((HgMaterial) material).latestModification(baseDir, execCtx);
            }
            if (material instanceof GitMaterial) {
                modifications = ((GitMaterial) material).latestModification(baseDir, execCtx);
            }
            if (material instanceof P4Material) {
                modifications = ((P4Material) material).latestModification(baseDir, execCtx);
            }
            if (material instanceof TfsMaterial) {
                modifications = ((TfsMaterial) material).latestModification(baseDir, execCtx);
            }
            if (material instanceof DependencyMaterial) {
                modifications = ((DependencyMaterial) material).latestModification(baseDir, execCtx);
            }
            revisions.addRevision(material, modifications);
        }
        return revisions;
    }

    public void cleanUp(File baseFolder, ConsoleOutputStreamConsumer consumer) {
        if (hasMaterialsWithNoDestinationFolder()) {
            return;
        }

        DirectoryCleaner cleaner = new DirectoryCleaner(baseFolder, consumer);
        cleaner.allowed(allowedFolders());
        cleaner.clean();
    }

    private List<String> allowedFolders() {
        ArrayList<String> allowed = new ArrayList<>();
        for (Material material : this) {
            if (!StringUtils.isBlank(material.getFolder())) {
                allowed.add(material.getFolder());
            }
        }
        allowed.add(ArtifactLogUtil.CRUISE_OUTPUT_FOLDER);
        return allowed;
    }

    boolean hasMaterialsWithNoDestinationFolder() {
        for (Material material : this) {
            AbstractMaterial abstractMaterial = (AbstractMaterial) material;
            if (abstractMaterial.supportsDestinationFolder() && !abstractMaterial.hasDestinationFolder()) {
                return true;
            }
        }
        return false;
    }

    public void accept(ConfigVisitor visitor) {
        for (Material material : this) {
            visitor.visit(material);
        }
    }

    public int count(Class<? extends Material> materialClass) {
        int count = 0;
        for (Material material : this) {
            if (materialClass.isInstance(material)) {
                count++;
            }
        }
        return count;
    }

    public Material byFolder(String folder) {
        for (Material material : this) {
            if ((material instanceof ScmMaterial || material instanceof PluggableSCMMaterial) && ObjectUtil.nullSafeEquals(folder, material.getFolder())) {
                return material;
            }
        }
        return null;
    }

    public Material getByFingerPrint(String fingerPrint) {
        for (Material material : this) {
            if (material.getPipelineUniqueFingerprint().equals(fingerPrint)) {
                return material;
            }
        }
        return null;
    }

    public Material get(Material other) {
        for (Material material : this) {
            if (material.isSameFlyweight(other)) {
                return material;
            }
        }
        throw new RuntimeException("Material not found: " + other);//IMP: because, config can change between BCPS call and build cause production - shilpa/jj
    }

    /*
    To two methods below are to avoid creating methods on already long Material interface with a No Op implementations.
 */

    private List<ScmMaterial> filterScmMaterials() {
        List<ScmMaterial> scmMaterials = new ArrayList<>();
        for (Material material : this) {
            if (material instanceof ScmMaterial) {
                scmMaterials.add((ScmMaterial) material);
            }
        }
        return scmMaterials;
    }

    public boolean scmMaterialsHaveDestination() {
        for (ScmMaterial scmMaterial : filterScmMaterials()) {
            if (!scmMaterial.hasDestinationFolder()) {
                return false;
            }
        }
        return true;
    }

    public SvnMaterial getSvnMaterial() {
        return getExistingOrDefaultMaterial(new SvnMaterial("", "", "", false));
    }

    public TfsMaterial getTfsMaterial() {
        return getExistingOrDefaultMaterial(new TfsMaterial(new GoCipher(), new UrlArgument(""), "", "", "", ""));
    }

    public HgMaterial getHgMaterial() {
        return getExistingOrDefaultMaterial(new HgMaterial("", null));
    }

    public GitMaterial getGitMaterial() {
        return getExistingOrDefaultMaterial(new GitMaterial(""));
    }

    public P4Material getP4Material() {
        return getExistingOrDefaultMaterial(new P4Material("", ""));
    }

    public DependencyMaterial getDependencyMaterial() {
        return getExistingOrDefaultMaterial(new DependencyMaterial(new CaseInsensitiveString(""), new CaseInsensitiveString("")));
    }

    private <T extends Material> T getExistingOrDefaultMaterial(T defaultMaterial) {
        for (Material material : this) {
            if (material.getClass().isAssignableFrom(defaultMaterial.getClass())) {
                return (T) material;
            }
        }
        return defaultMaterial;
    }

    public String getMaterialOptions() {
        return first() == null ? "" : first().getType();
    }

    private Material convertToMaterial(MaterialConfig materialConfig) {
        if (SvnMaterial.TYPE.equals(materialConfig.getType())) {
            return new SvnMaterial((SvnMaterialConfig) materialConfig);
        } else if (HgMaterial.TYPE.equals(materialConfig.getType())) {
            return new HgMaterial((HgMaterialConfig) materialConfig);
        } else if (GitMaterial.TYPE.equals(materialConfig.getType())) {
            return new GitMaterial((GitMaterialConfig) materialConfig);
        } else if (P4Material.TYPE.equals(materialConfig.getType())) {
            return new P4Material((P4MaterialConfig) materialConfig);
        } else if (DependencyMaterial.TYPE.equals(materialConfig.getType())) {
            return new DependencyMaterial((DependencyMaterialConfig) materialConfig);
        } else if (TfsMaterial.TYPE.equals(materialConfig.getType())) {
            return new TfsMaterial((TfsMaterialConfig) materialConfig);
        } else if (PackageMaterial.TYPE.equals(materialConfig.getType())) {
            return new PackageMaterial((PackageMaterialConfig) materialConfig);
        } else if (PluggableSCMMaterial.TYPE.equals(materialConfig.getType())) {
            return new PluggableSCMMaterial((PluggableSCMMaterialConfig) materialConfig);
        } else if (TestingMaterial.TYPE.equals(materialConfig.getType())) {
            return new TestingMaterial((TestingMaterialConfig) materialConfig);
        }
        throw new RuntimeException("Unexpected material type: " + materialConfig.getClass() + ": " + materialConfig);
    }

    public MaterialConfigs convertToConfigs() {
        MaterialConfigs configs = new MaterialConfigs();
        for (Material material : this) {
            configs.add(material.config());
        }
        return configs;
    }

    public boolean hasMaterialConfigWithFingerprint(MaterialConfig materialConfig) {
        for (Material material : this) {
            if (material.getFingerprint().equals(materialConfig.getFingerprint())) {
                return true;
            }
        }
        return false;
    }


    public BuildCommand cleanUpCommand(String baseDir) {
        if (hasMaterialsWithNoDestinationFolder()) {
            return noop();
        }
        List<String> allowed = allowedFolders();
        return cleandir(baseDir, allowed.toArray(new String[allowed.size()]));
    }

}
