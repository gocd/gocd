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
package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert;
import com.thoughtworks.go.server.newsecurity.filters.*;
import com.thoughtworks.go.server.newsecurity.handlers.BasicAuthenticationWithChallengeFailureResponseHandler;
import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.providers.AccessTokenBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.AnonymousAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static com.thoughtworks.go.helper.AccessTokenMother.randomAccessTokenForUser;
import static com.thoughtworks.go.server.newsecurity.filterchains.DenyGoCDAccessForArtifactsFilterChainTest.wrap;
import static com.thoughtworks.go.server.newsecurity.filters.InvalidateAuthenticationOnSecurityConfigChangeFilter.SECURITY_CONFIG_LAST_CHANGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AuthenticationFilterChainTest {

    private MockHttpServletResponse response;
    private MockHttpServletRequest request;
    private FilterChain filterChain;
    private SecurityService securityService;
    private TestingClock clock;
    private SystemEnvironment systemEnvironment;
    private AssumeAnonymousUserFilter assumeAnonymousUserFilter;

    @BeforeEach
    void setUp() throws IOException {
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        securityService = mock(SecurityService.class);

        clock = new TestingClock();
        systemEnvironment = new SystemEnvironment();

        final AnonymousAuthenticationProvider anonymousAuthenticationProvider = new AnonymousAuthenticationProvider(clock, new AuthorityGranter(securityService));
        assumeAnonymousUserFilter = new AssumeAnonymousUserFilter(securityService, anonymousAuthenticationProvider);
    }


    @Nested
    class SecurityEnabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(true);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/remoting/blah"})
        void shouldErrorOutWithStatusCode403WhenNoAgentTokenProvidedInRequest(String url) throws IOException, ServletException {
            request = HttpRequestBuilder.GET(url)
                    .build();

            final AgentAuthenticationFilter agentAuthenticationFilter = new AgentAuthenticationFilter(null, clock, null);
            final AuthenticationFilterChain authenticationFilterChain = new AuthenticationFilterChain(agentAuthenticationFilter, null, null, null, null, null, null, null);

            authenticationFilterChain.doFilter(request, response, filterChain);

            verifyNoInteractions(filterChain);
            MockHttpServletResponseAssert.assertThat(response).isForbidden();
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/config-repository.git/git-upload-something", "/cctray.xml", "/api/foo", "/blah"})
        void shouldInvalidateAuthenticationIfSecurityConfigIsChanged(String url) throws IOException, ServletException {

            request = HttpRequestBuilder.GET(url)
                    .withRequestedSessionIdFromSession()
                    .build();

            final AuthenticationToken authenticationToken = mock(AuthenticationToken.class);
            SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);

            request.getSession(false).setAttribute(SECURITY_CONFIG_LAST_CHANGE, clock.currentTimeMillis());

            final InvalidateAuthenticationOnSecurityConfigChangeFilter invalidateAuthenticationOnSecurityConfigChangeFilter = new InvalidateAuthenticationOnSecurityConfigChangeFilter(
                    mock(GoConfigService.class), clock, mock(AuthorizationExtensionCacheService.class), mock(PluginRoleService.class));

            clock.addSeconds(1000);
            invalidateAuthenticationOnSecurityConfigChangeFilter.onPluginRoleChange();

            final Filter filter = mock(Filter.class);

            final AuthenticationFilterChain authenticationFilterChain = new AuthenticationFilterChain(null, invalidateAuthenticationOnSecurityConfigChangeFilter, filter, filter, null, null, null, assumeAnonymousUserFilter);

            authenticationFilterChain.doFilter(request, response, filterChain);

            verify(filter).doFilter(wrap(request), wrap(response), any());
            verify(authenticationToken).invalidate();
            MockHttpServletResponseAssert.assertThat(response).isOk();
            assertThat(SessionUtils.getAuthenticationToken(request)).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/config-repository.git/git-upload-something", "/cctray.xml", "/api/foo", "/blah"})
        void shouldReauthenticateIfAuthenticationTokenIsInvalid(String url) throws IOException, ServletException {
            request = HttpRequestBuilder.GET(url)
                    .build();

            final UsernamePassword credentials = new UsernamePassword("bob", "p@ssword");
            final AuthenticationToken<UsernamePassword> authenticationToken = new AuthenticationToken<>(null, credentials, null, 0, null);
            SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);

            final PasswordBasedPluginAuthenticationProvider pluginAuthenticationProvider = mock(PasswordBasedPluginAuthenticationProvider.class);

            final AuthenticationToken reauthenticatedToken = mock(AuthenticationToken.class);
            when(pluginAuthenticationProvider.reauthenticate(authenticationToken)).thenReturn(reauthenticatedToken);

            new AuthenticationFilterChain(null, new NoOpFilter(),
                    new ReAuthenticationWithRedirectToLoginFilter(securityService, systemEnvironment, clock, pluginAuthenticationProvider, null, null),
                    new ReAuthenticationWithChallengeFilter(securityService, systemEnvironment, clock, mock(BasicAuthenticationWithChallengeFailureResponseHandler.class), pluginAuthenticationProvider, null, null),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    assumeAnonymousUserFilter)
                    .doFilter(request, response, filterChain);

            verify(filterChain).doFilter(wrap(request), wrap(response));
            MockHttpServletResponseAssert.assertThat(response).isOk();
            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(reauthenticatedToken);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/add-on/blah", "/api/webhooks/bitbucket/notify", "/api/webhooks/github/notify", "/api/webhooks/foo/notify"})
        void shouldAllowAnonymousAccessForWebhookAndAddonApis(String url) throws IOException, ServletException {
            request = HttpRequestBuilder.GET(url)
                    .build();

            new AuthenticationFilterChain(null, null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    assumeAnonymousUserFilter)
                    .doFilter(request, response, filterChain);

            verify(filterChain).doFilter(wrap(request), wrap(response));
            MockHttpServletResponseAssert.assertThat(response).isOk();
            assertThat(SessionUtils.getAuthenticationToken(request).getCredentials()).isSameAs(AnonymousCredential.INSTANCE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/config-repository.git/git-upload-something", "/cctray.xml", "/api/foo", "/blah"})
        void shouldAuthenticateUsingBasicAuthForAllCalls(String url) throws IOException, ServletException {
            request = HttpRequestBuilder.GET(url)
                    .withBasicAuth("bob", "password")
                    .build();

            HttpSession originalSession = request.getSession();

            final PasswordBasedPluginAuthenticationProvider pluginAuthenticationProvider = mock(PasswordBasedPluginAuthenticationProvider.class);

            final AuthenticationToken<UsernamePassword> authenticationToken = mock(AuthenticationToken.class);
            when(pluginAuthenticationProvider.authenticate(new UsernamePassword("bob", "password"), null)).thenReturn(authenticationToken);

            new AuthenticationFilterChain(null,
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new BasicAuthenticationWithChallengeFilter(securityService, new BasicAuthenticationWithChallengeFailureResponseHandler(securityService), pluginAuthenticationProvider),
                    new BasicAuthenticationWithRedirectToLoginFilter(securityService, pluginAuthenticationProvider), new NoOpFilter(),
                    assumeAnonymousUserFilter).doFilter(request, response, filterChain);

            verify(filterChain).doFilter(wrap(request), wrap(response));
            MockHttpServletResponseAssert.assertThat(response).isOk();
            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(authenticationToken);
            assertThat(request.getSession(false)).isNotSameAs(originalSession);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/cctray.xml", "/api/foo", "/api/blah", "/files/some-artifact", "/properties/moo"})
        void shouldAuthenticateUsingAccessTokenAuthenticationFilter(String url) throws IOException, ServletException {
            final AccessTokenService accessTokenService = mock(AccessTokenService.class);
            final SecurityAuthConfigService securityAuthConfigService = mock(SecurityAuthConfigService.class);
            final AccessTokenBasedPluginAuthenticationProvider provider = mock(AccessTokenBasedPluginAuthenticationProvider.class);
            final AccessToken accessToken = randomAccessTokenForUser("bob");
            request = HttpRequestBuilder.GET(url)
                    .withBearerAuth("some-access-token")
                    .build();

            when(securityAuthConfigService.findProfile(anyString())).thenReturn(new SecurityAuthConfig());
            HttpSession originalSession = request.getSession();
            when(accessTokenService.findByAccessToken("some-access-token")).thenReturn(accessToken);
            final AuthenticationToken authenticationToken = mock(AuthenticationToken.class);
            when(provider.authenticateUser(any(), any())).thenReturn(authenticationToken);
            final AccessTokenAuthenticationFilter accessTokenAuthenticationFilter = new AccessTokenAuthenticationFilter(securityService, accessTokenService, securityAuthConfigService, provider);

            new AuthenticationFilterChain(null,
                    new NoOpFilter(), new NoOpFilter(), new NoOpFilter(),
                    new NoOpFilter(), new NoOpFilter(), accessTokenAuthenticationFilter,
                    assumeAnonymousUserFilter).doFilter(request, response, filterChain);

            verify(filterChain).doFilter(wrap(request), wrap(response));
            MockHttpServletResponseAssert.assertThat(response).isOk();
            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(authenticationToken);
            assertThat(request.getSession(false)).isNotSameAs(originalSession);
        }
    }

    @Nested
    class SecurityDisabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(false);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/config-repository.git/git-upload-something", "/cctray.xml", "/api/foo",
                "/auth/login", "/auth/logout", "/plugin/foo/login", "/plugin/foo/authenticate", "/blah"
        })
        void shouldAuthenticateAsAnonymousWhenSecurityIsDisabled(String url) throws IOException, ServletException {
            request = HttpRequestBuilder.GET(url).build();

            new AuthenticationFilterChain(null,
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    assumeAnonymousUserFilter).doFilter(request, response, filterChain);

            verify(filterChain).doFilter(wrap(request), wrap(response));
            MockHttpServletResponseAssert.assertThat(response).isOk();

            final AuthenticationToken<?> authenticationToken = SessionUtils.getAuthenticationToken(request);
            assertThat(authenticationToken).isNotNull();
            assertThat(authenticationToken.getCredentials()).isEqualTo(AnonymousCredential.INSTANCE);
            assertThat(authenticationToken.getPluginId()).isEqualTo(null);
            assertThat(authenticationToken.getAuthConfigId()).isEqualTo(null);
            assertThat(authenticationToken.getUser().getUsername()).isEqualTo("anonymous");
            assertThat(authenticationToken.getUser().getDisplayName()).isEqualTo("anonymous");
            assertThat(authenticationToken.getUser().getAuthorities()).isEqualTo(GoAuthority.ALL_AUTHORITIES);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/remoting/foo"})
        void shouldNotApplyAnonymousAuthenticationFilterOnUrls(String url) throws IOException, ServletException {
            request = HttpRequestBuilder.GET(url).build();

            new AuthenticationFilterChain(
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    new NoOpFilter(),
                    null,
                    assumeAnonymousUserFilter).doFilter(request, response, filterChain);

            verify(filterChain).doFilter(wrap(request), wrap(response));
            MockHttpServletResponseAssert.assertThat(response).isOk();
            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
        }
    }

}

