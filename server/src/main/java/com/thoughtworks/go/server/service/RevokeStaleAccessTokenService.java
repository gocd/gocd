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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PluginProfile;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RevokeStaleAccessTokenService extends EntityConfigChangedListener<SecurityAuthConfig> {
    private GoConfigService goConfigService;
    private AccessTokenService accessTokenService;
    private SecurityAuthConfigs existingSecurityAuthConfigs;

    @Autowired
    public RevokeStaleAccessTokenService(GoConfigService goConfigService, AccessTokenService accessTokenService) {
        this.goConfigService = goConfigService;
        this.accessTokenService = accessTokenService;

        this.goConfigService.register(this);
    }

    public void initialize() {
        this.existingSecurityAuthConfigs = this.goConfigService.getConfigForEditing().server().security().securityAuthConfigs();
        List<String> authConfigIdsFromConfig = this.getIds(existingSecurityAuthConfigs);
        this.accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active).forEach(token -> {
            if (!authConfigIdsFromConfig.contains(token.getAuthConfigId())) {
                this.revokeAccessTokenDueToRemovalOfAuthConfig(token);
            }
        });
    }

    @Override
    public void onEntityConfigChange(SecurityAuthConfig securityAuthConfig) {
        this.removeAccessTokensFromStaleAuthConfigs(goConfigService.getConfigForEditing().server().security().securityAuthConfigs());
    }

    @Override
    public void onConfigChange(CruiseConfig cruiseConfig) {
        SecurityAuthConfigs updatedSecurityAuthConfigs = cruiseConfig.server().security().securityAuthConfigs();
        this.removeAccessTokensFromStaleAuthConfigs(updatedSecurityAuthConfigs);
    }

    private void removeAccessTokensFromStaleAuthConfigs(SecurityAuthConfigs updatedSecurityAuthConfigs) {
        if (this.existingSecurityAuthConfigs != null && !this.existingSecurityAuthConfigs.equals(updatedSecurityAuthConfigs)) {
            List<String> existing = getIds(existingSecurityAuthConfigs);
            List<String> updated = getIds(updatedSecurityAuthConfigs);
            List<String> removed = existing.stream().filter(id -> !updated.contains(id)).collect(Collectors.toList());

            this.accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active).forEach(token -> {
                if (removed.contains(token.getAuthConfigId())) {
                    this.revokeAccessTokenDueToRemovalOfAuthConfig(token);
                }
            });
        }

        this.existingSecurityAuthConfigs = updatedSecurityAuthConfigs;
    }

    private void revokeAccessTokenDueToRemovalOfAuthConfig(AccessToken token) {
        String revokeReason = String.format("Revoked by GoCD: The authorization configuration '%s' referenced from the current access token is deleted from GoCD.", token.getAuthConfigId());
        this.accessTokenService.revokeAccessTokenByGoCD(token.getId(), revokeReason);
    }

    private List<String> getIds(SecurityAuthConfigs securityAuthConfigs) {
        return securityAuthConfigs.stream().map(PluginProfile::getId).collect(Collectors.toList());
    }
}
