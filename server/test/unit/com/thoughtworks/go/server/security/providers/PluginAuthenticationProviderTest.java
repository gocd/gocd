/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginAuthenticationProviderTest {
    private static final AuthenticationResponse NULL_AUTH_RESPONSE = new AuthenticationResponse(null, null);

    @Mock
    private AuthorizationPluginConfigMetadataStore store;
    @Mock
    private AuthorizationExtension authorizationExtension;
    @Mock
    private AuthenticationExtension authenticationExtension;
    @Mock
    private AuthenticationPluginRegistry authenticationPluginRegistry;
    @Mock
    private AuthorityGranter authorityGranter;
    @Mock
    private UsernamePasswordAuthenticationToken authenticationToken;
    @Mock
    private GoConfigService goConfigService;

    private GrantedAuthority userAuthority;
    private PluginAuthenticationProvider provider;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private SecurityConfig securityConfig;

    @Before
    public void setUp() {
        initMocks(this);

        when(authenticationToken.getCredentials()).thenReturn("password");
        userAuthority = GoAuthority.ROLE_USER.asAuthority();
        when(authorityGranter.authorities("username")).thenReturn(new GrantedAuthority[]{userAuthority});

        provider = new PluginAuthenticationProvider(authenticationPluginRegistry, authenticationExtension, authorizationExtension, store, authorityGranter, goConfigService);
        securityConfig = new SecurityConfig();
        when(goConfigService.security()).thenReturn(securityConfig);
    }

    @Test
    public void shouldThrowUpWhenNoPluginCouldAuthenticateUser() throws Exception {
        exception.expect(UsernameNotFoundException.class);
        exception.expectMessage("Unable to authenticate user: bob");

        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList("password")));
        when(store.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList("ldap")));

        when(authorizationExtension.authenticateUser("ldap", "bob", "password")).thenReturn(NULL_AUTH_RESPONSE);
        when(authenticationExtension.authenticateUser("password", "bob", "password")).thenReturn(null);

        provider.retrieveUser("bob", authenticationToken);
    }

    @Test
    public void shouldAskAuthenticationPluginsWhenAuthorizationPluginIsUnableToAuthenticateUser() {
        String pluginId = "plugin-id-1";
        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList(pluginId)));
        when(store.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList(pluginId)));
        when(authorizationExtension.authenticateUser(pluginId, "username", "password")).thenReturn(NULL_AUTH_RESPONSE);

        try {
            provider.retrieveUser("username", authenticationToken);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(UsernameNotFoundException.class)));
            assertThat(e.getMessage(), is("Unable to authenticate user: username"));
        }
    }

    @Test
    public void shouldCreateGoUserPrincipalWhenAnAuthorizationPluginIsAbleToAuthenticateUser() {
        String pluginId1 = "plugin-id-1";
        String pluginId2 = "plugin-id-2";
        when(store.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList(pluginId1, pluginId2)));
        when(authorizationExtension.authenticateUser(pluginId1, "username", "password")).thenReturn(NULL_AUTH_RESPONSE);

        AuthenticationResponse response = new AuthenticationResponse(new User("username", "display-name", "test@test.com"), Collections.emptyList());
        when(authorizationExtension.authenticateUser(pluginId2, "username", "password")).thenReturn(response);

        UserDetails userDetails = provider.retrieveUser("username", authenticationToken);

        assertThat(userDetails, is(instanceOf(GoUserPrinciple.class)));
        GoUserPrinciple goUserPrincipal = (GoUserPrinciple) userDetails;
        assertThat(goUserPrincipal.getUsername(), is("username"));
        assertThat(goUserPrincipal.getDisplayName(), is("display-name"));
        assertThat(goUserPrincipal.getAuthorities().length, is(1));
        assertThat(goUserPrincipal.getAuthorities()[0], is(userAuthority));
    }

    @Test
    public void shouldCreateGoUserPrincipalWhenAnAuthenticationPluginIsAbleToAuthenticateUser() {
        String pluginId1 = "plugin-id-1";
        String pluginId2 = "plugin-id-2";
        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList(pluginId1, pluginId2)));
        when(authenticationExtension.authenticateUser(pluginId1, "username", "password")).thenReturn(null);

        when(authenticationExtension.authenticateUser(pluginId2, "username", "password")).thenReturn(new User("username", null, null));

        UserDetails userDetails = provider.retrieveUser("username", authenticationToken);

        assertThat(userDetails, is(instanceOf(GoUserPrinciple.class)));
        GoUserPrinciple goUserPrincipal = (GoUserPrinciple) userDetails;
        assertThat(goUserPrincipal.getUsername(), is("username"));
        assertThat(goUserPrincipal.getDisplayName(), is("username"));
        assertThat(goUserPrincipal.getAuthorities().length, is(1));
        assertThat(goUserPrincipal.getAuthorities()[0], is(userAuthority));
    }

    @Test
    public void shouldAnswerSupportsBasedOnPluginAvailability() {
        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>());
        when(store.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>());
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class), is(false));

        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>());
        when(store.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList("plugin-id-1")));
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class), is(true));

        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList("plugin-id-1")));
        when(store.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>());
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class), is(true));
    }

    @Test
    public void shouldAddUserToTheCorrectRole() throws Exception {
        securityConfig.addRole(new PluginRoleConfig("blackbird", "ldap"));
        securityConfig.addRole(new PluginRoleConfig("admins", "ldap"));
        securityConfig.addRole(new PluginRoleConfig("view", "github"));

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.github"));


        String pluginId1 = "cd.go.ldap";
        String pluginId2 = "cd.go.github";

        when(store.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList(pluginId1, pluginId2)));

        when(authorizationExtension.authenticateUser(pluginId1, "username", "password")).thenReturn(
                new AuthenticationResponse(
                        new User("username", "bob", "bob@example.com"),
                        Arrays.asList("blackbird", "admins")
                )
        );
        when(authorizationExtension.authenticateUser(pluginId2, "username", "password")).thenReturn(NULL_AUTH_RESPONSE);

        UserDetails userDetails = provider.retrieveUser("username", new UsernamePasswordAuthenticationToken(null, "password"));

        assertNotNull(userDetails);

        assertTrue(securityConfig.isUserMemberOfRole(new CaseInsensitiveString("username"), new CaseInsensitiveString("blackbird")));
        assertTrue(securityConfig.isUserMemberOfRole(new CaseInsensitiveString("username"), new CaseInsensitiveString("admins")));
        assertFalse(securityConfig.isUserMemberOfRole(new CaseInsensitiveString("username"), new CaseInsensitiveString("view")));
    }

    @Test
    public void shouldNotAddUserToRolesWhenPluginRespondsWithRoleNamesItDoesNotOwn() throws Exception {
        securityConfig.addRole(new PluginRoleConfig("blackbird", "ldap"));
        securityConfig.addRole(new PluginRoleConfig("admins", "ldap"));
        securityConfig.addRole(new PluginRoleConfig("view", "ldap"));

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.github"));

        String pluginId1 = "cd.go.ldap";
        String pluginId2 = "cd.go.github";

        when(store.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<>(Arrays.asList(pluginId1, pluginId2)));

        when(authorizationExtension.authenticateUser(pluginId1, "username", "password")).thenReturn(NULL_AUTH_RESPONSE);
        when(authorizationExtension.authenticateUser(pluginId2, "username", "password")).thenReturn(
                new AuthenticationResponse(
                        new User("username", "bob", "bob@example.com"),
                        Arrays.asList("blackbird", "admins", "view")
                )
        );

        UserDetails userDetails = provider.retrieveUser("username", new UsernamePasswordAuthenticationToken(null, "password"));

        assertNotNull(userDetails);

        assertFalse(securityConfig.isUserMemberOfRole(new CaseInsensitiveString("username"), new CaseInsensitiveString("blackbird")));
        assertFalse(securityConfig.isUserMemberOfRole(new CaseInsensitiveString("username"), new CaseInsensitiveString("admins")));
        assertFalse(securityConfig.isUserMemberOfRole(new CaseInsensitiveString("username"), new CaseInsensitiveString("view")));
    }
}
