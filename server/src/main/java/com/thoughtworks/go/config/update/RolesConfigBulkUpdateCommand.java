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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.validation.RolesConfigUpdateValidator;

import java.util.List;
import java.util.Objects;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

public class RolesConfigBulkUpdateCommand implements EntityConfigUpdateCommand<RolesConfig> {
    private final Username currentUser;
    private final GoConfigService goConfigService;
    private final LocalizedOperationResult result;
    private final GoCDRolesBulkUpdateRequest goCDRolesBulkUpdateRequest;
    private RolesConfig preProcessedRolesConfig;

    public RolesConfigBulkUpdateCommand(GoCDRolesBulkUpdateRequest goCDRolesBulkUpdateRequest, Username currentUser,
                                        GoConfigService goConfigService, LocalizedOperationResult result) {
        this.goCDRolesBulkUpdateRequest = goCDRolesBulkUpdateRequest;
        this.currentUser = currentUser;
        this.goConfigService = goConfigService;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        RolesConfig rolesInConfig = preprocessedConfig.server().security().getRoles();
        for (GoCDRolesBulkUpdateRequest.Operation operation : goCDRolesBulkUpdateRequest.getOperations()) {
            RoleConfig existingRole = rolesInConfig.findByNameAndType(new CaseInsensitiveString(operation.getRoleName()), RoleConfig.class);
            if (existingRole == null) {
                result.unprocessableEntity(EntityType.Role.notFoundMessage(operation.getRoleName()));
                throw new RecordNotFoundException(EntityType.Role, operation.getRoleName());
            }
            existingRole.addUsersWithName(operation.getUsersToAdd());
            existingRole.removeUsersWithName(operation.getUsersToRemove());
        }
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preProcessedRolesConfig = preprocessedConfig.server().security().getRoles();
        List<CaseInsensitiveString> roleNames = goCDRolesBulkUpdateRequest.getRolesToUpdate();
        boolean isValid = new RolesConfigUpdateValidator(roleNames).isValid(preprocessedConfig);
        if (!isValid) {
            result.unprocessableEntity("Validations failed for bulk update of roles. Error(s): " + preprocessedConfig.getAllErrors());
        }
        return isValid;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(preProcessedRolesConfig);
    }

    @Override
    public RolesConfig getPreprocessedEntityConfig() {
        return preProcessedRolesConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized();
    }

    private boolean isAuthorized() {
        if (goConfigService.isUserAdmin(currentUser)) {
            return true;
        }
        result.forbidden(forbiddenToEdit(), forbidden());
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolesConfigBulkUpdateCommand that = (RolesConfigBulkUpdateCommand) o;
        return Objects.equals(currentUser, that.currentUser) &&
                Objects.equals(goConfigService, that.goConfigService) &&
                Objects.equals(result, that.result) &&
                Objects.equals(goCDRolesBulkUpdateRequest, that.goCDRolesBulkUpdateRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentUser, goConfigService, result, goCDRolesBulkUpdateRequest);
    }
}
