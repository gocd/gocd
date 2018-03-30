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

package com.thoughtworks.go.server.newsecurity.authentication.providers;

import com.thoughtworks.go.server.oauth.OauthDataSource;
import com.thoughtworks.go.server.security.GoAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OAuthAuthenticationProvider {
    private static final List<GrantedAuthority> GRANTED_AUTHORITY_LIST = Collections.singletonList(GoAuthority.ROLE_OAUTH_USER.asAuthority());
    private final OauthDataSource oauthDataSource;

    @Autowired
    public OAuthAuthenticationProvider(OauthDataSource oauthDataSource) {
        this.oauthDataSource = oauthDataSource;
    }

    public User authenticate(String accessToken) throws AuthenticationException {
        OauthDataSource.OauthTokenDTO oauthToken = oauthDataSource.findOauthTokenByAccessToken(accessToken);

        if (oauthToken == null) {
            throw new BadCredentialsException(String.format("[OAuth Authentication Failure] No match for OAuth token: %s", accessToken));
        }

        String username = oauthToken.getUserId();
        return new User(username, accessToken, GRANTED_AUTHORITY_LIST);
    }
}
