/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.models.AuthTokenCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.providers.AuthTokenBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.AuthTokenService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AuthTokenAuthenticationFilterTest {
    private static final String BOB = "bob";
    private static final String TOKEN = "857faf4b42fc9e324fb40b7223f2a94a";
    private TestingClock clock;
    private SecurityService securityService;
    private AuthTokenService authTokenService;
    private AuthTokenBasedPluginAuthenticationProvider authenticationProvider;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private AuthTokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        clock = new TestingClock();
        securityService = mock(SecurityService.class);
        authTokenService = mock(AuthTokenService.class);
        authenticationProvider = mock(AuthTokenBasedPluginAuthenticationProvider.class);
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        filter = new AuthTokenAuthenticationFilter(securityService, authTokenService, authenticationProvider);
    }

    @AfterEach
    void tearDown() {
        SessionUtils.unsetCurrentUser();
    }

    private AuthenticationToken<AuthTokenCredential> createAuthentication(String username,
                                                                          GrantedAuthority... grantedAuthorities) {
        final GoUserPrinciple goUserPrinciple = new GoUserPrinciple(username, username, grantedAuthorities);
        return new AuthenticationToken<>(goUserPrinciple,
                new AuthTokenCredential(username),
                null,
                clock.currentTimeMillis(),
                null);
    }

    @Nested
    class SecurityDisabled {

        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(false);
        }

        @Test
        void shouldAllowAccessWhenCredentialsAreNotProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/").build();

            final HttpSession originalSession = request.getSession(true);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void shouldDisallowAccessWhenAnyCredentialsAreProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/api/blah")
                    .withBearerAuth(TOKEN)
                    .build();

            final HttpSession originalSession = request.getSession(true);

            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(filterChain);
            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).isEqualTo("{\n  \"message\": \"Bearer authentication credentials are not required, since security has been disabled on this server.\"\n}");
        }
    }

    @Nested
    class SecurityEnabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(true);
        }

        @Test
        void shouldContinueChainWhenCredentialsAreNotProvided() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/api/foo").build();

            final HttpSession originalSession = request.getSession(true);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void shouldDisallowAccessWhenInvalidCredentialsAreProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/api/blah")
                    .withBearerAuth(TOKEN)
                    .build();

            final HttpSession originalSession = request.getSession(true);

            when(authenticationProvider.authenticate(new AuthTokenCredential(BOB), null)).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(filterChain);
            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).isEqualTo("{\n  \"message\": \"Invalid auth token credential\"\n}");
        }

        @Test
        void shouldAllowAccessWhenGoodCredentialsAreProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/api/blah")
                    .withBearerAuth(TOKEN)
                    .build();

            final HttpSession originalSession = request.getSession(true);
            final AuthenticationToken<AuthTokenCredential> authenticationToken = createAuthentication(BOB);

            when(authTokenService.getUsernameFromToken(TOKEN)).thenReturn(BOB);
            when(authenticationProvider.authenticate(new AuthTokenCredential(BOB), null)).thenReturn(authenticationToken);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(request.getSession(false)).isNotSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isEqualTo(authenticationToken);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void shouldNotAuthenticateWhenRequestIsPreviouslyAuthenticated() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/")
                    .withBearerAuth(TOKEN)
                    .build();

            com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs(request, BOB);
            final HttpSession originalSession = request.getSession(true);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(request.getSession(false)).isSameAs(originalSession);

            verify(authenticationProvider, never()).authenticate(any(), anyString());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
