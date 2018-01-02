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

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.AccessDeniedException;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.ui.FilterChainOrder.EXCEPTION_TRANSLATION_FILTER;

public class DenyGoCDAccessForArtifactsFilterTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldDenyGoCDAccessForRequestsReferredFromArtifacts() throws Exception {
        when(request.getHeader("Referer")).thenReturn("http://example.com/go/files/foo");
        when(request.getRequestURI()).thenReturn("/go/api/admin");

        new DenyGoCDAccessForArtifactsFilter().doFilterHttp(request, response, chain);

        verifyNoMoreInteractions(chain);
    }

    @Test
    public void shouldAllowArtifactsToRequestOtherArtifacts() throws Exception {
        when(request.getHeader("Referer")).thenReturn("http://example.com/go/files/file1");
        when(request.getRequestURI()).thenReturn("/go/files/file2");

        new DenyGoCDAccessForArtifactsFilter().doFilterHttp(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void shouldDoNothingInAbsenceOfReferer() throws Exception {
        when(request.getHeader("Referer")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/go/files/file2");

        new DenyGoCDAccessForArtifactsFilter().doFilterHttp(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldNormalizeRequestUrlsBeforeComparing() throws Exception {
        when(request.getHeader("Referer")).thenReturn("http://example.com/go/files/foo");
        when(request.getRequestURI()).thenReturn("go/files/../admin/config_xml");

        new DenyGoCDAccessForArtifactsFilter().doFilterHttp(request, response, chain);

        verifyNoMoreInteractions(chain);
    }

    @Test
    public void shouldHaveAnOrderHigherThanExceptionTranslationFilter() throws Exception {
        assertThat(new DenyGoCDAccessForArtifactsFilter().getOrder(), greaterThan(EXCEPTION_TRANSLATION_FILTER));
    }
}