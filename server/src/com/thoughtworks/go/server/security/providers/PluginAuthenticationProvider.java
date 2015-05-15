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
import com.thoughtworks.go.plugin.access.authentication.model.User;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.AuthenticationException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.Set;

public class PluginAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    private AuthenticationPluginRegistry authenticationPluginRegistry;
    private AuthenticationExtension authenticationExtension;
    private final AuthorityGranter authorityGranter;

    @Autowired
    public PluginAuthenticationProvider(AuthenticationPluginRegistry authenticationPluginRegistry, AuthenticationExtension authenticationExtension, AuthorityGranter authorityGranter) {
        this.authenticationPluginRegistry = authenticationPluginRegistry;
        this.authenticationExtension = authenticationExtension;
        this.authorityGranter = authorityGranter;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        Set<String> plugins = authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication();
        for (String pluginId : plugins) {
            String password = (String) authentication.getCredentials();
            User user = authenticationExtension.authenticateUser(pluginId, username, password);
            if (user != null) {
                return new GoUserPrinciple(user.getUsername(), user.getDisplayName(), "", true, true, true, true, authorityGranter.authorities(username));
            }
        }
        throw new UsernameNotFoundException("Unable to authenticate user: " + username);
    }

    @Override
    public boolean supports(Class authentication) {
        return !authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication().isEmpty() && super.supports(authentication);
    }
}
