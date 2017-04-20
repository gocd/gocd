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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class RoleConfigUpdateCommand extends RoleConfigCommand {
    private final EntityHashingService hashingService;
    private final String md5;

    public RoleConfigUpdateCommand(GoConfigService goConfigService, Role newRole, AuthorizationExtension extension, Username currentUser, LocalizedOperationResult result, EntityHashingService hashingService, String md5) {
        super(goConfigService, newRole, currentUser, result);
        this.hashingService = hashingService;
        this.md5 = md5;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedConfig.server().security().getRoles().replace(findExistingRole(preprocessedConfig), role);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        Role existingRole = findExistingRole(cruiseConfig);

        if (existingRole == null) {
            result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", "role", role.getName()), HealthStateType.notFound());
            return false;
        }

        boolean freshRequest = hashingService.md5ForEntity(existingRole).equals(md5);

        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "role config", existingRole.getName()));
        }
        return freshRequest;
    }
}
