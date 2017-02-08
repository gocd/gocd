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
import com.thoughtworks.go.plugin.access.authorization.models.Capabilities;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

import java.util.List;
import java.util.Map;

public interface AuthorizationMessageConverter {
    Capabilities getCapabilitiesFromResponseBody(String responseBody);

    Image getImageResponseFromBody(String responseBody);

    PluginProfileMetadataKeys getPluginConfigMetadataResponseFromBody(String responseBody);

    String getPluginConfigurationViewFromResponseBody(String responseBody);

    String validatePluginConfigurationRequestBody(Map<String, String> configuration);

    ValidationResult getPluginConfigurationValidationResultFromResponseBody(String responseBody);

    PluginProfileMetadataKeys getRoleConfigMetadataResponseFromBody(String responseBody);

    String getRoleConfigurationViewFromResponseBody(String responseBody);

    ValidationResult getVerifyConnectionResultFromResponseBody(String responseBody);

    String verifyConnectionRequestBody(Map<String, String> configuration);

    String authenticateUserRequestBody(String username, String password, List<SecurityAuthConfig> authConfigs);

    AuthenticationResponse getAuthenticatedUserFromResponseBody(String responseBody);

    List<User> getSearchUsersFromResponseBody(String responseBody);

    String searchUsersRequestBody(String searchTerm, List<SecurityAuthConfig> authConfigs);

    String processGetRoleConfigsRequest(String requestBody);

    String getProcessRoleConfigsResponseBody(List<PluginRoleConfig> roles);
}
