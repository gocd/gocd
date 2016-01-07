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

package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.log4j.Logger.getLogger;

@Component
public class PackageAsRepositoryExtension implements PackageAsRepositoryExtensionContract, GoPluginExtension {

    PluginManager pluginManager;

    private static final Logger LOGGER = getLogger(PackageAsRepositoryExtension.class);
    private final ApiBasedPackageRepositoryExtension apiBasedPackageRepositoryExtension;
    private final JsonBasedPackageRepositoryExtension jsonBasedPackageRepositoryExtension;

    @Autowired
    public PackageAsRepositoryExtension(PluginManager defaultPluginManager) {
        this.pluginManager = defaultPluginManager;
        apiBasedPackageRepositoryExtension = new ApiBasedPackageRepositoryExtension(pluginManager);
        jsonBasedPackageRepositoryExtension = new JsonBasedPackageRepositoryExtension(pluginManager);
    }

    @Override
    public PluginSettingsConfiguration getPluginSettingsConfiguration(String pluginId) {
        return resolveBy(pluginId).getPluginSettingsConfiguration(pluginId);
    }

    @Override
    public String getPluginSettingsView(String pluginId) {
        return resolveBy(pluginId).getPluginSettingsView(pluginId);
    }

    @Override
    public ValidationResult validatePluginSettings(String pluginId, PluginSettingsConfiguration configuration) {
        return resolveBy(pluginId).validatePluginSettings(pluginId, configuration);
    }

    public RepositoryConfiguration getRepositoryConfiguration(String pluginId) {
        return resolveBy(pluginId).getRepositoryConfiguration(pluginId);
    }

    public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration getPackageConfiguration(String pluginId) {
        return resolveBy(pluginId).getPackageConfiguration(pluginId);
    }

    public ValidationResult isRepositoryConfigurationValid(String pluginId, final RepositoryConfiguration repositoryConfiguration) {
        return resolveBy(pluginId).isRepositoryConfigurationValid(pluginId, repositoryConfiguration);
    }

    public ValidationResult isPackageConfigurationValid(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return resolveBy(pluginId).isPackageConfigurationValid(pluginId, packageConfiguration, repositoryConfiguration);
    }

    public PackageRevision getLatestRevision(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return resolveBy(pluginId).getLatestRevision(pluginId, packageConfiguration, repositoryConfiguration);
    }

    public PackageRevision latestModificationSince(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration, final PackageRevision previouslyKnownRevision) {
        return resolveBy(pluginId).latestModificationSince(pluginId, packageConfiguration, repositoryConfiguration, previouslyKnownRevision);
    }

    public Result checkConnectionToRepository(String pluginId, final RepositoryConfiguration repositoryConfiguration) {
        return resolveBy(pluginId).checkConnectionToRepository(pluginId, repositoryConfiguration);
    }

    public Result checkConnectionToPackage(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return resolveBy(pluginId).checkConnectionToPackage(pluginId, packageConfiguration, repositoryConfiguration);
    }

    PackageAsRepositoryExtensionContract resolveBy(String pluginId) {
        if (pluginManager.hasReferenceFor(PackageMaterialProvider.class, pluginId)) {
            return apiBasedPackageRepositoryExtension;
        }
        return jsonBasedPackageRepositoryExtension;
    }

    @Override
    public boolean canHandlePlugin(String pluginId) {
        return pluginManager.hasReferenceFor(PackageMaterialProvider.class, pluginId) || pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, pluginId);
    }
}
