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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.ClassMockery;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AnonymousProcessingFilterTest {
    private AnonymousProcessingFilter filter;
    private ClassMockery context = new ClassMockery();
    private GoConfigService goConfigService = context.mock(GoConfigService.class);

    @Before
    public void setUp() {
        filter = new AnonymousProcessingFilter(goConfigService);
        filter.setKey("anonymousKey");
        UserAttribute userAttribute = createUserAttribute();
        filter.setUserAttribute(userAttribute);
    }

    private UserAttribute createUserAttribute() {
        UserAttribute userAttribute = new UserAttribute();
        userAttribute.setAuthoritiesAsString(new ArrayList<String>() {
            {
                add("ROLE_ASSHOLE");
            }
        });
        userAttribute.setPassword("anonymousPassword");
        return userAttribute;
    }

    @Test
    public void shouldGiveAnonymousUserRoleAnonymousAuthorityWhenSecurityIsONInCruiseConfig() {
        context.checking(new Expectations() {
            {
                allowing(goConfigService).isSecurityEnabled();
                will(returnValue(true));
            }
        });
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        Authentication authentication = filter.createAuthentication(mockHttpServletRequest);
        assertThat(authentication.getAuthorities().size(), is(1));
        final String role = new ArrayList<>(authentication.getAuthorities()).get(0).getAuthority();
        assertThat(role, is(GoAuthority.ROLE_ANONYMOUS.toString()));
    }

    @Test
    public void shouldGiveAnonymousUserRoleSupervisorAuthorityWhenSecurityIsOFFInCruiseConfig() {
        context.checking(new Expectations() {
            {
                allowing(goConfigService).isSecurityEnabled();
                will(returnValue(false));
            }
        });
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        Authentication authentication = filter.createAuthentication(mockHttpServletRequest);
        assertThat(authentication.getAuthorities().size(), is(1));
        assertThat(authentication.getAuthorities(), Matchers.contains(GoAuthority.ROLE_SUPERVISOR));
        assertTrue(authentication.getDetails() instanceof WebAuthenticationDetails);
    }

    @Test
    public void shouldInitialiseKeyAndAuthoritiesByDefault() {
        AnonymousProcessingFilter testFilter = new AnonymousProcessingFilter(null);
        assertThat(testFilter.getPrincipal(), is("anonymousUser"));
    }
}
