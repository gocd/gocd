/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;

import java.util.List;

public abstract class DelegatingValidationContext implements ValidationContext {
    protected ValidationContext validationContext;

    protected DelegatingValidationContext(ValidationContext validationContext) {
        this.validationContext = validationContext;
    }

    public ConfigReposConfig getConfigRepos() {
        return validationContext.getConfigRepos();
    }

    public boolean isWithinPipelines() {
        return validationContext.isWithinPipelines();
    }

    public PipelineConfig getPipeline() {
        return validationContext.getPipeline();
    }

    public MaterialConfigs getAllMaterialsByFingerPrint(String fingerprint) {
        return validationContext.getAllMaterialsByFingerPrint(fingerprint);
    }

    public StageConfig getStage() {
        return validationContext.getStage();
    }

    public boolean isWithinTemplates() {
        return validationContext.isWithinTemplates();
    }

    public String getParentDisplayName() {
        return validationContext.getParentDisplayName();
    }

    public Validatable getParent() {
        return validationContext.getParent();
    }

    public JobConfig getJob() {
        return validationContext.getJob();
    }

    public PipelineConfigs getPipelineGroup() {
        return validationContext.getPipelineGroup();
    }

    public PipelineTemplateConfig getTemplate() {
        return validationContext.getTemplate();
    }

    public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
        return validationContext.getPipelineConfigByName(pipelineName);
    }

    public boolean shouldCheckConfigRepo() {
        return validationContext.shouldCheckConfigRepo();
    }

    public SecurityConfig getServerSecurityConfig() {
        return validationContext.getServerSecurityConfig();
    }

    public boolean doesTemplateExist(CaseInsensitiveString template) {
        return validationContext.doesTemplateExist(template);
    }

    public SCM findScmById(String scmID) {
        return validationContext.findScmById(scmID);
    }

    public PackageRepository findPackageById(String packageId) {
        return validationContext.findPackageById(packageId);
    }

    public ValidationContext withParent(Validatable validatable) {
        return validationContext.withParent(validatable);
    }

    public boolean isValidProfileId(String profileId) {
        return validationContext.isValidProfileId(profileId);
    }

    @Override
    public ClusterProfiles getClusterProfiles() {
        return validationContext.getClusterProfiles();
    }

    @Override
    public boolean shouldNotCheckRole() {
        return validationContext.isWithinTemplates();
    }

    @Override
    public ArtifactStores artifactStores() {
        return validationContext.artifactStores();
    }

    @Override
    public CruiseConfig getCruiseConfig() {
        return validationContext.getCruiseConfig();
    }

    @Override
    public RulesValidationContext getRulesValidationContext() {
        return null;
    }
}
