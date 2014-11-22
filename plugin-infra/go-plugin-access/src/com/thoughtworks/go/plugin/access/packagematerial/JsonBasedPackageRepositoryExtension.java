package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;

public class JsonBasedPackageRepositoryExtension implements PackageAsRepositoryExtensionContract {

    public static final String EXTENSION_NAME = "package-repository";
    public static final String REQUEST_REPOSITORY_CONFIGURATION = "repository-configuration";
    public static final String REQUEST_PACKAGE_CONFIGURATION = "package-configuration";
    public static final String REQUEST_VALIDATE_REPOSITORY_CONFIGURATION = "validate-repository-configuration";
    public static final String REQUEST_VALIDATE_PACKAGE_CONFIGURATION = "validate-package-configuration";
    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISION_SINCE = "latest-revision-since";
    public static final String REQUEST_CHECK_REPOSITORY_CONNECTION = "check-repository-connection";
    public static final String REQUEST_CHECK_PACKAGE_CONNECTION = "check-package-connection";
    private static final List<String> goSupportedVersions = asList("1.0");
    private PluginManager pluginManager;
    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<String, JsonMessageHandler>();


    public JsonBasedPackageRepositoryExtension(PluginManager defaultPluginManager) {
        this.pluginManager = defaultPluginManager;
        messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    public RepositoryConfiguration getRepositoryConfiguration(String pluginId) {
        return submitRequest(pluginId, REQUEST_REPOSITORY_CONFIGURATION, new PluginInteractionCallback<RepositoryConfiguration>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public RepositoryConfiguration onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForRepositoryConfiguration(responseBody);
            }
        });
    }


    public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration getPackageConfiguration(String pluginId) {
        return submitRequest(pluginId, REQUEST_PACKAGE_CONFIGURATION, new PluginInteractionCallback<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForPackageConfiguration(responseBody);
            }
        });
    }

    public ValidationResult isRepositoryConfigurationValid(String pluginId, final RepositoryConfiguration repositoryConfiguration) {
        return submitRequest(pluginId, REQUEST_VALIDATE_REPOSITORY_CONFIGURATION, new PluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForIsRepositoryConfigurationValid(repositoryConfiguration);

            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForIsRepositoryConfigurationValid(responseBody);
            }
        });
    }


    public ValidationResult isPackageConfigurationValid(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return submitRequest(pluginId, REQUEST_VALIDATE_PACKAGE_CONFIGURATION, new PluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForIsPackageConfigurationValid(packageConfiguration, repositoryConfiguration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForIsPackageConfigurationValid(responseBody);
            }
        });
    }


    public PackageRevision getLatestRevision(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return submitRequest(pluginId, REQUEST_LATEST_REVISION, new PluginInteractionCallback<PackageRevision>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForLatestRevision(packageConfiguration, repositoryConfiguration);
            }

            @Override
            public PackageRevision onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForLatestRevision(responseBody);
            }
        });
    }

    public PackageRevision latestModificationSince(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration, final PackageRevision previouslyKnownRevision) {
        return submitRequest(pluginId, REQUEST_LATEST_REVISION_SINCE, new PluginInteractionCallback<PackageRevision>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForLatestRevisionSince(packageConfiguration, repositoryConfiguration, previouslyKnownRevision);
            }

            @Override
            public PackageRevision onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForLatestRevisionSince(responseBody);
            }
        });
    }

    public Result checkConnectionToRepository(String pluginId, final RepositoryConfiguration repositoryConfiguration) {
        return submitRequest(pluginId, REQUEST_CHECK_REPOSITORY_CONNECTION, new PluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForCheckConnectionToRepository(repositoryConfiguration);
            }

            @Override
            public Result onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForCheckConnectionToRepository(responseBody);
            }
        });
    }


    public Result checkConnectionToPackage(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return submitRequest(pluginId, REQUEST_CHECK_PACKAGE_CONNECTION, new PluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForCheckConnectionToPackage(packageConfiguration, repositoryConfiguration);
            }

            @Override
            public Result onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForCheckConnectionToPackage(responseBody);
            }
        });
    }

    private <T> T submitRequest(String pluginId, String requestName, PluginInteractionCallback<T> pluginInteractionCallback) {
        try {
            String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, goSupportedVersions);
            DefaultGoPluginApiRequest apiRequest = new DefaultGoPluginApiRequest(EXTENSION_NAME, resolvedExtensionVersion, requestName);
            apiRequest.setRequestBody(pluginInteractionCallback.requestBody(resolvedExtensionVersion));
            GoPluginApiResponse response = pluginManager.submitTo(pluginId, apiRequest);
            if (DefaultGoApiResponse.SUCCESS_RESPONSE_CODE == response.responseCode()) {
                return pluginInteractionCallback.onSuccess(response.responseBody(), resolvedExtensionVersion);
            }
            throw new RuntimeException(format("Unsuccessful response code from plugin %s with body %s", response.responseCode(), response.responseBody()));
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while interacting with plugin id %s, extension %s, request %s", pluginId, EXTENSION_NAME, requestName), e);
        }
    }

    private interface PluginInteractionCallback<T> {
        String requestBody(String resolvedExtensionVersion);

        T onSuccess(String responseBody, String resolvedExtensionVersion);
    }

}
