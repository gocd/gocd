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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.update.SecurityAuthConfigCreateCommand;
import com.thoughtworks.go.config.update.SecurityAuthConfigDeleteCommand;
import com.thoughtworks.go.config.update.SecurityAuthConfigUpdateCommand;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.common.ValidationError;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.ui.AuthPluginInfoViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SecurityAuthConfigService extends PluginProfilesService<SecurityAuthConfig> {
    private final AuthorizationExtension authorizationExtension;
    private final AuthorizationMetadataStore authorizationMetadataStore;

    @Autowired
    public SecurityAuthConfigService(GoConfigService goConfigService, EntityHashingService hashingService, AuthorizationExtension authorizationExtension) {
        this(goConfigService, hashingService, authorizationExtension, AuthorizationMetadataStore.instance());
    }

    protected SecurityAuthConfigService(GoConfigService goConfigService, EntityHashingService hashingService, AuthorizationExtension authorizationExtension, AuthorizationMetadataStore authorizationMetadataStore) {
        super(goConfigService, hashingService);
        this.authorizationExtension = authorizationExtension;
        this.authorizationMetadataStore = authorizationMetadataStore;
    }

    @Override
    public SecurityAuthConfigs getPluginProfiles() {
        return goConfigService.security().securityAuthConfigs();
    }

    public void update(Username currentUser, String md5, SecurityAuthConfig newSecurityAuthConfig, LocalizedOperationResult result) {
        SecurityAuthConfigUpdateCommand command = new SecurityAuthConfigUpdateCommand(goConfigService, newSecurityAuthConfig, authorizationExtension, currentUser, result, hashingService, md5);
        update(currentUser, newSecurityAuthConfig, result, command, true);
    }



    public void delete(Username currentUser, SecurityAuthConfig newSecurityAuthConfig, LocalizedOperationResult result) {
        update(currentUser, newSecurityAuthConfig, result, new SecurityAuthConfigDeleteCommand(goConfigService, newSecurityAuthConfig, authorizationExtension, currentUser, result), false);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.SecurityAuthConfig.deleteSuccessful(newSecurityAuthConfig.getId()));
        }
    }

    public void create(Username currentUser, SecurityAuthConfig securityAuthConfig, LocalizedOperationResult result) {
        SecurityAuthConfigCreateCommand command = new SecurityAuthConfigCreateCommand(goConfigService, securityAuthConfig, authorizationExtension, currentUser, result);
        update(currentUser, securityAuthConfig, result, command, true);
    }

    public VerifyConnectionResponse verifyConnection(SecurityAuthConfig securityAuthConfig) {
        final String pluginId = securityAuthConfig.getPluginId();

        try {
            VerifyConnectionResponse response = authorizationExtension.verifyConnection(pluginId, securityAuthConfig.getConfigurationAsMap(true));

            if (!response.isSuccessful()) {
                mapErrors(response, securityAuthConfig);
            }

            return response;
        } catch (RecordNotFoundException e) {
            String message = String.format("Unable to verify connection, missing plugin: %s", pluginId);

            return new VerifyConnectionResponse("failure", message, new com.thoughtworks.go.plugin.domain.common.ValidationResult());
        }
    }

    private void mapErrors(VerifyConnectionResponse response, SecurityAuthConfig authConfig) {
        com.thoughtworks.go.plugin.domain.common.ValidationResult validationResult = response.getValidationResult();

        if (validationResult == null) {
            return;
        }

        for (ValidationError error : validationResult.getErrors()) {
            ConfigurationProperty property = authConfig.getProperty(error.getKey());

            if (property == null) {
                authConfig.addNewConfiguration(error.getKey(), false);
                property = authConfig.getProperty(error.getKey());
            }
            property.addError(error.getKey(), error.getMessage());
        }
    }

    public List<AuthPluginInfoViewModel> getAllConfiguredWebBasedAuthorizationPlugins() {
        ArrayList<AuthPluginInfoViewModel> result = new ArrayList();
        Set<AuthorizationPluginInfo> loadedWebBasedAuthPlugins = authorizationMetadataStore.getPluginsThatSupportsWebBasedAuthentication();
        SecurityAuthConfigs configuredAuthPluginProfiles = getPluginProfiles();
        for (SecurityAuthConfig authConfig : configuredAuthPluginProfiles) {
            AuthorizationPluginInfo authorizationPluginInfo = loadedWebBasedAuthPlugins.stream().filter(authorizationPluginInfo1 -> authorizationPluginInfo1.getDescriptor().id().equals(authConfig.getPluginId())).findFirst().orElse(null);
            if (authorizationPluginInfo != null) {
                result.add(new AuthPluginInfoViewModel(authorizationPluginInfo));
            }
        }
        return result;
    }
}
