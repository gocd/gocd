/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands deletion of tokens on security config change
 */
@Service
public class OauthTokenSweeper implements ConfigChangedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(OauthTokenSweeper.class);
    private OauthRepository oauthRepo;
    private GoConfigService goConfigService;
    private CruiseConfig currentConfig;

    @Autowired
    public OauthTokenSweeper(OauthRepository oauthRepo, GoConfigService goConfigService) {
        this.oauthRepo = oauthRepo;
        this.goConfigService = goConfigService;
    }

    public void initialize() {
        goConfigService.register(this);
    }

    public void onConfigChange(CruiseConfig newConfig) {
        if (newConfig == null) {
            return;
        }
        if (this.currentConfig == null) {
            this.currentConfig = newConfig;
        }
        if (securityChanged(newConfig)) {
            oauthRepo.deleteAllOauthGrants();
            LOGGER.info("[Configuration Changed] Deleting all OAuth grants.");
        }
        this.currentConfig = newConfig;
    }

    private boolean securityChanged(CruiseConfig newConfig) {
        SecurityConfig currentSecurity = currentConfig.server().security();
        SecurityConfig newSecurity = newConfig.server().security();
        return (currentSecurity == null && newSecurity != null) ||
                (currentSecurity != null && currentSecurity.hasSecurityMethodChanged(newSecurity));
    }
}
