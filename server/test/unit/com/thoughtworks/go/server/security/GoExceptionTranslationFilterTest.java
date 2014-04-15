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
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.server.service.SecurityService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.AuthenticationException;
import org.springframework.security.ui.AuthenticationEntryPoint;
import org.springframework.security.ui.basicauth.BasicProcessingFilterEntryPoint;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class GoExceptionTranslationFilterTest {

    GoExceptionTranslationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private AuthenticationException authenticationException;
    private FilterChain filterChain;
    private BasicProcessingFilterEntryPoint basicAuth;
    private AuthenticationEntryPoint cruiseLoginFormAuth;
    private SecurityService securityService;

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        authenticationException = mock(AuthenticationException.class);
        basicAuth = mock(BasicProcessingFilterEntryPoint.class);
        cruiseLoginFormAuth = mock(AuthenticationEntryPoint.class);
        securityService = mock(SecurityService.class);

        filter = new GoExceptionTranslationFilter();
        filter.setUrlPatternsThatShouldNotBeRedirectedToAfterLogin("(\\.json)|(/images/)");
        filter.setAuthenticationEntryPoint(cruiseLoginFormAuth);
        filter.setBasicAuthenticationEntryPoint(basicAuth);
        filter.setSecurityService(securityService);
    }

    @Test
    public void shouldNotRedirectForJsonRequest() throws Exception {
        assertThat(filter.shouldRedirect("/go/any.json"), is(false));
        assertThat(filter.shouldRedirect("/go/any.json?id=blah"), is(false));
    }

    @Test
    public void shouldNotRedirectForImageRequest() throws Exception {
        assertThat(filter.shouldRedirect("/go/images/1.jpg"), is(false));
    }

    @Test
    public void shouldGetUnauthorizedResponseForJsonRequest() throws IOException, ServletException {
        request.setRequestURI("fkshkj.json");
        filter.sendStartAuthentication(request, response, filterChain, authenticationException);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_UNAUTHORIZED));
    }

    @Test
    public void shouldGetUnauthorizedResponseForFormatJsonRequest() throws IOException, ServletException {
        request.setRequestURI("pipelines");
        request.setParameter("format", "json");
        filter.sendStartAuthentication(request, response, filterChain, authenticationException);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_UNAUTHORIZED));
    }

    @Test
    public void shouldReturnWhateverAppliesToNonJsonFormatRequests() throws IOException, ServletException {
        request.setRequestURI("pipelines");
        filter.sendStartAuthentication(request, response, filterChain, authenticationException);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_OK));
    }

    @Test
    public void shouldSupportBasicAuthenticationForJsonpRequest() throws IOException, ServletException {
        request.setRequestURI("pipelineHistory.json");
        request.setParameter("callback", "foo");
        filter.sendStartAuthentication(request, response, filterChain, authenticationException);
        verify(basicAuth).commence(request, response, authenticationException);
    }

    @Test
    public void shouldRedirectToCruiseLoginPageForNonJsonpRequest() throws IOException, ServletException {
        request.setRequestURI("tab/pipeline");
        filter.sendStartAuthentication(request, response, filterChain, authenticationException);
        verify(cruiseLoginFormAuth).commence(request, response, authenticationException);
    }
}