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

package com.thoughtworks.go.server.web;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ConfigValidityFilterTest {
    private LicenseInterceptor mockLicenseInterceptor;
    private ConfigValidityFilter configValidityFilter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @Before public void setUp() throws Exception {
        mockLicenseInterceptor = Mockito.mock(LicenseInterceptor.class);

        configValidityFilter = new ConfigValidityFilter(mockLicenseInterceptor);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        filterChain = Mockito.mock(FilterChain.class);
    }

    @Test public void shouldCallRackWhenLicenseIsValid() throws Exception {
        Mockito.when(mockLicenseInterceptor.preHandle(request, response, null)).thenReturn(true);
        configValidityFilter.doFilter(request, response, filterChain);
        Mockito.verify(filterChain).doFilter(request, response);
    }

    @Test public void shouldNotCallRackWhenLicenseIsInvalid() throws Exception {
        Mockito.when(mockLicenseInterceptor.preHandle(request, response, null)).thenReturn(false);
        configValidityFilter.doFilter(request, response, filterChain);
        Mockito.verify(filterChain, Mockito.never()).doFilter(request, response);
    }

    @Test
    public void shouldThrowServletExceptionWhenLicenseInterceptorHasError() throws Exception {
        Mockito.when(mockLicenseInterceptor.preHandle(request, response, null)).thenThrow(new RuntimeException("foo"));
        try {
            configValidityFilter.doFilter(request, response, filterChain);
            fail("Should have thrown an exception");
        } catch (ServletException e) {
            assertThat(e.getMessage(), is("Exception while verifying license"));
        }
    }
}
