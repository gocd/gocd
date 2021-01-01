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
package com.thoughtworks.go.plugin.access.authorization.v1;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMessageConverter;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class AuthorizationMessageConverterV1 implements AuthorizationMessageConverter {
    public static final String VERSION = "1.0";
    private static final Gson GSON = new Gson();

    @Override
    public com.thoughtworks.go.plugin.domain.authorization.Capabilities getCapabilitiesFromResponseBody(String responseBody) {
        return CapabilitiesDTO.fromJSON(responseBody).toDomainModel();
    }

    @Override
    public com.thoughtworks.go.plugin.domain.common.Image getImageResponseFromBody(String responseBody) {
        return new ImageDeserializer().fromJSON(responseBody);
    }

    @Override
    public List<PluginConfiguration> getPluginConfigMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody).toPluginConfigurations();
    }

    @Override
    public String getPluginConfigurationViewFromResponseBody(String responseBody) {
        return getTemplateFromResponse(responseBody, "Plugin configuration `template` was blank!");
    }

    @Override
    public String validatePluginConfigurationRequestBody(Map<String, String> configuration) {
        return GSON.toJson(configuration);
    }

    @Override
    public ValidationResult getPluginConfigurationValidationResultFromResponseBody(String responseBody) {
        return new JSONResultMessageHandler().toValidationResult(responseBody);
    }

    @Override
    public List<PluginConfiguration> getRoleConfigMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody).toPluginConfigurations();
    }

    @Override
    public String getRoleConfigurationViewFromResponseBody(String responseBody) {
        return getTemplateFromResponse(responseBody, "Role configuration `template` was blank!");
    }

    @Override
    public VerifyConnectionResponse getVerifyConnectionResultFromResponseBody(String responseBody) {
        return VerifyConnectionResponseDTO.fromJSON(responseBody).response();
    }

    @Override
    public String verifyConnectionRequestBody(Map<String, String> configuration) {
        return validatePluginConfigurationRequestBody(configuration);
    }

    @Override
    public String authenticateUserRequestBody(String username, String password, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> roleConfigs) {
        Map<String, Object> requestMap = new HashMap<>();
        final Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        requestMap.put("credentials", credentials);
        requestMap.put("auth_configs", getAuthConfigs(authConfigs));
        requestMap.put("role_configs", getRoleConfigs(roleConfigs));
        return GSON.toJson(requestMap);
    }

    private List<Map<String, Object>> getRoleConfigs(List<PluginRoleConfig> roleConfigs) {
        List<Map<String, Object>> configs = new ArrayList<>();

        if (roleConfigs == null) {
            return configs;
        }

        for (PluginRoleConfig roleConfig : roleConfigs) {
            Map<String, Object> config = new HashMap<>();
            config.put("name", roleConfig.getName().toString());
            config.put("auth_config_id", roleConfig.getAuthConfigId());
            config.put("configuration", roleConfig.getConfigurationAsMap(true));
            configs.add(config);
        }
        return configs;
    }

    private List<Map<String, Object>> getAuthConfigs(List<SecurityAuthConfig> authConfigs) {
        List<Map<String, Object>> configs = new ArrayList<>();

        if (authConfigs == null) {
            return configs;
        }

        for (SecurityAuthConfig securityAuthConfig : authConfigs) {
            Map<String, Object> authConfig = new HashMap<>();
            authConfig.put("id", securityAuthConfig.getId());
            authConfig.put("configuration", securityAuthConfig.getConfigurationAsMap(true));
            configs.add(authConfig);
        }
        return configs;
    }

    @Override
    public AuthenticationResponse getAuthenticatedUserFromResponseBody(String responseBody) {
        return AuthenticationResponseDTO.fromJSON(responseBody).toDomainModel();
    }

    @Override
    public List<User> getSearchUsersFromResponseBody(String responseBody) {
        return UserDTO.fromJSONList(responseBody).stream().map(UserDTO::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public String searchUsersRequestBody(String searchTerm, List<SecurityAuthConfig> authConfigs) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("search_term", searchTerm);
        requestMap.put("auth_configs", getAuthConfigs(authConfigs));
        return GSON.toJson(requestMap);
    }

    @Override
    public String getProcessRoleConfigsResponseBody(List<PluginRoleConfig> roles) {
        List<Map> list = new ArrayList<>();
        for (PluginRoleConfig role : roles) {
            LinkedHashMap<String, Object> e = new LinkedHashMap<>();
            e.put("name", role.getName().toString());
            e.put("configuration", role.getConfigurationAsMap(true));
            list.add(e);
        }
        return GSON.toJson(list);
    }

    @Override
    public String grantAccessRequestBody(List<SecurityAuthConfig> authConfigs) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("auth_configs", getAuthConfigs(authConfigs));

        return GSON.toJson(requestMap);
    }

    @Override
    public Map<String, String> getCredentials(String responseBody) {
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();

        return GSON.fromJson(responseBody, type);
    }

    @Override
    public String authenticateUserRequestBody(Map<String, String> credentials, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> roleConfigs) {
        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put("credentials", credentials);
        requestMap.put("auth_configs", getAuthConfigs(authConfigs));
        requestMap.put("role_configs", getRoleConfigs(roleConfigs));
        return GSON.toJson(requestMap);
    }

    @Override
    public String getAuthorizationServerUrl(String responseBody) {
        return (String) new Gson().fromJson(responseBody, Map.class).get("authorization_server_url");
    }

    @Override
    public String authorizationServerUrlRequestBody(String pluginId, List<SecurityAuthConfig> authConfigs, String siteUrl) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("auth_configs", getAuthConfigs(authConfigs));
        requestMap.put("authorization_server_callback_url", authorizationServerCallbackUrl(pluginId, siteUrl));

        return GSON.toJson(requestMap);
    }

    @Override
    public String authenticateUserRequestBody(String username, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> roleConfigs) {
        return null;
    }

    private String getTemplateFromResponse(String responseBody, String message) {
        String template = (String) new Gson().fromJson(responseBody, Map.class).get("template");
        if (StringUtils.isBlank(template)) {
            throw new RuntimeException(message);
        }
        return template;
    }

    @Override
    public String isValidUserRequestBody(String username, SecurityAuthConfig authConfig) {
        throw new UnsupportedOperationException("Authorization Extension v1 does not implement is-valid-user call.");
    }

    @Override
    public String getUserRolesRequestBody(String username, SecurityAuthConfig authConfig, List<PluginRoleConfig> roleConfigs) {
        throw new UnsupportedOperationException("Authorization Extension v1 does not implement get-user-roles call.");
    }

    @Override
    public List<String> getUserRolesFromResponseBody(String responseBody) {
        throw new UnsupportedOperationException("Authorization Extension v1 does not implement get-user-roles call.");
    }

    private String authorizationServerCallbackUrl(String pluginId, String siteUrl) {
        return String.format("%s/go/plugin/%s/authenticate", siteUrl, pluginId);
    }
}
