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
package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;

import java.util.List;
import java.util.Map;

public interface AuthorizationMessageConverter {
    com.thoughtworks.go.plugin.domain.authorization.Capabilities getCapabilitiesFromResponseBody(String responseBody);

    com.thoughtworks.go.plugin.domain.common.Image getImageResponseFromBody(String responseBody);

    List<PluginConfiguration> getPluginConfigMetadataResponseFromBody(String responseBody);

    String getPluginConfigurationViewFromResponseBody(String responseBody);

    String validatePluginConfigurationRequestBody(Map<String, String> configuration);

    ValidationResult getPluginConfigurationValidationResultFromResponseBody(String responseBody);

    List<PluginConfiguration> getRoleConfigMetadataResponseFromBody(String responseBody);

    String getRoleConfigurationViewFromResponseBody(String responseBody);

    VerifyConnectionResponse getVerifyConnectionResultFromResponseBody(String responseBody);

    String verifyConnectionRequestBody(Map<String, String> configuration);

    String authenticateUserRequestBody(String username, String password, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> roleConfigs);

    String authenticateUserRequestBody(String username, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> roleConfigs);

    AuthenticationResponse getAuthenticatedUserFromResponseBody(String responseBody);

    List<User> getSearchUsersFromResponseBody(String responseBody);

    String searchUsersRequestBody(String searchTerm, List<SecurityAuthConfig> authConfigs);

    String getProcessRoleConfigsResponseBody(List<PluginRoleConfig> roles);

    String grantAccessRequestBody(List<SecurityAuthConfig> authConfigs);

    Map<String,String> getCredentials(String responseBody);

    String authenticateUserRequestBody(Map<String, String> credentials, List<SecurityAuthConfig> authConfigs, List<PluginRoleConfig> roleConfigs);

    String getAuthorizationServerUrl(String responseBody);

    String authorizationServerUrlRequestBody(String pluginId, List<SecurityAuthConfig> authConfigs, String siteUrl);

    String isValidUserRequestBody(String username, SecurityAuthConfig authConfig);

    String getUserRolesRequestBody(String username, SecurityAuthConfig authConfig, List<PluginRoleConfig> roleConfigs);

    List<String> getUserRolesFromResponseBody(String responseBody);
}
