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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.tokens.PreAuthenticatedAuthenticationToken;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.Authentication;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PreAuthenticatedAuthenticationProviderTest {
    private AuthorizationExtension authorizationExtension;
    private String pluginId;
    private AuthorityGranter authorityGranter;
    private UserService userService;
    private PluginRoleService pluginRoleService;
    private PreAuthenticatedAuthenticationProvider authenticationProvider;
    private User user;
    private GrantedAuthority[] authorities;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private GoConfigService goConfigService;
    private SecurityConfig securityConfig;

    @Before
    public void setUp() throws Exception {
        pluginId = "github.oauth";
        user = new User("username", "displayname", "emailId");
        authorities = new GrantedAuthority[]{GoAuthority.ROLE_USER.asAuthority()};

        authorizationExtension = mock(AuthorizationExtension.class);
        authorityGranter = mock(AuthorityGranter.class);
        userService = mock(UserService.class);
        pluginRoleService = mock(PluginRoleService.class);
        goConfigService = mock(GoConfigService.class);
        authenticationProvider = new PreAuthenticatedAuthenticationProvider(authorizationExtension, pluginRoleService, userService, authorityGranter, goConfigService);
        AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, asList("admin"));

        securityConfig = new SecurityConfig();
        stub(goConfigService.security()).toReturn(securityConfig);
        stub(authorizationExtension.authenticateUser(any(String.class), any(Map.class), any(List.class), any(List.class))).toReturn(authenticationResponse);
        stub(authorityGranter.authorities(anyString())).toReturn(authorities);
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", pluginId));
    }

    @Test
    public void authenticate_shouldAuthenticateUserAgainstTheSpecifiedPlugin() throws Exception {
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        SecurityAuthConfig githubConfig = new SecurityAuthConfig("github", pluginId);
        PluginRoleConfig adminRole = new PluginRoleConfig("admin", "github", new ConfigurationProperty());

        securityConfig.securityAuthConfigs().add(githubConfig);
        securityConfig.addRole(adminRole);
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);

        authenticationProvider.authenticate(authenticationToken);

        verify(authorizationExtension).authenticateUser(pluginId, credentials, Collections.singletonList(githubConfig), Collections.singletonList(adminRole));
    }

    @Test
    public void authenticate_inCaseOfMultipleAuthConfigsShouldTryAuthenticatingUserAgainstEachAuthConfig() throws Exception {
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        SecurityAuthConfig githubPublic = new SecurityAuthConfig("github_public", pluginId);
        SecurityAuthConfig githubEnterprise = new SecurityAuthConfig("github_enterprise", pluginId);
        PluginRoleConfig adminRole = new PluginRoleConfig("admin", githubPublic.getId(), new ConfigurationProperty());
        PluginRoleConfig operatorRole = new PluginRoleConfig("operator", githubEnterprise.getId(), new ConfigurationProperty());

        securityConfig.securityAuthConfigs().clear();
        securityConfig.securityAuthConfigs().add(githubPublic);
        securityConfig.securityAuthConfigs().add(githubEnterprise);
        securityConfig.addRole(adminRole);
        securityConfig.addRole(operatorRole);

        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);
        when(authorizationExtension.authenticateUser(pluginId, credentials, Collections.singletonList(githubPublic), Collections.singletonList(adminRole))).thenReturn(new AuthenticationResponse(null, null));
        when(authorizationExtension.authenticateUser(pluginId, credentials, Collections.singletonList(githubEnterprise), Collections.singletonList(operatorRole))).thenReturn(new AuthenticationResponse(null, null));

        Authentication authenticate = authenticationProvider.authenticate(authenticationToken);

        verify(authorizationExtension).authenticateUser(pluginId, credentials, Collections.singletonList(githubPublic), Collections.singletonList(adminRole));
        verify(authorizationExtension).authenticateUser(pluginId, credentials, Collections.singletonList(githubEnterprise), Collections.singletonList(operatorRole));
        assertNull(authenticate);
    }

    @Test
    public void authenticate_inCaseOfMultipleAuthConfigsOnSuccessfulAuthenticationShouldNotTryAuthenticatingUserUsingRemainingAuthConfig() throws Exception {
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        SecurityAuthConfig githubPublic = new SecurityAuthConfig("github_public", pluginId);
        SecurityAuthConfig githubEnterprise = new SecurityAuthConfig("github_enterprise", pluginId);
        PluginRoleConfig adminRole = new PluginRoleConfig("admin", githubPublic.getId(), new ConfigurationProperty());
        PluginRoleConfig operatorRole = new PluginRoleConfig("operator", githubEnterprise.getId(), new ConfigurationProperty());

        securityConfig.securityAuthConfigs().clear();
        securityConfig.securityAuthConfigs().add(githubPublic);
        securityConfig.securityAuthConfigs().add(githubEnterprise);
        securityConfig.addRole(adminRole);
        securityConfig.addRole(operatorRole);

        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);
        when(authorizationExtension.authenticateUser(pluginId, credentials, Collections.singletonList(githubPublic), Collections.singletonList(adminRole))).thenReturn(new AuthenticationResponse(user, asList("admin")));

        PreAuthenticatedAuthenticationToken authenticate = (PreAuthenticatedAuthenticationToken) authenticationProvider.authenticate(authenticationToken);

        assertThat(authenticate.getCredentials(), is(credentials));
        assertThat(authenticate.getPluginId(), is(pluginId));
        assertThat(authenticate.getAuthorities(), is(authorities));
        assertThat(authenticate.isAuthenticated(), is(true));

        verify(authorizationExtension).authenticateUser(pluginId, credentials, Collections.singletonList(githubPublic), Collections.singletonList(adminRole));
        verify(authorizationExtension, never()).authenticateUser(pluginId, credentials, Collections.singletonList(githubEnterprise), Collections.singletonList(operatorRole));
    }

    @Test
    public void authenticate_shouldCreateUserIfDoesNotExist() {
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);

        authenticationProvider.authenticate(authenticationToken);

        ArgumentCaptor<com.thoughtworks.go.domain.User> argumentCaptor = ArgumentCaptor.forClass(com.thoughtworks.go.domain.User.class);
        verify(userService).addUserIfDoesNotExist(argumentCaptor.capture());

        com.thoughtworks.go.domain.User user = argumentCaptor.getValue();
        assertThat(user.getName(), is(this.user.getUsername()));
        assertThat(user.getDisplayName(), is(this.user.getDisplayName()));
        assertThat(user.getEmail(), is(this.user.getEmailId()));
    }

    @Test
    public void authenticate_shouldAssignRolesToUser() throws Exception {
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);

        authenticationProvider.authenticate(authenticationToken);

        verify(pluginRoleService).updatePluginRoles(pluginId, user.getUsername(), CaseInsensitiveString.caseInsensitiveStrings("admin"));
    }

    @Test
    public void authenticate_shouldReturnAuthenticationTokenOnSuccessfulAuthorization() throws Exception {
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);

        PreAuthenticatedAuthenticationToken authenticate = (PreAuthenticatedAuthenticationToken) authenticationProvider.authenticate(authenticationToken);

        assertThat(authenticate.getCredentials(), is(credentials));
        assertThat(authenticate.getPluginId(), is(pluginId));
        assertThat(authenticate.getAuthorities(), is(authorities));
        assertThat(authenticate.isAuthenticated(), is(true));
    }

    @Test
    public void authenticate_shouldReturnAuthTokenWithUserDetails() throws Exception {
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);

        PreAuthenticatedAuthenticationToken authenticate = (PreAuthenticatedAuthenticationToken) authenticationProvider.authenticate(authenticationToken);

        GoUserPrinciple principal = (GoUserPrinciple) authenticate.getPrincipal();

        assertThat(principal.getDisplayName(), is(user.getDisplayName()));
        assertThat(principal.getUsername(), is(user.getUsername()));
        assertThat(principal.getAuthorities(), is(authorities));
    }

    @Test
    public void authenticate_shouldEnsureUserDetailsInAuthTokenHasDisplayName() {
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);
        AuthenticationResponse authenticationResponse = new AuthenticationResponse(new User("username", null, "email"), asList("admin"));

        when(authorizationExtension.authenticateUser(any(String.class), any(Map.class), any(List.class), any(List.class))).thenReturn(authenticationResponse);

        PreAuthenticatedAuthenticationToken authenticate = (PreAuthenticatedAuthenticationToken) authenticationProvider.authenticate(authenticationToken);

        GoUserPrinciple principal = (GoUserPrinciple) authenticate.getPrincipal();

        assertThat(principal.getDisplayName(), is(authenticationResponse.getUser().getUsername()));
    }

    @Test
    public void authenticate_shouldSupportAuthenticationForPreAuthenticatedAuthenticationTokenOnly() throws Exception {
        Authentication authenticate = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("p", "c"));

        assertNull(authenticate);

        verifyZeroInteractions(authorizationExtension);
    }

    @Test
    public void authenticate_shouldErrorOutInAbsenceOfCredentials() throws Exception {
        thrown.expect(BadCredentialsException.class);
        thrown.expectMessage("No pre-authenticated credentials found in request.");

        authenticationProvider.authenticate(new PreAuthenticatedAuthenticationToken(null, null, null));
    }

    @Test
    public void authenticate_shouldHandleFailedAuthentication() throws Exception {
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, Collections.singletonMap("access_token", "invalid_token"), pluginId);
        AuthenticationResponse authenticationResponse = new AuthenticationResponse(null, null);

        when(authorizationExtension.authenticateUser(any(String.class), any(Map.class), any(List.class), any(List.class))).thenReturn(authenticationResponse);

        PreAuthenticatedAuthenticationToken authenticate = (PreAuthenticatedAuthenticationToken) authenticationProvider.authenticate(authenticationToken);

        assertNull(authenticate);
    }

    @Test
    public void authenticate_shouldAssignRoleBeforeGrantingAnAuthority() throws Exception {
        final InOrder inOrder = inOrder(pluginRoleService, authorityGranter);
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);

        authenticationProvider.authenticate(authenticationToken);

        inOrder.verify(pluginRoleService).updatePluginRoles(pluginId, user.getUsername(), asList(new CaseInsensitiveString("admin")));
        inOrder.verify(authorityGranter).authorities(user.getUsername());
    }

    @Test
    public void authenticate_shouldPerformOperationInSequence() throws Exception {
        final InOrder inOrder = inOrder(authorizationExtension, pluginRoleService, authorityGranter, userService);
        Map<String, String> credentials = Collections.singletonMap("access_token", "some_token");
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(null, credentials, pluginId);

        authenticationProvider.authenticate(authenticationToken);

        inOrder.verify(authorizationExtension).authenticateUser(eq(pluginId), eq(credentials), any(List.class), any(List.class));
        inOrder.verify(pluginRoleService).updatePluginRoles(pluginId, user.getUsername(), asList(new CaseInsensitiveString("admin")));
        inOrder.verify(authorityGranter).authorities(user.getUsername());
        inOrder.verify(userService).addUserIfDoesNotExist(any(com.thoughtworks.go.domain.User.class));
    }
}

