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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @understands notifies when ldap configurations changes
 */
public class LdapConfigChangedListener implements ConfigChangedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigChangedListener.class);
    private LdapConfig currentLdapConfig;
    private final LdapContextFactory ldapContextFactory;

    public LdapConfigChangedListener(LdapConfig ldapConfig, LdapContextFactory ldapContextFactory) {
        this.currentLdapConfig = ldapConfig;
        this.ldapContextFactory = ldapContextFactory;
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        LdapConfig newLdapConfig = newCruiseConfig.server().security().ldapConfig();
        if (!currentLdapConfig.equals(newLdapConfig)) {
            ldapContextFactory.initializeDelegator();
            currentLdapConfig = newLdapConfig;
            LOGGER.info("[Configuration Changed] LDAP configuration changed.");
        }
    }
}
