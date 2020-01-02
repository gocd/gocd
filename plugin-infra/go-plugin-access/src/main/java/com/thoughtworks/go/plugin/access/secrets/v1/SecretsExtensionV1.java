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
package com.thoughtworks.go.plugin.access.secrets.v1;

import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.plugin.access.secrets.SecretsPluginConstants;
import com.thoughtworks.go.plugin.access.secrets.VersionedSecretsExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.secrets.Secret;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.plugin.access.secrets.SecretsPluginConstants.*;
import static java.lang.String.format;

public class SecretsExtensionV1 implements VersionedSecretsExtension {
    public static final String VERSION = "1.0";
    private final PluginRequestHelper pluginRequestHelper;
    private final SecretsMessageConverterV1 secretsMessageConverterV1;

    public SecretsExtensionV1(PluginRequestHelper pluginRequestHelper) {
        this.pluginRequestHelper = pluginRequestHelper;
        this.secretsMessageConverterV1 = new SecretsMessageConverterV1();
    }

    @Override
    public Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, SecretsPluginConstants.REQUEST_GET_PLUGIN_ICON,
                new DefaultPluginInteractionCallback<Image>() {
                    @Override
                    public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, Map<String,
                            String> responseHeaders, String resolvedExtensionVersion) {
                        return secretsMessageConverterV1.getImageFromResponseBody(responseBody);
                    }
                });
    }

    @Override
    public List<PluginConfiguration> getSecretsConfigMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_SECRETS_CONFIG_METADATA,
                new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
                    @Override
                    public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders,
                                                               String resolvedExtensionVersion) {
                        return secretsMessageConverterV1.getSecretsConfigMetadataFromResponse(responseBody);
                    }
                });
    }

    @Override
    public String getSecretsConfigView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_SECRETS_CONFIG_VIEW,
                new DefaultPluginInteractionCallback<String>() {
                    @Override
                    public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                        return secretsMessageConverterV1.getSecretsConfigViewFromResponse(responseBody);
                    }
                });
    }

    @Override
    public ValidationResult validateSecretsConfig(String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_SECRETS_CONFIG,
                new DefaultPluginInteractionCallback<ValidationResult>() {
                    @Override
                    public String requestBody(String resolvedExtensionVersion) {
                        return secretsMessageConverterV1.validatePluginConfigurationRequestBody(configuration);
                    }

                    @Override
                    public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders,
                                                      String resolvedExtensionVersion) {
                        return secretsMessageConverterV1.getSecretsConfigValidationResultFromResponse(responseBody);
                    }
                });
    }

    @Override
    public List<Secret> lookupSecrets(String pluginId, SecretConfig secretConfig, Set<String> keys) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_LOOKUP_SECRETS,
                new DefaultPluginInteractionCallback<List<Secret>>() {
                    @Override
                    public String requestBody(String resolvedExtensionVersion) {
                        return secretsMessageConverterV1.lookupSecretsRequestBody(keys, secretConfig.getConfiguration().getConfigurationAsMap(true));
                    }

                    @Override
                    public List<Secret> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                        return secretsMessageConverterV1.getSecretsFromResponse(responseBody);
                    }

                    @Override
                    public void onFailure(int responseCode, String responseBody, String resolvedExtensionVersion) {
                        String errorMessage = secretsMessageConverterV1.getErrorMessageFromResponse(responseBody);
                        throw new SecretResolutionFailureException(
                                format("Error looking up secrets, plugin returned error code '%s' with response: '%s'", responseCode, errorMessage));
                    }
                });
    }
}