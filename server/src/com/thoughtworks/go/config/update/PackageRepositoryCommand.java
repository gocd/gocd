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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import static com.thoughtworks.go.config.ErrorCollector.getAllErrors;

public class PackageRepositoryCommand {
    private final GoConfigService goConfigService;
    private final PackageRepository repository;
    private final Username username;
    private final PluginManager pluginManager;
    private final HttpLocalizedOperationResult result;

    public PackageRepositoryCommand(GoConfigService goConfigService, PackageRepository repository, Username username, PluginManager pluginManager, HttpLocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.repository = repository;
        this.username = username;
        this.pluginManager = pluginManager;
        this.result = result;
    }

    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedConfig.getPackageRepositories().validate(new ConfigSaveValidationContext(preprocessedConfig));
        PackageRepository packageRepository = preprocessedConfig.getPackageRepositories().find(this.repository.getRepoId());
        packageRepository.validate(new ConfigSaveValidationContext(preprocessedConfig.getPackageRepositories()));
        isValidPlugin(packageRepository.getPluginConfiguration());
        BasicCruiseConfig.copyErrors(packageRepository, this.repository);
        return getAllErrors(this.repository).isEmpty() && result.isSuccessful();
    }

    private void isValidPlugin(PluginConfiguration pluginConfiguration) {
        String pluginId = pluginConfiguration.getId();
        GoPluginDescriptor pluginDescriptor = this.pluginManager.getPluginDescriptorFor(pluginId);
        if(pluginDescriptor == null){
            result.unprocessableEntity(LocalizedMessage.string("INVALID_PLUGIN_TYPE", pluginId));
        }
    }

    public void clearErrors() {
        BasicCruiseConfig.clearErrors(this.repository);
    }

    public PackageRepository getPreprocessedEntityConfig() {
        return this.repository;
    }
}
