/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.authorization.v1.AuthorizationMessageConverterV1;
import com.thoughtworks.go.plugin.access.authorization.v2.AuthorizationMessageConverterV2;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants.*;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.AUTHORIZATION_EXTENSION;
import static java.lang.String.format;

@Component
public class AuthorizationExtension extends AbstractExtension {
    private final HashMap<String, AuthorizationMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public AuthorizationExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry) {
        super(pluginManager, extensionsRegistry, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, AUTHORIZATION_EXTENSION), AUTHORIZATION_EXTENSION);
        addHandler(AuthorizationMessageConverterV1.VERSION, new PluginSettingsJsonMessageHandler1_0(), new AuthorizationMessageConverterV1()
        );
        addHandler(AuthorizationMessageConverterV2.VERSION, new PluginSettingsJsonMessageHandler1_0(), new AuthorizationMessageConverterV2()
        );
    }

    private void addHandler(String version, PluginSettingsJsonMessageHandler messageHandler, AuthorizationMessageConverter extensionHandler) {
        registerHandler(version, messageHandler);
        messageHandlerMap.put(version, extensionHandler);
    }

    public com.thoughtworks.go.plugin.domain.authorization.Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_CAPABILITIES, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.authorization.Capabilities>() {
            @Override
            public com.thoughtworks.go.plugin.domain.authorization.Capabilities onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    List<PluginConfiguration> getAuthConfigMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_AUTH_CONFIG_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPluginConfigMetadataResponseFromBody(responseBody);
            }
        });
    }

    String getAuthConfigView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_AUTH_CONFIG_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
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
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
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
            public VerifyConnectionResponse onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getVerifyConnectionResultFromResponseBody(responseBody);
            }
        });
    }

    public AuthenticationResponse authenticateUser(String pluginId, final String username, final String password, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> pluginRoleConfigs) {
        errorOutIfEmpty(authConfigs, pluginId);
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AUTHENTICATE_USER, new DefaultPluginInteractionCallback<AuthenticationResponse>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).authenticateUserRequestBody(username, password, authConfigs, pluginRoleConfigs);
            }

            @Override
            public AuthenticationResponse onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getAuthenticatedUserFromResponseBody(responseBody);
            }
        });
    }

    public List<PluginConfiguration> getRoleConfigurationMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_ROLE_CONFIG_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getRoleConfigMetadataResponseFromBody(responseBody);
            }
        });
    }

    public String getRoleConfigurationView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_ROLE_CONFIG_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
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
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
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
            public List<User> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getSearchUsersFromResponseBody(responseBody);
            }
        });
    }

    com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getImageResponseFromBody(responseBody);
            }
        });
    }

    public Map<String, String> fetchAccessToken(String pluginId, Map<String, String> requestHeaders, final Map<String, String> requestParams, List<SecurityAuthConfig> authConfigs) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_ACCESS_TOKEN, new DefaultPluginInteractionCallback<Map<String, String>>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).grantAccessRequestBody(authConfigs);
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return requestParams;
            }

            @Override
            public Map<String, String> requestHeaders(String resolvedExtensionVersion) {
                return requestHeaders;
            }

            @Override
            public Map<String, String> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getCredentials(responseBody);
            }
        });
    }

    public List<String> getUserRoles(String pluginId, String username, SecurityAuthConfig authConfig, List<PluginRoleConfig> roleConfigs) {
        if (authConfig == null) {
            throw new MissingAuthConfigsException(format("Request '%s' requires an AuthConfig. Make sure Authconfig is configured for the plugin '%s'.", REQUEST_GET_USER_ROLES, pluginId));
        }

        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_USER_ROLES, new DefaultPluginInteractionCallback<List<String>>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getUserRolesRequestBody(username, authConfig, roleConfigs);
            }

            @Override
            public List<String> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getUserRolesFromResponseBody(responseBody);
            }
        });
    }

    public boolean supportsPluginAPICallsRequiredForAccessToken(SecurityAuthConfig authConfig) {
        String version = pluginManager.resolveExtensionVersion(authConfig.getPluginId(), AUTHORIZATION_EXTENSION, goSupportedVersions());
        return !AuthorizationMessageConverterV1.VERSION.equals(version);
    }

    public boolean isValidUser(String pluginId, String username, SecurityAuthConfig authConfig) {
        try {
            return pluginRequestHelper.submitRequest(pluginId, IS_VALID_USER, new DefaultPluginInteractionCallback<Boolean>() {
                @Override
                public String requestBody(String resolvedExtensionVersion) {
                    return getMessageConverter(resolvedExtensionVersion).isValidUserRequestBody(username, authConfig);
                }

                @Override
                public Boolean onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                    return true;
                }
            });
        } catch (Exception e) { //any error happened while verifying the user existence will lead to assume user does not exists.
            return false;
        }
    }

    public AuthorizationMessageConverter getMessageConverter(String version) {
        return messageHandlerMap.get(version);
    }

    public AuthenticationResponse authenticateUser(String pluginId, Map<String, String> credentials, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> roleConfigs) {
        errorOutIfEmpty(authConfigs, pluginId);

        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AUTHENTICATE_USER, new DefaultPluginInteractionCallback<AuthenticationResponse>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).authenticateUserRequestBody(credentials, authConfigs, roleConfigs);
            }

            @Override
            public AuthenticationResponse onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getAuthenticatedUserFromResponseBody(responseBody);
            }
        });
    }

    private void errorOutIfEmpty(List<SecurityAuthConfig> authConfigs, String pluginId) {
        if (authConfigs == null || authConfigs.isEmpty()) {
            throw new MissingAuthConfigsException(format("No AuthConfigs configured for plugin: %s, Plugin would need at-least one auth_config to authenticate user.", pluginId));
        }
    }

    public String getAuthorizationServerUrl(String pluginId, List<SecurityAuthConfig> authConfigs, String siteUrl) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AUTHORIZATION_SERVER_URL, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).authorizationServerUrlRequestBody(pluginId, authConfigs, siteUrl);
            }

            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getAuthorizationServerUrl(responseBody);
            }
        });
    }

    @Override
    public List<String> goSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }
}
