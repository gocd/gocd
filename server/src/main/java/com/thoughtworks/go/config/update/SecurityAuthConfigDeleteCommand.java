/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.join;

public class SecurityAuthConfigDeleteCommand extends SecurityAuthConfigCommand {

    public SecurityAuthConfigDeleteCommand(GoConfigService goConfigService, SecurityAuthConfig newSecurityAuthConfig, AuthorizationExtension extension, Username currentUser, LocalizedOperationResult result) {
        super(goConfigService, newSecurityAuthConfig, extension, currentUser, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedProfile = findExistingProfile(preprocessedConfig);
        getPluginProfiles(preprocessedConfig).remove(preprocessedProfile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValidForDelete(preprocessedConfig);
    }

    private boolean isValidForDelete(CruiseConfig preprocessedConfig) {
        RolesConfig rolesConfig = preprocessedConfig.server().security().getRoles();

        List<String> usedByRolesConfig = new ArrayList<>();
        for (PluginRoleConfig role : rolesConfig.getPluginRoleConfigs()) {
            if (profile.hasRole(role)) {
                usedByRolesConfig.add(role.getName().toString());
            }
        }

        if (!usedByRolesConfig.isEmpty()) {
            String usedByMessage = join(", ", usedByRolesConfig);
            result.unprocessableEntity("Cannot delete the %s '%s' as it is used by role(s): '%s'".formatted(getObjectDescriptor().getEntityNameLowerCase(), profile.getId(), usedByMessage));
            throw new GoConfigInvalidException(preprocessedConfig, String.format("The %s '%s' is being referenced by role(s): %s.", getObjectDescriptor().getEntityNameLowerCase(), profile.getId(), usedByMessage));
        }
        return true;
    }

}
