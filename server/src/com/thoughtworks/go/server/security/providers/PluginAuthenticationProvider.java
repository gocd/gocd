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

import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.Set;

public class PluginAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    private final AuthorityGranter authorityGranter;
    private AuthenticationPluginRegistry authenticationPluginRegistry;
    private AuthenticationExtension authenticationExtension;

    @Autowired
    public PluginAuthenticationProvider(AuthorityGranter authorityGranter, AuthenticationPluginRegistry authenticationPluginRegistry, AuthenticationExtension authenticationExtension) {
        this.authorityGranter = authorityGranter;
        this.authenticationPluginRegistry = authenticationPluginRegistry;
        this.authenticationExtension = authenticationExtension;
    }

    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        Set<String> plugins = authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication();
        boolean isAuthenticated = false;
        for (String pluginId : plugins) {
            Result result = authenticationExtension.authenticateUser(pluginId, userDetails.getUsername(), (String) authentication.getCredentials());
            if (result.isSuccessful()) {
                isAuthenticated = true;
                break;
            }
        }
        if (!isAuthenticated) {
            throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credential"));
        }
    }

    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        Set<String> plugins = authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication();
        for (String pluginId : plugins) {
            Result result = authenticationExtension.authenticateUser(pluginId, username, (String) authentication.getCredentials());
            if (result.isSuccessful()) {
                com.thoughtworks.go.plugin.access.authentication.model.User user = authenticationExtension.getUserDetails(pluginId, username);
                return new GoUserPrinciple(user.getUsername(), String.format("%s %s", user.getFirstName(), user.getLastName()), "", true, true, true, true, authorityGranter.authorities(username));
            }
        }
        throw new UsernameNotFoundException("Trying to authenticate user " + username + " but could not find user.");
    }
}
