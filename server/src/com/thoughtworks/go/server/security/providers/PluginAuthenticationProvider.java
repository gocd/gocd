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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PluginAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private AuthenticationPluginRegistry authenticationPluginRegistry;
    private AuthenticationExtension authenticationExtension;

    private final AuthorizationExtension authorizationExtension;
    private final AuthorizationMetadataStore store;
    private final AuthorityGranter authorityGranter;
    private GoConfigService configService;
    private PluginRoleService pluginRoleService;
    private UserService userService;

    @Autowired
    public PluginAuthenticationProvider(AuthenticationPluginRegistry authenticationPluginRegistry, AuthenticationExtension authenticationExtension,
                                        AuthorizationExtension authorizationExtension, AuthorityGranter authorityGranter, GoConfigService configService,
                                        PluginRoleService pluginRoleService, UserService userService) {
        this.authenticationPluginRegistry = authenticationPluginRegistry;
        this.authenticationExtension = authenticationExtension;
        this.authorizationExtension = authorizationExtension;
        this.store = AuthorizationMetadataStore.instance();
        this.authorityGranter = authorityGranter;
        this.configService = configService;
        this.pluginRoleService = pluginRoleService;
        this.userService = userService;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        assertPasswordNotBlank(authentication);
        boolean authenticatedUsingAuthorizationPlugin = true;
        User user = getUserDetailsFromAuthorizationPlugins(username, authentication);

        if (user == null) {
            user = getUserDetailsFromAuthenticationPlugins(username, authentication);
            authenticatedUsingAuthorizationPlugin = false;
        }

        if (user == null) {
            removeAnyAssociatedPluginRolesFor(username);
            throw new UsernameNotFoundException("Unable to authenticate user: " + username);
        }

        userService.addUserIfDoesNotExist(toDomainUser(user));
        GoUserPrinciple goUserPrinciple = getGoUserPrinciple(user, loginName(username, authentication), authenticatedUsingAuthorizationPlugin);
        return goUserPrinciple;
    }

    private void assertPasswordNotBlank(UsernamePasswordAuthenticationToken authentication) {
        String password = (String) authentication.getCredentials();

        if (StringUtils.isBlank(password)) {
            throw new BadCredentialsException("Empty Password");
        }
    }

    private com.thoughtworks.go.domain.User toDomainUser(User user) {
        return new com.thoughtworks.go.domain.User(user.getUsername(),user.getDisplayName(),user.getEmailId());
    }

    private void removeAnyAssociatedPluginRolesFor(String username) {
        pluginRoleService.revokeAllRolesFor(username);
    }

    private User getUserDetailsFromAuthenticationPlugins(String username, UsernamePasswordAuthenticationToken authentication) {
        Set<String> plugins = authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication();
        for (String pluginId : plugins) {
            try {
                LOGGER.debug("[Authenticate] Authenticating user: `{}` using the authentication plugin: `{}`", username, pluginId);
                String password = (String) authentication.getCredentials();
                User user = ensureDisplayNamePresent(authenticationExtension.authenticateUser(pluginId, username, password));
                if (user != null) {
                    LOGGER.debug("[Authenticate] Successfully authenticated user: `{}` using the authentication plugin: `{}`", username, pluginId);
                    return user;
                }
            } catch (Exception e) {
                LOGGER.error("[Authenticate] Error while authenticating user: `{}` using the authorization plugin: {} ", username, pluginId);
            }
        }
        return null;
    }

    private User getUserDetailsFromAuthorizationPlugins(String username, UsernamePasswordAuthenticationToken authentication) {
        String loginName = loginName(username, authentication);
        String password = (String) authentication.getCredentials();

        for (SecurityAuthConfig authConfig : configService.security().securityAuthConfigs()) {
            String pluginId = authConfig.getPluginId();

            if (!store.doesPluginSupportPasswordBasedAuthentication(pluginId)) {
                continue;
            }

            final List<PluginRoleConfig> roleConfigs = configService.security().getRoles().pluginRoleConfigsFor(authConfig.getId());

            try {
                LOGGER.debug("[Authenticate] Authenticating user: `{}` using the authorization plugin: `{}`", loginName, pluginId);
                AuthenticationResponse response = authorizationExtension.authenticateUser(pluginId, loginName, password, Collections.singletonList(authConfig), roleConfigs);
                User user = ensureDisplayNamePresent(response.getUser());
                if (user != null) {
                    pluginRoleService.updatePluginRoles(pluginId, user.getUsername(), CaseInsensitiveString.caseInsensitiveStrings(response.getRoles()));
                    LOGGER.debug("[Authenticate] Successfully authenticated user: `{}` using the authorization plugin: `{}`", loginName, pluginId);
                    return user;
                }
            } catch (Exception e) {
                LOGGER.error("[Authenticate] Error while authenticating user: `{}` using the authorization plugin: {} ", loginName, pluginId);
            }
            LOGGER.debug("[Authenticate] Authentication failed for user: `{}` using the authorization plugin: `{}`", loginName, pluginId);
        }
        return null;
    }

    private GoUserPrinciple getGoUserPrinciple(User user, String loginName, boolean requiresPeriodicReAuthentication) {
        return new GoUserPrinciple(user.getUsername(), user.getDisplayName(), "", true, true, true, true,
                authorityGranter.authorities(user.getUsername()), loginName, requiresPeriodicReAuthentication);
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

    private User ensureDisplayNamePresent(User user) {
        if (user == null) {
            return null;
        }

        if (isNotBlank(user.getDisplayName())) {
            return user;
        }

        return new User(user.getUsername(), user.getUsername(), user.getEmailId());
    }

    private String loginName(String username, UsernamePasswordAuthenticationToken authenticationToken) {
        Object principal = authenticationToken.getPrincipal();

        if (!(principal instanceof GoUserPrinciple)) {
            return username;
        }

        String loginName = ((GoUserPrinciple) principal).getLoginName();
        return isNotBlank(loginName) ? loginName : username;
    }
}
