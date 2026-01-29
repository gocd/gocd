/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.newsecurity.controllers;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationServerUrlResponse;
import com.thoughtworks.go.server.newsecurity.models.AccessToken;
import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrincipal;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthenticationControllerTest {
    private static final String BOB = "bob";
    private static final String PASSWORD = "p@ssw0rd";
    private static final UsernamePassword CREDENTIALS = new UsernamePassword(BOB, PASSWORD);
    private static final String DISPLAY_NAME = "Bob";
    private final PasswordBasedPluginAuthenticationProvider authenticationProvider = mock(PasswordBasedPluginAuthenticationProvider.class);
    private final SecurityService securityService = mock(SecurityService.class);
    private final WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider = mock(WebBasedPluginAuthenticationProvider.class);
    private final SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
    private final TestingClock clock = new TestingClock();
    private final AuthenticationController controller = new AuthenticationController(securityService, systemEnvironment, clock, authenticationProvider, webBasedPluginAuthenticationProvider);
    private final MockHttpServletRequest request = HttpRequestBuilder.GET("/").build();
    private HttpSession originalSession = request.getSession(true);

    private void authenticateAsAnonymous() {
        final AuthenticationToken<AnonymousCredential> authenticationToken = new AuthenticationToken<>(null, AnonymousCredential.INSTANCE, null, 0L, null);
        SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
        originalSession = request.getSession(false);
    }

    @Nested
    class PerformLogin {
        @Nested
        class SecurityDisabled {
            @BeforeEach
            void setUp() {
                when(securityService.isSecurityEnabled()).thenReturn(false);
            }

            @Test
            void shouldRedirectToHomePageOnFormSubmit() {
                assertThat(controller.performLogin(null, null, null).getUrl()).isEqualTo("/pipelines");

                assertThat(originalSession).isSameAs(request.getSession(false));
            }
        }

        @Nested
        class SecurityEnabled {
            @BeforeEach
            void setUp() {
                when(securityService.isSecurityEnabled()).thenReturn(true);
            }

            @Test
            void shouldValidateMandatoryUsernameAndPasswordParams() {
                authenticateAsAnonymous();

                final RedirectView redirectView = controller.performLogin(null, "  ", request);

                assertThat(redirectView.getUrl()).isEqualTo("/auth/login");
                assertThat(SessionUtils.getAuthenticationError(request)).isEqualTo("Username cannot be blank!");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }

            @Test
            void shouldRedirectToHomePageIfUserSuccessfullyAuthenticatedByPlugin() {
                authenticateAsAnonymous();

                final GoUserPrincipal goUserPrincipal = new GoUserPrincipal(BOB, DISPLAY_NAME, GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
                final AuthenticationToken<UsernamePassword> usernamePasswordAuthenticationToken = new AuthenticationToken<>(goUserPrincipal, CREDENTIALS, null, 0, null);

                when(authenticationProvider.authenticate(CREDENTIALS, null)).thenReturn(usernamePasswordAuthenticationToken);

                final RedirectView redirectView = controller.performLogin(BOB, PASSWORD, request);

                final AuthenticationToken<?> authenticationToken = SessionUtils.getAuthenticationToken(request);
                assertThat(authenticationToken.getUser().getUsername()).isEqualTo(BOB);
                assertThat(authenticationToken.getCredentials()).isEqualTo(CREDENTIALS);
                assertThat(authenticationToken.getUser().getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
                assertThat(originalSession).isNotSameAs(request.getSession(false));

                assertThat(redirectView.getUrl()).isEqualTo("/go/pipelines");
            }

            @Test
            void shouldRedirectToLastSavedUrlIfCredentialsAreAuthenticatedByPlugin() {
                authenticateAsAnonymous();

                final GoUserPrincipal goUserPrincipal = new GoUserPrincipal(BOB, DISPLAY_NAME, GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
                final AuthenticationToken<UsernamePassword> usernamePasswordAuthenticationToken = new AuthenticationToken<>(goUserPrincipal, CREDENTIALS, null, 0, null);

                when(authenticationProvider.authenticate(CREDENTIALS, null)).thenReturn(usernamePasswordAuthenticationToken);

                final MockHttpServletRequest originalRequest = HttpRequestBuilder
                    .GET("/blah")
                    .withSession(request.getSession())
                    .build();
                SessionUtils.saveRequest(originalRequest);

                final RedirectView redirectView = controller.performLogin(BOB, PASSWORD, request);

                assertThat(redirectView.getUrl()).isEqualTo("http://test.host/go/blah");
                assertThat(originalSession).isNotSameAs(request.getSession(false));
            }

            @Test
            void shouldRedirectToLoginPageWithErrorIfCredentialsAreNotAuthenticatedByPlugin() {
                authenticateAsAnonymous();

                when(authenticationProvider.authenticate(CREDENTIALS, null)).thenReturn(null);

                final RedirectView redirectView = controller.performLogin(BOB, PASSWORD, request);

                assertThat(redirectView.getUrl()).isEqualTo("/auth/login");
            }

            @Test
            void shouldRedirectToHomepageIfUserIsAlreadyAuthenticated() {
                final GoUserPrincipal goUserPrincipal = new GoUserPrincipal(BOB, DISPLAY_NAME);
                final AuthenticationToken<UsernamePassword> usernamePasswordAuthenticationToken = new AuthenticationToken<>(goUserPrincipal, CREDENTIALS, null, 0, null);
                SessionUtils.setAuthenticationTokenAfterRecreatingSession(usernamePasswordAuthenticationToken, request);

                originalSession = request.getSession(false);

                final RedirectView redirectView = controller.performLogin(BOB, DISPLAY_NAME, request);

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }
        }
    }

    @Nested
    class WebBasedPluginRedirect {
        private static final String PLUGIN_ID = " cd.go.authorization.github";

        @Nested
        class SecurityDisabled {
            @BeforeEach
            void setUp() {
                when(securityService.isSecurityEnabled()).thenReturn(false);
            }

            @Test
            void shouldRedirectToHomePageOnFormSubmit() {
                assertThat(controller.redirectToThirdPartyLoginPage(PLUGIN_ID, request).getUrl()).isEqualTo("/pipelines");

                assertThat(originalSession).isSameAs(request.getSession(false));
            }
        }

        @Nested
        class SecurityEnabled {
            @BeforeEach
            void setUp() {
                when(securityService.isSecurityEnabled()).thenReturn(true);
            }

            @Test
            void shouldRedirectToHomepageIfUserIsAlreadyAuthenticated() {
                final GoUserPrincipal goUserPrincipal = new GoUserPrincipal(BOB, DISPLAY_NAME);
                final AuthenticationToken<UsernamePassword> usernamePasswordAuthenticationToken = new AuthenticationToken<>(goUserPrincipal, CREDENTIALS, null, 0, null);

                SessionUtils.setAuthenticationTokenAfterRecreatingSession(usernamePasswordAuthenticationToken, request);
                originalSession = request.getSession();

                final RedirectView redirectView = controller.redirectToThirdPartyLoginPage(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }

            @Test
            void shouldRedirectToThirdPartyLoginPage() {
                authenticateAsAnonymous();

                when(webBasedPluginAuthenticationProvider.getAuthorizationServerUrl(eq(PLUGIN_ID), any()))
                    .thenReturn(new AuthorizationServerUrlResponse("https://example.com/oauth", null));

                final RedirectView redirectView = controller.redirectToThirdPartyLoginPage(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("https://example.com/oauth");
            }

            @Test
            void shouldPersistAuthSessionContextReturnedFromPlugin() {
                authenticateAsAnonymous();

                Map<String, String> expectedAuthSessionContext = Map.of(
                    "foo", "bar"
                );
                when(webBasedPluginAuthenticationProvider.getAuthorizationServerUrl(eq(PLUGIN_ID), any()))
                    .thenReturn(new AuthorizationServerUrlResponse("https://example.com/oauth", expectedAuthSessionContext));

                controller.redirectToThirdPartyLoginPage(PLUGIN_ID, request);

                Map<String, String> actual = SessionUtils.getPluginAuthSessionContext(request, PLUGIN_ID);

                assertThat(actual).isEqualTo(expectedAuthSessionContext);
            }
        }
    }

    @Nested
    class WebBasedPluginAuthentication {
        private static final String PLUGIN_ID = " cd.go.authorization.github";

        @Nested
        class SecurityDisabled {
            @BeforeEach
            void setUp() {
                when(securityService.isSecurityEnabled()).thenReturn(false);
            }

            @Test
            void shouldRedirectToHomePageOnFormSubmit() {
                assertThat(controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request).getUrl()).isEqualTo("/pipelines");

                assertThat(originalSession).isSameAs(request.getSession(false));
            }
        }

        @Nested
        class SecurityEnabled {

            @BeforeEach
            void setUp() {
                when(securityService.isSecurityEnabled()).thenReturn(true);
            }

            @Test
            void shouldRedirectToHomepageIfUserIsAlreadyAuthenticated() {
                final GoUserPrincipal goUserPrincipal = new GoUserPrincipal(BOB, DISPLAY_NAME);
                final AuthenticationToken<UsernamePassword> usernamePasswordAuthenticationToken = new AuthenticationToken<>(goUserPrincipal, CREDENTIALS, null, 0, null);
                SessionUtils.setAuthenticationTokenAfterRecreatingSession(usernamePasswordAuthenticationToken, request);
                originalSession = request.getSession(false);

                final RedirectView redirectView = controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }

            @Test
            void shouldAuthenticateUserUsingPlugin() {
                authenticateAsAnonymous();
                originalSession = request.getSession(false);

                final AccessToken credentials = new AccessToken(Map.of("Foo", "Bar"));
                final GoUserPrincipal goUserPrincipal = new GoUserPrincipal(BOB, DISPLAY_NAME, GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
                final AuthenticationToken<AccessToken> usernamePasswordAuthenticationToken = new AuthenticationToken<>(goUserPrincipal, credentials, null, 0, null);

                when(webBasedPluginAuthenticationProvider.fetchAccessToken(eq(PLUGIN_ID), any(), any(), any()))
                    .thenReturn(credentials);
                when(webBasedPluginAuthenticationProvider.authenticate(credentials, PLUGIN_ID)).thenReturn(usernamePasswordAuthenticationToken);

                final RedirectView redirectView = controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("/go/pipelines");
                assertThat(originalSession).isNotSameAs(request.getSession(false));

                final AuthenticationToken<?> authenticationToken = SessionUtils.getAuthenticationToken(request);
                assertThat(authenticationToken.getUser().getUsername()).isEqualTo(BOB);
                assertThat(authenticationToken.getCredentials()).isEqualTo(credentials);
                assertThat(authenticationToken.getUser().getAuthorities())
                    .hasSize(1)
                    .contains(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
            }

            @Test
            void shouldAuthenticateUserUsingPlugin_PassingAnyAuthContextToFetchAccessToken() {
                authenticateAsAnonymous();
                originalSession = request.getSession(false);
                SessionUtils.setPluginAuthSessionContext(request, PLUGIN_ID, Map.of("apple", "banana"));

                controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                verify(webBasedPluginAuthenticationProvider).fetchAccessToken(eq(PLUGIN_ID), any(), any(), eq(Map.of("apple", "banana")));
            }

            @Test
            void shouldAuthenticateUserUsingPlugin_ClearingAnyAuthContextAfterAuthentication() {
                authenticateAsAnonymous();
                originalSession = request.getSession(false);
                SessionUtils.setPluginAuthSessionContext(request, PLUGIN_ID, Map.of("apple", "banana"));

                controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                Map<String, String> actual = SessionUtils.getPluginAuthSessionContext(request, PLUGIN_ID);
                assertThat(actual).isEmpty();
            }

            @Test
            void shouldRedirectToLastSavedUrlAfterSuccessfulAuthenticationByPlugin() {
                authenticateAsAnonymous();

                final GoUserPrincipal goUserPrincipal = new GoUserPrincipal(BOB, DISPLAY_NAME, GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
                final AccessToken credentials = new AccessToken(Map.of("Foo", "Bar"));
                final AuthenticationToken<AccessToken> usernamePasswordAuthenticationToken = new AuthenticationToken<>(goUserPrincipal, credentials, null, 0, null);

                when(webBasedPluginAuthenticationProvider.fetchAccessToken(PLUGIN_ID, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()))
                    .thenReturn(credentials);
                when(webBasedPluginAuthenticationProvider.authenticate(credentials, PLUGIN_ID)).thenReturn(usernamePasswordAuthenticationToken);

                final MockHttpServletRequest originalRequest = HttpRequestBuilder
                    .GET("/blah")
                    .withSession(request.getSession())
                    .build();

                SessionUtils.saveRequest(originalRequest);

                final RedirectView redirectView = controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("http://test.host/go/blah");
                assertThat(originalSession).isNotSameAs(request.getSession(false));
            }

            @Test
            void shouldRedirectToLoginPageWithErrorIfCredentialsAreNotAuthenticatedByPlugin() {
                authenticateAsAnonymous();

                when(webBasedPluginAuthenticationProvider.fetchAccessToken(PLUGIN_ID, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()))
                    .thenReturn(null);

                final RedirectView redirectView = controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("/auth/login");
            }
        }
    }
}
