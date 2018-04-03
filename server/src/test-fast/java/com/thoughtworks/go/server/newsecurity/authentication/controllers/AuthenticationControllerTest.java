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

package com.thoughtworks.go.server.newsecurity.authentication.controllers;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.newsecurity.authentication.HttpRequestBuilder;
import com.thoughtworks.go.server.newsecurity.authentication.mocks.MockHttpServletRequest;
import com.thoughtworks.go.server.newsecurity.authentication.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.authentication.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.authentication.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.SecurityAuthConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.web.GoVelocityView;
import com.thoughtworks.go.server.web.SiteUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import java.util.Collections;

import static com.thoughtworks.go.server.newsecurity.authentication.utils.SessionUtils.AUTHENTICATION_ERROR;
import static com.thoughtworks.go.server.newsecurity.authentication.utils.SessionUtils.CURRENT_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationControllerTest {
    private final PasswordBasedPluginAuthenticationProvider authenticationProvider = mock(PasswordBasedPluginAuthenticationProvider.class);
    private final SecurityService securityService = mock(SecurityService.class);
    private final Localizer localizer = mock(Localizer.class);
    private final WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider = mock(WebBasedPluginAuthenticationProvider.class);
    private final SiteUrlProvider siteUrlProvider = mock(SiteUrlProvider.class);
    private final SecurityAuthConfigService securityAuthConfigService = mock(SecurityAuthConfigService.class);

    private final AuthenticationController controller = new AuthenticationController(localizer, securityService, securityAuthConfigService, authenticationProvider, webBasedPluginAuthenticationProvider, siteUrlProvider);

    private final MockHttpServletRequest request = HttpRequestBuilder.GET("/").build();
    private HttpSession originalSession = request.getSession(true);
    private static final String BOB = "bob";
    private static final String PASSWORD = "p@ssw0rd";

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
                final RedirectView redirectView = controller.performLogin(null, "   ", request);

                assertThat(redirectView.getUrl()).isEqualTo("/auth/login");
                assertThat(request.getSession(false).getAttribute(AUTHENTICATION_ERROR)).isEqualTo("Username and password must be specified!");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }

            @Test
            void shouldRedirectToHomePageIfCredentialsAreAuthenticatedByPlugin() {
                when(authenticationProvider.authenticate(BOB, PASSWORD)).thenReturn(new User(BOB, PASSWORD, Collections.singletonList(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority())));

                final RedirectView redirectView = controller.performLogin(BOB, PASSWORD, request);

                final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);
                assertThat(user.getUsername()).isEqualTo(BOB);
                assertThat(user.getPassword()).isEqualTo(PASSWORD);
                assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
                assertThat(originalSession).isNotSameAs(request.getSession(false));

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
            }

            @Test
            void shouldRedirectToLastSavedUrlIfCredentialsAreAuthenticatedByPlugin() {
                when(authenticationProvider.authenticate(BOB, PASSWORD))
                        .thenReturn(new User(BOB, PASSWORD, Collections.singletonList(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority())));

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
                when(authenticationProvider.authenticate(BOB, PASSWORD)).thenReturn(null);

                final RedirectView redirectView = controller.performLogin(BOB, PASSWORD, request);

                assertThat(redirectView.getUrl()).isEqualTo("/auth/login");
            }

            @Test
            void shouldRedirectToHomepageIfUserIsAlreadyAuthenticated() {
                request.getSession().setAttribute(CURRENT_USER, new User(BOB, PASSWORD, Collections.emptyList()));

                final RedirectView redirectView = controller.performLogin(BOB, PASSWORD, request);

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }
        }
    }

    @Nested
    class RenderLoginPage {
        @Nested
        class SecurityDisabled {
            @BeforeEach
            void setUp() {
                when(securityService.isSecurityEnabled()).thenReturn(false);
            }

            @Test
            void shouldRedirectToHomePageOnFormSubmit() {
                assertThat(((RedirectView) controller.renderLoginPage(request)).getUrl()).isEqualTo("/pipelines");

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
                request.getSession().setAttribute(CURRENT_USER, new User(BOB, PASSWORD, Collections.emptyList()));

                final RedirectView redirectView = (RedirectView) controller.renderLoginPage(request);

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }

            @Test
            void shouldRenderLoginPage() {
                final ModelAndView modelAndView = (ModelAndView) controller.renderLoginPage(request);
                assertThat(modelAndView.getModel())
                        .hasSize(3)
                        .containsEntry("l", localizer)
                        .containsEntry("security_auth_config_service", securityAuthConfigService)
                        .containsEntry(GoVelocityView.CURRENT_GOCD_VERSION, CurrentGoCDVersion.getInstance());

                assertThat(modelAndView.getViewName()).isEqualTo("auth/login");
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
                request.getSession().setAttribute(CURRENT_USER, new User(BOB, PASSWORD, Collections.emptyList()));

                final RedirectView redirectView = controller.redirectToThirdPartyLoginPage(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }

            @Test
            void shouldRedirectToThirdPartyLoginPage() {
                when(webBasedPluginAuthenticationProvider.getAuthorizationServerUrl(eq(PLUGIN_ID), anyString()))
                        .thenReturn("https://example.com/oauth");

                final RedirectView redirectView = controller.redirectToThirdPartyLoginPage(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("https://example.com/oauth");
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
                request.getSession().setAttribute(CURRENT_USER, new User(BOB, PASSWORD, Collections.emptyList()));

                final RedirectView redirectView = controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
                assertThat(originalSession).isSameAs(request.getSession(false));
            }

            @Test
            void shouldAuthenticateUserUsingPlugin() {
                when(webBasedPluginAuthenticationProvider.fetchAccessToken(PLUGIN_ID, Collections.emptyMap(), Collections.emptyMap()))
                        .thenReturn(new User(BOB, PASSWORD, Collections.singletonList(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority())));

                final RedirectView redirectView = controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("/pipelines");
                assertThat(originalSession).isNotSameAs(request.getSession(false));

                final User user = SessionUtils.getUser(request);
                assertThat(user.getUsername()).isEqualTo(BOB);
                assertThat(user.getPassword()).isEqualTo(PASSWORD);
                assertThat(user.getAuthorities())
                        .hasSize(1)
                        .contains(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
            }

            @Test
            void shouldRedirectToLastSavedUrlAfterSuccessfulAuthenticationByPlugin() {
                when(webBasedPluginAuthenticationProvider.fetchAccessToken(eq(PLUGIN_ID), anyMap(), anyMap()))
                        .thenReturn(new User(BOB, PASSWORD, Collections.singletonList(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority())));

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
                when(webBasedPluginAuthenticationProvider.fetchAccessToken(PLUGIN_ID, Collections.emptyMap(), Collections.emptyMap()))
                        .thenReturn(null);

                final RedirectView redirectView = controller.authenticateWithWebBasedPlugin(PLUGIN_ID, request);

                assertThat(redirectView.getUrl()).isEqualTo("/auth/login");
            }
        }
    }
}
