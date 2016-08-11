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
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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
    public static final String PLUGIN_ID = "plugin.id";
    public static final String REQUEST_NAME = "request.name";

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
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(200));
        when(pluginManager.isPluginOfType(any(String.class), any(String.class))).thenReturn(true);

        Map<String, String[]> springParameterMap = new HashMap<String, String[]>();
        springParameterMap.put("k1", new String[]{"v1"});
        springParameterMap.put("k2", new String[]{"v2", "v3"});
        springParameterMap.put("k3", new String[]{});
        springParameterMap.put("k4", null);
        when(servletRequest.getParameterMap()).thenReturn(springParameterMap);

        List<String> elements = Arrays.asList("h1", "h2", "h3");
        when(servletRequest.getHeader("h1")).thenReturn("v1");
        when(servletRequest.getHeader("h2")).thenReturn("");
        when(servletRequest.getHeader("h3")).thenReturn(null);
        when(servletRequest.getHeaderNames()).thenReturn(getMockEnumeration(elements));

        pluginController.handlePluginInteractRequest(PLUGIN_ID, REQUEST_NAME, servletRequest);

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("k1", "v1");
        requestParameters.put("k2", "v2");
        requestParameters.put("k3", null);
        requestParameters.put("k4", null);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("h1", "v1");
        requestHeaders.put("h2", "");
        requestHeaders.put("h3", null);
        assertRequest(requestArgumentCaptor.getValue(), REQUEST_NAME, requestParameters, requestHeaders);
    }

    @Test
    public void shouldRenderPluginResponseWithDefaultContentTypeOn200() throws Exception {
        when(pluginManager.isPluginOfType(any(String.class), any(String.class))).thenReturn(true);
        DefaultGoPluginApiResponse apiResponse = new DefaultGoPluginApiResponse(200);
        String responseBody = "response-body";
        apiResponse.setResponseBody(responseBody);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(apiResponse);

        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());
        when(servletRequest.getHeaderNames()).thenReturn(getMockEnumeration(new ArrayList<String>()));

        ModelAndView modelAndView = pluginController.handlePluginInteractRequest(PLUGIN_ID, REQUEST_NAME, servletRequest);
        modelAndView.getView().render(null, servletRequest, servletResponse);

        assertThat(modelAndView.getView().getContentType(), is(PluginController.CONTENT_TYPE_HTML));
        verify(writer).write(responseBody);
        assertRequest(requestArgumentCaptor.getValue(), REQUEST_NAME, new HashMap<String, String>(), new HashMap<String, String>());
    }

    @Test
    public void shouldRenderPluginResponseWithSpecifiedContentTypeOn200() throws Exception {
        when(pluginManager.isPluginOfType(any(String.class), any(String.class))).thenReturn(true);
        DefaultGoPluginApiResponse apiResponse = new DefaultGoPluginApiResponse(200);
        String contentType = "image/png";
        apiResponse.responseHeaders().put("Content-Type", contentType);
        String responseBody = "response-body";
        apiResponse.setResponseBody(responseBody);
        when(pluginManager.submitTo(eq(PLUGIN_ID), any(GoPluginApiRequest.class))).thenReturn(apiResponse);

        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());
        when(servletRequest.getHeaderNames()).thenReturn(getMockEnumeration(new ArrayList<String>()));

        ModelAndView modelAndView = pluginController.handlePluginInteractRequest(PLUGIN_ID, REQUEST_NAME, servletRequest);
        modelAndView.getView().render(null, servletRequest, servletResponse);

        assertThat(modelAndView.getView().getContentType(), is(contentType));
        verify(writer).write(responseBody);
    }

    @Test
    public void shouldRedirectToSpecifiedLocationOn302() throws Exception {
        when(pluginManager.isPluginOfType(any(String.class), any(String.class))).thenReturn(true);
        DefaultGoPluginApiResponse apiResponse = new DefaultGoPluginApiResponse(302);
        String redirectLocation = "/go/plugin/interact/plugin.id/request.name";
        apiResponse.responseHeaders().put("Location", redirectLocation);
        when(pluginManager.submitTo(eq(PLUGIN_ID), any(GoPluginApiRequest.class))).thenReturn(apiResponse);

        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());
        when(servletRequest.getHeaderNames()).thenReturn(getMockEnumeration(new ArrayList<String>()));

        ModelAndView modelAndView = pluginController.handlePluginInteractRequest(PLUGIN_ID, REQUEST_NAME, servletRequest);


        assertThat(modelAndView.getViewName(), is("redirect:" + redirectLocation));
    }

    @Test
    public void shouldAllowInteractionOnlyForAuthPlugins() {
        when(pluginManager.isPluginOfType("authentication", "github.pr")).thenReturn(false);

        ModelAndView modelAndView = pluginController.handlePluginInteractRequest(PLUGIN_ID, REQUEST_NAME, servletRequest);
        ResponseCodeView view = (ResponseCodeView) modelAndView.getView();

        assertThat(view.getStatusCode(), is(403));
    }

    @Test
    public void shouldDisallowRequestsWhichNeedAuthentication() {
        when(pluginManager.isPluginOfType(any(String.class), any(String.class))).thenReturn(true);

        List<String> restrictedRequests = Arrays.asList("go.plugin-settings.get-configuration",
                                                        "go.plugin-settings.get-view",
                                                        "go.plugin-settings.validate-configuration",
                                                        "go.authentication.plugin-configuration",
                                                        "go.authentication.authenticate-user",
                                                        "go.authentication.search-user");

        for (String requestName : restrictedRequests) {
            ModelAndView modelAndView = pluginController.handlePluginInteractRequest(PLUGIN_ID, requestName, servletRequest);
            ResponseCodeView view = (ResponseCodeView) modelAndView.getView();

            assertThat(view.getStatusCode(), is(403));
        }
    }

    private Enumeration<String> getMockEnumeration(List<String> elements) {
        Enumeration<String> enumeration = new Enumeration<String>() {
            private List<String> elements;
            int i = 0;

            @Override
            public boolean hasMoreElements() {
                return i < elements.size();
            }

            @Override
            public String nextElement() {
                return elements.get(i++);
            }
        };
        ReflectionUtil.setField(enumeration, "elements", elements);
        return enumeration;
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String requestName, Map<String, String> requestParameters, Map<String, String> requestHeaders) {
        assertThat(goPluginApiRequest.extension(), is(nullValue()));
        assertThat(goPluginApiRequest.extensionVersion(), is(nullValue()));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertEquals(requestParameters, goPluginApiRequest.requestParameters());
        assertEquals(requestHeaders, goPluginApiRequest.requestHeaders());
        assertThat(goPluginApiRequest.requestBody(), is(nullValue()));
    }
}
