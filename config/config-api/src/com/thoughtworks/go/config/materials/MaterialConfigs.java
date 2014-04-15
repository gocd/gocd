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

package com.thoughtworks.go.config.materials;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.StringUtils;

@ConfigTag("materials")
@ConfigCollection(MaterialConfig.class)
public class MaterialConfigs extends BaseCollection<MaterialConfig> implements Validatable, ParamsAttributeAware {
    private static final int DEFAULT_INTERVAL = 100;
    private int intervalInSeconds = DEFAULT_INTERVAL;
    private ConfigErrors configErrors;

    public MaterialConfigs() {
    }

    public MaterialConfigs(MaterialConfig... materials) {
        super(materials);
    }

    public MaterialConfigs(List<MaterialConfig> materials) {
        this(DEFAULT_INTERVAL, materials);
    }

    public MaterialConfigs(int intervalInSeconds, List<MaterialConfig> materials) {
        super(materials);
        this.intervalInSeconds = intervalInSeconds;
    }

    public int interval() {
        return intervalInSeconds;
    }

    private List<String> allowedFolders() {
        ArrayList<String> allowed = new ArrayList<String>();
        for (MaterialConfig material : this) {
            if (!StringUtils.isBlank(material.getFolder())) {
                allowed.add(material.getFolder());
            }
        }
        allowed.add(ArtifactLogUtil.CRUISE_OUTPUT_FOLDER);
        return allowed;
    }

    boolean hasOneMaterialUseBaseFolder() {
        for (MaterialConfig material : this) {
            if (material.getFolder() == null) {
                return true;
            }
        }
        return false;
    }

    public DependencyMaterialConfig findDependencyMaterial(final CaseInsensitiveString upstreamPipeline) {
        for (MaterialConfig material : this) {
            if (material instanceof DependencyMaterialConfig) {
                DependencyMaterialConfig dependencyMaterialConfig = (DependencyMaterialConfig) material;
                if (upstreamPipeline.equals(dependencyMaterialConfig.getPipelineName())) {
                    return dependencyMaterialConfig;
                }
            }
        }
        return null;
    }

    public List<CaseInsensitiveString> getDependentPipelineNames() {
        Set<CaseInsensitiveString> names = new TreeSet<CaseInsensitiveString>();
        for (MaterialConfig material : this) {
            if (material instanceof DependencyMaterialConfig) {
                names.add(((DependencyMaterialConfig) material).getPipelineName());
            }
        }
        return new ArrayList<CaseInsensitiveString>(names);
    }

    public boolean hasMaterialWithFingerprint(MaterialConfig other) {
        for (MaterialConfig material : this) {
            if (material.isSameFlyweight(other)) {
                return true;
            }
        }
        return false;
    }

    public MaterialConfig getByFingerPrint(String fingerPrint) {
        for (MaterialConfig material : this) {
            if (material.getPipelineUniqueFingerprint().equals(fingerPrint)) {
                return material;
            }
        }
        return null;
    }

    // TODO: Probably will go away when filter.shouldIgnore is handled.
    public MaterialConfig get(MaterialConfig other) {
        for (MaterialConfig material : this) {
            if (material.isSameFlyweight(other)) {
                return material;
            }
        }
        throw new RuntimeException("Material not found: " + other);//IMP: because, config can change between BCPS call and build cause production - shilpa/jj 
    }

    public void validate(ValidationContext validationContext) {
        validateNameUniqueness();
        validateAutoUpdateState(validationContext);

        validateScmMaterials();

        Set<CaseInsensitiveString> dependencies = new HashSet<CaseInsensitiveString>();
        for (DependencyMaterialConfig material : filterDependencyMaterials()) {
            material.validateUniqueness(dependencies);
        }
    }

    private void validateScmMaterials() {
        List<ScmMaterialConfig> scmMaterials = filterScmMaterials();
        int numberOfMaterials = scmMaterials.size();
        for (ScmMaterialConfig material : scmMaterials) {
            if (numberOfMaterials > 1) {
                if (StringUtil.isBlank(material.getFolder())) {
                    material.setDestinationFolderError("Destination directory is required when specifying multiple scm materials");
                }
                for (ScmMaterialConfig otherMaterial : scmMaterials) {
                    if (otherMaterial != material) {
                        otherMaterial.validateNotSubdirectoryOf(material);
                        otherMaterial.validateDestinationDirectoryName(material);
                    }
                }
            }
        }
    }
/*
    To two methods below are to avoid creating methods on already long Material interface with a No Op implementations.
 */

    private List<ScmMaterialConfig> filterScmMaterials() {
        List<ScmMaterialConfig> scmMaterials = new ArrayList<ScmMaterialConfig>();
        for (MaterialConfig material : this) {
            if (material instanceof ScmMaterialConfig) {
                scmMaterials.add((ScmMaterialConfig) material);
            }
        }
        return scmMaterials;
    }

    private List<DependencyMaterialConfig> filterDependencyMaterials() {
        List<DependencyMaterialConfig> dependencyMaterials = new ArrayList<DependencyMaterialConfig>();
        for (MaterialConfig material : this) {
            if (material instanceof DependencyMaterialConfig) {
                dependencyMaterials.add((DependencyMaterialConfig) material);
            }
        }
        return dependencyMaterials;
    }

    private void validateAutoUpdateState(ValidationContext validationContext) {
        for (MaterialConfig material : filterScmMaterials()) {
            MaterialConfigs allMaterialsByFingerPrint = validationContext.getAllMaterialsByFingerPrint(material.getFingerprint());
            if (allMaterialsByFingerPrint != null && ((ScmMaterialConfig) material).isAutoUpdateStateMismatch(allMaterialsByFingerPrint)) {
                ((ScmMaterialConfig) material).setAutoUpdateMismatchError();
            }
        }
    }

    private void validateNameUniqueness() {
        Map<CaseInsensitiveString, AbstractMaterialConfig> materialHashMap = new HashMap<CaseInsensitiveString, AbstractMaterialConfig>();
        for (MaterialConfig material : this) {
            material.validateNameUniqueness(materialHashMap);
        }
    }

    public ConfigErrors errors() {
        initErrors();
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        initErrors();
        configErrors.add(fieldName, message);
    }

    private void initErrors() {
        if (configErrors == null) {
            configErrors = new ConfigErrors();
        }
    }

    public SvnMaterialConfig getSvnMaterial() {
        return getExistingOrDefaultMaterial(new SvnMaterialConfig("", "", "", false));
    }

    public TfsMaterialConfig getTfsMaterial() {
        return getExistingOrDefaultMaterial(new TfsMaterialConfig(new GoCipher(), new UrlArgument(""), "", "", "", ""));
    }

    public HgMaterialConfig getHgMaterial() {
        return getExistingOrDefaultMaterial(new HgMaterialConfig("", null));
    }

    public GitMaterialConfig getGitMaterial() {
        return getExistingOrDefaultMaterial(new GitMaterialConfig(""));
    }

    public P4MaterialConfig getP4Material() {
        return getExistingOrDefaultMaterial(new P4MaterialConfig("", ""));
    }

    public DependencyMaterialConfig getDependencyMaterial() {
        return getExistingOrDefaultMaterial(new DependencyMaterialConfig(new CaseInsensitiveString(""), new CaseInsensitiveString("")));
    }

    public PackageMaterialConfig getPackageMaterial() {
        return getExistingOrDefaultMaterial(new PackageMaterialConfig());
    }

    <T extends MaterialConfig> T getExistingOrDefaultMaterial(T defaultMaterial) {
        for (MaterialConfig material : this) {
            if (material.getClass().isAssignableFrom(defaultMaterial.getClass())) {
                return (T) material;
            }
        }
        return defaultMaterial;
    }

    public String getMaterialOptions() {
        return first() == null ? "" : first().getType();
    }

    public void setConfigAttributes(Object attributes) {
        clear();
        if (attributes == null) {
            return;
        }
        Map attributeMap = (Map) attributes;
        String materialType = (String) attributeMap.get(AbstractMaterialConfig.MATERIAL_TYPE);
        if (SvnMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getSvnMaterial(), (Map) attributeMap.get(SvnMaterialConfig.TYPE));
        } else if (HgMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getHgMaterial(), (Map) attributeMap.get(HgMaterialConfig.TYPE));
        } else if (GitMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getGitMaterial(), (Map) attributeMap.get(GitMaterialConfig.TYPE));
        } else if (P4MaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getP4Material(), (Map) attributeMap.get(P4MaterialConfig.TYPE));
        } else if (DependencyMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getDependencyMaterial(), (Map) attributeMap.get(DependencyMaterialConfig.TYPE));
        } else if (TfsMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getTfsMaterial(), (Map) attributeMap.get(TfsMaterialConfig.TYPE));
        } else if (PackageMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getPackageMaterial(), (Map) attributeMap.get(PackageMaterialConfig.TYPE));
        }
    }

    public boolean scmMaterialsHaveDestination() {
        for (ScmMaterialConfig scmMaterial : filterScmMaterials()) {
            if (!scmMaterial.hasDestination()) {
                return false;
            }
        }
        return true;
    }

    private void addMaterialConfig(MaterialConfig materialConfig, Map attributes) {
        materialConfig.setConfigAttributes(attributes);
        add(materialConfig);
    }
}
