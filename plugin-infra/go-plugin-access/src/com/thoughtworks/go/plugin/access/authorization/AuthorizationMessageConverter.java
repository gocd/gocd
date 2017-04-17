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
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
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

    AuthenticationResponse getAuthenticatedUserFromResponseBody(String responseBody);

    List<User> getSearchUsersFromResponseBody(String responseBody);

    String searchUsersRequestBody(String searchTerm, List<SecurityAuthConfig> authConfigs);

    String processGetRoleConfigsRequest(String requestBody);

    String getProcessRoleConfigsResponseBody(List<PluginRoleConfig> roles);
}
