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
package com.thoughtworks.go.config.helper;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.rules.RulesValidationContext;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;

public class ValidationContextMother {

    public static ValidationContext validationContext(SecurityConfig securityConfig) {
        return new DefaultValidationContext(securityConfig);
    }

    public static ValidationContext validationContext(ArtifactStores artifactStores) {
        return new DefaultValidationContext(artifactStores);
    }

    static class DefaultValidationContext implements ValidationContext {
        private SecurityConfig securityConfig;
        private ArtifactStores artifactStores;

        public DefaultValidationContext(SecurityConfig securityConfig) {
            this.securityConfig = securityConfig;
        }

        public DefaultValidationContext(ArtifactStores artifactStores) {
            this.artifactStores = artifactStores;
        }
        @Override
        public ConfigReposConfig getConfigRepos() {
            return null;
        }

        @Override
        public boolean isWithinPipelines() {
            return false;
        }

        @Override
        public PipelineConfig getPipeline() {
            return null;
        }

        @Override
        public MaterialConfigs getAllMaterialsByFingerPrint(String fingerprint) {
            return null;
        }

        @Override
        public StageConfig getStage() {
            return null;
        }

        @Override
        public boolean isWithinTemplates() {
            return false;
        }

        @Override
        public String getParentDisplayName() {
            return null;
        }

        @Override
        public Validatable getParent() {
            return null;
        }

        @Override
        public JobConfig getJob() {
            return null;
        }

        @Override
        public boolean isWithinEnvironment() {
            return false;
        }

        @Override
        public PipelineConfigs getPipelineGroup() {
            return null;
        }

        @Override
        public PipelineTemplateConfig getTemplate() {
            return null;
        }

        @Override
        public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
            return null;
        }

        @Override
        public boolean shouldCheckConfigRepo() {
            return false;
        }

        @Override
        public SecurityConfig getServerSecurityConfig() {
            return securityConfig;
        }

        @Override
        public boolean doesTemplateExist(CaseInsensitiveString template) {
            return false;
        }

        @Override
        public SCM findScmById(String scmID) {
            return null;
        }

        @Override
        public PackageRepository findPackageById(String packageId) {
            return null;
        }

        @Override
        public ValidationContext withParent(Validatable validatable) {
            return null;
        }

        @Override
        public EnvironmentConfig getEnvironment() {
            return null;
        }

        @Override
        public boolean isValidProfileId(String profileId) {
            return false;
        }

        @Override
        public ClusterProfiles getClusterProfiles() {
            return null;
        }

        @Override
        public boolean shouldNotCheckRole() {
            return false;
        }

        @Override
        public ArtifactStores artifactStores() {
            return artifactStores;
        }

        @Override
        public CruiseConfig getCruiseConfig() {
            return null;
        }

        @Override
        public RulesValidationContext getRulesValidationContext() {
            return null;
        }
    }
}
