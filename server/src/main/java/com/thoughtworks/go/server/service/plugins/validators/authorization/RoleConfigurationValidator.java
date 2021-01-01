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
package com.thoughtworks.go.server.service.plugins.validators.authorization;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

public class RoleConfigurationValidator {
    private final AuthorizationExtension authorizationExtension;

    public RoleConfigurationValidator(AuthorizationExtension authorizationExtension) {
        this.authorizationExtension = authorizationExtension;
    }

    public void validate(PluginRoleConfig role, String pluginId) {
        try {
            ValidationResult result = authorizationExtension.validateRoleConfiguration(pluginId, role.getConfigurationAsMap(true));

            if (!result.isSuccessful()) {
                for (ValidationError error : result.getErrors()) {
                    ConfigurationProperty property = role.getProperty(error.getKey());

                    if (property == null) {
                        role.addNewConfiguration(error.getKey(), false);
                        property = role.getProperty(error.getKey());
                    }
                    property.addError(error.getKey(), error.getMessage());
                }
            }
        } catch (RecordNotFoundException e) {
            role.addError("pluginRole", String.format("Unable to validate `pluginRole` configuration, missing plugin: %s", pluginId));
        }
    }
}
