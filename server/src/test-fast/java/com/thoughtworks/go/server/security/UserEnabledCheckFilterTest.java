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
import javax.servlet.http.HttpSession;

import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.helpers.SecurityContextHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.UserService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.security.Authentication;
import org.springframework.security.DisabledException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.FilterChainOrder;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.ui.AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY;

public class UserEnabledCheckFilterTest {
    private static SecurityContext originalContext;
    private static final String USERID_ATTR = "USERID";
    private HttpServletRequest req;
    private HttpServletResponse res;
    private FilterChain chain;

    private UserEnabledCheckFilter filter;
    private UserService userService;
    private HttpSession session;

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
        originalContext = SecurityContextHolder.getContext();
        userService = mock(UserService.class);
        filter = new UserEnabledCheckFilter(userService);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);
        when(req.getSession()).thenReturn(session);
    }

    @After
    public void tearDown() {
        if (originalContext != null) {
            SecurityContextHolder.setContext(originalContext);
        }
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void shouldClearAuthFromRequestOnFindingDisabledUser() throws IOException, ServletException {
        SecurityContextHelper.setCurrentUser("loser");
        long userId = 1;
        User disabledUser1 = getUser("loser", userId);
        disabledUser1.disable();
        User disabledUser = disabledUser1;
        when(userService.findUserByName(disabledUser.getName())).thenReturn(disabledUser);
        filter.doFilterHttp(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication(), is(nullValue()));
        verify(session).setAttribute(eq(SPRING_SECURITY_LAST_EXCEPTION_KEY), any(DisabledException.class));
        verify(req).setAttribute(SessionDenialAwareAuthenticationProcessingFilterEntryPoint.SESSION_DENIED, true);
        verify(chain).doFilter(req, res);
    }

    @Test
    public void shouldAllowNormalChainingOfRequestIfUserEnabled() throws IOException, ServletException {
        SecurityContextHelper.setCurrentUser("winner");
        Long userId = 1L;
        User user = getUser("winner", userId);
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        when(session.getAttribute(USERID_ATTR)).thenReturn(userId);
        when(userService.load(userId)).thenReturn(user);
        filter.doFilterHttp(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication(), is(actual));
        verify(chain).doFilter(req, res);
    }

    @Test
    public void shouldSetUserIdInSession() throws IOException, ServletException {
        SecurityContextHelper.setCurrentUser("winner");
        Long userId = 1L;
        User user = getUser("winner", userId);
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        when(session.getAttribute(USERID_ATTR)).thenReturn(null);
        when(userService.findUserByName(user.getName())).thenReturn(user);

        filter.doFilterHttp(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication(), is(actual));
        verify(session).setAttribute(USERID_ATTR, userId);
        verify(chain).doFilter(req, res);
    }

    @Test
    public void shouldNotSetUserIdInSessionIfAlreadySet() throws IOException, ServletException {
        SecurityContextHelper.setCurrentUser("winner");
        Long userId = 1L;
        User user = getUser("winner", userId);
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        when(session.getAttribute(USERID_ATTR)).thenReturn(userId);
        when(userService.load(userId)).thenReturn(user);

        filter.doFilterHttp(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication(), is(actual));
        verify(session, never()).setAttribute(USERID_ATTR, userId);
        verify(chain).doFilter(req, res);
    }

    @Test
    public void shouldNotSetUserIdInSessionIfUserServiceReturnANullUser() throws IOException, ServletException {
        String userName = "none";
        SecurityContextHelper.setCurrentUser(userName);
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        when(session.getAttribute(USERID_ATTR)).thenReturn(null);
        NullUser nullUser = new NullUser();
        when(userService.findUserByName(userName)).thenReturn(nullUser);

        filter.doFilterHttp(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication(), is(actual));
        verify(session, never()).setAttribute(eq(USERID_ATTR), Matchers.<Object>any());
        verify(chain).doFilter(req, res);
    }

    @Test
    public void shouldClearUserIdFromSessionWhenLoggedInUserIsDisabled() throws IOException, ServletException {
        String userName = "winner";
        SecurityContextHelper.setCurrentUser(userName);
        Long userId = 1L;
        User user = getUser(userName, userId);
        user.disable();
        when(session.getAttribute(USERID_ATTR)).thenReturn(null);
        when(userService.findUserByName(userName)).thenReturn(user);

        filter.doFilterHttp(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication(), is(nullValue()));
        verify(session).setAttribute(USERID_ATTR, null);
        verify(chain).doFilter(req, res);
    }

    @Test
    public void shouldBePlacedAfterAuthProcessingFilter() throws Exception {
        assertThat(filter.getOrder(), is(FilterChainOrder.AUTHENTICATION_PROCESSING_FILTER + 1));
    }

    @Test
    public void shouldNotLoadUserFromServiceWhenSecurityIsDisabled() throws Exception {
        SecurityContextHelper.setCurrentUser(Username.ANONYMOUS.getUsername().toString());
        when(session.getAttribute(USERID_ATTR)).thenReturn(null);

        filter.doFilterHttp(req, res, chain);

        verifyZeroInteractions(userService);
        verify(chain).doFilter(req, res);
    }

    private User getUser(String name, long id) {
        User user = new User(name);
        user.setId(id);
        return user;
    }
}
