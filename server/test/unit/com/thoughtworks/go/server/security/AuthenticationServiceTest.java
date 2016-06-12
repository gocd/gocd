package com.thoughtworks.go.server.security;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRequestHelper;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationServiceTest {

    private AuthenticationPluginRegistry authenticationPluginRegistry = mock(AuthenticationPluginRegistry.class);
    private AuthenticationPluginRequestHelper authenticationPluginRequestHelper = mock(AuthenticationPluginRequestHelper.class);

    @Before
    public void setup() {
        when(authenticationPluginRegistry.getAuthenticationPlugins()).thenReturn(ImmutableSet.of("authFoo", "authBar"));
        when(authenticationPluginRequestHelper.isEnabled(any(String.class))).thenReturn(false);
    }

    @Test
    public void shouldBeEnabledIfAnyPluginIndicatesEnabled() {
        when(authenticationPluginRequestHelper.isEnabled(eq("authFoo"))).thenReturn(true);
        AuthenticationService authenticationService = new AuthenticationService(
                authenticationPluginRegistry,
                authenticationPluginRequestHelper

        );
        assertThat(authenticationService.isAuthEnabled(), is(true));
    }

    @Test
    public void shouldNotBeEnabledIfNoPluginIndicatesEnabled() {
        AuthenticationService authenticationService = new AuthenticationService(
                authenticationPluginRegistry,
                authenticationPluginRequestHelper

        );
        assertThat(authenticationService.isAuthEnabled(), is(false));
    }

}