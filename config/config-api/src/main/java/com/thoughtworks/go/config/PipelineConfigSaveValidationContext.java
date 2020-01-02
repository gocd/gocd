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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.rules.RulesValidationContext;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.util.Node;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PipelineConfigSaveValidationContext implements ValidationContext {
    private final Boolean isPipelineBeingCreated;
    private final String groupName;
    private final Validatable immediateParent;
    private BasicCruiseConfig cruiseConfig;
    private PipelineConfig pipelineBeingValidated;
    private boolean isPipeline = true;//Does not support template editing yet
    private final PipelineConfigSaveValidationContext parentContext;
    private PipelineConfig pipeline;
    private StageConfig stage;
    private JobConfig job;
    private MaterialConfigFingerprintMap materialConfigsFingerprintMap;

    private PipelineConfigSaveValidationContext(Boolean isPipelineBeingCreated, String groupName, Validatable immediateParent) {
        this.isPipelineBeingCreated = isPipelineBeingCreated;
        this.groupName = groupName;
        this.immediateParent = immediateParent;
        this.parentContext = null;
    }

    private PipelineConfigSaveValidationContext(Validatable immediateParent, PipelineConfigSaveValidationContext parentContext) {
        this.isPipelineBeingCreated = parentContext.isPipelineBeingCreated();
        this.groupName = parentContext.groupName;
        this.immediateParent = immediateParent;
        this.parentContext = parentContext;
        if (immediateParent instanceof BasicCruiseConfig) {
            this.cruiseConfig = (BasicCruiseConfig) immediateParent;
        } else if (parentContext.cruiseConfig != null) {
            this.cruiseConfig = parentContext.cruiseConfig;
        }
        if (immediateParent instanceof PipelineConfig) {
            this.pipeline = (PipelineConfig) immediateParent;
        } else if (parentContext.pipeline != null) {
            this.pipeline = parentContext.pipeline;
        }
        if (immediateParent instanceof JobConfig) {
            this.job = (JobConfig) immediateParent;
        } else if (parentContext.getJob() != null) {
            this.job = parentContext.job;
        }
        if (immediateParent instanceof StageConfig) {
            this.stage = (StageConfig) immediateParent;
        } else if (parentContext.stage != null) {
            this.stage = parentContext.stage;
        }
        this.pipelineBeingValidated = parentContext.pipelineBeingValidated;
        if (parentContext.pipelineBeingValidated == null) {
            this.pipelineBeingValidated = this.pipeline;
        }
    }

    public static PipelineConfigSaveValidationContext forChain(Boolean isPipelineBeingCreated, String groupName, Validatable... validatables) {
        PipelineConfigSaveValidationContext tail = new PipelineConfigSaveValidationContext(isPipelineBeingCreated, groupName, null);
        for (Validatable validatable : validatables) {
            tail = tail.withParent(validatable);
        }
        return tail;
    }

    @Override
    public PipelineConfigSaveValidationContext withParent(Validatable current) {
        return new PipelineConfigSaveValidationContext(current, this);
    }

    @Override
    public EnvironmentConfig getEnvironment() {
        throw new NotImplementedException("Operation not supported");
    }

    @Override
    public ConfigReposConfig getConfigRepos() {
        throw new NotImplementedException("Operation not supported");
    }

    @Override
    public JobConfig getJob() {
        return this.job;
    }

    @Override
    public boolean isWithinEnvironment() {
        return false;
    }

    @Override
    public StageConfig getStage() {
        return this.stage;
    }

    @Override
    public PipelineConfig getPipeline() {
        return this.pipeline;
    }

    @Override
    public PipelineTemplateConfig getTemplate() {
        throw new NotImplementedException("Operation not supported");
    }

    @Override
    public String getParentDisplayName() {
        return getParent().getClass().getAnnotation(ConfigTag.class).value();
    }

    @Override
    public Validatable getParent() {
        return immediateParent;
    }

    @Override
    public boolean isWithinTemplates() {
        return !isPipeline;
    }

    @Override
    public boolean isWithinPipelines() {
        return isPipeline;
    }

    @Override
    public PipelineConfigs getPipelineGroup() {
        if (cruiseConfig.hasPipelineGroup(groupName))
            return cruiseConfig.findGroup(groupName);
        else return null;
    }


    public Node getDependencyMaterialsFor(CaseInsensitiveString pipelineName) {
        if (getDependencies().containsKey(pipelineName)) {
            return getDependencies().get(pipelineName);
        }
        return new Node(new ArrayList<>());
    }

    @Override
    public String toString() {
        return "ValidationContext{" +
                "immediateParent=" + immediateParent +
                ", parentContext=" + parentContext +
                '}';
    }

    @Override
    public MaterialConfigs getAllMaterialsByFingerPrint(String fingerprint) {
        initMaterialConfigMap();
        return materialConfigsFingerprintMap.get(fingerprint);
    }

    @Override
    public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
        try {
            return cruiseConfig.pipelineConfigByName(pipelineName);
        } catch (RecordNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean shouldCheckConfigRepo() {
        return false;
    }

    @Override
    public SecurityConfig getServerSecurityConfig() {
        return cruiseConfig.server().security();
    }

    @Override
    public boolean doesTemplateExist(CaseInsensitiveString template) {
        return cruiseConfig.getTemplates().hasTemplateNamed(template);
    }

    @Override
    public SCM findScmById(String scmID) {
        return cruiseConfig.getSCMs().find(scmID);
    }

    @Override
    public PackageRepository findPackageById(String packageId) {
        return cruiseConfig.getPackageRepositories().findPackageRepositoryHaving(packageId);
    }

    public Set<CaseInsensitiveString> getPipelinesWithDependencyMaterials() {
        return getDependencies().keySet();
    }

    private Hashtable<CaseInsensitiveString, Node> getDependencies() {
        if (dependencies == null) {
            dependencies = new Hashtable<>();
            for (PipelineConfig pipeline : cruiseConfig.getAllPipelineConfigs()) {
                dependencies.put(pipeline.name(), pipeline.getDependenciesAsNode());
            }
        }
        return dependencies;
    }

    private Hashtable<CaseInsensitiveString, Node> dependencies;

    public PipelineGroups getGroups() {
        return cruiseConfig.getGroups();
    }

    public boolean isPipelineBeingCreated() {
        return isPipelineBeingCreated;
    }

    private void initMaterialConfigMap() {
        if (materialConfigsFingerprintMap == null) {
            materialConfigsFingerprintMap = new MaterialConfigFingerprintMap(cruiseConfig);
        }
    }

    private class MaterialConfigFingerprintMap {
        private Map<String, MaterialConfigs> map = new ConcurrentHashMap<>();
        private Map<String, MaterialConfigs> pipelineMaterialMap = new ConcurrentHashMap<>();

        public MaterialConfigFingerprintMap(CruiseConfig cruiseConfig) {
            for (PipelineConfigs group : cruiseConfig.getGroups()) {
                for (PipelineConfig pipelineConfig : group) {
                    for (MaterialConfig material : pipelineConfig.materialConfigs()) {
                        String fingerprint = material.getFingerprint();
                        if (fingerprint != null) {
                            if (!map.containsKey(fingerprint)) {
                                map.put(fingerprint, new MaterialConfigs());
                            }
                            map.get(fingerprint).add(material);
                        }

                        if (!pipelineMaterialMap.containsKey(pipelineConfig.name().toString())) {
                            pipelineMaterialMap.put(pipelineConfig.name().toString(), new MaterialConfigs());
                        }
                        pipelineMaterialMap.get(pipelineConfig.name().toString()).add(material);
                    }
                }
            }
        }

        public MaterialConfigs get(String fingerprint) {
            return map.get(fingerprint);
        }
    }

    @Override
    public boolean isValidProfileId(String profileId) {
        return this.cruiseConfig.getElasticConfig().getProfiles().find(profileId) != null;
    }

    @Override
    public ClusterProfiles getClusterProfiles() {
        return this.cruiseConfig.getElasticConfig().getClusterProfiles();
    }

    @Override
    public boolean shouldNotCheckRole() {
        return isWithinTemplates();
    }

    @Override
    public ArtifactStores artifactStores() {
        return this.cruiseConfig.getArtifactStores();
    }

    @Override
    public CruiseConfig getCruiseConfig() {
        return this.cruiseConfig;
    }

    @Override
    public RulesValidationContext getRulesValidationContext() {
        return null;
    }
}
