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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleUser;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.AuthenticationException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PluginAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private AuthenticationPluginRegistry authenticationPluginRegistry;
    private AuthenticationExtension authenticationExtension;

    private final AuthorizationExtension authorizationExtension;
    private final AuthorizationPluginConfigMetadataStore store;
    private final AuthorityGranter authorityGranter;
    private GoConfigService configService;

    @Autowired
    public PluginAuthenticationProvider(AuthenticationPluginRegistry authenticationPluginRegistry, AuthenticationExtension authenticationExtension, AuthorizationExtension authorizationExtension, AuthorizationPluginConfigMetadataStore store, AuthorityGranter authorityGranter, GoConfigService configService) {
        this.authenticationPluginRegistry = authenticationPluginRegistry;
        this.authenticationExtension = authenticationExtension;
        this.authorizationExtension = authorizationExtension;
        this.store = store;
        this.authorityGranter = authorityGranter;
        this.configService = configService;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        UserDetails user = getUserDetailsFromAuthorizationPlugins(username, authentication);
        if (user != null) return user;

        user = getUserDetailsFromAuthenticationPlugins(username, authentication);
        if (user != null) return user;

        throw new UsernameNotFoundException("Unable to authenticate user: " + username);
    }

    private UserDetails getUserDetailsFromAuthenticationPlugins(String username, UsernamePasswordAuthenticationToken authentication) {
        Set<String> plugins = authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication();
        for (String pluginId : plugins) {
            String password = (String) authentication.getCredentials();
            User user = ensureDisplayNamePresent(authenticationExtension.authenticateUser(pluginId, username, password));
            if (user != null) {
                return getGoUserPrinciple(user);
            }
        }
        return null;
    }

    private UserDetails getUserDetailsFromAuthorizationPlugins(String username, UsernamePasswordAuthenticationToken authentication) {
        Set<String> plugins = store.getPluginsThatSupportsPasswordBasedAuthentication();
        for (String pluginId : plugins) {
            String password = (String) authentication.getCredentials();
            AuthenticationResponse response = authorizationExtension.authenticateUser(pluginId, username, password);
            User user = ensureDisplayNamePresent(response.getUser());
            if (user != null) {
                List<String> roleNames = response.getRoles();
                addUserToRoles(pluginId, user, roleNames);
                return getGoUserPrinciple(user);
            }
        }
        return null;
    }

    private GoUserPrinciple getGoUserPrinciple(User user) {
        return new GoUserPrinciple(user.getUsername(), user.getDisplayName(), "", true, true, true, true, authorityGranter.authorities(user.getUsername()));
    }

    @Override
    public boolean supports(Class authentication) {
        if (store.getPluginsThatSupportsPasswordBasedAuthentication().size() > 0) {
            return super.supports(authentication);
        }

        if (authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication().size() > 0) {
            return super.supports(authentication);
        }

        return false;
    }


    private void addUserToRoles(String pluginId, User user, List<String> roleNames) {
        for (String roleName : roleNames) {
            Role role = configService.security().getRoles().findPluggableRole(pluginId, roleName);
            if (role == null) {
                LOGGER.error("Role {} does not exists.", role);
                continue;
            }

            if (!role.hasMember(new CaseInsensitiveString(user.getUsername()))) {
                role.addUser(new RoleUser(user.getUsername()));
            }
        }
    }


    private User ensureDisplayNamePresent(User user) {
        if (user == null) {
            return null;
        }

        if (isNotBlank(user.getDisplayName())) {
            return user;
        }

        return new User(user.getUsername(), user.getUsername(), user.getEmailId());
    }
}
