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

package com.thoughtworks.go.server.newsecurity.authentication.filterchains;

import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.server.newsecurity.authentication.HttpRequestBuilder;
import com.thoughtworks.go.server.newsecurity.authentication.filters.BasicAuthenticationFilter;
import com.thoughtworks.go.server.newsecurity.authentication.filters.CachingSubjectDnX509PrincipalExtractor;
import com.thoughtworks.go.server.newsecurity.authentication.filters.FormLoginFilter;
import com.thoughtworks.go.server.newsecurity.authentication.filters.X509AuthenticationFilter;
import com.thoughtworks.go.server.newsecurity.authentication.mocks.MockHttpServletRequest;
import com.thoughtworks.go.server.newsecurity.authentication.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.authentication.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.SecurityService;
import net.sf.ehcache.Ehcache;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.springframework.security.core.userdetails.User;

import javax.servlet.FilterChain;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static com.thoughtworks.go.server.newsecurity.authentication.filters.AbstractAuthenticationFilter.CURRENT_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AuthenticationFilterChainTest {

    @Nested
    @EnableRuleMigrationSupport
    class Remoting extends X509ChainTest {
        @Override
        String url() {
            return "/remoting/foo";
        }
    }

    @Nested
    @EnableRuleMigrationSupport
    class AgentWebSocket extends X509ChainTest {
        @Override
        String url() {
            return "/agent-websocket/foo";
        }
    }

    @Nested
    class CCTray extends BasicAuthenticationTest {
        @Override
        String url() {
            return "/cctray.xml";
        }
    }

    @Nested
    class API extends BasicAuthenticationTest {
        @Override
        String url() {
            return "/api/version";
        }
    }

    @Nested
    class EverythingElse {

        private final SecurityService securityService = mock(SecurityService.class);
        private final PasswordBasedPluginAuthenticationProvider authenticationProvider = mock(PasswordBasedPluginAuthenticationProvider.class);
        private final AssumeAnonymousUserFilter assumeAnonymousUserFilter = new AssumeAnonymousUserFilter(securityService);
        private final FormLoginFilter formLoginFilter = new FormLoginFilter(securityService, authenticationProvider);
        private final AuthenticationFilterChain authenticationFilterChainChain = new AuthenticationFilterChain(
                null,
                new BasicAuthenticationFilter(securityService, authenticationProvider),
                null,
                formLoginFilter, assumeAnonymousUserFilter);
        private final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        @Nested
        class SecurityEnabled {
            @BeforeEach
            void setUp() {
                when(securityService.isSecurityEnabled()).thenReturn(true);
            }

            @Test
            void shouldCreateAnonymousUserWithRoleAnonymousWhenNoAuthorizationHeaderOrNoFormLoginCredentialsProvided() throws Exception {
                MockHttpServletRequest request = HttpRequestBuilder.GET("/foobar/").build();

                authenticationFilterChainChain.doFilter(request, response, filterChain);

                assertThat(response.getStatus()).isEqualTo(200);
                final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);
                assertThat(user.getUsername()).isEqualTo("anonymous");
                assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_ANONYMOUS.asAuthority());
            }

            @Test
            void shouldCreateUserWithAuthorityWhenBasicAuthenticationCredentialsAreProvided() throws Exception {
                MockHttpServletRequest request = HttpRequestBuilder.GET("/foobar/")
                        .withBasicAuth("bob", "p@ssw0rd").build();

                when(authenticationProvider.hasPluginsForUsernamePasswordAuth()).thenReturn(true);
                when(authenticationProvider.authenticate("bob", "p@ssw0rd")).thenReturn(new User("bob", "p@ssw0rd", Collections.singletonList(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority())));

                authenticationFilterChainChain.doFilter(request, response, filterChain);

                assertThat(response.getStatus()).isEqualTo(200);
                final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);
                assertThat(user.getUsername()).isEqualTo("bob");
                assertThat(user.getPassword()).isEqualTo("p@ssw0rd");
                assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
            }

            @Test
            void shouldCreateUserWithAuthorityWhenFormLoginCredentialsProvidedByUserAreAuthenticatedByPlugin() throws Exception {
                MockHttpServletRequest request = HttpRequestBuilder.POST("/auth/security_check")
                        .withFormData("j_username", "bob")
                        .withFormData("j_password", "p@ssw0rd")
                        .build();

                when(authenticationProvider.hasPluginsForUsernamePasswordAuth()).thenReturn(true);
                when(authenticationProvider.authenticate("bob", "p@ssw0rd")).thenReturn(new User("bob", "p@ssw0rd", Collections.singletonList(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority())));

                authenticationFilterChainChain.doFilter(request, response, filterChain);

                assertThat(response.getStatus()).isEqualTo(200);
                final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);
                assertThat(user.getUsername()).isEqualTo("bob");
                assertThat(user.getPassword()).isEqualTo("p@ssw0rd");
                assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
            }

            @Test
            void shouldCreateUserWithAuthorityWhenFormLoginCredentialsProvidedByUserAreNotAuthenticatedByPlugin() throws Exception {
                MockHttpServletRequest request = HttpRequestBuilder.POST("/auth/security_check")
                        .withFormData("j_username", "bob")
                        .withFormData("j_password", "p@ssw0rd")
                        .build();

                when(authenticationProvider.hasPluginsForUsernamePasswordAuth()).thenReturn(true);
                when(authenticationProvider.authenticate("bob", "p@ssw0rd")).thenReturn(null);

                authenticationFilterChainChain.doFilter(request, response, filterChain);

                assertThat(response.getStatus()).isEqualTo(200);
                final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);
                assertThat(user.getUsername()).isEqualTo("bob");
                assertThat(user.getPassword()).isEqualTo("p@ssw0rd");
                assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
            }
        }

    }

    private abstract class X509ChainTest {
        @Rule
        public final TemporaryFolder tmpFolder = new TemporaryFolder();

        @Test
        public void shouldRespondWith403IfAuthenticationNotProvided() throws Exception {
            MockHttpServletRequest request = HttpRequestBuilder.GET(url()).build();
            final AuthenticationFilterChain authenticationFilterChainChain = new AuthenticationFilterChain(new X509AuthenticationFilter(mock(CachingSubjectDnX509PrincipalExtractor.class)), null, null, null, null);

            final MockHttpServletResponse response = new MockHttpServletResponse();

            final FilterChain filterChain = mock(FilterChain.class);
            authenticationFilterChainChain.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(403);
            verify(filterChain, never()).doFilter(any(), any());
            assertThat(request.getSession(false).getAttribute(CURRENT_USER)).isNull();
        }

        abstract String url();

        @Test
        public void shouldContinueChainAndCreateUserInSessionIfAuthenticationIsProvided() throws Exception {
            final Registration registration = createRegistration("blah");
            MockHttpServletRequest request = HttpRequestBuilder.GET(url())
                    .withAttribute("javax.servlet.request.X509Certificate", registration.getChain())
                    .build();
            final CachingSubjectDnX509PrincipalExtractor subjectDnX509PrincipalExtractor = new CachingSubjectDnX509PrincipalExtractor(mock(Ehcache.class));
            final AuthenticationFilterChain authenticationFilterChainChain = new AuthenticationFilterChain(new X509AuthenticationFilter(subjectDnX509PrincipalExtractor), null, null, null, null);

            final MockHttpServletResponse response = new MockHttpServletResponse();

            final FilterChain filterChain = mock(FilterChain.class);
            authenticationFilterChainChain.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(filterChain).doFilter(any(), any());

            final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);
            assertThat(user.getUsername()).isEqualTo("_go_agent_blah");
            assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_AGENT.asAuthority());
        }

        private Registration createRegistration(String hostname) throws IOException {
            File tempKeystoreFile = tmpFolder.newFile();
            X509CertificateGenerator certificateGenerator = new X509CertificateGenerator();
            certificateGenerator.createAndStoreCACertificates(tempKeystoreFile);
            return certificateGenerator.createAgentCertificate(tempKeystoreFile, hostname);
        }
    }

    private abstract class BasicAuthenticationTest {

        abstract String url();

        private final SecurityService securityService = mock(SecurityService.class);
        private final PasswordBasedPluginAuthenticationProvider authenticationProvider = mock(PasswordBasedPluginAuthenticationProvider.class);
        private final AssumeAnonymousUserFilter assumeAnonymousUserFilter = new AssumeAnonymousUserFilter(securityService);
        private final AuthenticationFilterChain authenticationFilterChainChain = new AuthenticationFilterChain(
                null,
                new BasicAuthenticationFilter(securityService, authenticationProvider),
                null,
                null, assumeAnonymousUserFilter);
        private final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        @Test
        public void shouldContinueChainWithAnonymousUserHavingAnonymousRoleWhenSecurityIsEnabledWhenAuthenticationNotProvided() throws Exception {
            final MockHttpServletRequest request = HttpRequestBuilder.GET(url()).build();
            when(securityService.isSecurityEnabled()).thenReturn(true);

            authenticationFilterChainChain.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);

            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("anonymous");
            assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_ANONYMOUS.asAuthority());
        }

        @Test
        public void shouldContinueChainWithAnonymousUserHavingSupervisorRoleWhenSecurityIsDisabledWhenAuthenticationNotProvided() throws Exception {
            final MockHttpServletRequest request = HttpRequestBuilder.GET(url()).build();
            when(securityService.isSecurityEnabled()).thenReturn(false);

            authenticationFilterChainChain.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);

            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("anonymous");
            assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_SUPERVISOR.asAuthority());
        }

        @Test
        public void shouldContinueChainAndCreateUserInSessionIfAuthenticationProvidedByUserIsAuthenticatedByPlugin() throws Exception {
            final MockHttpServletRequest request = HttpRequestBuilder.GET(url())
                    .withBasicAuth("bob", "p@ssw0rd")
                    .build();

            when(authenticationProvider.authenticate("bob", "p@ssw0rd")).thenReturn(new User("bob", "p@ssw0rd", Collections.singletonList(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority())));
            when(securityService.isSecurityEnabled()).thenReturn(true);
            when(authenticationProvider.hasPluginsForUsernamePasswordAuth()).thenReturn(true);

            authenticationFilterChainChain.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);

            final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);

            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("bob");
            assertThat(user.getPassword()).isEqualTo("p@ssw0rd");
            assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        }

        @Test
        public void shouldContinueChainAndCreateUserInSessionIfAuthenticationProvidedByUserIsBlowsUpThePlugin() throws Exception {
            final MockHttpServletRequest request = HttpRequestBuilder.GET(url())
                    .withBasicAuth("bob", "p@ssw0rd")
                    .build();

            when(authenticationProvider.authenticate("bob", "p@ssw0rd")).thenThrow(new RuntimeException("Unable to connect to ldap!"));
            when(securityService.isSecurityEnabled()).thenReturn(true);
            when(authenticationProvider.hasPluginsForUsernamePasswordAuth()).thenReturn(true);

            authenticationFilterChainChain.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);

            final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);

            assertThat(user).isNull();
        }

        @Test
        public void shouldContinueChainAndCreateUserInSessionIfAuthenticationProvidedByUserIsNotAuthenticatedByPlugin() throws Exception {
            final MockHttpServletRequest request = HttpRequestBuilder.GET(url())
                    .withBasicAuth("bob", "p@ssw0rd")
                    .build();

            when(authenticationProvider.authenticate("bob", "p@ssw0rd")).thenReturn(null);
            when(securityService.isSecurityEnabled()).thenReturn(true);
            when(authenticationProvider.hasPluginsForUsernamePasswordAuth()).thenReturn(true);

            authenticationFilterChainChain.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);

            final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);

            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("anonymous");
            assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_ANONYMOUS.asAuthority());
        }

        @Test
        public void shouldContinueChainAndCreateAnonymousUserInSessionIfAuthenticationIsProvidedWhenSecurityIsTurnedOff() throws Exception {
            final MockHttpServletRequest request = HttpRequestBuilder.GET(url())
                    .withBasicAuth("bob", "p@ssw0rd")
                    .build();

            when(securityService.isSecurityEnabled()).thenReturn(false);

            authenticationFilterChainChain.doFilter(request, response, filterChain);

            verifyZeroInteractions(authenticationProvider);

            assertThat(response.getStatus()).isEqualTo(200);

            final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);

            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("anonymous");
            assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_SUPERVISOR.asAuthority());
        }

        @Test
        void shouldContinueChainAndCreateAnonymousUserWhenSecurityIsEnabledButNoAuthenticationPluginsPresent() throws Exception {
            final MockHttpServletRequest request = HttpRequestBuilder.GET(url())
                    .withBasicAuth("bob", "p@ssw0rd")
                    .build();

            when(securityService.isSecurityEnabled()).thenReturn(true);
            when(authenticationProvider.hasPluginsForUsernamePasswordAuth()).thenReturn(false);

            authenticationFilterChainChain.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(authenticationProvider, times(0)).authenticate("bob", "p@ssw0rd");

            final User user = (User) request.getSession(false).getAttribute(CURRENT_USER);

            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("anonymous");
            assertThat(user.getAuthorities()).hasSize(1).containsExactly(GoAuthority.ROLE_ANONYMOUS.asAuthority());
        }
    }
}
