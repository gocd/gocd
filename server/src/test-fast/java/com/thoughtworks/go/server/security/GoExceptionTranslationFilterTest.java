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

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        authenticationException = mock(AuthenticationException.class);
        basicAuth = mock(BasicProcessingFilterEntryPoint.class);
        cruiseLoginFormAuth = mock(AuthenticationEntryPoint.class);

        filter = new GoExceptionTranslationFilter();
        filter.setAuthenticationEntryPoint(cruiseLoginFormAuth);
        filter.setBasicAuthenticationEntryPoint(basicAuth);
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
