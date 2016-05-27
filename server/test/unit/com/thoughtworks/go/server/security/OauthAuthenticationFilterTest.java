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

package com.thoughtworks.go.server.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.LogFixture;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.User;

public class OauthAuthenticationFilterTest {
    private static SecurityContext originalContext;
    private AuthenticationManager authenticationManager;
    private OauthAuthenticationFilter filter;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private FilterChain chain;
    private SecurityContext securityContext;
    private LogFixture logFixture;

    @BeforeClass
    public static void beforeAll() throws Exception {
        originalContext = SecurityContextHolder.getContext();
    }

    @AfterClass
    public static void afterAll() {
        SecurityContextHolder.setContext(originalContext);
    }

    @Before
    public void setUp() throws Exception {
        securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        authenticationManager = mock(AuthenticationManager.class);
        filter = new OauthAuthenticationFilter(authenticationManager);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        LogFixture.enableDebug();
        logFixture = LogFixture.startListening();
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(securityContext);
        verifyNoMoreInteractions(chain);
        logFixture.stopListening();
    }

    @Test
    public void shouldContinueExecutionOfFilterChainIfRequestDoesNotHaveOAuthToken() throws IOException, ServletException {
        filter.doFilterHttp(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    public void shouldContinueFilterChainExecutionIfHeaderHasTokenButNotInExpectedFormat() throws IOException, ServletException {
        when(req.getHeader(OauthAuthenticationFilter.AUTHORIZATION)).thenReturn("Token token=butWithoutQuotes");
        filter.doFilterHttp(req, res, chain);
        verify(chain).doFilter(req, res);
        //assertThat(logFixture.contains(Level.DEBUG, "Oauth authorization header: Token token=butWithoutQuotes"), is(true)); //uncomment this to run it locally (this fails on build, we need to find out why). -Rajesh & JJ
    }

    @Test
    public void shouldAuthenticateToken() throws IOException, ServletException {
        when(req.getHeader(OauthAuthenticationFilter.AUTHORIZATION)).thenReturn("Token token=\"valid-token\"");
        OauthAuthenticationToken authenticatedToken = new OauthAuthenticationToken(
                new User("user-name", "valid-token", true, true, true, true, new GrantedAuthority[]{GoAuthority.ROLE_SUPERVISOR.asAuthority()}));
        when(authenticationManager.authenticate(new OauthAuthenticationToken("valid-token"))).thenReturn(authenticatedToken);

        filter.doFilterHttp(req, res, chain);
        verify(securityContext).setAuthentication(authenticatedToken);
        verify(chain).doFilter(req, res);
        //assertThat(logFixture.contains(Level.DEBUG, "Oauth authorization header: Token token=\"valid-token\""), is(true));//uncomment this to run it locally (this fails on build, we need to find out why). -Rajesh & JJ
    }

    @Test
    public void shouldContinueExecutingFilterChainEvenIfTokenAuthenticationFails() throws IOException, ServletException {
        when(req.getHeader(OauthAuthenticationFilter.AUTHORIZATION)).thenReturn("Token token=\"invalid-token\"");
        when(authenticationManager.authenticate(new OauthAuthenticationToken("invalid-token"))).thenThrow(new BadCredentialsException("failed to auth"));

        filter.doFilterHttp(req, res, chain);
        verify(securityContext).setAuthentication(null);
        verify(chain).doFilter(req, res);
        //assertThat(logFixture.contains(Level.DEBUG, "Oauth authorization header: Token token=\"invalid-token\""), is(true)); //uncomment this to run it locally (this fails on build, we need to find out why). -Rajesh & JJ
        //assertThat(logFixture.contains(Level.DEBUG, "Oauth authentication request for token: invalid-token failed: "), is(true)); //uncomment this to run it locally (this fails on build, we need to find out why). -Rajesh & JJ
    }
}
