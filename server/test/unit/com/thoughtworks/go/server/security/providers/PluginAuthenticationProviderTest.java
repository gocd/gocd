/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.access.authentication.model.User;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginAuthenticationProviderTest {
    @Mock
    private AuthenticationPluginRegistry authenticationPluginRegistry;
    @Mock
    private AuthenticationExtension authenticationExtension;
    @Mock
    private AuthorityGranter authorityGranter;
    @Mock
    private UsernamePasswordAuthenticationToken authenticationToken;

    private GrantedAuthority userAuthority;
    private PluginAuthenticationProvider provider;

    @Before
    public void setUp() {
        initMocks(this);

        when(authenticationToken.getCredentials()).thenReturn("password");
        userAuthority = GoAuthority.ROLE_USER.asAuthority();
        when(authorityGranter.authorities("username")).thenReturn(new GrantedAuthority[]{userAuthority});

        provider = new PluginAuthenticationProvider(authenticationPluginRegistry, authenticationExtension, authorityGranter);
    }

    @Test
    public void shouldThrowUpWhenNoPluginIsAbleToAuthenticateUser() {
        String pluginId = "plugin-id-1";
        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<String>(Arrays.asList(pluginId)));
        when(authenticationExtension.authenticateUser(pluginId, "username", "password")).thenReturn(null);

        try {
            provider.retrieveUser("username", authenticationToken);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(UsernameNotFoundException.class)));
            assertThat(e.getMessage(), is("Unable to authenticate user: username"));
        }
    }

    @Test
    public void shouldCreateGoUserPrincipalWhenAPluginIsAbleToAuthenticateUser() {
        String pluginId1 = "plugin-id-1";
        String pluginId2 = "plugin-id-2";
        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<String>(Arrays.asList(pluginId1, pluginId2)));
        when(authenticationExtension.authenticateUser(pluginId1, "username", "password")).thenReturn(null);
        when(authenticationExtension.authenticateUser(pluginId2, "username", "password")).thenReturn(new User("username", "display-name", "test@test.com"));

        UserDetails userDetails = provider.retrieveUser("username", authenticationToken);

        assertThat(userDetails, is(instanceOf(GoUserPrinciple.class)));
        GoUserPrinciple goUserPrincipal = (GoUserPrinciple) userDetails;
        assertThat(goUserPrincipal.getUsername(), is("username"));
        assertThat(goUserPrincipal.getDisplayName(), is("display-name"));
        assertThat(goUserPrincipal.getAuthorities().length, is(1));
        assertThat(goUserPrincipal.getAuthorities()[0], is(userAuthority));
    }

    @Test
    public void shouldAnswerSupportsBasedOnPluginAvailability() {
        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<String>());
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class), is(false));

        when(authenticationPluginRegistry.getPluginsThatSupportsPasswordBasedAuthentication()).thenReturn(new HashSet<String>(Arrays.asList("plugin-id-1")));
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class), is(true));
    }
}
