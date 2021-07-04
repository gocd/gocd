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
package com.thoughtworks.go.server.newsecurity.providers;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.*;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.server.security.GoAuthority.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(ClearSingleton.class)
class PasswordBasedPluginAuthenticationProviderTest {
    private static final String USERNAME = "bob";
    private static final String PASSWORD = "p@ssw0rd";
    private static final UsernamePassword CREDENTIALS = new UsernamePassword(USERNAME, PASSWORD);
    private AuthorizationExtension authorizationExtension;
    private PluginRoleService pluginRoleService;
    private TestingClock clock;
    private static final String PLUGIN_ID_1 = "plugin-id-1";
    private static final String PLUGIN_ID_2 = "plugin-id-2";
    private static final AuthenticationResponse NULL_AUTH_RESPONSE = new AuthenticationResponse(null, null);
    private PasswordBasedPluginAuthenticationProvider provider;
    private SecurityConfig securityConfig;
    private UserService userService;

    @BeforeEach
    void setUp() {
        SecurityService securityService = mock(SecurityService.class);
        AuthorityGranter authorityGranter = new AuthorityGranter(securityService);
        GoConfigService goConfigService = mock(GoConfigService.class);
        userService = mock(UserService.class);

        authorizationExtension = mock(AuthorizationExtension.class);
        pluginRoleService = mock(PluginRoleService.class);
        clock = new TestingClock();

        securityConfig = new SecurityConfig();
        when(goConfigService.security()).thenReturn(securityConfig);

        provider = new PasswordBasedPluginAuthenticationProvider(authorizationExtension, authorityGranter, goConfigService, pluginRoleService, userService, clock);
    }


    @Nested
    class Authenticate {
        @Test
        void shouldBeAbleToAuthenticateUserUsingAnyOfTheAuthorizationPlugins() {
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_1);
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_2);

            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", PLUGIN_ID_2));
            securityConfig.addRole(new PluginRoleConfig("admin", "github", ConfigurationPropertyMother.create("foo")));

            when(authorizationExtension.authenticateUser(PLUGIN_ID_1, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_1), null)).thenReturn(NULL_AUTH_RESPONSE);

            AuthenticationResponse response = new AuthenticationResponse(new User(USERNAME, "display-name", "test@test.com"), Collections.emptyList());

            when(authorizationExtension.authenticateUser(PLUGIN_ID_2, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_2), securityConfig.getPluginRoles(PLUGIN_ID_2))).thenReturn(response);


            AuthenticationToken<UsernamePassword> authenticationToken = provider.authenticate(CREDENTIALS, null);

            assertThat(authenticationToken.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(authenticationToken.getUser().getDisplayName()).isEqualTo("display-name");
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(ROLE_USER.asAuthority());
        }

        @Test
        void shouldTryAuthenticatingAgainstEachAuthorizationPluginInCaseOfErrors() {
            SecurityAuthConfig fileAuthConfig = new SecurityAuthConfig("file_based", PLUGIN_ID_1);
            SecurityAuthConfig ldapAuthConfig = new SecurityAuthConfig("ldap_based", PLUGIN_ID_2);

            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_1);
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_2);

            securityConfig.securityAuthConfigs().add(fileAuthConfig);
            securityConfig.securityAuthConfigs().add(ldapAuthConfig);

            when(authorizationExtension.authenticateUser(PLUGIN_ID_1, USERNAME, PASSWORD, Collections.singletonList(fileAuthConfig), Collections.emptyList())).
                    thenThrow(new RuntimeException());

            when(authorizationExtension.authenticateUser(PLUGIN_ID_2, USERNAME, PASSWORD, Collections.singletonList(ldapAuthConfig), Collections.emptyList())).
                    thenReturn(new AuthenticationResponse(new User(USERNAME, "display-name", "test@test.com"), Collections.emptyList()));

            AuthenticationToken<UsernamePassword> authenticationToken = provider.authenticate(CREDENTIALS, null);

            assertThat(authenticationToken.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(authenticationToken.getUser().getDisplayName()).isEqualTo("display-name");
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(ROLE_USER.asAuthority());
        }

        @Test
        void shouldRelyOnTheAuthConfigOrderWhileAuthenticatingUser() throws Exception {
            SecurityAuthConfig sha1Passwords = new SecurityAuthConfig("sha1Passwords", "file");
            SecurityAuthConfig corporateLDAP = new SecurityAuthConfig("corporateLDAP", "ldap");
            SecurityAuthConfig bcryptPasswords = new SecurityAuthConfig("bcryptPasswords", "file");
            SecurityAuthConfig internalLDAP = new SecurityAuthConfig("internalLDAP", "ldap");

            addPluginSupportingPasswordBasedAuthentication("file");
            addPluginSupportingPasswordBasedAuthentication("ldap");
            securityConfig.securityAuthConfigs().add(sha1Passwords);
            securityConfig.securityAuthConfigs().add(corporateLDAP);
            securityConfig.securityAuthConfigs().add(bcryptPasswords);
            securityConfig.securityAuthConfigs().add(internalLDAP);

            InOrder inOrder = inOrder(authorizationExtension);

            when(authorizationExtension.authenticateUser("ldap", USERNAME, PASSWORD, Collections.singletonList(internalLDAP), Collections.emptyList())).
                    thenReturn(new AuthenticationResponse(new User(USERNAME, null, null), Collections.emptyList()));

            provider.authenticate(CREDENTIALS, null);

            inOrder.verify(authorizationExtension).authenticateUser("file", USERNAME, PASSWORD, Collections.singletonList(sha1Passwords), Collections.emptyList());
            inOrder.verify(authorizationExtension).authenticateUser("ldap", USERNAME, PASSWORD, Collections.singletonList(corporateLDAP), Collections.emptyList());
            inOrder.verify(authorizationExtension).authenticateUser("file", USERNAME, PASSWORD, Collections.singletonList(bcryptPasswords), Collections.emptyList());
            inOrder.verify(authorizationExtension).authenticateUser("ldap", USERNAME, PASSWORD, Collections.singletonList(internalLDAP), Collections.emptyList());
        }

        @Test
        void authenticateUserShouldReceiveAuthConfigAndCorrespondingRoleConfigs() throws Exception {
            SecurityAuthConfig corporateLDAP = new SecurityAuthConfig("corporateLDAP", "ldap");
            SecurityAuthConfig internalLDAP = new SecurityAuthConfig("internalLDAP", "ldap");
            PluginRoleConfig admin = new PluginRoleConfig("admin", "corporateLDAP", new ConfigurationProperty());
            PluginRoleConfig operator = new PluginRoleConfig("operator", "internalLDAP", new ConfigurationProperty());

            addPluginSupportingPasswordBasedAuthentication("ldap");

            securityConfig.securityAuthConfigs().add(corporateLDAP);
            securityConfig.securityAuthConfigs().add(internalLDAP);
            securityConfig.addRole(admin);
            securityConfig.addRole(operator);

            InOrder inOrder = inOrder(authorizationExtension);
            when(authorizationExtension.authenticateUser("ldap", USERNAME, PASSWORD, Collections.singletonList(internalLDAP), Collections.singletonList(operator))).
                    thenReturn(new AuthenticationResponse(new User(USERNAME, null, null), Collections.emptyList()));

            provider.authenticate(CREDENTIALS, null);

            inOrder.verify(authorizationExtension).authenticateUser("ldap", USERNAME, PASSWORD, Collections.singletonList(corporateLDAP), Collections.singletonList(admin));
            inOrder.verify(authorizationExtension).authenticateUser("ldap", USERNAME, PASSWORD, Collections.singletonList(internalLDAP), Collections.singletonList(operator));
        }

        @Test
        void shouldUpdatePluginRolesForAUserPostAuthentication() {
            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", PLUGIN_ID_1));
            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.github"));

            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_1);
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_2);
            when(authorizationExtension.authenticateUser(PLUGIN_ID_1, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_1), securityConfig.getPluginRoles(PLUGIN_ID_1))).thenReturn(
                    new AuthenticationResponse(
                            new User(USERNAME, USERNAME, "bob@example.com"),
                            Arrays.asList("blackbird", "admins")
                    )
            );

            when(authorizationExtension.authenticateUser(PLUGIN_ID_2, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_2), securityConfig.getPluginRoles(PLUGIN_ID_2))).thenReturn(NULL_AUTH_RESPONSE);

            AuthenticationToken<UsernamePassword> authenticationToken = provider.authenticate(CREDENTIALS, null);

            assertThat(authenticationToken).isNotNull();
            verify(pluginRoleService).updatePluginRoles(PLUGIN_ID_1, USERNAME, CaseInsensitiveString.list("blackbird", "admins"));
        }

        @Test
        void authenticatedUsersUsernameShouldBeUsedToAssignRoles() {
            final UsernamePassword credentials = new UsernamePassword("foo@bar.com", PASSWORD);
            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", PLUGIN_ID_1));
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_1);

            when(authorizationExtension.authenticateUser(PLUGIN_ID_1, "foo@bar.com", PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_1), securityConfig.getPluginRoles(PLUGIN_ID_1))).thenReturn(
                    new AuthenticationResponse(new User(USERNAME, USERNAME, "bob@example.com"), Arrays.asList("blackbird", "admins"))
            );

            AuthenticationToken<UsernamePassword> authenticationToken = provider.authenticate(credentials, null);

            assertThat(authenticationToken).isNotNull();
            verify(pluginRoleService).updatePluginRoles(PLUGIN_ID_1, USERNAME, CaseInsensitiveString.list("blackbird", "admins"));
        }

        @Test
        void shouldErrorOutWhenAutoRegistrationOfNewUserIsDisabledByAdmin() {
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_2);

            SecurityAuthConfig githubSecurityAuthconfig = new SecurityAuthConfig("github", PLUGIN_ID_2);
            securityConfig.securityAuthConfigs().add(githubSecurityAuthconfig);
            securityConfig.addRole(new PluginRoleConfig("admin", "github", ConfigurationPropertyMother.create("foo")));

            AuthenticationResponse response = new AuthenticationResponse(new User(USERNAME, "display-name", "test@test.com"), Collections.emptyList());

            doThrow(new OnlyKnownUsersAllowedException(USERNAME, "Please ask the administrator to add you to GoCD.")).when(userService).addOrUpdateUser(any(), eq(githubSecurityAuthconfig));
            when(authorizationExtension.authenticateUser(PLUGIN_ID_2, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_2), securityConfig.getPluginRoles(PLUGIN_ID_2))).thenReturn(response);

            assertThatThrownBy(() -> provider.authenticate(CREDENTIALS, null))
                    .isInstanceOf(OnlyKnownUsersAllowedException.class)
                    .hasMessageContaining("Please ask the administrator to add you to GoCD.");
        }
    }

    @Nested
    class ReAuthenticate {
        @Test
        void shouldReAuthenticateUserUsingAuthenticationToken() {
            final GoUserPrinciple user = new GoUserPrinciple("bob", "Bob");
            final AuthenticationToken<UsernamePassword> oldAuthenticationToken = new AuthenticationToken<>(user, CREDENTIALS, PLUGIN_ID_2, clock.currentTimeMillis(), "github");

            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_1);
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_2);

            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", PLUGIN_ID_2));
            securityConfig.addRole(new PluginRoleConfig("admin", "github", ConfigurationPropertyMother.create("foo")));

            AuthenticationResponse response = new AuthenticationResponse(new User(USERNAME, "display-name", "test@test.com"), Collections.emptyList());

            when(authorizationExtension.authenticateUser(PLUGIN_ID_2, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_2), securityConfig.getPluginRoles(PLUGIN_ID_2))).thenReturn(response);

            AuthenticationToken<UsernamePassword> newAuthenticationToken = provider.reauthenticate(oldAuthenticationToken);

            verify(authorizationExtension, never()).authenticateUser(PLUGIN_ID_1, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_1), securityConfig.getPluginRoles(PLUGIN_ID_1));
            assertThat(newAuthenticationToken.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(newAuthenticationToken.getUser().getDisplayName()).isEqualTo("display-name");
            assertThat(newAuthenticationToken.getUser().getAuthorities())
                    .containsExactly(ROLE_USER.asAuthority());
        }

        @Test
        void shouldReturnNullInCaseOfErrors() {
            final GoUserPrinciple user = new GoUserPrinciple("bob", "Bob");
            final AuthenticationToken<UsernamePassword> oldAuthenticationToken = new AuthenticationToken<>(user, CREDENTIALS, PLUGIN_ID_1, clock.currentTimeMillis(), "file_based");

            SecurityAuthConfig fileAuthConfig = new SecurityAuthConfig("file_based", PLUGIN_ID_1);
            SecurityAuthConfig ldapAuthConfig = new SecurityAuthConfig("ldap_based", PLUGIN_ID_2);

            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_1);
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_2);

            securityConfig.securityAuthConfigs().add(fileAuthConfig);
            securityConfig.securityAuthConfigs().add(ldapAuthConfig);

            when(authorizationExtension.authenticateUser(PLUGIN_ID_1, USERNAME, PASSWORD, Collections.singletonList(fileAuthConfig), Collections.emptyList())).thenThrow(new RuntimeException());

            when(authorizationExtension.authenticateUser(PLUGIN_ID_2, USERNAME, PASSWORD, Collections.singletonList(ldapAuthConfig), Collections.emptyList())).thenReturn(new AuthenticationResponse(new User(USERNAME, "display-name", "test@test.com"), Collections.emptyList()));

            AuthenticationToken<UsernamePassword> authenticationToken = provider.reauthenticate(oldAuthenticationToken);

            assertThat(authenticationToken).isNull();
        }

        @Test
        void shouldTryToReAuthenticateUserAgainWhenPreviouslyAuthenticatedAuthConfigIsDeleted() throws Exception {
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_1);
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_2);

            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", PLUGIN_ID_1));
            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", PLUGIN_ID_2));

            AuthenticationResponse response = new AuthenticationResponse(new User(USERNAME, "display-name", "test@test.com"), Collections.emptyList());
            when(authorizationExtension.authenticateUser(PLUGIN_ID_1, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_1), securityConfig.getPluginRoles(PLUGIN_ID_1))).thenReturn(response);

            when(authorizationExtension.authenticateUser(PLUGIN_ID_2, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_2), securityConfig.getPluginRoles(PLUGIN_ID_2))).thenReturn(response);

            AuthenticationToken<UsernamePassword> authenticationToken = provider.authenticate(CREDENTIALS, null);

            assertThat(authenticationToken.getPluginId()).isEqualTo(PLUGIN_ID_1);
            assertThat(authenticationToken.getAuthConfigId()).isEqualTo("github");
            assertThat(authenticationToken.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(authenticationToken.getUser().getDisplayName()).isEqualTo("display-name");
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(ROLE_USER.asAuthority());

            securityConfig.securityAuthConfigs().remove(new SecurityAuthConfig("github", PLUGIN_ID_1));
            AuthenticationToken<UsernamePassword> newAuthenticationToken = provider.reauthenticate(authenticationToken);

            assertThat(newAuthenticationToken.getPluginId()).isEqualTo(PLUGIN_ID_2);
            assertThat(newAuthenticationToken.getAuthConfigId()).isEqualTo("ldap");
            assertThat(newAuthenticationToken.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(newAuthenticationToken.getUser().getDisplayName()).isEqualTo("display-name");
            assertThat(newAuthenticationToken.getUser().getAuthorities())
                    .containsExactly(ROLE_USER.asAuthority());
        }

        @Test
        void shouldUseTheLoginNameAvailableInGoUserPrinciple() {
            final UsernamePassword credentials = new UsernamePassword("foo@bar.com", PASSWORD);
            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", PLUGIN_ID_1));
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_1);
            when(authorizationExtension.authenticateUser(PLUGIN_ID_1, "foo@bar.com", PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_1), securityConfig.getPluginRoles(PLUGIN_ID_1))).thenReturn(
                    new AuthenticationResponse(
                            new User(USERNAME, USERNAME, "bob@example.com"),
                            Arrays.asList("blackbird", "admins")
                    )
            );

            GoUserPrinciple principal = new GoUserPrinciple(USERNAME, "Display");
            final AuthenticationToken<UsernamePassword> authenticationToken = new AuthenticationToken<>(principal, credentials, PLUGIN_ID_1, clock.currentTimeMillis(), "ldap");

            AuthenticationToken<UsernamePassword> newAuthenticationToken = provider.reauthenticate(authenticationToken);

            GoUserPrinciple goUserPrincipal = newAuthenticationToken.getUser();
            assertThat(goUserPrincipal.getUsername()).isEqualTo(USERNAME);
            assertThat(newAuthenticationToken.getCredentials().getUsername()).isEqualTo("foo@bar.com");

            verify(pluginRoleService).updatePluginRoles(PLUGIN_ID_1, USERNAME, CaseInsensitiveString.list("blackbird", "admins"));
        }

        @Test
        void shouldErrorOutWhenAutoRegistrationOfNewUserIsDisabledByAdmin() {
            addPluginSupportingPasswordBasedAuthentication(PLUGIN_ID_2);

            securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", PLUGIN_ID_2));
            securityConfig.addRole(new PluginRoleConfig("admin", "github", ConfigurationPropertyMother.create("foo")));

            GoUserPrinciple principal = new GoUserPrinciple(USERNAME, "Display");
            final AuthenticationToken<UsernamePassword> authenticationToken = new AuthenticationToken<>(principal, CREDENTIALS, PLUGIN_ID_1, clock.currentTimeMillis(), "ldap");

            AuthenticationResponse response = new AuthenticationResponse(new User(USERNAME, "display-name", "test@test.com"), Collections.emptyList());

            doThrow(new OnlyKnownUsersAllowedException(USERNAME, "Please ask the administrator to add you to GoCD.")).when(userService).addOrUpdateUser(any(), any());
            when(authorizationExtension.authenticateUser(PLUGIN_ID_2, USERNAME, PASSWORD, securityConfig.securityAuthConfigs().findByPluginId(PLUGIN_ID_2), securityConfig.getPluginRoles(PLUGIN_ID_2))).thenReturn(response);

            assertThatThrownBy(() -> provider.reauthenticate(authenticationToken))
                    .isInstanceOf(OnlyKnownUsersAllowedException.class)
                    .hasMessageContaining("Please ask the administrator to add you to GoCD.");
        }
    }

    private void addPluginSupportingPasswordBasedAuthentication(String pluginId) {
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(
                GoPluginDescriptor.builder().id(pluginId).build(), null, null, null,
                new Capabilities(SupportedAuthType.Password, true, false, false));
        AuthorizationMetadataStore.instance().setPluginInfo(pluginInfo);
    }
}
