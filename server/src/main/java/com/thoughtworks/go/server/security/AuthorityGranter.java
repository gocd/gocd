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
package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.thoughtworks.go.server.security.GoAuthority.ALL_AUTHORITIES;
import static com.thoughtworks.go.server.security.GoAuthority.ROLE_ANONYMOUS;

@Component
public class AuthorityGranter {
    private final SecurityService securityService;

    @Autowired
    public AuthorityGranter(SecurityService securityService) {
        this.securityService = securityService;
    }

    public Set<GrantedAuthority> authorities(String username) {
        if (username.equals("anonymous")) {
            return authoritiesForAnonymousUser();
        } else {
            return authoritiesBasedOnConfiguration(username);
        }
    }

    private Set<GrantedAuthority> authoritiesForAnonymousUser() {
        if (securityService.isSecurityEnabled()) {
            return anonymousOnlyAuthority();
        } else {
            return ALL_AUTHORITIES;
        }
    }

    private Set<GrantedAuthority> authoritiesBasedOnConfiguration(String username) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        checkAndAddSuperAdmin(username, authorities);
        checkAndAddGroupAdmin(username, authorities);
        checkAndAddTemplateAdmin(username, authorities);
        checkAndAddTemplateViewUser(username, authorities);
        authorities.add(GoAuthority.ROLE_USER.asAuthority());
        return authorities;
    }

    private Set<GrantedAuthority> anonymousOnlyAuthority() {
        return Collections.singleton(ROLE_ANONYMOUS.asAuthority());
    }

    private void checkAndAddTemplateAdmin(String username, Set<GrantedAuthority> authorities) {
        if (securityService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString(username)))) {
            authorities.add(GoAuthority.ROLE_TEMPLATE_SUPERVISOR.asAuthority());
        }
    }

    private void checkAndAddTemplateViewUser(String userName, Set<GrantedAuthority> authorities) {
        if (securityService.isAuthorizedToViewTemplates(new Username(userName))) {
            authorities.add(GoAuthority.ROLE_TEMPLATE_VIEW_USER.asAuthority());
        }
    }

    private void checkAndAddGroupAdmin(String username, Set<GrantedAuthority> authorities) {
        if (securityService.isUserGroupAdmin(new Username(new CaseInsensitiveString(username)))) {
            authorities.add(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        }
    }

    private void checkAndAddSuperAdmin(String username, Set<GrantedAuthority> authorities) {
        if (securityService.isUserAdmin(new Username(new CaseInsensitiveString(username)))) {
            authorities.add(GoAuthority.ROLE_SUPERVISOR.asAuthority());
        }
    }
}
