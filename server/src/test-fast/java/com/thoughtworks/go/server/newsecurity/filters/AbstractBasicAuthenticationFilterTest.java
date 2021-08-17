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

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AbstractBasicAuthenticationFilterTest {

    private static final String BOB = "bob";
    private static final String PASSWORD = "p@ssw0rd";
    private TestingClock clock;
    private SecurityService securityService;
    private PasswordBasedPluginAuthenticationProvider authenticationProvider;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private AbstractBasicAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        clock = new TestingClock();
        securityService = mock(SecurityService.class);
        authenticationProvider = mock(PasswordBasedPluginAuthenticationProvider.class);
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        filter = spy(new AbstractBasicAuthenticationFilter(securityService, authenticationProvider) {
            @Override
            protected void onAuthenticationFailure(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   String errorMessage) throws IOException {

            }
        });
    }

    @AfterEach
    void tearDown() {
        SessionUtils.unsetCurrentUser();
    }

    private AuthenticationToken<UsernamePassword> createAuthentication(String username, String password,
                                                                       GrantedAuthority... grantedAuthorities) {
        final GoUserPrinciple goUserPrinciple = new GoUserPrinciple(username, username, grantedAuthorities);
        return new AuthenticationToken<>(goUserPrinciple,
                new UsernamePassword(username, password),
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
            request = HttpRequestBuilder.GET("/")
                    .build();
            final HttpSession originalSession = request.getSession(true);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();

            verify(filter, never()).onAuthenticationFailure(any(), any(), any());

        }

        @Test
        void shouldDisallowAccessWhenAnyCredentialsAreProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/api/blah")
                    .withBasicAuth(BOB, PASSWORD)
                    .build();

            final HttpSession originalSession = request.getSession(true);

            filter.doFilter(request, response, filterChain);

            verifyNoInteractions(filterChain);
            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();

            verify(filter).onAuthenticationFailure(request, response, "Basic authentication credentials are not required, since security has been disabled on this server.");
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
            request = HttpRequestBuilder.GET("/")
                    .build();

            final HttpSession originalSession = request.getSession(true);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();

            verify(filter, never()).onAuthenticationFailure(any(), any(), anyString());
        }

        @Test
        void shouldDisallowAccessWhenInvalidCredentialsAreProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/api/blah")
                    .withBasicAuth(BOB, PASSWORD)
                    .build();

            final HttpSession originalSession = request.getSession(true);

            when(authenticationProvider.authenticate(new UsernamePassword(BOB, PASSWORD), null)).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            verifyNoInteractions(filterChain);
            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();

            verify(filter).onAuthenticationFailure(request, response, "Invalid credentials. Either your username and password are incorrect, or there is a problem with your browser cookies. Please check with your administrator.");
        }

        @Test
        void shouldAllowAccessWhenGoodCredentialsAreProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/api/blah")
                    .withBasicAuth(BOB, PASSWORD)
                    .build();

            final HttpSession originalSession = request.getSession(true);

            final AuthenticationToken<UsernamePassword> authenticationToken = createAuthentication(BOB, PASSWORD);
            when(authenticationProvider.authenticate(new UsernamePassword(BOB, PASSWORD), null)).thenReturn(authenticationToken);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(request.getSession(false)).isNotSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isEqualTo(authenticationToken);

            verify(filter, never()).onAuthenticationFailure(any(), any(), any());

        }

        @Test
        void shouldNotAuthenticateWhenRequestIsPreviouslyAuthenticated() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/")
                    .withBasicAuth(BOB, PASSWORD)
                    .build();

            com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs(request, BOB, PASSWORD);
            final HttpSession originalSession = request.getSession(true);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(request.getSession(false)).isSameAs(originalSession);

            verify(authenticationProvider, never()).authenticate(any(), anyString());
            verify(filter, never()).onAuthenticationFailure(any(), any(), anyString());
        }
    }
}
