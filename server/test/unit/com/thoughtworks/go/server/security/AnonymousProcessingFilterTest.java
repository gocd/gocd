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

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.ClassMockery;
import org.jmock.Expectations;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AnonymousProcessingFilterTest {
    private ClassMockery context = new ClassMockery();
    private GoConfigService goConfigService = context.mock(GoConfigService.class);

    @Test
    public void shouldGiveAnonymousUserRoleAnonymousAuthorityWhenSecurityIsONInCruiseConfig() throws Exception {
        setExpectations(true);
        AnonymousProcessingFilter filter = new AnonymousProcessingFilter(goConfigService);
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        Authentication authentication = filter.createAuthentication(mockHttpServletRequest);

        assertThat(authentication.getAuthorities().size(), is(1));
        final String role = new ArrayList<GrantedAuthority>(authentication.getAuthorities()).get(0).getAuthority();
        assertThat(role, is(GoAuthority.ROLE_ANONYMOUS.toString()));
    }

    @Test
    public void shouldGiveAnonymousUserRoleSupervisorAuthorityWhenSecurityIsOFFInCruiseConfig() throws Exception {
        setExpectations(false);
        AnonymousProcessingFilter filter = new AnonymousProcessingFilter(goConfigService);
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        Authentication authentication = filter.createAuthentication(mockHttpServletRequest);

        assertThat(authentication.getAuthorities().size(), is(1));
        final String role = new ArrayList<GrantedAuthority>(authentication.getAuthorities()).get(0).getAuthority();
        assertThat(role, is(GoAuthority.ROLE_SUPERVISOR.toString()));
        assertTrue(authentication.getDetails() instanceof WebAuthenticationDetails);
    }

    @Test
    public void shouldInitialiseKeyAndAuthoritiesByDefault() throws Exception {
        setExpectations(true);

        AnonymousProcessingFilter testFilter = new AnonymousProcessingFilter(goConfigService);
        assertThat(testFilter.getPrincipal(), is("anonymousUser"));
        assertThat(testFilter.getAuthorities().get(0).getAuthority(), is("ROLE_ANONYMOUS"));
    }

    private void setExpectations(final boolean isSecurityEnabled) {
        context.checking(new Expectations() {
            {
                allowing(goConfigService).isSecurityEnabled();
                will(returnValue(isSecurityEnabled));
            }
        });
    }
}
