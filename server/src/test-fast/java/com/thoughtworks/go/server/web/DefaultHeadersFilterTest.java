/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class DefaultHeadersFilterTest {

    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private ServletRequest request;
    @Mock
    private FeatureToggleService featureToggleService;
    private DefaultHeadersFilter filter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        filter = new DefaultHeadersFilter();
        Toggles.initializeWith(featureToggleService);
    }

    @Test
    public void shouldAddDefaultHeaders() throws Exception {
        when(featureToggleService.isToggleOn(Toggles.USE_HSTS_HEADER)).thenReturn(true);
        filter.doFilter(request, response, chain);

        verify(response).isCommitted();
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response).setHeader("X-UA-Compatible", "chrome=1");
        verify(response).setHeader("Strict-Transport-Security", "31536000");
    }

    @Test
    public void shouldNotAddHstsHeaderWhenToggledOff() throws ServletException, IOException {
        when(featureToggleService.isToggleOn(Toggles.USE_HSTS_HEADER)).thenReturn(false);
        filter.doFilter(request, response, chain);

        verify(response).isCommitted();
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response).setHeader("X-UA-Compatible", "chrome=1");
        verify(response, never()).setHeader(eq("Strict-Transport-Security"), anyString());
    }

    @Test
    public void shouldNotAddDefaultHeadersIfResponseIsCommitted() throws Exception {
        when(response.isCommitted()).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(response).isCommitted();
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldPropogateFilterChain() throws Exception {
        DefaultHeadersFilter filter = new DefaultHeadersFilter();
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
