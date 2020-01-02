/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AbstractUserEnabledCheckFilterTest {
    private MockHttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    private AbstractUserEnabledCheckFilter filter;
    private UserService userService;
    private SecurityService securityService;

    @BeforeEach
    void setUp() throws Exception {
        userService = mock(UserService.class);
        securityService = mock(SecurityService.class);
        filter = spy(new AbstractUserEnabledCheckFilter(userService, securityService) {

            @Override
            void handleFailure(HttpServletRequest request,
                               HttpServletResponse response,
                               String errorMessage) throws IOException {

            }
        });
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Nested
    class SecurityEnabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(true);
        }


        @Test
        void shouldClearAuthFromRequestOnFindingDisabledUser() throws IOException, ServletException {
            SessionUtilsHelper.setCurrentUser(request, "loser");
            User disabledUser = createUser("loser", 1L);
            disabledUser.disable();

            when(userService.findUserByName(disabledUser.getName())).thenReturn(disabledUser);

            filter.doFilter(request, response, filterChain);

            assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
            assertThat(SessionUtils.getUserId(request)).isNull();
            verify(filterChain, never()).doFilter(request, response);
            verify(filter).handleFailure(request, response, "Your account has been disabled by the administrator");
        }

        @Test
        void shouldAllowNormalChainingOfRequestIfUserEnabled() throws IOException, ServletException {
            SessionUtilsHelper.setCurrentUser(request, "winner");
            final AuthenticationToken<?> actualAuthenticationToken = SessionUtils.getAuthenticationToken(request);
            final User enabledUser = createUser("winner", 1L);

            when(userService.findUserByName(enabledUser.getName())).thenReturn(enabledUser);

            assertThat(SessionUtils.getUserId(request)).isNull();

            filter.doFilter(request, response, filterChain);

            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(actualAuthenticationToken);
            assertThat(SessionUtils.getUserId(request)).isEqualTo(1L);
            assertThat(SessionUtils.getAuthenticationError(request)).isNull();
            verify(filter, never()).handleFailure(any(), any(), anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void shouldNotSetUserIdInSessionIfAlreadySet() throws IOException, ServletException {
            final HttpSession session = mock(HttpSession.class);
            request.setSession(session);
            when(session.getAttribute("GOCD_SECURITY_CURRENT_USER_ID")).thenReturn(1L);

            final User enabledUser = createUser("winner", 1L);
            when(userService.load(1L)).thenReturn(enabledUser);

            filter.doFilter(request, response, filterChain);

            verify(session, never()).setAttribute("GOCD_SECURITY_CURRENT_USER_ID", 1L);
            verify(session, never()).removeAttribute("GOCD_SECURITY_CURRENT_USER_ID");
            verify(filterChain).doFilter(request, response);
            verify(filter, never()).handleFailure(any(), any(), anyString());
        }

        @Test
        void shouldNotSetUserIdInSessionIfUserServiceReturnANullUser() throws IOException, ServletException {
            SessionUtilsHelper.setCurrentUser(request, "winner");
            final AuthenticationToken<?> actualAuthenticationToken = SessionUtils.getAuthenticationToken(request);
            when(userService.findUserByName("winner")).thenReturn(new NullUser());

            assertThat(SessionUtils.getUserId(request)).isNull();

            filter.doFilter(request, response, filterChain);

            assertThat(SessionUtils.getUserId(request)).isNull();
            assertThat(SessionUtils.getAuthenticationToken(request)).isSameAs(actualAuthenticationToken);
            assertThat(SessionUtils.getAuthenticationError(request)).isNull();
            verify(filterChain).doFilter(request, response);
            verify(filter, never()).handleFailure(any(), any(), anyString());
        }

        @Test
        void shouldNotLoadUserFromServiceWhenSecurityIsDisabled() throws Exception {
            SessionUtilsHelper.setCurrentUser(request, "anonymous");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyZeroInteractions(userService);
            verify(filter, never()).handleFailure(any(), any(), anyString());
        }

        @Test
        void shouldNotLoadUserUsingUserServiceWhenUserIsGoAgentUser() throws IOException, ServletException {
            SessionUtilsHelper.setCurrentUser(request, "anonymous");

            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(userService);
            verify(filterChain).doFilter(request, response);
            verify(filter, never()).handleFailure(any(), any(), anyString());
        }
    }

    @Nested
    class SecurityDisabled {
        @BeforeEach
        void setUp() {
            when(securityService.isSecurityEnabled()).thenReturn(false);
        }

        @Test
        void shouldContinueFilterChain() throws IOException, ServletException {
            filter.doFilter(request, response, filterChain);

            verifyZeroInteractions(userService);
            verify(filterChain).doFilter(request, response);
            verify(filter, never()).handleFailure(any(), any(), anyString());
        }
    }

    private User createUser(String name, long id) {
        User user = new User(name);
        user.setId(id);
        return user;
    }
}
