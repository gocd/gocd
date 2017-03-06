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

import com.google.gson.Gson;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.plugin.access.authorization.models.Capabilities;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class AuthorizationMessageConverterV1 implements AuthorizationMessageConverter {
    public static final String VERSION = "1.0";
    private static final Gson GSON = new Gson();

    @Override
    public Capabilities getCapabilitiesFromResponseBody(String responseBody) {
        return Capabilities.fromJSON(responseBody);
    }

    @Override
    public Image getImageResponseFromBody(String responseBody) {
        return Image.fromJSON(responseBody);
    }

    @Override
    public PluginProfileMetadataKeys getPluginConfigMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody);
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
    public PluginProfileMetadataKeys getRoleConfigMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody);
    }

    @Override
    public String getRoleConfigurationViewFromResponseBody(String responseBody) {
        return getTemplateFromResponse(responseBody, "Role configuration `template` was blank!");
    }

    @Override
    public ValidationResult getVerifyConnectionResultFromResponseBody(String responseBody) {
        return getPluginConfigurationValidationResultFromResponseBody(responseBody);
    }

    @Override
    public String verifyConnectionRequestBody(Map<String, String> configuration) {
        return validatePluginConfigurationRequestBody(configuration);
    }

    @Override
    public String authenticateUserRequestBody(String username, String password, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> roleConfigs) {
        Map<String, Object> requestMap = new HashMap<>();
        final Map<String, String> credentials = new HashedMap();
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
            Map<String, Object> config = new HashedMap();
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
            Map<String, Object> authConfig = new HashedMap();
            authConfig.put("id", securityAuthConfig.getId());
            authConfig.put("configuration", securityAuthConfig.getConfigurationAsMap(true));
            configs.add(authConfig);
        }
        return configs;
    }

    @Override
    public AuthenticationResponse getAuthenticatedUserFromResponseBody(String responseBody) {
        return AuthenticationResponse.fromJSON(responseBody);
    }

    @Override
    public List<User> getSearchUsersFromResponseBody(String responseBody) {
        return User.fromJSONList(responseBody);
    }

    @Override
    public String searchUsersRequestBody(String searchTerm, List<SecurityAuthConfig> authConfigs) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("search_term", searchTerm);
        requestMap.put("auth_configs", getAuthConfigs(authConfigs));
        return GSON.toJson(requestMap);
    }

    @Override
    public String processGetRoleConfigsRequest(String requestBody) {
        return (String) GSON.fromJson(requestBody, Map.class).get("auth_config_id");
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

    private String getTemplateFromResponse(String responseBody, String message) {
        String template = (String) new Gson().fromJson(responseBody, Map.class).get("template");
        if (StringUtils.isBlank(template)) {
            throw new RuntimeException(message);
        }
        return template;
    }

}
