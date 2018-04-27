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
import com.thoughtworks.go.util.TestingClock;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.ExpectedException;
import org.springframework.security.authentication.BadCredentialsException;

import static com.thoughtworks.go.server.security.GoAuthority.ROLE_OAUTH_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public class OAuthAuthenticationProviderTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private OauthDataSource oauthDataSource;
    private OAuthAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        oauthDataSource = mock(OauthDataSource.class);
        provider = new OAuthAuthenticationProvider(oauthDataSource, new TestingClock());
    }

    @Test
    void shouldAuthenticateUsingOAuthToken() {
        final OAuthCredentials credentials = new OAuthCredentials("some-token");
        final OauthDataSource.OauthTokenDTO tokenDTO = new OauthDataSource.OauthTokenDTO();
        tokenDTO.setUserId("bob");
        when(oauthDataSource.findOauthTokenByAccessToken(credentials.getOauthToken()))
                .thenReturn(tokenDTO);

        final AuthenticationToken<OAuthCredentials> authenticationToken = provider.authenticate(credentials, null);

        assertThat(authenticationToken.getCredentials()).isEqualTo(credentials);
        assertThat(authenticationToken.getUser().getUsername()).isEqualTo("bob");
        assertThat(authenticationToken.getUser().getDisplayName()).isEqualTo("bob");
        assertThat(authenticationToken.getUser().getAuthorities())
                .containsExactly(ROLE_OAUTH_USER.asAuthority());

        assertThat(authenticationToken.getPluginId()).isEqualTo(null);
        assertThat(authenticationToken.getAuthConfigId()).isEqualTo(null);
    }

    @Test
    void shouldErrorOutOAuthTokenForAccessTokenDoesNotExist() {
        thrown.expect(BadCredentialsException.class);
        thrown.expectMessage("Invalid OAuth token provided!");

        provider.authenticate(new OAuthCredentials("some-token"), null);
    }
}
