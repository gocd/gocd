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
import com.thoughtworks.go.server.security.tokens.PreAuthenticatedAuthenticationToken;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.web.SiteUrlProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class WebBasedAuthenticationFilterTest {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private WebBasedAuthenticationFilter filter;
    private AuthorizationExtension authorizationExtension;
    private GoConfigService goConfigService;
    private SecurityConfig securityConfig;
    private SecurityAuthConfig securityAuthConfig;
    private SiteUrlProvider siteUrlProvider;

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        authorizationExtension = mock(AuthorizationExtension.class);
        goConfigService = mock(GoConfigService.class);
        siteUrlProvider = mock(SiteUrlProvider.class);
        securityConfig = new SecurityConfig();

        securityAuthConfig = new SecurityAuthConfig("github", "github.oauth", new ConfigurationProperty());
        securityConfig.securityAuthConfigs().add(securityAuthConfig);
        stub(goConfigService.security()).toReturn(securityConfig);
        filter = new WebBasedAuthenticationFilter(authorizationExtension, goConfigService, siteUrlProvider);
    }

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void shouldHandleOnlyWebBasedPluginAuthenticationRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/login");
        when(siteUrlProvider.siteUrl(request)).thenReturn("http://go.site.url");

        filter.doFilter(request, response, filterChain);

        verify(authorizationExtension).getAuthorizationServerUrl("github.oauth", Collections.singletonList(securityAuthConfig), "http://go.site.url");
    }

    @Test
    public void shouldRedirectToAuthorizationServerUrlProvidedByPlugin() throws Exception {
        String redirectUrl = "http://github/oauth/login";

        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/login");
        when(authorizationExtension.getAuthorizationServerUrl(eq("github.oauth"), any(List.class), any(String.class))).thenReturn(redirectUrl);

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect(redirectUrl);
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void shouldIgnoreRequestsToAuthenticationPlugins() throws Exception {
        when(request.getRequestURI()).thenReturn("/go/plugin/interact/github.oauth/login");

        filter.doFilter(request, response, filterChain);

        verifyZeroInteractions(authorizationExtension);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldRedirectToHomePageIfAuthenticatedUserTriesToReauthenticate() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(null, null, null));
        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/login");

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("/");
        verifyZeroInteractions(authorizationExtension);
        verifyNoMoreInteractions(filterChain);
    }
}