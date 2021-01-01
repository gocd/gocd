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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.rules.RulesValidationContext;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;

public abstract class DelegatingValidationContext implements ValidationContext {
    protected ValidationContext validationContext;

    protected DelegatingValidationContext(ValidationContext validationContext) {
        this.validationContext = validationContext;
    }

    @Override
    public ConfigReposConfig getConfigRepos() {
        return validationContext.getConfigRepos();
    }

    @Override
    public boolean isWithinPipelines() {
        return validationContext.isWithinPipelines();
    }

    @Override
    public PipelineConfig getPipeline() {
        return validationContext.getPipeline();
    }

    @Override
    public MaterialConfigs getAllMaterialsByFingerPrint(String fingerprint) {
        return validationContext.getAllMaterialsByFingerPrint(fingerprint);
    }

    @Override
    public StageConfig getStage() {
        return validationContext.getStage();
    }

    @Override
    public boolean isWithinTemplates() {
        return validationContext.isWithinTemplates();
    }

    @Override
    public String getParentDisplayName() {
        return validationContext.getParentDisplayName();
    }

    @Override
    public Validatable getParent() {
        return validationContext.getParent();
    }

    @Override
    public JobConfig getJob() {
        return validationContext.getJob();
    }

    @Override
    public PipelineConfigs getPipelineGroup() {
        return validationContext.getPipelineGroup();
    }

    @Override
    public PipelineTemplateConfig getTemplate() {
        return validationContext.getTemplate();
    }

    @Override
    public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
        return validationContext.getPipelineConfigByName(pipelineName);
    }

    @Override
    public boolean shouldCheckConfigRepo() {
        return validationContext.shouldCheckConfigRepo();
    }

    @Override
    public SecurityConfig getServerSecurityConfig() {
        return validationContext.getServerSecurityConfig();
    }

    @Override
    public boolean doesTemplateExist(CaseInsensitiveString template) {
        return validationContext.doesTemplateExist(template);
    }

    @Override
    public SCM findScmById(String scmID) {
        return validationContext.findScmById(scmID);
    }

    @Override
    public PackageRepository findPackageById(String packageId) {
        return validationContext.findPackageById(packageId);
    }

    @Override
    public ValidationContext withParent(Validatable validatable) {
        return validationContext.withParent(validatable);
    }

    @Override
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

    @Override
    public boolean isWithinEnvironment() {
        return validationContext.isWithinEnvironment();
    }

    @Override
    public EnvironmentConfig getEnvironment() {
        return validationContext.getEnvironment();
    }
}
