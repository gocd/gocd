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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.config.update.SecurityAuthConfigCreateCommand;
import com.thoughtworks.go.config.update.SecurityAuthConfigDeleteCommand;
import com.thoughtworks.go.config.update.SecurityAuthConfigUpdateCommand;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.PluginNotFoundException;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.i18n.LocalizedMessage.string;

@Component
public class SecurityAuthConfigService extends PluginProfilesService<SecurityAuthConfig> {
    private final AuthorizationExtension authorizationExtension;
    private final AuthorizationMetadataStore store;

    @Autowired
    public SecurityAuthConfigService(GoConfigService goConfigService, EntityHashingService hashingService, AuthorizationExtension authorizationExtension) {
        super(goConfigService, hashingService);
        this.authorizationExtension = authorizationExtension;
        this.store = AuthorizationMetadataStore.instance();
    }

    SecurityAuthConfigService(GoConfigService goConfigService, EntityHashingService hashingService,
                              AuthorizationExtension authorizationExtension, AuthorizationMetadataStore store) {
        super(goConfigService, hashingService);
        this.authorizationExtension = authorizationExtension;
        this.store = store;
    }

    protected SecurityAuthConfigs getPluginProfiles() {
        return goConfigService.serverConfig().security().securityAuthConfigs();
    }

    public void update(Username currentUser, String md5, SecurityAuthConfig newSecurityAuthConfig, LocalizedOperationResult result) {
        update(currentUser, newSecurityAuthConfig, result, new SecurityAuthConfigUpdateCommand(goConfigService, newSecurityAuthConfig, authorizationExtension, currentUser, result, hashingService, md5));
    }

    public void delete(Username currentUser, SecurityAuthConfig newSecurityAuthConfig, LocalizedOperationResult result) {
        update(currentUser, newSecurityAuthConfig, result, new SecurityAuthConfigDeleteCommand(goConfigService, newSecurityAuthConfig, authorizationExtension, currentUser, result));
        if (result.isSuccessful()) {
            result.setMessage(string("RESOURCE_DELETE_SUCCESSFUL", "security auth config", newSecurityAuthConfig.getId()));
        }
    }

    public void create(Username currentUser, SecurityAuthConfig securityAuthConfig, LocalizedOperationResult result) {
        update(currentUser, securityAuthConfig, result, new SecurityAuthConfigCreateCommand(goConfigService, securityAuthConfig, authorizationExtension, currentUser, result));
    }

    public void verifyConnection(SecurityAuthConfig securityAuthConfig, LocalizedOperationResult result) {
        final String pluginId = securityAuthConfig.getPluginId();

        try {
            ValidationResult validationResult = authorizationExtension.verifyConnection(pluginId, securityAuthConfig.getConfigurationAsMap(true));
            if (validationResult.isSuccessful()) {
                return;
            }

            validationResultToLocalizedOperationResult(securityAuthConfig, result, validationResult);

            if (!result.hasMessage()) {
                result.unprocessableEntity(string("CHECK_CONNECTION_FAILED", securityAuthConfig.getId(), "Could not verify connection!"));
            }

        } catch (PluginNotFoundException e) {
            result.internalServerError(string("ASSOCIATED_PLUGIN_NOT_FOUND", pluginId));
        }
    }

    private void validationResultToLocalizedOperationResult(SecurityAuthConfig securityAuthConfig, LocalizedOperationResult result, ValidationResult validationResult) {
        for (ValidationError validationError : validationResult.getErrors()) {
            if (StringUtils.isBlank(validationError.getKey())) {
                result.unprocessableEntity(string("CHECK_CONNECTION_FAILED", securityAuthConfig.getId(), validationError.getMessage()));
            } else {
                ConfigurationProperty property = securityAuthConfig.getProperty(validationError.getKey());
                if (property != null) {
                    property.addError(validationError.getKey(), validationError.getMessage());
                }
            }
        }
    }

}
