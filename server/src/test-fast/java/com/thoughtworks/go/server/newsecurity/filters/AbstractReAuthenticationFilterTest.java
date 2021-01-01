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

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.http.mocks.*;
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.newsecurity.models.AccessToken;
import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.providers.AnonymousAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class AbstractReAuthenticationFilterTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private SecurityService securityService;
    private PasswordBasedPluginAuthenticationProvider passwordBasedPluginAuthenticationProvider;
    private WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider;
    private TestingClock clock;
    private AbstractReAuthenticationFilter filter;
    private SystemEnvironment systemEnvironment;
    private AnonymousAuthenticationProvider anonymousAuthenticationProvider;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        securityService = mock(SecurityService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true);
        passwordBasedPluginAuthenticationProvider = mock(PasswordBasedPluginAuthenticationProvider.class);
        webBasedPluginAuthenticationProvider = mock(WebBasedPluginAuthenticationProvider.class);
        anonymousAuthenticationProvider = mock(AnonymousAuthenticationProvider.class);

        clock = new TestingClock();
        filter = spy(new AbstractReAuthenticationFilter(securityService, systemEnvironment, clock, passwordBasedPluginAuthenticationProvider, webBasedPluginAuthenticationProvider, anonymousAuthenticationProvider) {

            @Override
            protected void onAuthenticationFailure(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   String errorMessage) throws IOException {

            }
        });
    }

    @Nested
    class SecurityDisabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(false);
        }

        @Test
        void shouldContinueWithChain() throws ServletException, IOException {
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
            verify(filter, never()).onAuthenticationFailure(any(), any(), any());
        }
    }

    @Nested
    class SecurityEnabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(true);
        }

        @Test
        void shouldContinueExecutionOfFilterChainIfSessionDoesNotHaveAuthenticationToken() throws IOException, ServletException {
            request = new MockHttpServletRequest();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            MockHttpServletResponseAssert.assertThat(response)
                    .isOk();
            verify(filter, never()).onAuthenticationFailure(any(), any(), any());
        }

        @Test
        void shouldReAuthenticateUsernamePasswordTokenWhenItHasExpired() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/").build();
            final AuthenticationToken<UsernamePassword> authenticationToken = SessionUtilsHelper.createUsernamePasswordAuthentication("bob", "p@ssw0rd", clock.currentTimeMillis());
            SessionUtilsHelper.setAuthenticationToken(request, authenticationToken);

            clock.addSeconds(3601);
            when(systemEnvironment.getReAuthenticationTimeInterval()).thenReturn(3600 * 1000L);

            final AuthenticationToken<UsernamePassword> reAuthenticatedToken = SessionUtilsHelper.createUsernamePasswordAuthentication("bob", "p@ssw0rd", clock.currentTimeMillis());
            when(passwordBasedPluginAuthenticationProvider.reauthenticate(authenticationToken)).thenReturn(reAuthenticatedToken);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            assertThat(authenticationToken).isNotSameAs(reAuthenticatedToken);

            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(reAuthenticatedToken);

            MockHttpServletResponseAssert.assertThat(response)
                    .isOk();
            verify(filter, never()).onAuthenticationFailure(any(), any(), any());
        }

        @Test
        void shouldReAuthenticateWebBasedTokenWhenItHasExpired() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/").build();
            final AuthenticationToken<AccessToken> authenticationToken = SessionUtilsHelper.createWebAuthentication(Collections.singletonMap("access_token", "some-token"), clock.currentTimeMillis());
            SessionUtilsHelper.setAuthenticationToken(request, authenticationToken);

            clock.addSeconds(3601);
            when(systemEnvironment.getReAuthenticationTimeInterval()).thenReturn(3600 * 1000L);

            final AuthenticationToken<AccessToken> reAuthenticatedToken = SessionUtilsHelper.createWebAuthentication(Collections.singletonMap("access_token", "some-token"), clock.currentTimeMillis());

            when(webBasedPluginAuthenticationProvider.reauthenticate(authenticationToken)).thenReturn(reAuthenticatedToken);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            assertThat(authenticationToken).isNotSameAs(reAuthenticatedToken);

            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(reAuthenticatedToken);

            MockHttpServletResponseAssert.assertThat(response)
                    .isOk();
            verify(filter, never()).onAuthenticationFailure(any(), any(), any());
        }

        @Test
        void shouldReAuthenticateAnonymousTokenWhenItHasExpired() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/").build();
            SessionUtilsHelper.loginAsAnonymous(request);
            AuthenticationToken<AnonymousCredential> authenticationToken = (AuthenticationToken<AnonymousCredential>) SessionUtils.getAuthenticationToken(request);

            clock.addSeconds(3601);
            when(systemEnvironment.getReAuthenticationTimeInterval()).thenReturn(3600 * 1000L);

            final AuthenticationToken<AnonymousCredential> reAuthenticatedToken = SessionUtilsHelper.createAnonymousAuthentication(clock.currentTimeMillis());

            when(anonymousAuthenticationProvider.reauthenticate(authenticationToken)).thenReturn(reAuthenticatedToken);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);

            assertThat(authenticationToken).isNotSameAs(reAuthenticatedToken);

            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(reAuthenticatedToken);

            MockHttpServletResponseAssert.assertThat(response)
                    .isOk();
            verify(filter, never()).onAuthenticationFailure(any(), any(), any());
        }

        @Test
        void shouldErrorOutIfUsernamePasswordTokenReAuthenticationFails() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/").build();
            final AuthenticationToken<UsernamePassword> authenticationToken = SessionUtilsHelper.createUsernamePasswordAuthentication("bob", "p@ssw0rd", clock.currentTimeMillis());
            SessionUtilsHelper.setAuthenticationToken(request, authenticationToken);

            clock.addSeconds(3601);
            when(systemEnvironment.getReAuthenticationTimeInterval()).thenReturn(3600 * 1000L);

            when(passwordBasedPluginAuthenticationProvider.reauthenticate(authenticationToken)).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(filterChain);

            verify(filter).onAuthenticationFailure(request, response, "Unable to re-authenticate user after timeout.");
        }

        @Test
        void shouldErrorOutIfWebBasedTokenReAuthenticationFails() throws IOException, ServletException {
            request = HttpRequestBuilder.GET("/").build();
            final AuthenticationToken<AccessToken> authenticationToken = SessionUtilsHelper.createWebAuthentication(Collections.singletonMap("access_token", "some-token"), clock.currentTimeMillis());
            SessionUtilsHelper.setAuthenticationToken(request, authenticationToken);

            clock.addSeconds(3601);
            when(systemEnvironment.getReAuthenticationTimeInterval()).thenReturn(3600 * 1000L);

            when(webBasedPluginAuthenticationProvider.reauthenticate(authenticationToken)).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(filterChain);

            verify(filter).onAuthenticationFailure(request, response, "Unable to re-authenticate user after timeout.");
        }
    }
}
