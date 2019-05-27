/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.validation;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.rules.RulesValidationContext;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;

import java.util.List;

public class RolesConfigUpdateValidator implements ConfigUpdateValidator {

    private final List<CaseInsensitiveString> roles;

    public RolesConfigUpdateValidator(List<CaseInsensitiveString> roles) {
        this.roles = roles;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        boolean isValid = true;
        for (CaseInsensitiveString role : roles) {
            Role roleConfig = preprocessedConfig.server().security().getRoles().findByName(role);
            if (roleConfig == null) {
                isValid = false;
            } else {
                isValid = roleConfig.validateTree(validationContextWithSecurityConfig(preprocessedConfig)) && isValid;
            }
        }
        return isValid;
    }

    public static ValidationContext validationContextWithSecurityConfig(final CruiseConfig preprocessedConfig) {
        return new ValidationContext() {
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
                return preprocessedConfig.server().security();
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
                return null;
            }

            @Override
            public CruiseConfig getCruiseConfig() {
                return null;
            }

            @Override
            public RulesValidationContext getRulesValidationContext() {
                return null;
            }
        };
    }
}
