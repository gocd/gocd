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

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.plugin.access.authorization.models.Capabilities;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
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

    public Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_CAPABILITIES, new DefaultPluginInteractionCallback<Capabilities>() {
            @Override
            public Capabilities onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    PluginProfileMetadataKeys getPluginConfigurationMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_CONFIG_METADATA, new DefaultPluginInteractionCallback<PluginProfileMetadataKeys>() {
            @Override
            public PluginProfileMetadataKeys onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPluginConfigMetadataResponseFromBody(responseBody);
            }
        });
    }

    String getPluginConfigurationView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_CONFIG_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPluginConfigurationViewFromResponseBody(responseBody);
            }
        });
    }

    public ValidationResult validatePluginConfiguration(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_PLUGIN_CONFIG, new DefaultPluginInteractionCallback<ValidationResult>() {
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

    public ValidationResult verifyConnection(final String pluginId, final Map<String, String> configuration) {
        ValidationResult validationResult = validatePluginConfiguration(pluginId, configuration);
        if (!validationResult.isSuccessful()) {
            return validationResult;
        }

        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VERIFY_CONNECTION, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).verifyConnectionRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getVerifyConnectionResultFromResponseBody(responseBody);
            }
        });
    }

    public AuthenticationResponse authenticateUser(String pluginId, final String username, final String password) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AUTHENTICATE_USER, new DefaultPluginInteractionCallback<AuthenticationResponse>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).authenticateUserRequestBody(username, password);
            }

            @Override
            public AuthenticationResponse onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getAuthenticatedUserFromResponseBody(responseBody);
            }
        });
    }

    public PluginProfileMetadataKeys getRoleConfigurationMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_ROLE_CONFIG_METADATA, new DefaultPluginInteractionCallback<PluginProfileMetadataKeys>() {
            @Override
            public PluginProfileMetadataKeys onSuccess(String responseBody, String resolvedExtensionVersion) {
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

    public List<User> searchUsers(String pluginId, final String searchTerm) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SEARCH_USERS, new DefaultPluginInteractionCallback<List<User>>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).searchUsersRequestBody(searchTerm);
            }

            @Override
            public List<User> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getSearchUsersFromResponseBody(responseBody);
            }
        });
    }

    Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_ICON, new DefaultPluginInteractionCallback<Image>() {
            @Override
            public Image onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getImageResponseFromBody(responseBody);
            }
        });
    }


    public AuthorizationMessageConverter getMessageConverter(String version) {
        return messageHandlerMap.get(version);
    }
}
