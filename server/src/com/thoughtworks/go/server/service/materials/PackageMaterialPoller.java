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

package com.thoughtworks.go.server.service.materials;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialRevision;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.util.json.JsonHelper;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

public class PackageMaterialPoller implements MaterialPoller<PackageMaterial> {

    private PluginManager pluginManager;

    public PackageMaterialPoller(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public List<Modification> latestModification(final PackageMaterial material, File baseDir, SubprocessExecutionContext execCtx) {
        ActionWithReturn<PackageMaterialProvider, PackageRevision> action = new ActionWithReturn<PackageMaterialProvider, PackageRevision>() {
            @Override
            public PackageRevision execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                return packageRepositoryMaterial.getPoller().getLatestRevision(buildPackageConfigurations(material.getPackageDefinition()),
                        buildRepositoryConfigurations(material.getPackageDefinition().getRepository()));
            }
        };
        return getModifications(material, action);
    }

    @Override
    public List<Modification> modificationsSince(final PackageMaterial material, File baseDir, final Revision revision, SubprocessExecutionContext execCtx) {
        ActionWithReturn<PackageMaterialProvider, PackageRevision> action = new ActionWithReturn<PackageMaterialProvider, PackageRevision>() {
            @Override
            public PackageRevision execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                PackageMaterialRevision packageMaterialRevision = (PackageMaterialRevision) revision;
                PackageConfiguration packageConfiguration = buildPackageConfigurations(material.getPackageDefinition());
                RepositoryConfiguration repositoryConfiguration = buildRepositoryConfigurations(material.getPackageDefinition().getRepository());
                PackageRevision previouslyKnownRevision = new PackageRevision(packageMaterialRevision.getRevision(), packageMaterialRevision.getTimestamp(), null, packageMaterialRevision.getData());

                return packageRepositoryMaterial.getPoller().latestModificationSince(packageConfiguration, repositoryConfiguration, previouslyKnownRevision);
            }
        };
        return getModifications(material, action);
    }

    private List<Modification> getModifications(PackageMaterial material, ActionWithReturn<PackageMaterialProvider, PackageRevision> action) {
        PackageRevision packageRevision = pluginManager.doOn(PackageMaterialProvider.class, material.getPluginId(), action);
        if (packageRevision == null) {
            return new Modifications();
        }
        return new Modifications(
                new Modification(packageRevision.getUser(), JsonHelper.toJsonString(getCommentParameters(packageRevision)), null,
                        packageRevision.getTimestamp(), packageRevision.getRevision(), JsonHelper.toJsonString(packageRevision.getData())));
    }

    private Map<String, String> getCommentParameters(PackageRevision packageRevision) {
        HashMap<String, String> commentParametersMap = new HashMap<String, String>();
        commentParametersMap.put("TYPE", "PACKAGE_MATERIAL");
        commentParametersMap.put("TRACKBACK_URL", packageRevision.getTrackbackUrl());
        commentParametersMap.put("COMMENT", packageRevision.getRevisionComment());
        return commentParametersMap;
    }

    private RepositoryConfiguration buildRepositoryConfigurations(PackageRepository packageRepository) {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        populateConfiguration(packageRepository.getConfiguration(), repositoryConfiguration);
        return repositoryConfiguration;
    }

    private com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration buildPackageConfigurations(PackageDefinition packageDefinition) {
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        populateConfiguration(packageDefinition.getConfiguration(), packageConfiguration);
        return packageConfiguration;
    }

    private void populateConfiguration(Configuration configuration, com.thoughtworks.go.plugin.api.config.Configuration pluginConfiguration) {
        for (ConfigurationProperty configurationProperty : configuration) {
            pluginConfiguration.add(new PackageMaterialProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
        }
    }
}
