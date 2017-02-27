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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class WebBasedPluginAuthenticationFilterTest {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private WebBasedPluginAuthenticationFilter filter;
    private AuthorizationExtension authorizationExtension;
    private GoConfigService goConfigService;
    private SecurityConfig securityConfig;
    private SecurityAuthConfig securityAuthConfig;

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        authorizationExtension = mock(AuthorizationExtension.class);
        goConfigService = mock(GoConfigService.class);
        securityConfig = new SecurityConfig();

        securityAuthConfig = new SecurityAuthConfig("github", "github.oauth", new ConfigurationProperty());
        securityConfig.securityAuthConfigs().add(securityAuthConfig);
        stub(goConfigService.security()).toReturn(securityConfig);
        filter = new WebBasedPluginAuthenticationFilter(authorizationExtension, goConfigService);
    }

    @Test
    public void shouldHandleOnlyWebBasedPluginAuthenticationRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/login");

        filter.doFilter(request, response, filterChain);

        verify(authorizationExtension).getIdentityProviderRedirectUrl("github.oauth", Collections.singletonList(securityAuthConfig));
    }

    @Test
    public void shouldRedirectToIdentityProviderUrlProvidedByPlugin() throws Exception {
        String redirectUrl = "http://github/oauth/login";

        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/login");
        when(authorizationExtension.getIdentityProviderRedirectUrl(eq("github.oauth"), any(List.class))).thenReturn(redirectUrl);

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect(redirectUrl);
    }

    @Test
    public void shouldIgnoreNonWebBasedAuthenticationRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/go/api/agents");

        filter.doFilter(request, response, filterChain);

        verifyZeroInteractions(authorizationExtension);
        verify(filterChain).doFilter(request, response);
    }
}