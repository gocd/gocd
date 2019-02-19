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

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.exceptions.InvalidAccessTokenException;
import com.thoughtworks.go.server.newsecurity.models.AccessTokenCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.providers.AccessTokenBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.AccessTokenService;
import com.thoughtworks.go.server.service.SecurityAuthConfigService;
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

import static com.thoughtworks.go.helper.AccessTokenMother.randomAccessTokenForUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AccessTokenAuthenticationFilterTest {
    private static final String BOB = "bob";
    private static final String TOKEN = "857faf4b42fc9e324fb40b7223f2a94a";
    private static final String PLUGIN_ID = "plugin1";
    private TestingClock clock;
    private SecurityService securityService;
    private AccessTokenService accessTokenService;
    private AccessTokenBasedPluginAuthenticationProvider authenticationProvider;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private AccessTokenAuthenticationFilter filter;
    private SecurityAuthConfigService securityAuthConfigService;
    private AccessToken accessToken;
    private SecurityAuthConfig authConfig;

    @BeforeEach
    void setUp() throws Exception {
        clock = new TestingClock();
        securityService = mock(SecurityService.class);
        accessTokenService = mock(AccessTokenService.class);
        authenticationProvider = mock(AccessTokenBasedPluginAuthenticationProvider.class);
        securityAuthConfigService = mock(SecurityAuthConfigService.class);
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        filter = new AccessTokenAuthenticationFilter(securityService, accessTokenService, securityAuthConfigService, authenticationProvider);
        accessToken = randomAccessTokenForUser(BOB);
        when(accessTokenService.findByAccessToken(TOKEN)).thenReturn(accessToken);
        authConfig = new SecurityAuthConfig(accessToken.getAuthConfigId(), PLUGIN_ID);
        when(securityAuthConfigService.findProfile(accessToken.getAuthConfigId())).thenReturn(authConfig);
    }

    @AfterEach
    void tearDown() {
        SessionUtils.unsetCurrentUser();
    }

    private AuthenticationToken<AccessTokenCredential> createAuthentication(String username, GrantedAuthority... grantedAuthorities) {
        final GoUserPrinciple goUserPrinciple = new GoUserPrinciple(username, username, grantedAuthorities);
        return new AuthenticationToken<>(goUserPrinciple, new AccessTokenCredential(accessToken), null, clock.currentTimeMillis(), null);
    }

    @Nested
    class SecurityDisabled {

        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(false);
        }

        @AfterEach
        void tearDown() {
            verify(accessTokenService, never()).updateLastUsedCacheWith(any());
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
            verifyZeroInteractions(accessTokenService);
            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void shouldDisallowAccessWhenInvalidCredentialsAreProvided() throws Exception {
            request = HttpRequestBuilder.GET("/api/blah")
                    .withBearerAuth(TOKEN)
                    .build();

            final HttpSession originalSession = request.getSession(true);

            when(accessTokenService.findByAccessToken(TOKEN)).thenThrow(new InvalidAccessTokenException());

            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(filterChain);
            verify(accessTokenService, never()).updateLastUsedCacheWith(any());
            assertThat(request.getSession(false)).isSameAs(originalSession);
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).isEqualTo("{\n  \"message\": \"Invalid Personal Access Token.\"\n}");
        }

        @Test
        void shouldAllowAccessWhenGoodCredentialsAreProvided() throws Exception {
            request = HttpRequestBuilder.GET("/api/blah")
                    .withBearerAuth(TOKEN)
                    .build();

            final HttpSession originalSession = request.getSession(true);
            final AuthenticationToken<AccessTokenCredential> authenticationToken = createAuthentication(BOB);

            when(authenticationProvider.authenticateUser(new AccessTokenCredential(accessToken), authConfig)).thenReturn(authenticationToken);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(accessTokenService).updateLastUsedCacheWith(accessToken);
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
            verifyZeroInteractions(accessTokenService);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
