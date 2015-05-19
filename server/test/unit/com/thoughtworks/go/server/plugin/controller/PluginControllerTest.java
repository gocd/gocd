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

package com.thoughtworks.go.server.plugin.controller;

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginControllerTest {
    @Mock
    private PluginManager pluginManager;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse servletResponse;
    @Mock
    private PrintWriter writer;

    private PluginController pluginController;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(servletResponse.getWriter()).thenReturn(writer);

        pluginController = new PluginController(pluginManager);
    }

    @Test
    public void shouldForwardWebRequestToPlugin() throws Exception {
        when(pluginManager.submitTo(eq("plugin.id"), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(200));

        Map<String, String[]> springParameterMap = new HashMap<String, String[]>();
        springParameterMap.put("k1", new String[]{"v1"});
        springParameterMap.put("k2", new String[]{"v2", "v3"});
        springParameterMap.put("k3", new String[]{});
        springParameterMap.put("k4", null);
        when(servletRequest.getParameterMap()).thenReturn(springParameterMap);

        pluginController.handlePluginInteractRequest("plugin.id", "request.name", servletRequest);

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("k1", "v1");
        requestParameters.put("k2", "v2");
        requestParameters.put("k3", null);
        requestParameters.put("k4", null);
        assertRequest(requestArgumentCaptor.getValue(), "request.name", requestParameters);
    }

    @Test
    public void shouldRenderPluginResponseWithDefaultContentTypeOn200() throws Exception {
        DefaultGoPluginApiResponse apiResponse = new DefaultGoPluginApiResponse(200);
        String responseBody = "response-body";
        apiResponse.setResponseBody(responseBody);
        when(pluginManager.submitTo(eq("plugin.id"), any(GoPluginApiRequest.class))).thenReturn(apiResponse);

        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());

        ModelAndView modelAndView = pluginController.handlePluginInteractRequest("plugin.id", "request.name", servletRequest);
        modelAndView.getView().render(null, servletRequest, servletResponse);

        assertThat(modelAndView.getView().getContentType(), is(PluginController.CONTENT_TYPE_HTML));
        verify(writer).write(responseBody);
    }

    @Test
    public void shouldRenderPluginResponseWithSpecifiedContentTypeOn200() throws Exception {
        DefaultGoPluginApiResponse apiResponse = new DefaultGoPluginApiResponse(200);
        String contentType = "image/png";
        apiResponse.responseHeaders().put("Content-Type", contentType);
        String responseBody = "response-body";
        apiResponse.setResponseBody(responseBody);
        when(pluginManager.submitTo(eq("plugin.id"), any(GoPluginApiRequest.class))).thenReturn(apiResponse);

        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());

        ModelAndView modelAndView = pluginController.handlePluginInteractRequest("plugin.id", "request.name", servletRequest);
        modelAndView.getView().render(null, servletRequest, servletResponse);

        assertThat(modelAndView.getView().getContentType(), is(contentType));
        verify(writer).write(responseBody);
    }

    @Test
    public void shouldRedirectToSpecifiedLocationOn302() throws Exception {
        DefaultGoPluginApiResponse apiResponse = new DefaultGoPluginApiResponse(302);
        String redirectLocation = "/go/plugin/interact/plugin.id/request.name";
        apiResponse.responseHeaders().put("Location", redirectLocation);
        when(pluginManager.submitTo(eq("plugin.id"), any(GoPluginApiRequest.class))).thenReturn(apiResponse);

        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());

        ModelAndView modelAndView = pluginController.handlePluginInteractRequest("plugin.id", "request.name", servletRequest);


        assertThat(modelAndView.getViewName(), is("redirect:" + redirectLocation));
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String requestName, Map<String, String> requestParameters) {
        assertThat(goPluginApiRequest.extension(), is(nullValue()));
        assertThat(goPluginApiRequest.extensionVersion(), is(nullValue()));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertEquals(requestParameters, goPluginApiRequest.requestParameters());
        assertThat(goPluginApiRequest.requestBody(), is(nullValue()));
    }
}
