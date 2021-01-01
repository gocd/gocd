/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.http.mocks.MockFilterChain;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import org.junit.jupiter.api.Test;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class ThreadLocalUserFilterTest {

    @Test
    void shouldSetUserToThreadLocalWhenFilterIsCalledAndRemoveUserFromThreadLocalOnceRequestIsCompleted() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final AuthenticationToken<UsernamePassword> authenticationToken = SessionUtilsHelper.createUsernamePasswordAuthentication("bob", "p@ssw0rd", 0L);
        SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);

        final GoUserPrinciple[] currentUserInFilter = {null};

        final FilterChain filterChain = new MockFilterChain(mock(Servlet.class), spy(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                currentUserInFilter[0] = SessionUtils.getCurrentUser();
            }
        }));


        new ThreadLocalUserFilter().doFilter(request, response, filterChain);

        assertThat(currentUserInFilter[0]).isNotNull();
        assertThat(SessionUtils.getCurrentUser().getUsername()).isEqualTo("anonymous");
        assertThat(SessionUtils.getCurrentUser().getAuthorities()).containsExactly(GoAuthority.ROLE_ANONYMOUS.asAuthority());
    }
}
