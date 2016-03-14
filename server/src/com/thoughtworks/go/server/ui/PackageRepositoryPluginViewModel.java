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

package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.plugin.access.packagematerial.*;

import java.util.List;

public class PackageRepositoryPluginViewModel extends PluginViewModel {
    private PackageConfigurations packageConfigurations;
    private PackageConfigurations repositoryConfigurations;
    public static final String TYPE = JsonBasedPackageRepositoryExtension.EXTENSION_NAME;

    public PackageRepositoryPluginViewModel() {
    }

    public PackageRepositoryPluginViewModel(String pluginId, String version, String message) {
        super(pluginId, version, message);
    }

    public PackageRepositoryPluginViewModel(String pluginId, String version, String message, PackageConfigurations packageConfigurations, PackageConfigurations repositoryConfigurations) {
        super(pluginId, version, message);
        this.packageConfigurations = packageConfigurations;
        this.repositoryConfigurations = repositoryConfigurations;
    }


    @Override
    public String getType() {
        return TYPE;
    }

    public List<PackageConfiguration> getPackageConfigurations() {
        if (packageConfigurations == null) {
            this.packageConfigurations = PackageMetadataStore.getInstance().getMetadata(getPluginId());
        }
        return packageConfigurations.list();
    }

    public List<PackageConfiguration> getRepositoryConfigurations() {
        if (repositoryConfigurations == null) {
            this.repositoryConfigurations = RepositoryMetadataStore.getInstance().getMetadata(getPluginId());
        }
        return repositoryConfigurations.list();
    }
    public Boolean hasPlugin(String pluginId){
        return RepositoryMetadataStore.getInstance().hasPlugin(pluginId);
    }
}
