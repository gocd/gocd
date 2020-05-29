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
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.SecretConfigs;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class SecretConfigUpdateCommand extends SecretConfigCommand {
    private final EntityHashingService hashingService;
    private final String md5;

    public SecretConfigUpdateCommand(GoConfigService goConfigService, SecretConfig secretConfig, SecretsExtension extension, Username currentUser, LocalizedOperationResult result, EntityHashingService hashingService, String md5) {
        super(goConfigService, secretConfig, extension, currentUser, result);
        this.hashingService = hashingService;
        this.md5 = md5;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        SecretConfig existingSecretConfig = findExistingProfile(preprocessedConfig);
        SecretConfigs secretConfigs = getPluginProfiles(preprocessedConfig);
        secretConfigs.set(secretConfigs.indexOf(existingSecretConfig), profile);
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
        SecretConfig existingSecretConfig = findExistingProfile(cruiseConfig);
        boolean freshRequest = hashingService.hashForEntity(existingSecretConfig).equals(md5);
        if (!freshRequest) {
            result.stale(getObjectDescriptor().staleConfig(existingSecretConfig.getId()));
        }
        return freshRequest;
    }
}
