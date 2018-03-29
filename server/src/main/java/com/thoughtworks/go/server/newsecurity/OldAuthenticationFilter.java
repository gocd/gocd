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

package com.thoughtworks.go.server.newsecurity;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Component
public class OldAuthenticationFilter extends OncePerRequestFilter {

    private static final String CURRENT_USER = "CURRENT_USER";
    private static final String ANONYMOUS_AUTHENTICATION_KEY = UUID.randomUUID().toString();

    private final LoginHandler loginHandler = new LoginHandler();

    @Autowired
    private final NewPluginAuthenticationProvider pluginAuthenticationProvider;
    @Autowired
    private final GoConfigService goConfigService;

    public OldAuthenticationFilter(NewPluginAuthenticationProvider pluginAuthenticationProvider, GoConfigService goConfigService) {
        this.pluginAuthenticationProvider = pluginAuthenticationProvider;
        this.goConfigService = goConfigService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute(CURRENT_USER);

        if (currentUser == null) {
            currentUser = verifyX509Cert(request);
        }

        if (currentUser == null && goConfigService.isSecurityEnabled()) {
            currentUser = verifyBasicAuthCredentials(request);

            if (currentUser == null) {
                currentUser = verifyOauthCredentials(request);
            }

            if (currentUser == null && supportsAnonymous(request)) {
                currentUser = new GoUserPrinciple("anonymous", "Anonymous", "", singletonList(GoAuthority.ROLE_ANONYMOUS.asAuthority()), "anonymous");
            }

        } else {
            currentUser = new GoUserPrinciple("anonymous", "Anonymous", "", singletonList(GoAuthority.ROLE_SUPERVISOR.asAuthority()), "anonymous");
        }

        if (currentUser == null) {
            loginHandler.handle(request, response);
        } else {
            session.setAttribute(CURRENT_USER, currentUser);
            filterChain.doFilter(request, response);
        }
    }

    private User verifyBasicAuthCredentials(HttpServletRequest request) {
        if (!pluginAuthenticationProvider.hasPluginsForUsernamePasswordAuth()) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (isBlank(header)) {
            return null;
        }

        final Pattern pattern = Pattern.compile("basic (.*)", Pattern.CASE_INSENSITIVE);

        final Matcher matcher = pattern.matcher(header);
        if (matcher.matches()) {
            final String encodedCredentials = matcher.group(1);
            final byte[] decode = Base64.getDecoder().decode(encodedCredentials);
            String decodedCredentials = new String(decode, StandardCharsets.UTF_8);

            final int indexOfSeparator = decodedCredentials.indexOf(':');
            if (indexOfSeparator == -1) {
                throw new BadCredentialsException("Invalid basic authentication token");
            }

            final String username = decodedCredentials.substring(0, indexOfSeparator);
            final String password = decodedCredentials.substring(indexOfSeparator + 1);

            return pluginAuthenticationProvider.authenticate(username, password);
        }
    }

    private boolean supportsAnonymous(HttpServletRequest request) {
        return new AntPathRequestMatcher("/cctray.xml").matches(request);
    }

    private class LoginHandler {
        public void handle(HttpServletRequest request, HttpServletResponse response) {

        }
    }

    @Component
    public static class NewPluginAuthenticationProvider {
        private static final Logger LOGGER = LoggerFactory.getLogger(NewPluginAuthenticationProvider.class);

        private final AuthorizationExtension authorizationExtension;
        private final AuthorityGranter authorityGranter;
        private final GoConfigService configService;
        private final PluginRoleService pluginRoleService;
        private final UserService userService;
        private final AuthorizationMetadataStore store;

        @Autowired
        public NewPluginAuthenticationProvider(AuthorizationExtension authorizationExtension, AuthorityGranter authorityGranter, GoConfigService configService,
                                               PluginRoleService pluginRoleService, UserService userService) {
            this.authorizationExtension = authorizationExtension;
            this.authorityGranter = authorityGranter;
            this.configService = configService;
            this.pluginRoleService = pluginRoleService;
            this.userService = userService;
            this.store = AuthorizationMetadataStore.instance();
        }

        public boolean hasPluginsForUsernamePasswordAuth() {
            return true;
        }

        public User authenticate(String username, String password) {
            if (StringUtils.isBlank(password)) {
                throw new BadCredentialsException("Empty Password");
            }

            com.thoughtworks.go.plugin.access.authorization.models.User user = getUserDetailsFromAuthorizationPlugins(username, password);

            if (user == null) {
                removeAnyAssociatedPluginRolesFor(username);
                throw new UsernameNotFoundException("Unable to authenticate user: " + username);
            }

            userService.addUserIfDoesNotExist(toDomainUser(user));
            return new GoUserPrinciple(user.getUsername(), user.getDisplayName(), "",
                    authorityGranter.authorities(user.getUsername()), user.getUsername());
        }

        private com.thoughtworks.go.domain.User toDomainUser(com.thoughtworks.go.plugin.access.authorization.models.User user) {
            return new com.thoughtworks.go.domain.User(user.getUsername(), user.getDisplayName(), user.getEmailId());
        }

        private void removeAnyAssociatedPluginRolesFor(String username) {
            pluginRoleService.revokeAllRolesFor(username);
        }

        private com.thoughtworks.go.plugin.access.authorization.models.User getUserDetailsFromAuthorizationPlugins(String username, String password) {
            for (SecurityAuthConfig authConfig : configService.security().securityAuthConfigs()) {
                String pluginId = authConfig.getPluginId();

                if (!store.doesPluginSupportPasswordBasedAuthentication(pluginId)) {
                    continue;
                }

                final List<PluginRoleConfig> roleConfigs = configService.security().getRoles().pluginRoleConfigsFor(authConfig.getId());

                try {
                    LOGGER.debug("[Authenticate] Authenticating user: `{}` using the authorization plugin: `{}`", username, pluginId);
                    AuthenticationResponse response = authorizationExtension.authenticateUser(pluginId, username, password, singletonList(authConfig), roleConfigs);
                    com.thoughtworks.go.plugin.access.authorization.models.User user = ensureDisplayNamePresent(response.getUser());
                    if (user != null) {
                        pluginRoleService.updatePluginRoles(pluginId, user.getUsername(), CaseInsensitiveString.caseInsensitiveStrings(response.getRoles()));
                        LOGGER.debug("[Authenticate] Successfully authenticated user: `{}` using the authorization plugin: `{}`", username, pluginId);
                        return user;
                    }
                } catch (Exception e) {
                    LOGGER.error("[Authenticate] Error while authenticating user: `{}` using the authorization plugin: {} ", username, pluginId);
                }
                LOGGER.debug("[Authenticate] Authentication failed for user: `{}` using the authorization plugin: `{}`", username, pluginId);
            }
            return null;
        }


        private com.thoughtworks.go.plugin.access.authorization.models.User ensureDisplayNamePresent(com.thoughtworks.go.plugin.access.authorization.models.User user) {
            if (user == null) {
                return null;
            }

            if (isNotBlank(user.getDisplayName())) {
                return user;
            }

            return new com.thoughtworks.go.plugin.access.authorization.models.User(user.getUsername(), user.getUsername(), user.getEmailId());
        }
    }
}
