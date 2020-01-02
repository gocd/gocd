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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import java.util.Map;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

public abstract class SecretConfigCommand extends RuleAwarePluginProfileCommand<SecretConfig, SecretConfigs> {
    private final SecretsExtension extension;

    public SecretConfigCommand(GoConfigService goConfigService, SecretConfig secretConfig, SecretsExtension extension, Username currentUser, LocalizedOperationResult result) {
        super(goConfigService, secretConfig, currentUser, result);
        this.extension = extension;
    }

    @Override
    protected SecretConfigs getPluginProfiles(CruiseConfig preprocessedConfig) {
        return preprocessedConfig.getSecretConfigs();
    }

    @Override
    public ValidationResult validateUsingExtension(String pluginId, Map<String, String> configuration) {
        return extension.validateSecretsConfig(pluginId, configuration);
    }

    @Override
    protected EntityType getObjectDescriptor() {
        return EntityType.SecretConfig;
    }

    @Override
    protected boolean isAuthorized() {
        if (!(goConfigService.isUserAdmin(currentUser))) {
            result.forbidden(EntityType.SecretConfig.forbiddenToEdit(profile.getId(), currentUser.getUsername()), forbidden());
            return false;
        }
        return true;
    }
}
