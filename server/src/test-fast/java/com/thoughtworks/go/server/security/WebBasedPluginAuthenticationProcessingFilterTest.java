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

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.security.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.security.tokens.PreAuthenticatedAuthenticationToken;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class WebBasedPluginAuthenticationProcessingFilterTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private AuthenticationManager authenticationManager;
    private WebBasedPluginAuthenticationProcessingFilter filter;
    private AuthorizationExtension authorizationExtension;
    private GoConfigService configService;
    private SecurityConfig securityConfig;
    private WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider;

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        authenticationManager = mock(AuthenticationManager.class);
        authorizationExtension = mock(AuthorizationExtension.class);
        configService = mock(GoConfigService.class);
        webBasedPluginAuthenticationProvider = mock(WebBasedPluginAuthenticationProvider.class);
        filter = new WebBasedPluginAuthenticationProcessingFilter(authorizationExtension, configService, webBasedPluginAuthenticationProvider);
        securityConfig = new SecurityConfig();

        filter.setAuthenticationManager(authenticationManager);
        filter.setFilterProcessesUrl("^/go/plugin/([\\w\\-.]+)/authenticate$");
        when(configService.security()).thenReturn(securityConfig);
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    }

    @Test
    public void shouldAttemptAuthenticationOnlyForPluginAuthRequests() {
        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/authenticate");

        filter.attemptAuthentication(request, response);

        verify(authenticationManager).authenticate(any(PreAuthenticatedAuthenticationToken.class));
    }

    @Test
    public void shouldNotAttemptAuthenticationForAuthenticationPluginRequests() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/go/plugin/interact/github.oauth/authenticate");

        filter.doFilter(request, response, filterChain);

        verifyZeroInteractions(authenticationManager);
        verifyZeroInteractions(authorizationExtension);
    }

    @Test
    public void shouldIgnoreAuthenticationIfUserIsAlreadyAuthenticated() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/authenticate");
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(null, null, null));

        filter.setAuthenticationManager(authenticationManager);

        filter.doFilter(request, response, filterChain);

        verifyZeroInteractions(authenticationManager);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldIgnoreNonPluginAuthenticationRequests() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/go/api/agents");

        filter.setAuthenticationManager(authenticationManager);

        filter.doFilter(request, response, filterChain);

        verifyZeroInteractions(authenticationManager);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldFetchAuthorizationServerAccessTokenFromThePlugin() {
        HashMap<String, String[]> params = new HashMap<>();
        params.put("code", new String[]{"some_auth_code"});
        SecurityAuthConfig githubAuthConfig = new SecurityAuthConfig("github", "github.oauth");
        securityConfig.securityAuthConfigs().add(githubAuthConfig);

        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/authenticate");
        when(request.getParameterMap()).thenReturn(params);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("Authorization")));
        when(request.getHeader("Authorization")).thenReturn("qwe123");

        when(authorizationExtension.fetchAccessToken("github.oauth", Collections.singletonMap("Authorization", "qwe123"),
                Collections.singletonMap("code", "some_auth_code"), Collections.singletonList(githubAuthConfig))).
                thenReturn(Collections.singletonMap("access_token", "token"));

        Map<String, String> credentials = filter.fetchAuthorizationServerAccessToken(request);

        assertThat(credentials, hasEntry("access_token", "token"));
    }

    @Test
    public void shouldAuthenticateUsersWithCredentials() throws IOException, ServletException {
        PreAuthenticatedAuthenticationToken token = mock(PreAuthenticatedAuthenticationToken.class);
        HashMap<String, String[]> params = new HashMap<>();
        params.put("code", new String[]{"some_auth_code"});
        SecurityAuthConfig githubAuthConfig = new SecurityAuthConfig("github", "github.oauth");
        securityConfig.securityAuthConfigs().add(githubAuthConfig);

        when(request.getRequestURI()).thenReturn("/go/plugin/github.oauth/authenticate");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("Authorization")));
        when(request.getHeader("Authorization")).thenReturn("qwe123");
        when(request.getParameterMap()).thenReturn(params);
        when(authorizationExtension.fetchAccessToken("github.oauth", Collections.singletonMap("Authorization", "qwe123"),
                Collections.singletonMap("code", "some_auth_code"), Collections.singletonList(githubAuthConfig))).
                thenReturn(Collections.singletonMap("access_token", "token"));
        when(authenticationManager.authenticate(any(PreAuthenticatedAuthenticationToken.class))).thenReturn(token);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication, is(token));
    }
}
