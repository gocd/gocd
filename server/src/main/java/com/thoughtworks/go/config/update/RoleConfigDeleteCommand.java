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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.serverhealth.HealthStateType.notFound;

public class RoleConfigDeleteCommand extends RoleConfigCommand {

    public RoleConfigDeleteCommand(GoConfigService goConfigService, Role role, AuthorizationExtension extension, Username currentUser, LocalizedOperationResult result) {
        super(goConfigService, role, currentUser, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedRole = findExistingRole(preprocessedConfig);
        if (preprocessedRole == null) {
            throw new RecordNotFoundException(EntityType.Role, role.getName());
        }

        removeFromServerRole(preprocessedConfig, preprocessedRole);
        removeFromServerAdmins(preprocessedConfig);
        removeFromAllGroups(preprocessedConfig);
        removeFromAllTemplates(preprocessedConfig);
        removeFromSecurity(preprocessedConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!roleExists(cruiseConfig)) {
            result.notFound(EntityType.Role.notFoundMessage(role.getName()), notFound());
            return false;
        }
        return super.canContinue(cruiseConfig);
    }

    private boolean roleExists(CruiseConfig cruiseConfig) {
        return findExistingRole(cruiseConfig) != null;
    }

    private void removeFromServerAdmins(CruiseConfig preprocessedConfig) {
        preprocessedConfig.server().security().adminsConfig().remove(new AdminRole(role.getName()));
    }

    private void removeFromSecurity(CruiseConfig preprocessedConfig) {
        preprocessedConfig.server().security().getRoles().removeIfExists(role);
    }

    private void removeFromAllTemplates(CruiseConfig preprocessedConfig) {
        TemplatesConfig templates = preprocessedConfig.getTemplates();

        for (PipelineTemplateConfig template : templates) {
            template.getAuthorization().removeAllUsagesOfRole(role);
            template.cleanupAllUsagesOfRole(role);
        }
    }

    private void removeFromAllGroups(CruiseConfig preprocessedConfig) {
        PipelineGroups groups = preprocessedConfig.getGroups();
        for (PipelineConfigs group : groups) {
            group.cleanupAllUsagesOfRole(role);
        }
    }

    @Override
    public void encrypt(CruiseConfig preprocessedConfig) {
        //do nothing
    }

    private void removeFromServerRole(CruiseConfig preprocessedConfig, Role existingRole) {
        preprocessedConfig.server().security().getRoles().removeIfExists(existingRole);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return true;
    }
}
