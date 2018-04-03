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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.server.oauth.OauthDataSource;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.OauthAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class OauthAuthenticationProvider implements AuthenticationProvider {
    private final OauthDataSource oauthDataSource;

    @Autowired
    public OauthAuthenticationProvider(OauthDataSource oauthDataSource) {
        this.oauthDataSource = oauthDataSource;
    }

    public OauthAuthenticationToken authenticate(Authentication authentication) throws AuthenticationException {
        OauthAuthenticationToken authenticationToken = (OauthAuthenticationToken) authentication;
        String token = authenticationToken.getCredentials();
        OauthDataSource.OauthTokenDTO oauthToken = oauthDataSource.findOauthTokenByAccessToken(token);
        if (oauthToken == null) {
            throw new BadCredentialsException("No match for OAuth token: " + token);
        }
        String username = oauthToken.getUserId();
        UserDetails user = new User(username, token, true, true, true, true, Collections.singletonList(GoAuthority.ROLE_OAUTH_USER.asAuthority()));

        return new OauthAuthenticationToken(user);
    }

    public boolean supports(Class authentication) {
        return authentication.isAssignableFrom(OauthAuthenticationToken.class);
    }
}
