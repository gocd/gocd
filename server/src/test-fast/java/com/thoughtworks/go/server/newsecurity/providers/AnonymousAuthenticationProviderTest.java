/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnonymousAuthenticationProviderTest {
    private TestingClock clock;
    private SecurityService securityService;
    private AuthorityGranter authorityGranter;
    private AnonymousAuthenticationProvider anonymousAuthenticationProvider;

    @BeforeEach
    void setUp() {
        clock = new TestingClock();
        securityService = mock(SecurityService.class);
        authorityGranter = new AuthorityGranter(securityService);

        anonymousAuthenticationProvider = new AnonymousAuthenticationProvider(clock, authorityGranter);
    }

    @Nested
    class SecurityEnabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(true);
        }

        @Test
        void shouldAuthenticateRequest() {
            final AuthenticationToken<AnonymousCredential> authenticationToken = anonymousAuthenticationProvider.authenticate(null, null);

            assertThat(authenticationToken.getUser().getUsername()).isEqualTo("anonymous");
            assertThat(authenticationToken.getCredentials()).isEqualTo(AnonymousCredential.INSTANCE);
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(GoAuthority.ROLE_ANONYMOUS.asAuthority());
            assertThat(authenticationToken.getAuthConfigId()).isNull();
            assertThat(authenticationToken.getPluginId()).isNull();
        }

        @Test
        void shouldReAuthenticateRequest() {
            final AuthenticationToken<AnonymousCredential> authenticationToken = anonymousAuthenticationProvider.reauthenticate(null);

            assertThat(authenticationToken.getUser().getUsername()).isEqualTo("anonymous");
            assertThat(authenticationToken.getCredentials()).isEqualTo(AnonymousCredential.INSTANCE);
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(GoAuthority.ROLE_ANONYMOUS.asAuthority());
            assertThat(authenticationToken.getAuthConfigId()).isNull();
            assertThat(authenticationToken.getPluginId()).isNull();
        }
    }

    @Nested
    class SecurityDisabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(false);
        }

        @Test
        void shouldAuthenticateRequest() {
            final AuthenticationToken<AnonymousCredential> authenticationToken = anonymousAuthenticationProvider.authenticate(null, null);

            assertThat(authenticationToken.getUser().getUsername()).isEqualTo("anonymous");
            assertThat(authenticationToken.getCredentials()).isEqualTo(AnonymousCredential.INSTANCE);
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(GoAuthority.ALL_AUTHORITIES.toArray(new GrantedAuthority[0]));
            assertThat(authenticationToken.getAuthConfigId()).isNull();
            assertThat(authenticationToken.getPluginId()).isNull();
        }

        @Test
        void shouldReAuthenticateRequest() {
            final AuthenticationToken<AnonymousCredential> authenticationToken = anonymousAuthenticationProvider.reauthenticate(null);

            assertThat(authenticationToken.getUser().getUsername()).isEqualTo("anonymous");
            assertThat(authenticationToken.getCredentials()).isEqualTo(AnonymousCredential.INSTANCE);
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(GoAuthority.ALL_AUTHORITIES.toArray(new GrantedAuthority[0]));
            assertThat(authenticationToken.getAuthConfigId()).isNull();
            assertThat(authenticationToken.getPluginId()).isNull();
        }
    }
}
