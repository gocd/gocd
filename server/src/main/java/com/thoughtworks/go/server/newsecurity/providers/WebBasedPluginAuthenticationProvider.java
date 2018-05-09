/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.newsecurity.providers;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.newsecurity.models.AccessToken;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

@Component
public class WebBasedPluginAuthenticationProvider extends AbstractPluginAuthenticationProvider<AccessToken> {
    private final AuthorizationExtension authorizationExtension;
    private final AuthorityGranter authorityGranter;
    private final UserService userService;
    private final Clock clock;
    private final AuthorizationMetadataStore store;

    @Autowired
    public WebBasedPluginAuthenticationProvider(AuthorizationExtension authorizationExtension,
                                                AuthorityGranter authorityGranter,
                                                GoConfigService goConfigService,
                                                PluginRoleService pluginRoleService,
                                                UserService userService,
                                                Clock clock) {
        super(goConfigService, pluginRoleService);
        this.authorizationExtension = authorizationExtension;
        this.authorityGranter = authorityGranter;
        this.userService = userService;
        this.clock = clock;
        this.store = AuthorizationMetadataStore.instance();
    }

    @Override
    protected String getUsername(AuthenticationToken<AccessToken> authenticationToken) {
        return authenticationToken.getUser().getUsername();
    }

    @Override
    protected List<SecurityAuthConfig> getSecurityAuthConfigsToAuthenticateWith(String pluginId) {
        return goConfigService.security().securityAuthConfigs().findByPluginId(pluginId);
    }

    @Override
    protected AuthenticationToken<AccessToken> authenticateUser(AccessToken accessToken,
                                                                SecurityAuthConfig authConfig) {
        String pluginId = authConfig.getPluginId();

        try {
            if (!store.doesPluginSupportWebBasedAuthentication(pluginId)) {
                return null;
            }

            final List<PluginRoleConfig> roleConfigs = goConfigService.security().getRoles().pluginRoleConfigsFor(authConfig.getId());
            LOGGER.debug("Authenticating user using the authorization plugin: `{}`", pluginId);
            AuthenticationResponse response = authorizationExtension.authenticateUser(pluginId, accessToken.getCredentials(), singletonList(authConfig), roleConfigs);
            com.thoughtworks.go.plugin.access.authorization.models.User user = ensureDisplayNamePresent(response.getUser());
            if (user != null) {
                userService.addUserIfDoesNotExist(toDomainUser(user));

                pluginRoleService.updatePluginRoles(pluginId, user.getUsername(), CaseInsensitiveString.caseInsensitiveStrings(response.getRoles()));
                LOGGER.debug("Successfully authenticated user: `{}` using the authorization plugin: `{}`", user.getUsername(), pluginId);

                final GoUserPrinciple goUserPrinciple = new GoUserPrinciple(user.getUsername(), user.getDisplayName(),
                        authorityGranter.authorities(user.getUsername()));

                return new AuthenticationToken<>(goUserPrinciple, accessToken, pluginId, clock.currentTimeMillis(), authConfig.getId());

            }
        } catch (Exception e) {
            LOGGER.error("Error while authenticating user using auth_config: {} with the authorization plugin: {} ", authConfig.getId(), pluginId);
        }
        LOGGER.debug("Authentication failed using the authorization plugin: `{}`", pluginId);
        return null;
    }


    private String getRootUrl(String string) {
        try {
            final URL url = new URL(string);
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), "").toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public AccessToken fetchAccessToken(String pluginId,
                                        Map<String, String> requestHeaders,
                                        Map<String, String> parameterMap) {
        return new AccessToken(authorizationExtension.fetchAccessToken(pluginId, requestHeaders, parameterMap, getAuthConfigs(pluginId)));
    }

    private List<SecurityAuthConfig> getAuthConfigs(String pluginId) {
        return goConfigService.security().securityAuthConfigs().findByPluginId(pluginId);
    }

    public String getAuthorizationServerUrl(String pluginId, String rootURL) {
        if (goConfigService.serverConfig().hasAnyUrlConfigured()) {
            rootURL = getRootUrl(goConfigService.serverConfig().getSiteUrlPreferablySecured().getUrl());
        }
        return authorizationExtension.getAuthorizationServerUrl(pluginId, getAuthConfigs(pluginId), rootURL);
    }

}
