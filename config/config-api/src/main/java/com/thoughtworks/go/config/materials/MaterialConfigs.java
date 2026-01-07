/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.*;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

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

    public DependencyMaterialConfig findDependencyMaterial(final CaseInsensitiveString upstreamPipeline) {
        for (MaterialConfig material : this) {
            if (material instanceof DependencyMaterialConfig dependencyMaterialConfig) {
                if (upstreamPipeline.equals(dependencyMaterialConfig.getPipelineName())) {
                    return dependencyMaterialConfig;
                }
            }
        }
        return null;
    }

    public List<CaseInsensitiveString> getDependentPipelineNames() {
        Set<CaseInsensitiveString> names = new TreeSet<>();
        for (MaterialConfig material : this) {
            if (material instanceof DependencyMaterialConfig) {
                names.add(((DependencyMaterialConfig) material).getPipelineName());
            }
        }
        return new ArrayList<>(names);
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

    public boolean validateTree(PipelineConfigSaveValidationContext validationContext) {
        if (isEmpty()) {
            errors().add("materials", "A pipeline must have at least one material");
        }
        validate(validationContext);
        boolean isValid = errors().isEmpty();

        for (MaterialConfig materialConfig : this) {
            isValid = materialConfig.validateTree(validationContext) && isValid;
        }
        return isValid;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateNameUniqueness();
        validateAutoUpdateState(validationContext);

        validateScmMaterials();

        Set<CaseInsensitiveString> dependencies = new HashSet<>();
        for (DependencyMaterialConfig material : filterDependencyMaterials()) {
            material.validateUniqueness(dependencies);
        }

        if (validationContext.isWithinPipelines()) {
            PipelineConfig currentPipeline = validationContext.getPipeline();
            validateOrigins(currentPipeline, validationContext);
        }
    }

    private void validateOrigins(PipelineConfig currentPipeline, ValidationContext validationContext) {
        for (DependencyMaterialConfig material : filterDependencyMaterials()) {
            PipelineConfig upstream = validationContext.getPipelineConfigByName(material.getPipelineName());
            if (upstream == null) {
                continue; // other rule validates existence of upstream
            }
            ConfigOrigin myOrigin = currentPipeline.getOrigin();
            ConfigOrigin upstreamOrigin = upstream.getOrigin();

            if (validationContext.shouldCheckConfigRepo()) {
                if (!validationContext.getConfigRepos().isReferenceAllowed(myOrigin, upstreamOrigin)) {
                    material.addError(DependencyMaterialConfig.ORIGIN,
                            String.format("Dependency from pipeline defined in %s to pipeline defined in %s is not allowed",
                                    displayNameFor(myOrigin), displayNameFor(upstreamOrigin)));
                }
            }
        }
    }

    private String displayNameFor(ConfigOrigin origin) {
        return origin != null ? origin.displayName() : "cruise-config.xml";
    }

    private void validateScmMaterials() {
        List<MaterialConfig> allSCMMaterials = getSCMAndPluggableSCMConfigs().toList();
        if (allSCMMaterials.size() > 1) {
            for (MaterialConfig material : allSCMMaterials) {
                if (isBlank(material.getFolder())) {
                    String fieldName = material instanceof ScmMaterialConfig ? ScmMaterialConfig.FOLDER : PluggableSCMMaterialConfig.FOLDER;
                    material.addError(fieldName, "Destination directory is required when a pipeline has multiple SCM materials.");
                } else {
                    validateDestinationFolder(allSCMMaterials, material);
                }
            }
        }
    }

    private Stream<MaterialConfig> getSCMAndPluggableSCMConfigs() {
        return materialsOfType(ScmMaterialConfig.class, PluggableSCMMaterialConfig.class);
    }

    private void validateDestinationFolder(List<MaterialConfig> allSCMMaterials, MaterialConfig material) {
        String materialFolder = material.getFolder();
        allSCMMaterials.stream()
            .filter(other -> other != material)
            .forEach(otherMaterial -> {
                if (otherMaterial instanceof ScmMaterialConfig scmMaterial) {
                    scmMaterial.validateNotSubdirectoryOf(materialFolder);
                    scmMaterial.validateDestinationDirectoryName(materialFolder);
                } else if (otherMaterial instanceof PluggableSCMMaterialConfig pluggableMaterial) {
                    pluggableMaterial.validateNotSubdirectoryOf(materialFolder);
                    pluggableMaterial.validateDestinationDirectoryName(materialFolder);
                } else {
                    throw new IllegalStateException("Unknown SCM material type: " + otherMaterial.getClass().getName());
                }
            });
    }

/*
    To two methods below are to avoid creating methods on already long Material interface with a No Op implementations.
 */

    private <T extends MaterialConfig> Stream<T> materialsOfType(Class<T> klass) {
        return this.stream().filter(klass::isInstance).map(klass::cast);
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends MaterialConfig, T2 extends MaterialConfig> Stream<MaterialConfig> materialsOfType(Class<T> klass, Class<T2> klass2) {
        return this.stream().filter(o -> klass.isInstance(o) || klass2.isInstance(o));
    }

    private List<DependencyMaterialConfig> filterDependencyMaterials() {
        List<DependencyMaterialConfig> dependencyMaterials = new ArrayList<>();
        for (MaterialConfig material : this) {
            if (material instanceof DependencyMaterialConfig) {
                dependencyMaterials.add((DependencyMaterialConfig) material);
            }
        }
        return dependencyMaterials;
    }

    private void validateAutoUpdateState(ValidationContext validationContext) {
        materialsOfType(ScmMaterialConfig.class).forEach(material -> {
            String fingerprint;
            try {
                fingerprint = material.getFingerprint();
            } catch (Exception ignore) {
                return;
            }
            MaterialConfigs allMaterialsByFingerPrint = validationContext.getAllMaterialsByFingerPrint(fingerprint);
            if (allMaterialsByFingerPrint != null && allMaterialsByFingerPrint.size() > 1 && allMaterialsByFingerPrint.stream().anyMatch(m -> material.isAutoUpdate() != m.isAutoUpdate())) {
                Map<CaseInsensitiveString, Boolean> pipelinesWithMaterial = validationContext.getPipelineToMaterialAutoUpdateMapByFingerprint(fingerprint);
                material.addError(
                    ScmMaterialConfig.AUTO_UPDATE,
                    String.format(
                        "The material of type %s (%s) is used elsewhere with a different value for autoUpdate (poll for changes). Those values should be the same. Pipelines:\n%s",
                        material.getTypeForDisplay(),
                        material.getDescription(),
                        MaterialErrors.autoUpdatePipelineErrorDisplay(pipelinesWithMaterial)
                    )
                );
            }
        });
    }

    private void validateNameUniqueness() {
        Map<CaseInsensitiveString, AbstractMaterialConfig> materialHashMap = new HashMap<>();
        for (MaterialConfig material : this) {
            material.validateNameUniqueness(materialHashMap);
        }
    }

    @Override
    public ConfigErrors errors() {
        initErrors();
        return configErrors;
    }

    @Override
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
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        svnMaterialConfig.setUrl("");
        svnMaterialConfig.setUserName("");
        svnMaterialConfig.setPassword("");
        svnMaterialConfig.setCheckExternals(false);
        return getExistingOrDefaultMaterial(svnMaterialConfig);
    }

    public TfsMaterialConfig getTfsMaterial() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig();
        tfsMaterialConfig.setUrl("");
        tfsMaterialConfig.setUserName("");
        tfsMaterialConfig.setPassword("");
        tfsMaterialConfig.setProjectPath("");
        return getExistingOrDefaultMaterial(tfsMaterialConfig);
    }

    public HgMaterialConfig getHgMaterial() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig();
        hgMaterialConfig.setUrl("");
        return getExistingOrDefaultMaterial(hgMaterialConfig);
    }

    public GitMaterialConfig getGitMaterial() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig();
        gitMaterialConfig.setUrl("");
        return getExistingOrDefaultMaterial(gitMaterialConfig);
    }

    public P4MaterialConfig getP4Material() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig();
        p4MaterialConfig.setUrl("");
        p4MaterialConfig.setView("");
        return getExistingOrDefaultMaterial(p4MaterialConfig);
    }

    public DependencyMaterialConfig getDependencyMaterial() {
        return getExistingOrDefaultMaterial(new DependencyMaterialConfig(new CaseInsensitiveString(""), new CaseInsensitiveString("")));
    }

    public PackageMaterialConfig getPackageMaterial() {
        return getExistingOrDefaultMaterial(new PackageMaterialConfig());
    }

    public PluggableSCMMaterialConfig getSCMMaterial() {
        return getExistingOrDefaultMaterial(new PluggableSCMMaterialConfig());
    }

    @SuppressWarnings("unchecked")
    <T extends MaterialConfig> T getExistingOrDefaultMaterial(T defaultMaterial) {
        return this.stream()
            .filter(m -> defaultMaterial.getClass().isInstance(m))
            .map(m -> (T) m)
            .findFirst()
            .orElse(defaultMaterial);
    }

    public String getMaterialOptions() {
        return Optional.ofNullable(getFirstOrNull()).map(MaterialConfig::getType).orElse("");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setConfigAttributes(Object attributes) {
        clear();
        if (attributes == null) {
            return;
        }
        Map<String, Object> attributeMap = (Map<String, Object>) attributes;
        String materialType = (String) attributeMap.get(AbstractMaterialConfig.MATERIAL_TYPE);
        if (SvnMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getSvnMaterial(), attributeMap.get(SvnMaterialConfig.TYPE));
        } else if (HgMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getHgMaterial(), attributeMap.get(HgMaterialConfig.TYPE));
        } else if (GitMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getGitMaterial(), attributeMap.get(GitMaterialConfig.TYPE));
        } else if (P4MaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getP4Material(), attributeMap.get(P4MaterialConfig.TYPE));
        } else if (DependencyMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getDependencyMaterial(), attributeMap.get(DependencyMaterialConfig.TYPE));
        } else if (TfsMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getTfsMaterial(), attributeMap.get(TfsMaterialConfig.TYPE));
        } else if (PackageMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getPackageMaterial(), attributeMap.get(PackageMaterialConfig.TYPE));
        } else if (PluggableSCMMaterialConfig.TYPE.equals(materialType)) {
            addMaterialConfig(getSCMMaterial(), attributeMap.get(PluggableSCMMaterialConfig.TYPE));
        }
    }

    public List<CaseInsensitiveString> materialNames() {
        List<CaseInsensitiveString> names = new ArrayList<>();
        for (MaterialConfig material : this) {
            if (!CaseInsensitiveString.isEmpty(material.getName())) {
                names.add(material.getName());
            }
        }
        return names;
    }

    private void addMaterialConfig(MaterialConfig materialConfig, Object attributes) {
        materialConfig.setConfigAttributes(attributes);
        add(materialConfig);
    }

    public boolean hasDependencyMaterial(PipelineConfig pipeline) {
        return findDependencyMaterial(pipeline.name()) != null;
    }

    public MaterialConfig getByMaterialFingerPrint(String materialFingerprint) {
        List<MaterialConfig> materialConfigs = this
                .stream()
                .filter(materialConfig -> materialConfig.getFingerprint().equals(materialFingerprint))
                .toList();
        if (!materialConfigs.isEmpty()) {
            return materialConfigs.getFirst();
        }
        return null;
    }
}
