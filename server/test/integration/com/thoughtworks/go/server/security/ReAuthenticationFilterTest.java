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


import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ReAuthenticationFilterTest {
    private SystemEnvironment systemEnvironment;
    private ReAuthenticationFilter filter;
    private HttpSession session;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private TimeProvider timeProvider;

    @Before
    public void setUp() throws Exception {
        timeProvider = mock(TimeProvider.class);
        session = mock(HttpSession.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        systemEnvironment = mock(SystemEnvironment.class);
        filter = new ReAuthenticationFilter(systemEnvironment, timeProvider);

        stub(request.getSession()).toReturn(session);
        stub(session.getId()).toReturn("session_id");
    }

    @Test
    public void shouldContinueWithChainAndReturnIfReAuthenticationIsDisabled() throws IOException, ServletException {
        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(false);

        filter.doFilterHttp(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void shouldContinueWithChainAndReturnIfNotAuthenticated() throws IOException, ServletException {
        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true);

        filter.doFilterHttp(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void shouldContinueWithChainAndReturnForAuthenticationUsingAPluginOtherThanAuthorization() throws IOException, ServletException {
        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true);
        setupAuthentication(false);

        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true);
        filter.doFilterHttp(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void shouldContinueWithChainAndReturnIfAuthenticationDoesNotHavePrincipalDefined() throws IOException, ServletException {
        Authentication authentication = new TestingAuthenticationToken(null, null, new GrantedAuthority[]{});
        SecurityContextHolder.getContext().setAuthentication(authentication);
        authentication.setAuthenticated(true);

        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true);
        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true);
        filter.doFilterHttp(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void shouldContinueWithChainAndReturnForAuthenticatedSessionWithoutLastAuthenticationTimeStamp() throws IOException, ServletException {
        long currentTimeMillis = DateTimeUtils.currentTimeMillis();
        Authentication authentication = setupAuthentication(true);

        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true);
        when(timeProvider.currentTimeMillis()).thenReturn(currentTimeMillis);

        filter.doFilterHttp(request, response, filterChain);

        verify(session).setAttribute(ReAuthenticationFilter.LAST_REAUTHENICATION_CHECK_TIME, currentTimeMillis);
        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
        assertTrue(authentication.isAuthenticated());
    }

    @Test
    public void shouldReAuthenticateIfReAuthTimeIntervalHasElapsed() throws IOException, ServletException {
        long currentTimeMillis = DateTimeUtils.currentTimeMillis();
        long minuteBack = DateTimeUtils.currentTimeMillis() - 60000;
        Authentication authentication = setupAuthentication(true);

        when(timeProvider.currentTimeMillis()).thenReturn(currentTimeMillis);
        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true);
        when(systemEnvironment.getReAuthenticationTimeInterval()).thenReturn(55000L);
        when(session.getAttribute(ReAuthenticationFilter.LAST_REAUTHENICATION_CHECK_TIME)).thenReturn(minuteBack);

        filter.doFilterHttp(request, response, filterChain);

        verify(session).setAttribute(ReAuthenticationFilter.LAST_REAUTHENICATION_CHECK_TIME, currentTimeMillis);
        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
        assertFalse(authentication.isAuthenticated());
    }

    private Authentication setupAuthentication(boolean authenticatedUsingAuthorizationPlugin) {
        GrantedAuthority[] authorities = {};
        Authentication authentication = new TestingAuthenticationToken(new GoUserPrinciple("user", "displayName", "password",
                true, true,true, true, authorities, "loginName", authenticatedUsingAuthorizationPlugin), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        authentication.setAuthenticated(true);

        return authentication;
    }
}
