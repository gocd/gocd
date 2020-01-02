/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

import static com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert.assertThat;
import static org.mockito.Mockito.*;

class DenyGoCDAccessForArtifactsFilterChainTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Filter filter;
    private FilterChain filterChain;

    public static ServletResponse wrap(MockHttpServletResponse response) {
        return argThat(new ArgumentMatcher<ServletResponse>() {
            @Override
            public boolean matches(ServletResponse actualResponse) {
                while (actualResponse instanceof HttpServletResponseWrapper) {
                    actualResponse = ((HttpServletResponseWrapper) actualResponse).getResponse();
                }

                return actualResponse == response;
            }
        });
    }

    public static ServletRequest wrap(MockHttpServletRequest request) {
        return argThat(new ArgumentMatcher<ServletRequest>() {
            @Override
            public boolean matches(ServletRequest actualRequest) {
                while (actualRequest instanceof HttpServletRequestWrapper) {
                    actualRequest = ((HttpServletRequestWrapper) actualRequest).getRequest();
                }

                return actualRequest == request;
            }
        });
    }

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        filter = new DenyGoCDAccessForArtifactsFilterChain();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowAccessToFiles() throws IOException, ServletException {
        request = HttpRequestBuilder.GET("/files/foo.zip").build();
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(wrap(request), wrap(response));
    }

    @Test
    void shouldNotAllowAccessToOtherUrlsIfRefererIsFiles() throws IOException, ServletException {
        request = HttpRequestBuilder.GET("/api/something-bad")
                .withHeader("Referer", "https://foo.com/go/files/hax/index.html")
                .build();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response)
                .isBadRequest();
    }

    @Test
    void shouldAllowAccessToOtherUrlsIfRefererIsNotPresent() throws IOException, ServletException {
        request = HttpRequestBuilder.GET("/api/something-bad")
                .withHeader("Referer", "")
                .build();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(wrap(request), wrap(response));
        assertThat(response)
                .isOk();
    }
}
