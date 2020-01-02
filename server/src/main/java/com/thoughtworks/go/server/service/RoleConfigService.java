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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.*;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.validators.authorization.RoleConfigurationValidator;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;

@Component
public class RoleConfigService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RoleConfigService.class);
    private final AuthorizationExtension authorizationExtension;
    private final GoConfigService goConfigService;
    private final EntityHashingService hashingService;
    private final RoleConfigurationValidator roleConfigurationValidator;

    @Autowired
    public RoleConfigService(GoConfigService goConfigService, EntityHashingService hashingService, AuthorizationExtension authorizationExtension) {
        this.goConfigService = goConfigService;
        this.hashingService = hashingService;
        this.authorizationExtension = authorizationExtension;
        roleConfigurationValidator = new RoleConfigurationValidator(authorizationExtension);
    }

    protected RoleConfigService(GoConfigService goConfigService, EntityHashingService hashingService, AuthorizationExtension authorizationExtension,
                                RoleConfigurationValidator roleConfigurationValidator) {
        this.authorizationExtension = authorizationExtension;
        this.goConfigService = goConfigService;
        this.hashingService = hashingService;
        this.roleConfigurationValidator = roleConfigurationValidator;
    }

    public Role findRole(String name) {
        return getRoles().findByName(new CaseInsensitiveString(name));
    }

    public RolesConfig listAll() {
        return getRoles();
    }

    protected void update(Username currentUser, Role role, LocalizedOperationResult result, EntityConfigUpdateCommand<Role> command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(entityConfigValidationFailed(getTagName(role.getClass()), role.getName(), ((GoConfigInvalidException) e).getAllErrorMessages()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while saving the role config. Please check the logs for more information."));
                }
            }
        }
    }

    private String getTagName(Class<?> clazz) {
        return clazz.getAnnotation(ConfigTag.class).value();
    }

    public RolesConfig getRoles() {
        return goConfigService.getConfigForEditing().server().security().getRoles();
    }

    public HashMap<Username, RolesConfig> getRolesForUser(List<Username> users) {
        HashMap<Username, RolesConfig> userToRolesMap = new HashMap<>();

        getRoles().stream().<Consumer<? super Username>>map(role -> user -> {
            if (role.hasMember(user.getUsername())) {
                if (!userToRolesMap.containsKey(user)) {
                    userToRolesMap.put(user, new RolesConfig());
                }

                userToRolesMap.get(user).add(role);
            }
        }).forEach(users::forEach);

        return userToRolesMap;
    }

    public void update(Username currentUser, String md5, Role newRole, LocalizedOperationResult result) {
        validatePluginRoleMetadata(newRole);
        update(currentUser, newRole, result, new RoleConfigUpdateCommand(goConfigService, newRole, currentUser, result, hashingService, md5));
    }

    public void bulkUpdate(GoCDRolesBulkUpdateRequest bulkUpdateRequest, Username currentUser,
                           HttpLocalizedOperationResult result) {
        RolesConfigBulkUpdateCommand command = new RolesConfigBulkUpdateCommand(bulkUpdateRequest, currentUser,
                goConfigService, result);

        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(entityConfigValidationFailed(getTagName(RolesConfig.class), bulkUpdateRequest.getRolesToUpdateAsString(), ((GoConfigInvalidException) e).getAllErrorMessages()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while saving the role config. Please check the logs for more information."));
                }
            }
        }
    }

    public void delete(Username currentUser, Role role, LocalizedOperationResult result) {
        update(currentUser, role, result, new RoleConfigDeleteCommand(goConfigService, role, authorizationExtension, currentUser, result));
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.resourceDeleteSuccessful(getTagName(role.getClass()).toLowerCase(), role.getName()));
        }
    }

    public void create(Username currentUser, Role newRole, LocalizedOperationResult result) {
        validatePluginRoleMetadata(newRole);
        update(currentUser, newRole, result, new RoleConfigCreateCommand(goConfigService, newRole, currentUser, result));
    }

    private void validatePluginRoleMetadata(Role newRole) {
        if (newRole instanceof PluginRoleConfig) {
            PluginRoleConfig role = (PluginRoleConfig) newRole;
            String pluginId = pluginIdForRole(role);

            if (pluginId == null) {
                return;
            }

            roleConfigurationValidator.validate(role, pluginId);
        }
    }

    private String pluginIdForRole(PluginRoleConfig role) {
        SecurityAuthConfig authConfig = goConfigService.cruiseConfig().server().security().securityAuthConfigs().find(role.getAuthConfigId());
        if (authConfig == null) {
            return null;
        }
        return authConfig.getPluginId();
    }
}
