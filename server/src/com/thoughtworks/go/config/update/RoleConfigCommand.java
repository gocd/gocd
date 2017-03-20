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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.PluginNotFoundException;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.RoleNotFoundException;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import static com.thoughtworks.go.util.StringUtil.isBlank;

abstract class RoleConfigCommand implements EntityConfigUpdateCommand<Role> {
    protected final GoConfigService goConfigService;
    private AuthorizationExtension extension;
    protected final Role role;
    protected final Username currentUser;
    protected final LocalizedOperationResult result;
    protected Role preprocessedRole;

    public RoleConfigCommand(GoConfigService goConfigService, Role role, AuthorizationExtension extension, Username currentUser, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.extension = extension;
        this.role = role;
        this.currentUser = currentUser;
        this.result = result;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(role);
    }

    @Override
    public Role getPreprocessedEntityConfig() {
        return preprocessedRole;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized();
    }

    protected boolean isValidForCreateOrUpdate(CruiseConfig preprocessedConfig) {
        preprocessedRole = findExistingRole(preprocessedConfig);
        preprocessedRole.validate(null);
        validate(preprocessedConfig);
        BasicCruiseConfig.copyErrors(preprocessedRole, role);
        final RolesConfig rolesConfig = preprocessedConfig.server().security().getRoles();

        if (preprocessedRole.errors().isEmpty()) {
            rolesConfig.validate(null);
            role.errors().addAll(rolesConfig.errors());
            return preprocessedRole.getAllErrors().isEmpty() && role.errors().isEmpty();
        }

        return false;
    }

    private void validate(CruiseConfig preprocessedConfig) {
        if (role instanceof PluginRoleConfig) {
            PluginRoleConfig role = (PluginRoleConfig) this.role;
            PluginRoleConfig preprocessedRole = (PluginRoleConfig) this.preprocessedRole;

            SecurityAuthConfig securityAuthConfig = preprocessedConfig.server().security().securityAuthConfigs().find(role.getAuthConfigId());
            if (securityAuthConfig == null) {
                role.addError("authConfigId", "No such security auth configuration present " + role.getAuthConfigId());
                return;
            }

            try {
                ValidationResult result = extension.validateRoleConfiguration(securityAuthConfig.getPluginId(), role.getConfigurationAsMap(true));
                if (!result.isSuccessful()) {
                    for (ValidationError validationError : result.getErrors()) {
                        ConfigurationProperty property = preprocessedRole.getProperty(validationError.getKey());
                        if (property == null) {
                            role.addNewConfiguration(validationError.getKey(), false);
                            preprocessedRole.addNewConfiguration(validationError.getKey(), false);
                            property = preprocessedRole.getProperty(validationError.getKey());
                        }
                        property.addError(validationError.getKey(), validationError.getMessage());

                    }
                }
            } catch (PluginNotFoundException e) {
                role.addError("authConfigId", e.getMessage());
            }
        }
    }


    final Role findExistingRole(CruiseConfig cruiseConfig) {
        if (role == null || isBlank(role.getName().toString())) {
            if (role != null) {
                role.addError("name", "Role cannot have a blank name.");
            }
            result.unprocessableEntity(LocalizedMessage.string("ENTITY_ATTRIBUTE_NULL", getTagName(), "name"));
            throw new IllegalArgumentException("Role name cannot be null.");
        } else {
            Role t = cruiseConfig.server().security().getRoles().findByName(role.getName());
            if (t == null) {
                result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", getTagName(), role.getName()), HealthStateType.notFound());
                throw new RoleNotFoundException("Role `" + role.getName() + "` does not exist.");
            }
            return t;
        }
    }

    private String getTagName() {
        return role.getClass().getAnnotation(ConfigTag.class).value();
    }

    protected final boolean isAuthorized() {
        if (goConfigService.isUserAdmin(currentUser)) {
            return true;
        }
        result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
        return false;
    }

}
