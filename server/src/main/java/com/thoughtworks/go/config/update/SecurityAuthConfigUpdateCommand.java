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
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class SecurityAuthConfigUpdateCommand extends SecurityAuthConfigCommand {

    private final EntityHashingService hashingService;
    private final String digest;

    public SecurityAuthConfigUpdateCommand(GoConfigService goConfigService, SecurityAuthConfig newSecurityAuthConfig, AuthorizationExtension extension, Username currentUser, LocalizedOperationResult result, EntityHashingService hashingService, String digest) {
        super(goConfigService, newSecurityAuthConfig, extension, currentUser, result);
        this.hashingService = hashingService;
        this.digest = digest;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        SecurityAuthConfig existingProfile = findExistingProfile(preprocessedConfig);
        SecurityAuthConfigs profiles = getPluginProfiles(preprocessedConfig);
        profiles.set(profiles.indexOf(existingProfile), profile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValidForCreateOrUpdate(preprocessedConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        SecurityAuthConfig existingProfile = findExistingProfile(cruiseConfig);
        boolean freshRequest = hashingService.hashForEntity(existingProfile).equals(digest);
        if (!freshRequest) {
            result.stale(getObjectDescriptor().staleConfig(existingProfile.getId()));
        }
        return freshRequest;
    }
}
