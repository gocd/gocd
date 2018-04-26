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

package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.server.newsecurity.filters.*;
import com.thoughtworks.go.server.newsecurity.handlers.BasicAuthenticationWithChallengeFailureResponseHandler;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.OAuthCredentials;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.models.X509Credential;
import com.thoughtworks.go.server.newsecurity.providers.OAuthAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.rules.TemporaryFolder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.server.newsecurity.filterchains.DenyGoCDAccessForArtifactsFilterChainTest.wrap;
import static com.thoughtworks.go.server.newsecurity.filters.InvalidateAuthenticationOnSecurityConfigChangeFilter.SECURITY_CONFIG_LAST_CHANGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class AuthenticationFilterChainTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private MockHttpServletResponse response;
    private MockHttpServletRequest request;
    private FilterChain filterChain;
    private SecurityService securityService;
    private TestingClock clock;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        securityService = mock(SecurityService.class);
        when(securityService.isSecurityEnabled()).thenReturn(true);

        clock = new TestingClock();
        systemEnvironment = new SystemEnvironment();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/remoting/blah", "/agent-websocket/blah"})
    void shouldAuthenticateAgentUsingX509Certificate(String url) throws IOException, ServletException {
        final Registration registration = createRegistration("blah");
        final X509AuthenticationFilter x509AuthenticationFilter = new X509AuthenticationFilter(clock);
        final AuthenticationFilterChain authenticationFilterChain = new AuthenticationFilterChain(x509AuthenticationFilter, null, null, null, null, null, null);

        request = HttpRequestBuilder.GET(url)
                .withX509(registration.getChain())
                .build();

        authenticationFilterChain.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(wrap(request), wrap(response));
        MockHttpServletResponseAssert.assertThat(response).isOk();
        assertThat(SessionUtils.getAuthenticationToken(request)).isNotNull();
        assertThat(SessionUtils.getAuthenticationToken(request).getCredentials()).isInstanceOf(X509Credential.class);
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(strings = {"/remoting/blah", "/agent-websocket/blah"})
    void shouldSkipAuthenticationIfRequestIsAlreadyAuthenticated(String url) throws IOException, ServletException {
        request = HttpRequestBuilder.GET(url)
                .build();

        final AuthenticationToken authenticationToken = mock(AuthenticationToken.class);
        SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);

        final X509AuthenticationFilter x509AuthenticationFilter = new X509AuthenticationFilter(clock);
        final AuthenticationFilterChain authenticationFilterChain = new AuthenticationFilterChain(x509AuthenticationFilter, null, null, null, null, null, null);

        authenticationFilterChain.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(wrap(request), wrap(response));
        MockHttpServletResponseAssert.assertThat(response).isOk();
        assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(authenticationToken);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/remoting/blah", "/agent-websocket/blah"})
    void shouldErrorOutWithStatusCode403WhenNoX509CertificateProvidedInRequest(String url) throws IOException, ServletException {
        request = HttpRequestBuilder.GET(url)
                .build();

        final X509AuthenticationFilter x509AuthenticationFilter = new X509AuthenticationFilter(clock);
        final AuthenticationFilterChain authenticationFilterChain = new AuthenticationFilterChain(x509AuthenticationFilter, null, null, null, null, null, null);

        authenticationFilterChain.doFilter(request, response, filterChain);

        verifyZeroInteractions(filterChain);
        MockHttpServletResponseAssert.assertThat(response).isForbidden();
        assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/add-on/foo/api/bar", "/api/config-repository.git/git-upload-something", "/cctray.xml", "/api/foo", "/blah"})
    void shouldInvalidateAuthenticationIfSecurityConfigIsChanged(String url) throws IOException, ServletException {

        request = HttpRequestBuilder.GET(url)
                .withRequestedSessionIdFromSession()
                .build();

        final AuthenticationToken authenticationToken = mock(AuthenticationToken.class);
        SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);

        request.getSession(false).setAttribute(SECURITY_CONFIG_LAST_CHANGE, clock.currentTimeMillis());

        final InvalidateAuthenticationOnSecurityConfigChangeFilter invalidateAuthenticationOnSecurityConfigChangeFilter = new InvalidateAuthenticationOnSecurityConfigChangeFilter(
                mock(GoConfigService.class), clock, mock(PluginRoleService.class));

        clock.addSeconds(1000);
        invalidateAuthenticationOnSecurityConfigChangeFilter.onPluginRoleChange();

        final Filter filter = mock(Filter.class);

        final AuthenticationFilterChain authenticationFilterChain = new AuthenticationFilterChain(null, invalidateAuthenticationOnSecurityConfigChangeFilter, filter, filter, null, null, null);

        authenticationFilterChain.doFilter(request, response, filterChain);

        verify(filter).doFilter(wrap(request), wrap(response), any());
        verify(authenticationToken).invalidate();
        MockHttpServletResponseAssert.assertThat(response).isOk();
        assertThat(SessionUtils.getAuthenticationToken(request)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/add-on/foo/api/bar", "/api/config-repository.git/git-upload-something", "/cctray.xml", "/api/foo", "/blah"})
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
                new ReAuthenticationWithRedirectToLoginFilter(securityService, systemEnvironment, clock, pluginAuthenticationProvider, null),
                new ReAuthenticationWithChallengeFilter(securityService, systemEnvironment, clock, mock(BasicAuthenticationWithChallengeFailureResponseHandler.class), pluginAuthenticationProvider, null),
                new NoOpFilter(),
                new NoOpFilter(),
                new NoOpFilter())
                .doFilter(request, response, filterChain);

        verify(filterChain).doFilter(wrap(request), wrap(response));
        MockHttpServletResponseAssert.assertThat(response).isOk();
        assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(reauthenticatedToken);
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
                new BasicAuthenticationWithRedirectToLoginFilter(securityService, pluginAuthenticationProvider),
                new NoOpFilter()).doFilter(request, response, filterChain);

        verify(filterChain).doFilter(wrap(request), wrap(response));
        MockHttpServletResponseAssert.assertThat(response).isOk();
        assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(authenticationToken);
        assertThat(request.getSession(false)).isNotSameAs(originalSession);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/add-on/foo/api/bar", "/add-on/foo/api/baz"})
    void shouldAuthenticateOAuthAuthenticationRequest(String url) throws IOException, ServletException {
        request = HttpRequestBuilder.GET(url)
                .withOAuth("some-token")
                .build();

        HttpSession originalSession = request.getSession();

        final OAuthAuthenticationProvider oAuthAuthenticationProvider = mock(OAuthAuthenticationProvider.class);

        final AuthenticationToken<OAuthCredentials> authenticationToken = mock(AuthenticationToken.class);
        when(oAuthAuthenticationProvider.authenticate(new OAuthCredentials("some-token"), null)).thenReturn(authenticationToken);

        new AuthenticationFilterChain(null,
                new NoOpFilter(),
                new NoOpFilter(),
                new NoOpFilter(),
                new NoOpFilter(),
                new NoOpFilter(),
                new OauthAuthenticationFilter(securityService, oAuthAuthenticationProvider)
        ).doFilter(request, response, filterChain);

        verify(filterChain).doFilter(wrap(request), wrap(response));
        MockHttpServletResponseAssert.assertThat(response).isOk();
        assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(authenticationToken);
        assertThat(request.getSession(false)).isNotSameAs(originalSession);
    }


    private Registration createRegistration(String hostname) throws IOException {
        File tempKeystoreFile = temporaryFolder.newFile();
        X509CertificateGenerator certificateGenerator = new X509CertificateGenerator();
        certificateGenerator.createAndStoreCACertificates(tempKeystoreFile);
        return certificateGenerator.createAgentCertificate(tempKeystoreFile, hostname);
    }
}

