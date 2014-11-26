package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

public class ApiBasedPackageRepositoryExtension implements PackageAsRepositoryExtensionContract {
    PluginManager pluginManager;


    public ApiBasedPackageRepositoryExtension(PluginManager defaultPluginManager) {
        this.pluginManager = defaultPluginManager;
    }

    public RepositoryConfiguration getRepositoryConfiguration(String pluginId) {
        ActionWithReturn<PackageMaterialProvider, RepositoryConfiguration> action = new ActionWithReturn<PackageMaterialProvider, RepositoryConfiguration>() {
            @Override
            public RepositoryConfiguration execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                return packageRepositoryMaterial.getConfig().getRepositoryConfiguration();

            }
        };
        return pluginManager.doOn(PackageMaterialProvider.class, pluginId, action);
    }

    public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration getPackageConfiguration(String pluginId) {
        ActionWithReturn<PackageMaterialProvider, com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> action = new ActionWithReturn<PackageMaterialProvider, com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration>() {
            @Override
            public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                return packageRepositoryMaterial.getConfig().getPackageConfiguration();

            }
        };
        return pluginManager.doOn(PackageMaterialProvider.class, pluginId, action);
    }

    public ValidationResult isRepositoryConfigurationValid(String pluginId, final RepositoryConfiguration repositoryConfiguration) {
        return pluginManager.doOn(PackageMaterialProvider.class, pluginId, new ActionWithReturn<PackageMaterialProvider, ValidationResult>() {
            @Override
            public ValidationResult execute(PackageMaterialProvider packageMaterialProvider, GoPluginDescriptor pluginDescriptor) {
                return packageMaterialProvider.getConfig().isRepositoryConfigurationValid(repositoryConfiguration);
            }
        });
    }

    public ValidationResult isPackageConfigurationValid(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return pluginManager.doOn(PackageMaterialProvider.class, pluginId, new ActionWithReturn<PackageMaterialProvider, ValidationResult>() {
            @Override
            public ValidationResult execute(PackageMaterialProvider packageMaterialProvider, GoPluginDescriptor pluginDescriptor) {
                return packageMaterialProvider.getConfig().isPackageConfigurationValid(packageConfiguration,
                        repositoryConfiguration);
            }
        });
    }

    public PackageRevision getLatestRevision(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        ActionWithReturn<PackageMaterialProvider, PackageRevision> action = new ActionWithReturn<PackageMaterialProvider, PackageRevision>() {
            @Override
            public PackageRevision execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                return packageRepositoryMaterial.getPoller().getLatestRevision(packageConfiguration, repositoryConfiguration);
            }
        };
        return pluginManager.doOn(PackageMaterialProvider.class, pluginId, action);
    }

    public PackageRevision latestModificationSince(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration, final PackageRevision previouslyKnownRevision) {
        ActionWithReturn<PackageMaterialProvider, PackageRevision> action = new ActionWithReturn<PackageMaterialProvider, PackageRevision>() {
            @Override
            public PackageRevision execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                return packageRepositoryMaterial.getPoller().latestModificationSince(packageConfiguration, repositoryConfiguration, previouslyKnownRevision);
            }
        };

        return pluginManager.doOn(PackageMaterialProvider.class, pluginId, action);
    }

    public Result checkConnectionToRepository(String pluginId, final RepositoryConfiguration repositoryConfiguration) {
        return pluginManager.doOn(PackageMaterialProvider.class, pluginId, new ActionWithReturn<PackageMaterialProvider, Result>() {
            @Override
            public Result execute(PackageMaterialProvider packageMaterialProvider, GoPluginDescriptor pluginDescriptor) {
                return packageMaterialProvider.getPoller().checkConnectionToRepository(repositoryConfiguration);
            }
        });
    }

    public Result checkConnectionToPackage(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return pluginManager.doOn(PackageMaterialProvider.class, pluginId, new ActionWithReturn<PackageMaterialProvider, Result>() {
            @Override
            public Result execute(PackageMaterialProvider packageMaterialProvider, GoPluginDescriptor pluginDescriptor) {
                return packageMaterialProvider.getPoller().checkConnectionToPackage(packageConfiguration, repositoryConfiguration);
            }
        });
    }
}
