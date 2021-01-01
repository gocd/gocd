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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.providers.AnonymousAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.FilterChain;

import static com.thoughtworks.go.server.security.GoAuthority.ROLE_ANONYMOUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AssumeAnonymousUserFilterTest {
    private AnonymousAuthenticationProvider anonymousAuthenticationProvider;
    private SecurityService securityService;
    private MockHttpServletRequest request;
    private FilterChain filterChain;
    private MockHttpServletResponse response;
    private TestingClock clock;

    @BeforeEach
    void setUp() {
        clock = new TestingClock();
        securityService = mock(SecurityService.class);
        anonymousAuthenticationProvider = new AnonymousAuthenticationProvider(clock, new AuthorityGranter(securityService));
        filterChain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
    }

    @Nested
    class SecurityEnabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(true);
        }


        @Test
        void shouldGiveAnonymousUserRoleAnonymousAuthorityWhenSecurityIsEnabledInCruiseConfig() throws Exception {
            new AssumeAnonymousUserFilter(securityService, anonymousAuthenticationProvider)
                    .doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SessionUtils.getAuthenticationToken(request).getCredentials()).isSameAs(AnonymousCredential.INSTANCE);
            assertThat(SessionUtils.getAuthenticationToken(request).getUser().getUsername()).isEqualTo("anonymous");
            assertThat(SessionUtils.getAuthenticationToken(request).getUser().getAuthorities())
                    .hasSize(1)
                    .contains(ROLE_ANONYMOUS.asAuthority());
        }
    }


    @Nested
    class SecurityDisabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(false);
        }

        @Test
        void shouldAlwaysGiveAnonymousUserRoleSupervisorAuthority() throws Exception {
            new AssumeAnonymousUserFilter(securityService, anonymousAuthenticationProvider)
                    .doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SessionUtils.getAuthenticationToken(request).getCredentials()).isSameAs(AnonymousCredential.INSTANCE);
            assertThat(SessionUtils.getAuthenticationToken(request).getUser().getUsername()).isEqualTo("anonymous");
            assertThat(SessionUtils.getAuthenticationToken(request).getUser().getAuthorities())
                    .hasSize(GoAuthority.values().length)
                    .containsExactly(GoAuthority.ALL_AUTHORITIES.toArray(new GrantedAuthority[0]));
        }
    }
}
