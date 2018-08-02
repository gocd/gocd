/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

abstract class AdminConfigCommand implements EntityConfigUpdateCommand<AdminsConfig> {
    protected final GoConfigService goConfigService;
    protected final AdminsConfig admin;
    protected final Username currentUser;
    protected final LocalizedOperationResult result;
    protected AdminsConfig preprocessedAdmin;

    public AdminConfigCommand(GoConfigService goConfigService, AdminsConfig adminsConfig, Username currentUser, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.admin = adminsConfig;
        this.currentUser = currentUser;
        this.result = result;
        this.preprocessedAdmin = adminsConfig;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(admin);
    }

    @Override
    public AdminsConfig getPreprocessedEntityConfig() {
        return preprocessedAdmin;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized();
    }


    protected ValidationContext validationContextWithSecurityConfig(final CruiseConfig preprocessedConfig) {
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
        };
    }

    protected final boolean isAuthorized() {
        if (goConfigService.isUserAdmin(currentUser)) {
            return true;
        }
        result.forbidden(forbiddenToEdit(), forbidden());
        return false;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {

        return true;
    }
}
