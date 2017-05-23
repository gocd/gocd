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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.security.userdetails.memory.UserMap;
import org.springframework.security.userdetails.memory.UserMapEditor;

public class FileAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    private final GoConfigService goConfigService;
    private final AuthorityGranter authorityGranter;
    private final UserService userService;
    private final SecurityService securityService;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public FileAuthenticationProvider(GoConfigService goConfigService, AuthorityGranter authorityGranter, UserService userService, SecurityService securityService, SystemEnvironment systemEnvironment) {
        this.goConfigService = goConfigService;
        this.authorityGranter = authorityGranter;
        this.userService = userService;
        this.securityService = securityService;
        this.systemEnvironment = systemEnvironment;
    }

    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        final String plainTextPassword = (String) authentication.getCredentials();
        if (!userDetails.getPassword().equals(StringUtil.sha1Digest(plainTextPassword.getBytes()))) {
            throw new BadCredentialsException(
                    messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials",
                            "Bad credential"));
        }
    }

    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final String passwordFilePath = goConfigService.security().passwordFileConfig().path();
        try {
            UserMap userMap = UserMapEditor.addUsersFromProperties(new UserMap(),
                    addDummyRoleToPropertiesIfRequired(stripShaFromPasswordsIfRequired(loadPasswordFile(passwordFilePath))));
            final UserDetails details = userMap.getUser(username);
            return userStrippedOfAnyAuthoritiesSpecifiedInFile(username, details);
        } catch (IOException e) {
            throw new UsernameNotFoundException(
                    "Trying to authenticate user " + username + " but could not open file: " + passwordFilePath);
        }

    }

    private Properties loadPasswordFile(String passwordFilePath) throws IOException {
        final Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(passwordFilePath);
            properties.load(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return properties;
    }

    private User userStrippedOfAnyAuthoritiesSpecifiedInFile(String username, UserDetails details) {
        com.thoughtworks.go.domain.User user = userService.findUserByName(details.getUsername());
        String displayName = username;
        if (user != null && !user.getDisplayName().isEmpty()) {
            displayName = user.getDisplayName();
        }
        return new GoUserPrinciple(details.getUsername(), displayName, details.getPassword(), details.isEnabled(),
                details.isAccountNonExpired(), details.isCredentialsNonExpired(), details.isAccountNonLocked(),
                authorityGranter.authorities(username));
    }

    private Properties addDummyRoleToPropertiesIfRequired(Properties properties) {
        for (Object key : properties.keySet()) {
            String value = properties.getProperty(String.valueOf(key));
            if (!value.contains(",")) {
                properties.setProperty(String.valueOf(key), value + ",ROLE_USER");
            }
        }
        return properties;
    }

    private Properties stripShaFromPasswordsIfRequired(Properties properties) {
        for (Object key : properties.keySet()) {
            String value = properties.getProperty(String.valueOf(key));
            if (value.startsWith("{SHA}")) {
                properties.setProperty(String.valueOf(key), value.substring(5));
            }
        }
        return properties;
    }

    @Override public boolean supports(Class authentication) {
        return systemEnvironment.inbuiltLdapPasswordAuthEnabled() && isPasswordFileConfigured() && super.supports(authentication);
    }

    private boolean isPasswordFileConfigured() {
        return goConfigService.security().passwordFileConfig().isEnabled();
    }
}
