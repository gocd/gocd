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

import com.google.gson.JsonObject;
import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.server.service.DrainModeService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class ModeAwareFilterTest {
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private DrainModeService drainModeService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private PrintWriter writer;

    private ModeAwareFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        initMocks(this);
        when(response.getWriter()).thenReturn(writer);
        filter = new ModeAwareFilter(systemEnvironment, drainModeService);
    }

    @Test
    void shouldNotBlockNonGetRequestWhenInActiveStateAndNotUnderDrainMode() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(false);

        request = HttpRequestBuilder.GET("/foo").build();
        filter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        request = HttpRequestBuilder.HEAD("/foo").build();
        filter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        request = HttpRequestBuilder.POST("/foo").build();
        filter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        request = HttpRequestBuilder.PUT("/foo").build();
        filter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        request = HttpRequestBuilder.DELETE("/foo").build();
        filter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldNotBlockGetOrHeadRequestWhenInPassiveState() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(false);
        when(drainModeService.isDrainMode()).thenReturn(false);

        request = HttpRequestBuilder.GET("/foo").build();
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);

        request = HttpRequestBuilder.HEAD("/foo").build();
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldNotBlockGetOrHeadRequestWhenInDrainMode() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(true);

        request = HttpRequestBuilder.GET("/foo").build();
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);

        request = HttpRequestBuilder.HEAD("/foo").build();
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldBlockNonGetRequestWhenInPassiveState() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(false);
        when(drainModeService.isDrainMode()).thenReturn(false);

        request = HttpRequestBuilder.POST("/foo").build();
        filter.doFilter(request, response, filterChain);

        request = HttpRequestBuilder.PUT("/foo").build();
        filter.doFilter(request, response, filterChain);

        request = HttpRequestBuilder.DELETE("/foo").build();
        filter.doFilter(request, response, filterChain);

        request = HttpRequestBuilder.PATCH("/foo").build();
        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldBlockNonGetRequestWhenInDrainMode() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(true);

        request = HttpRequestBuilder.POST("/foo").build();
        filter.doFilter(request, response, filterChain);

        request = HttpRequestBuilder.PUT("/foo").build();
        filter.doFilter(request, response, filterChain);

        request = HttpRequestBuilder.DELETE("/foo").build();
        filter.doFilter(request, response, filterChain);

        request = HttpRequestBuilder.PATCH("/foo").build();
        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldAllowLoginPostRequestInPassiveState() throws Exception {
        request = HttpRequestBuilder.POST("/auth/security_check").build();

        when(systemEnvironment.isServerActive()).thenReturn(false);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowLoginPostRequestInDrainMode() throws Exception {
        request = HttpRequestBuilder.POST("/auth/security_check").build();

        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(true);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowSwitchToActiveStateChangePostRequestInPassiveState() throws Exception {
        request = HttpRequestBuilder.POST("/api/state/active").build();

        when(systemEnvironment.isServerActive()).thenReturn(false);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRedirectToPassiveServerErrorPageForNonGetRequestWhenInPassiveState() throws Exception {
        request = HttpRequestBuilder.POST("/foo").build();

        when(systemEnvironment.isServerActive()).thenReturn(false);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).sendRedirect("/go/errors/inactive");
    }

    @Test
    public void shouldReturn503WhenPOSTCallIsMadeWhileServerIsInDrainMode() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(true);

        request = HttpRequestBuilder.POST("/pipelines").build();

        filter.doFilter(request, response, filterChain);

        verify(response, times(1)).setContentType("text/html");
        verify(writer).print(filter.generateHTMLResponse());
        verify(response).setHeader("Cache-Control", "private, max-age=0, no-cache");
        verify(response).setDateHeader("Expires", 0);
        verify(response).setStatus(503);
    }

    @Test
    public void shouldAllowStageCancelPOSTCallWhileServerIsInDrainMode() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(true);

        request = HttpRequestBuilder.POST("/api/stages/1/cancel").build();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void shouldAllowStageCancelPOSTNewAPICallWhileServerIsInDrainMode() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(true);

        request = HttpRequestBuilder.POST("/api/stages/up42_pipeline/up42_stage/cancel").build();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void shouldAllowDrainModeTogglePOSTCallWhileServerIsInDrainMode() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(true);

        request = HttpRequestBuilder.POST("/api/drain_mode/settings").build();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void shouldReturn503WhenPOSTAPICallIsMadeWhileServerIsInDrainMode() throws Exception {
        when(systemEnvironment.isServerActive()).thenReturn(true);
        when(drainModeService.isDrainMode()).thenReturn(true);

        request = HttpRequestBuilder.POST("/api/state/active").withHeader("content-type", "application/json").build();

        filter.doFilter(request, response, filterChain);

        verify(response, times(1)).setContentType("application/json");
        JsonObject json = new JsonObject();
        json.addProperty("message", "server is in drain state (Maintenance mode), please try later.");
        verify(writer).print(json);

        verify(response).setHeader("Cache-Control", "private, max-age=0, no-cache");
        verify(response).setDateHeader("Expires", 0);
        verify(response).setStatus(503);
    }

    private Request request(HttpMethod method, String contentType, String uri) {
        Request request = new Request(mock(HttpChannel.class), mock(HttpInput.class));
        HttpURI httpURI = new HttpURI("https", "url", 8153, uri);
        MetaData.Request metadata = new MetaData.Request(method.asString(), httpURI, HttpVersion.HTTP_2, new HttpFields());
        request.setMetaData(metadata);
        request.setContentType(contentType);
        request.setHttpURI(httpURI);
        return request;
    }
}
