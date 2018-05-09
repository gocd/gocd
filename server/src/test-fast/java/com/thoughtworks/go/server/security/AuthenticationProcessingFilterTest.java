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

import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.AuthenticationServiceException;
import org.springframework.security.BadCredentialsException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AuthenticationProcessingFilterTest {

    private AuthenticationProcessingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpSession session;

    @Before public void setUp() throws Exception {
        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
        filter = new AuthenticationProcessingFilter(mock(GoConfigService.class));
    }

    @Test
    public void shouldSetSecurityExceptionMessageOnSessionWhenAuthenticationServiceExceptionIsThrownBySpring() throws Exception {
        filter.onUnsuccessfulAuthentication(request, null, new AuthenticationServiceException("foobar"));
        assertThat(((Exception) session.getAttribute(AuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY)).getMessage(), is("Failed to authenticate with your authentication provider. Please check if your authentication provider is up and available to serve requests."));
    }

    @Test
    public void shouldNotSetSecurityExceptionMessageOnSessionWhenBadCredentialsExceptionIsThrownBySpring() throws Exception {
        filter.onUnsuccessfulAuthentication(request, null, new BadCredentialsException("foobar"));
        assertThat(session.getAttribute(AuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY), is(nullValue()));
    }

}
