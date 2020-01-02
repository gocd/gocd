/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.PACKAGE_MATERIAL_EXTENSION;
import static java.util.Arrays.asList;

@Component
public class PackageRepositoryExtension extends AbstractExtension {

    private static final List<String> goSupportedVersions = asList("1.0");

    public static final String REQUEST_REPOSITORY_CONFIGURATION = "repository-configuration";
    public static final String REQUEST_PACKAGE_CONFIGURATION = "package-configuration";
    public static final String REQUEST_VALIDATE_REPOSITORY_CONFIGURATION = "validate-repository-configuration";
    public static final String REQUEST_VALIDATE_PACKAGE_CONFIGURATION = "validate-package-configuration";
    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISION_SINCE = "latest-revision-since";
    public static final String REQUEST_CHECK_REPOSITORY_CONNECTION = "check-repository-connection";
    public static final String REQUEST_CHECK_PACKAGE_CONNECTION = "check-package-connection";
    final Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public PackageRepositoryExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry) {
        super(pluginManager, extensionsRegistry, new PluginRequestHelper(pluginManager, goSupportedVersions, PACKAGE_MATERIAL_EXTENSION), PACKAGE_MATERIAL_EXTENSION);
        registerHandler("1.0", new PluginSettingsJsonMessageHandler1_0());
        messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }


    public RepositoryConfiguration getRepositoryConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_REPOSITORY_CONFIGURATION, new DefaultPluginInteractionCallback<RepositoryConfiguration>() {

            @Override
            public RepositoryConfiguration onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).responseMessageForRepositoryConfiguration(responseBody);
            }
        });
    }

    public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration getPackageConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PACKAGE_CONFIGURATION, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration>() {

            @Override
            public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).responseMessageForPackageConfiguration(responseBody);
            }
        });
    }

    public ValidationResult isRepositoryConfigurationValid(String pluginId, final RepositoryConfiguration repositoryConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_REPOSITORY_CONFIGURATION, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).requestMessageForIsRepositoryConfigurationValid(repositoryConfiguration);

            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).responseMessageForIsRepositoryConfigurationValid(responseBody);
            }
        });
    }

    public ValidationResult isPackageConfigurationValid(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_PACKAGE_CONFIGURATION, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).requestMessageForIsPackageConfigurationValid(packageConfiguration, repositoryConfiguration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).responseMessageForIsPackageConfigurationValid(responseBody);
            }
        });
    }

    public PackageRevision getLatestRevision(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_LATEST_REVISION, new DefaultPluginInteractionCallback<PackageRevision>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).requestMessageForLatestRevision(packageConfiguration, repositoryConfiguration);
            }

            @Override
            public PackageRevision onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).responseMessageForLatestRevision(responseBody);
            }
        });
    }

    public PackageRevision latestModificationSince(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration, final PackageRevision previouslyKnownRevision) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_LATEST_REVISION_SINCE, new DefaultPluginInteractionCallback<PackageRevision>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).requestMessageForLatestRevisionSince(packageConfiguration, repositoryConfiguration, previouslyKnownRevision);
            }

            @Override
            public PackageRevision onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).responseMessageForLatestRevisionSince(responseBody);
            }
        });
    }

    public Result checkConnectionToRepository(String pluginId, final RepositoryConfiguration repositoryConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_CHECK_REPOSITORY_CONNECTION, new DefaultPluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).requestMessageForCheckConnectionToRepository(repositoryConfiguration);
            }

            @Override
            public Result onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).responseMessageForCheckConnectionToRepository(responseBody);
            }
        });
    }

    public Result checkConnectionToPackage(String pluginId, final com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, final RepositoryConfiguration repositoryConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_CHECK_PACKAGE_CONNECTION, new DefaultPluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).requestMessageForCheckConnectionToPackage(packageConfiguration, repositoryConfiguration);
            }

            @Override
            public Result onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageConverter(resolvedExtensionVersion).responseMessageForCheckConnectionToPackage(responseBody);
            }
        });
    }

    JsonMessageHandler messageConverter(String resolvedExtensionVersion) {
        return messageHandlerMap.get(resolvedExtensionVersion);
    }

    @Override
    public List<String> goSupportedVersions() {
        return goSupportedVersions;
    }
}
