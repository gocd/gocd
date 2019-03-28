/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.NewPluginProfiles;
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.SecretConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.update.SecretConfigCreateCommand;
import com.thoughtworks.go.config.update.SecretConfigUpdateCommand;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecretConfigService extends NewPluginProfilesService<SecretConfig> {
    private final SecretsExtension secretsExtension;

    @Autowired
    public SecretConfigService(GoConfigService goConfigService, EntityHashingService hashingService, SecretsExtension secretsExtension) {
        super(goConfigService, hashingService);
        this.secretsExtension = secretsExtension;
    }

    @Override
    protected NewPluginProfiles<SecretConfig> getPluginProfiles() {
        return goConfigService.cruiseConfig().getSecretConfigs();
    }

    public SecretConfigs getAllSecretConfigs() {
        return (SecretConfigs) getPluginProfiles();
    }

    public void create(Username currentUser, SecretConfig secretConfig, LocalizedOperationResult result) {
        SecretConfigCreateCommand command = new SecretConfigCreateCommand(goConfigService, secretConfig, secretsExtension, currentUser, result);
        update(currentUser, secretConfig, result, command);
    }

    public void update(Username currentUser, String md5, SecretConfig secretConfig, LocalizedOperationResult result) {
        SecretConfigUpdateCommand command = new SecretConfigUpdateCommand(goConfigService, secretConfig, secretsExtension, currentUser, result, hashingService, md5);
        update(currentUser, secretConfig, result, command);
    }


}
