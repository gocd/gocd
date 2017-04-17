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

package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants.*;

@Component
public class AuthorizationExtension extends AbstractExtension {

    private final HashMap<String, AuthorizationMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public AuthorizationExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, EXTENSION_NAME), EXTENSION_NAME);
        addHandler(AuthorizationMessageConverterV1.VERSION, new PluginSettingsJsonMessageHandler1_0(), new AuthorizationMessageConverterV1());
    }

    private void addHandler(String version, PluginSettingsJsonMessageHandler messageHandler, AuthorizationMessageConverterV1 extensionHandler) {
        pluginSettingsMessageHandlerMap.put(version, messageHandler);
        messageHandlerMap.put(AuthorizationMessageConverterV1.VERSION, extensionHandler);
    }

    public com.thoughtworks.go.plugin.domain.authorization.Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_CAPABILITIES, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.authorization.Capabilities>() {
            @Override
            public com.thoughtworks.go.plugin.domain.authorization.Capabilities onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    List<PluginConfiguration> getAuthConfigMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_AUTH_CONFIG_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPluginConfigMetadataResponseFromBody(responseBody);
            }
        });
    }

    String getAuthConfigView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_AUTH_CONFIG_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPluginConfigurationViewFromResponseBody(responseBody);
            }
        });
    }

    public ValidationResult validateAuthConfig(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_AUTH_CONFIG, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).validatePluginConfigurationRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPluginConfigurationValidationResultFromResponseBody(responseBody);
            }
        });
    }

    public VerifyConnectionResponse verifyConnection(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VERIFY_CONNECTION, new DefaultPluginInteractionCallback<VerifyConnectionResponse>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).verifyConnectionRequestBody(configuration);
            }

            @Override
            public VerifyConnectionResponse onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getVerifyConnectionResultFromResponseBody(responseBody);
            }
        });
    }

    public AuthenticationResponse authenticateUser(String pluginId, final String username, final String password, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> pluginRoleConfigs) {
        if (authConfigs == null || authConfigs.isEmpty()) {
            throw new MissingAuthConfigsException(String.format("No AuthConfigs configured for plugin: %s, Plugin would need at-least one auth_config to authenticate user.", pluginId));
        }
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AUTHENTICATE_USER, new DefaultPluginInteractionCallback<AuthenticationResponse>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).authenticateUserRequestBody(username, password, authConfigs, pluginRoleConfigs);
            }

            @Override
            public AuthenticationResponse onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getAuthenticatedUserFromResponseBody(responseBody);
            }
        });
    }

    public List<PluginConfiguration> getRoleConfigurationMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_ROLE_CONFIG_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getRoleConfigMetadataResponseFromBody(responseBody);
            }
        });
    }

    public String getRoleConfigurationView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_ROLE_CONFIG_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getRoleConfigurationViewFromResponseBody(responseBody);
            }
        });
    }

    public ValidationResult validateRoleConfiguration(final String pluginId, final Map<String, String> roleConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_ROLE_CONFIG, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).validatePluginConfigurationRequestBody(roleConfiguration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPluginConfigurationValidationResultFromResponseBody(responseBody);
            }
        });
    }

    public List<User> searchUsers(String pluginId, final String searchTerm, List<SecurityAuthConfig> authConfigs) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SEARCH_USERS, new DefaultPluginInteractionCallback<List<User>>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).searchUsersRequestBody(searchTerm, authConfigs);
            }

            @Override
            public List<User> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getSearchUsersFromResponseBody(responseBody);
            }
        });
    }

    com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getImageResponseFromBody(responseBody);
            }
        });
    }

    public AuthorizationMessageConverter getMessageConverter(String version) {
        return messageHandlerMap.get(version);
    }
}
