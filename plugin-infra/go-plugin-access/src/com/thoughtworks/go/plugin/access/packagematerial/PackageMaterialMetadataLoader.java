/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.GoPluginFrameworkException;
import com.thoughtworks.go.plugin.infra.GoPluginOSGiFramework;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.log4j.Logger.getLogger;

@Component
public class PackageMaterialMetadataLoader implements PluginChangeListener {

    private RepositoryMetadataStore repositoryMetadataStore = RepositoryMetadataStore.getInstance();
    private PackageMetadataStore packageMetadataStore = PackageMetadataStore.getInstance();

    private PluginManager pluginManager;
    private static final Logger LOGGER = getLogger(PackageMaterialMetadataLoader.class);

    @Autowired
    public PackageMaterialMetadataLoader(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        pluginManager.addPluginChangeListener(this, PackageMaterialProvider.class);
    }

    void fetchRepositoryAndPackageMetaData(GoPluginDescriptor pluginDescriptor) {
        try {
            pluginManager.doOnIfHasReference(PackageMaterialProvider.class, pluginDescriptor.id(), new Action<PackageMaterialProvider>() {
                @Override
                public void execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                    repositoryMetadataStore.addMetadataFor(pluginDescriptor.id(),
                            new PackageConfigurations(packageRepositoryMaterial.getConfig().getRepositoryConfiguration()));
                    packageMetadataStore.addMetadataFor(pluginDescriptor.id(),
                            new PackageConfigurations(packageRepositoryMaterial.getConfig().getPackageConfiguration()));
                }
            });
        } catch (GoPluginFrameworkException e) {
            LOGGER.error(String.format("Failed to fetch package metadata for plugin : %s", pluginDescriptor.id()), e);
        }
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        fetchRepositoryAndPackageMetaData(pluginDescriptor);
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        repositoryMetadataStore.removeMetadata(pluginDescriptor.id());
        packageMetadataStore.removeMetadata(pluginDescriptor.id());
    }
}
