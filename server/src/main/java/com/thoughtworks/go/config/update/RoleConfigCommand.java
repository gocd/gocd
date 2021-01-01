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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ErrorCollector;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.validation.RolesConfigUpdateValidator;

import java.util.List;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

abstract class RoleConfigCommand implements EntityConfigUpdateCommand<Role> {
    protected final GoConfigService goConfigService;
    protected final Role role;
    protected final Username currentUser;
    protected final LocalizedOperationResult result;
    protected Role preprocessedRole;

    public RoleConfigCommand(GoConfigService goConfigService, Role role, Username currentUser, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
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

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedRole = preprocessedConfig.server().security().getRoles().findByNameAndType(role.getName(), role.getClass());

        preprocessedRole.validateTree(RolesConfigUpdateValidator.validationContextWithSecurityConfig(preprocessedConfig));

        List<ConfigErrors> allErrors = ErrorCollector.getAllErrors(preprocessedRole);
        boolean isEmpty = allErrors.isEmpty();
        if (!isEmpty) {
            BasicCruiseConfig.copyErrors(preprocessedRole, role);
        }
        return isEmpty;
    }

    final Role findExistingRole(CruiseConfig cruiseConfig) {
        return cruiseConfig.server().security().getRoles().findByName(role.getName());
    }

    @Override
    public void encrypt(CruiseConfig preprocessedConfig) {
        this.role.encryptSecureProperties(preprocessedConfig);
    }

    protected final boolean isAuthorized() {
        if (goConfigService.isUserAdmin(currentUser)) {
            return true;
        }
        result.forbidden(EntityType.Role.forbiddenToEdit(role.getName(), currentUser.getUsername()), forbidden());
        return false;
    }

}
