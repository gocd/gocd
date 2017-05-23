/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ldap.LdapAuthoritiesPopulator;

public class LdapAuthenticationProvider extends org.springframework.security.providers.ldap.LdapAuthenticationProvider {
    private final GoConfigService goConfigService;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public LdapAuthenticationProvider(GoConfigService goConfigService, org.springframework.security.providers.ldap.LdapAuthenticator authenticator, LdapAuthoritiesPopulator authoritiesPopulator, SystemEnvironment systemEnvironment) {
        super(authenticator, authoritiesPopulator);
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
    }

    public boolean supports(Class authentication) {
        SecurityConfig securityConfig = goConfigService.security();
        if (!securityConfig.isSecurityEnabled() || !securityConfig.ldapConfig().isEnabled() || !systemEnvironment.inbuiltLdapPasswordAuthEnabled()) {
            return false;
        }
        return super.supports(authentication);
    }
}
