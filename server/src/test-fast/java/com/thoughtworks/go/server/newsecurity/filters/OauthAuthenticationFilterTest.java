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

package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.http.mocks.*;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.OAuthCredentials;
import com.thoughtworks.go.server.newsecurity.providers.OAuthAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.SecurityService;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
class OauthAuthenticationFilterTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private SecurityService securityService;
    private OAuthAuthenticationProvider authenticationProvider;
    private OauthAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        securityService = mock(SecurityService.class);
        authenticationProvider = mock(OAuthAuthenticationProvider.class);
        filter = new OauthAuthenticationFilter(securityService, authenticationProvider);
    }

    @Nested
    class SecurityDisabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(false);
        }

        @Test
        void shouldContinueWithChainWhenCredentialsAreNotProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/")
                    .build();
            final HttpSession originalSession = request.getSession(true);

            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();

            MockHttpServletRequestAssert.assertThat(request)
                    .hasSameSession(originalSession);

            MockHttpServletResponseAssert.assertThat(response)
                    .isOk();
        }

        @Test
        void shouldErrorOutWhenCredentialsAreProvided() throws ServletException, IOException {
            request = HttpRequestBuilder.GET("/")
                    .withOAuth("valid-token")
                    .build();
            final HttpSession originalSession = request.getSession(true);

            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();

            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(filterChain);

            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            MockHttpServletRequestAssert.assertThat(request)
                    .hasSameSession(originalSession);

            MockHttpServletResponseAssert.assertThat(response)
                    .isUnauthorized()
                    .hasBody("OAuth access token is not required, since security has been disabled on this server.");
        }
    }

    @Nested
    class SecurityEnabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(true);
        }

        @Test
        void shouldContinueExecutionOfFilterChainIfRequestDoesNotHaveOAuthToken() throws IOException, ServletException {
            request = new MockHttpServletRequest();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            MockHttpServletResponseAssert.assertThat(response)
                    .isOk();
        }

        @Test
        void shouldContinueFilterChainExecutionIfHeaderHasTokenButNotInExpectedFormat() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/")
                    .withHeader("Authorization", "Token token=butWithoutQuotes")
                    .build();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            MockHttpServletResponseAssert.assertThat(response)
                    .isOk();
        }


        @Test
        void shouldAuthenticateToken() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/")
                    .withOAuth("valid-token")
                    .build();

            final GoUserPrinciple goUserPrinciple = new GoUserPrinciple("bob", "Bob", GoAuthority.ROLE_SUPERVISOR.asAuthority());
            final OAuthCredentials oAuthCredentials = new OAuthCredentials("valid-token");
            final AuthenticationToken<OAuthCredentials> authenticationToken = new AuthenticationToken<>(goUserPrinciple, oAuthCredentials, null, System.currentTimeMillis(), null);

            when(authenticationProvider.authenticate(oAuthCredentials, null)).thenReturn(authenticationToken);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(authenticationToken);

            MockHttpServletResponseAssert.assertThat(response)
                    .isOk();
        }

        @Test
        void shouldContinueExecutingFilterChainEvenIfTokenAuthenticationFails() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/")
                    .withOAuth("invalid-token")
                    .build();

            final OAuthCredentials oAuthCredentials = new OAuthCredentials("invalid-token");

            when(authenticationProvider.authenticate(oAuthCredentials, null)).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(filterChain);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            MockHttpServletResponseAssert.assertThat(response)
                    .isUnauthorized()
                    .hasBody("Provided OAuth token is invalid.");
        }
    }
}
