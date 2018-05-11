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


import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.OAuthCredentials;
import com.thoughtworks.go.server.oauth.OauthDataSource;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class OAuthAuthenticationProvider extends AbstractAuthenticationProvider<OAuthCredentials> {
    private final OauthDataSource oauthDataSource;
    private final Clock clock;

    @Autowired
    public OAuthAuthenticationProvider(OauthDataSource oauthDataSource, Clock clock) {
        this.oauthDataSource = oauthDataSource;
        this.clock = clock;
    }

    @Override
    public AuthenticationToken<OAuthCredentials> reauthenticate(AuthenticationToken<OAuthCredentials> authenticationToken) {
        return authenticate(authenticationToken.getCredentials(), null);
    }

    @Override
    public AuthenticationToken<OAuthCredentials> authenticate(OAuthCredentials credentials, String pluginId) {
        OauthDataSource.OauthTokenDTO oauthToken = oauthDataSource.findOauthTokenByAccessToken(credentials.getOauthToken());
        if (oauthToken == null) {
            throw new BadCredentialsException("Invalid OAuth token provided!");
        }

        GoUserPrinciple user = new GoUserPrinciple(oauthToken.getUserId(), oauthToken.getUserId(), oauthAuthority());
        return new AuthenticationToken<>(user, credentials, null, clock.currentTimeMillis(), null);
    }

    private GrantedAuthority[] oauthAuthority() {
        return new GrantedAuthority[]{GoAuthority.ROLE_OAUTH_USER.asAuthority()};
    }
}
