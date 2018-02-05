/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.plugin.access.authorization.models.User;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginAuthenticationProviderTest {
    private static final AuthenticationResponse NULL_AUTH_RESPONSE = new AuthenticationResponse(null, null);

    @Mock
    private AuthorizationExtension authorizationExtension;
    @Mock
    private AuthorityGranter authorityGranter;
    @Mock
    private UsernamePasswordAuthenticationToken authenticationToken;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PluginRoleService pluginRoleService;
    @Mock
    private UserService userService;

    private GrantedAuthority userAuthority;
    private PluginAuthenticationProvider provider;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private SecurityConfig securityConfig;

    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Before
    public void setUp() {
        initMocks(this);

        when(authenticationToken.getCredentials()).thenReturn("password");
        userAuthority = GoAuthority.ROLE_USER.asAuthority();
        when(authorityGranter.authorities("username")).thenReturn(new GrantedAuthority[]{userAuthority});

        provider = new PluginAuthenticationProvider(authorizationExtension, authorityGranter,
                goConfigService, pluginRoleService, userService);

        securityConfig = new SecurityConfig();
        when(goConfigService.security()).thenReturn(securityConfig);
    }

    @Test
    public void shouldBeAbleToAuthenticateUserUsingAnyOfTheAuthorizationPlugins() {
        String pluginId1 = "plugin-id-1";
        String pluginId2 = "plugin-id-2";

        addPluginSupportingPasswordBasedAuthentication(pluginId1);
        addPluginSupportingPasswordBasedAuthentication(pluginId2);
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", pluginId2));
        securityConfig.addRole(new PluginRoleConfig("admin", "github", ConfigurationPropertyMother.create("foo")));
        when(authorizationExtension.authenticateUser(pluginId1, "username", "password", securityConfig.securityAuthConfigs().findByPluginId(pluginId1), null)).thenReturn(NULL_AUTH_RESPONSE);

        AuthenticationResponse response = new AuthenticationResponse(new User("username", "display-name", "test@test.com"), Collections.emptyList());
        when(authorizationExtension.authenticateUser(pluginId2, "username", "password", securityConfig.securityAuthConfigs().findByPluginId(pluginId2), securityConfig.getPluginRoles(pluginId2))).thenReturn(response);


        UserDetails userDetails = provider.retrieveUser("username", authenticationToken);

        assertThat(userDetails, is(instanceOf(GoUserPrinciple.class)));
        GoUserPrinciple goUserPrincipal = (GoUserPrinciple) userDetails;
        assertThat(goUserPrincipal.getUsername(), is("username"));
        assertThat(goUserPrincipal.getDisplayName(), is("display-name"));
        assertThat(goUserPrincipal.getAuthorities().length, is(1));
        assertThat(goUserPrincipal.getAuthorities()[0], is(userAuthority));
    }

    @Test(expected = BadCredentialsException.class)
    public void shouldValidatePresenceOfPassword() throws Exception {
        provider.retrieveUser("username", new UsernamePasswordAuthenticationToken("principal", " \t"));
    }

    @Test
    public void shouldTryAuthenticatingAgainstEachAuthorizationPluginInCaseOfErrors() throws Exception {
        SecurityAuthConfig fileAuthConfig = new SecurityAuthConfig("file_based", "file");
        SecurityAuthConfig ldapAuthConfig = new SecurityAuthConfig("ldap_based", "ldap");

        addPluginSupportingPasswordBasedAuthentication("file");
        addPluginSupportingPasswordBasedAuthentication("ldap");
        securityConfig.securityAuthConfigs().add(fileAuthConfig);
        securityConfig.securityAuthConfigs().add(ldapAuthConfig);

        when(authorizationExtension.authenticateUser("file", "username", "password", Collections.singletonList(fileAuthConfig), Collections.emptyList())).
                thenThrow(new RuntimeException());
        when(authorizationExtension.authenticateUser("ldap", "username", "password", Collections.singletonList(ldapAuthConfig), Collections.emptyList())).
                thenReturn(new AuthenticationResponse(new User("username", null, null), Collections.emptyList()));

        UserDetails bob = provider.retrieveUser("username", authenticationToken);

        assertThat(bob.getUsername(), is("username"));
    }

    @Test
    public void shouldRelyOnTheAuthConfigOrderWhileAuthenticatingUser() throws Exception {
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

        when(authorizationExtension.authenticateUser("ldap", "username", "password", Collections.singletonList(internalLDAP), Collections.emptyList())).
                thenReturn(new AuthenticationResponse(new User("username", null, null), Collections.emptyList()));

        provider.retrieveUser("username", authenticationToken);

        inOrder.verify(authorizationExtension).authenticateUser("file", "username", "password", Collections.singletonList(sha1Passwords), Collections.emptyList());
        inOrder.verify(authorizationExtension).authenticateUser("ldap", "username", "password", Collections.singletonList(corporateLDAP), Collections.emptyList());
        inOrder.verify(authorizationExtension).authenticateUser("file", "username", "password", Collections.singletonList(bcryptPasswords), Collections.emptyList());
        inOrder.verify(authorizationExtension).authenticateUser("ldap", "username", "password", Collections.singletonList(internalLDAP), Collections.emptyList());
    }

    @Test
    public void authenticateUserShouldReceiveAuthConfigAndCorrespondingRoleConfigs() throws Exception {
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
        when(authorizationExtension.authenticateUser("ldap", "username", "password", Collections.singletonList(internalLDAP), Collections.singletonList(operator))).
                thenReturn(new AuthenticationResponse(new User("username", null, null), Collections.emptyList()));

        provider.retrieveUser("username", authenticationToken);

        inOrder.verify(authorizationExtension).authenticateUser("ldap", "username", "password", Collections.singletonList(corporateLDAP), Collections.singletonList(admin));
        inOrder.verify(authorizationExtension).authenticateUser("ldap", "username", "password", Collections.singletonList(internalLDAP), Collections.singletonList(operator));
    }

    @Test
    public void shouldThrowUpWhenNoPluginCouldAuthenticateUser() throws Exception {
        exception.expect(UsernameNotFoundException.class);
        exception.expectMessage("Unable to authenticate user: bob");

        addPluginSupportingPasswordBasedAuthentication("ldap");
        when(authorizationExtension.authenticateUser("ldap", "bob", "password", securityConfig.securityAuthConfigs().findByPluginId(null), null)).thenReturn(NULL_AUTH_RESPONSE);

        provider.retrieveUser("bob", authenticationToken);
    }

    @Test(expected = UsernameNotFoundException.class)
    public void shouldErrorOutIfUnableToAuthenticateUsingAnyOfThePlugins() {
        try {
            provider.retrieveUser("username", authenticationToken);
            fail("should have thrown up");
        } finally {
            verify(userService, never()).addUserIfDoesNotExist(any(com.thoughtworks.go.domain.User.class));
        }
    }

    @Test
    public void shouldAnswerSupportsBasedOnPluginAvailability() {
        addPluginSupportingPasswordBasedAuthentication("plugin-id-1");
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class), is(true));

        AuthorizationMetadataStore.instance().clear();
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class), is(false));
    }

    @Test
    public void shouldUpdatePluginRolesForAUserPostAuthentication() {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.github"));

        String pluginId1 = "cd.go.ldap";
        String pluginId2 = "cd.go.github";

        addPluginSupportingPasswordBasedAuthentication(pluginId1);
        addPluginSupportingPasswordBasedAuthentication(pluginId2);
        when(authorizationExtension.authenticateUser(pluginId1, "username", "password", securityConfig.securityAuthConfigs().findByPluginId(pluginId1), securityConfig.getPluginRoles(pluginId1))).thenReturn(
                new AuthenticationResponse(
                        new User("username", "bob", "bob@example.com"),
                        Arrays.asList("blackbird", "admins")
                )
        );

        when(authorizationExtension.authenticateUser(pluginId2, "username", "password", securityConfig.securityAuthConfigs().findByPluginId(pluginId2), securityConfig.getPluginRoles(pluginId2))).thenReturn(NULL_AUTH_RESPONSE);

        UserDetails userDetails = provider.retrieveUser("username", new UsernamePasswordAuthenticationToken(null, "password"));

        assertNotNull(userDetails);

        verify(pluginRoleService).updatePluginRoles("cd.go.ldap", "username", CaseInsensitiveString.caseInsensitiveStrings(Arrays.asList("blackbird", "admins")));
    }

    @Test
    public void authenticatedUsersUsernameShouldBeUsedToAssignRoles() throws Exception {
        String pluginId1 = "cd.go.ldap";

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        addPluginSupportingPasswordBasedAuthentication(pluginId1);
        when(authorizationExtension.authenticateUser(pluginId1, "foo@bar.com", "password", securityConfig.securityAuthConfigs().findByPluginId(pluginId1), securityConfig.getPluginRoles(pluginId1))).thenReturn(
                new AuthenticationResponse(
                        new User("username", "bob", "bob@example.com"),
                        Arrays.asList("blackbird", "admins")
                )
        );

        UserDetails userDetails = provider.retrieveUser("foo@bar.com", new UsernamePasswordAuthenticationToken(null, "password"));

        assertNotNull(userDetails);

        verify(pluginRoleService).updatePluginRoles("cd.go.ldap", "username", CaseInsensitiveString.caseInsensitiveStrings(Arrays.asList("blackbird", "admins")));
    }

    @Test
    public void reuthenticationUsingAuthorizationPlugins_shouldUseTheLoginNameAvailableInGoUserPrinciple() throws Exception {
        String pluginId1 = "cd.go.ldap";

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        addPluginSupportingPasswordBasedAuthentication(pluginId1);
        when(authorizationExtension.authenticateUser(pluginId1, "foo@bar.com", "password", securityConfig.securityAuthConfigs().findByPluginId(pluginId1), securityConfig.getPluginRoles(pluginId1))).thenReturn(
                new AuthenticationResponse(
                        new User("username", "bob", "bob@example.com"),
                        Arrays.asList("blackbird", "admins")
                )
        );
        GoUserPrinciple principal = new GoUserPrinciple("username", "Display", "password", true, true, true, true, new GrantedAuthority[]{}, "foo@bar.com");

        UserDetails userDetails = provider.retrieveUser("username", new UsernamePasswordAuthenticationToken(principal, "password"));

        assertThat(userDetails, is(instanceOf(GoUserPrinciple.class)));
        GoUserPrinciple goUserPrincipal = (GoUserPrinciple) userDetails;
        assertThat(goUserPrincipal.getUsername(), is("username"));
        assertThat(goUserPrincipal.getLoginName(), is("foo@bar.com"));

        verify(pluginRoleService).updatePluginRoles("cd.go.ldap", "username", CaseInsensitiveString.caseInsensitiveStrings(Arrays.asList("blackbird", "admins")));
    }

    @Test
    public void reuthenticationUsingAuthorizationPlugins_shouldFallbackOnUserNameInAbsenceOfGoUserPrinciple() throws Exception {
        String pluginId1 = "cd.go.ldap";

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        addPluginSupportingPasswordBasedAuthentication(pluginId1);
        when(authorizationExtension.authenticateUser(pluginId1, "username", "password", securityConfig.securityAuthConfigs().findByPluginId(pluginId1), securityConfig.getPluginRoles(pluginId1))).thenReturn(
                new AuthenticationResponse(
                        new User("username", "bob", "bob@example.com"),
                        Arrays.asList("blackbird", "admins")
                )
        );

        UserDetails userDetails = provider.retrieveUser("username", new UsernamePasswordAuthenticationToken(null, "password"));

        assertNotNull(userDetails);

        verify(pluginRoleService).updatePluginRoles("cd.go.ldap", "username", CaseInsensitiveString.caseInsensitiveStrings(Arrays.asList("blackbird", "admins")));
    }

    @Test
    public void reuthenticationUsingAuthorizationPlugins_shouldFallbackOnUserNameInAbsenceOfLoginNameInGoUserPrinciple() throws Exception {
        String pluginId1 = "cd.go.ldap";

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        addPluginSupportingPasswordBasedAuthentication(pluginId1);
        when(authorizationExtension.authenticateUser(pluginId1, "username", "password", securityConfig.securityAuthConfigs().findByPluginId(pluginId1), securityConfig.getPluginRoles(pluginId1))).thenReturn(
                new AuthenticationResponse(
                        new User("username", "bob", "bob@example.com"),
                        Arrays.asList("blackbird", "admins")
                )
        );
        GoUserPrinciple principal = new GoUserPrinciple("username", "Display", "password", true, true, true, true, new GrantedAuthority[]{}, null);

        UserDetails userDetails = provider.retrieveUser("username", new UsernamePasswordAuthenticationToken(principal, "password"));

        assertNotNull(userDetails);

        verify(pluginRoleService).updatePluginRoles("cd.go.ldap", "username", CaseInsensitiveString.caseInsensitiveStrings(Arrays.asList("blackbird", "admins")));
    }

    private void addPluginSupportingPasswordBasedAuthentication(String pluginId) {
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(
                new GoPluginDescriptor(pluginId, null, null, null, null, false), null, null, null,
                new Capabilities(SupportedAuthType.Password, true, false));
        AuthorizationMetadataStore.instance().setPluginInfo(pluginInfo);
    }
}
