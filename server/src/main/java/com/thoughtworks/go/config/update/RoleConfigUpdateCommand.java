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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.serverhealth.HealthStateType.notFound;

public class RoleConfigUpdateCommand extends RoleConfigCommand {
    private final EntityHashingService hashingService;
    private final String digest;

    public RoleConfigUpdateCommand(GoConfigService goConfigService, Role newRole, Username currentUser, LocalizedOperationResult result, EntityHashingService hashingService, String digest) {
        super(goConfigService, newRole, currentUser, result);
        this.hashingService = hashingService;
        this.digest = digest;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedConfig.server().security().getRoles().replace(findExistingRole(preprocessedConfig), role);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        Role existingRole = findExistingRole(cruiseConfig);

        if (existingRole == null) {
            result.notFound(EntityType.Role.notFoundMessage(role.getName()), notFound());
            return false;
        }

        boolean freshRequest = hashingService.hashForEntity(existingRole).equals(digest);

        if (!freshRequest) {
            result.stale(EntityType.Role.staleConfig(existingRole.getName()));
        }
        return freshRequest;
    }
}
