/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.FilterChain;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AssumeAnonymousUserFilterTest {

    @Test
    void shouldGiveAnonymousUserRoleAnonymousAuthorityWhenSecurityIsONInCruiseConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        final TestingClock clock = new TestingClock();
        final FilterChain filterChain = mock(FilterChain.class);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final SecurityService securityService = mock(SecurityService.class);
        when(securityService.isSecurityEnabled()).thenReturn(true);

        new AssumeAnonymousUserFilter(securityService, clock)
                .doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SessionUtils.getAuthenticationToken(request).getCredentials()).isSameAs(AnonymousCredential.INSTANCE);
        assertThat(SessionUtils.getAuthenticationToken(request).getUser().getUsername()).isEqualTo("anonymous");
        assertThat(SessionUtils.getAuthenticationToken(request).getUser().getAuthorities())
                .hasSize(1)
                .contains(GoAuthority.ROLE_ANONYMOUS.asAuthority());
    }

    @Test
    void shouldGiveAnonymousUserRoleSupervisorAuthorityWhenSecurityIsOFFInCruiseConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        final TestingClock clock = new TestingClock();
        final FilterChain filterChain = mock(FilterChain.class);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final SecurityService securityService = mock(SecurityService.class);
        when(securityService.isSecurityEnabled()).thenReturn(false);

        new AssumeAnonymousUserFilter(securityService, clock)
                .doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SessionUtils.getAuthenticationToken(request).getCredentials()).isSameAs(AnonymousCredential.INSTANCE);
        assertThat(SessionUtils.getAuthenticationToken(request).getUser().getUsername()).isEqualTo("anonymous");
        assertThat(SessionUtils.getAuthenticationToken(request).getUser().getAuthorities())
                .containsAll(Arrays.stream(GoAuthority.values()).map(new Function<GoAuthority, GrantedAuthority>() {
                    @Override
                    public GrantedAuthority apply(GoAuthority goAuthority) {
                        return goAuthority.asAuthority();
                    }
                }).collect(Collectors.toSet()));
    }
}
