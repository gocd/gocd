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

import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.OauthAuthenticationToken;
import com.thoughtworks.go.server.oauth.OauthDataSource;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        UserDetails user = new User(username, token, true, true, true, true, oauthAuthority());

        return new OauthAuthenticationToken(user);
    }

    private GrantedAuthority[] oauthAuthority() {
        return new GrantedAuthority[] { GoAuthority.ROLE_OAUTH_USER.asAuthority() };
    }

    public boolean supports(Class authentication) {
        return authentication.isAssignableFrom(OauthAuthenticationToken.class);
    }
}
