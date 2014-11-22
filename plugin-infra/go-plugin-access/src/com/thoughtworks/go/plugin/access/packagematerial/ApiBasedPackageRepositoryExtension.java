package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.ArrayList;
import java.util.List;

public class ApiBasedPackageRepositoryExtension implements PackageAsRepositoryExtensionContract {
    PluginManager pluginManager;


    public ApiBasedPackageRepositoryExtension(PluginManager defaultPluginManager) {
        this.pluginManager = defaultPluginManager;
    }

    public RepositoryConfiguration getRepositoryConfiguration(String pluginId) {
        final List<RepositoryConfiguration> returnValue = new ArrayList<RepositoryConfiguration>();
        Action<PackageMaterialProvider> action = new Action<PackageMaterialProvider>() {
            @Override
            public void execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                returnValue.add(packageRepositoryMaterial.getConfig().getRepositoryConfiguration());

            }
        };
        pluginManager.doOnIfHasReference(PackageMaterialProvider.class, pluginId, action);
        return returnValue.isEmpty() ? null : returnValue.get(0);
    }

    public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration getPackageConfiguration(String pluginId) {
        final List<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> returnValue = new ArrayList<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration>();
        pluginManager.doOnIfHasReference(PackageMaterialProvider.class, pluginId, new Action<PackageMaterialProvider>() {
            @Override
            public void execute(PackageMaterialProvider packageRepositoryMaterial, GoPluginDescriptor pluginDescriptor) {
                returnValue.add(packageRepositoryMaterial.getConfig().getPackageConfiguration());

            }
        });
        return returnValue.isEmpty() ? null : returnValue.get(0);
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
