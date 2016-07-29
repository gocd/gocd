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

package com.thoughtworks.go.server.service.plugins.processor.session;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SessionRequestProcessorTest {
    @Mock
    private PluginRequestProcessorRegistry applicationAccessor;
    @Mock
    private HttpSession session;
    @Mock
    private JsonMessageHandler jsonMessageHandler;
    @Mock
    private GoPluginDescriptor pluginDescriptor;
    private SessionRequestProcessor processor;

    @Before
    public void setUp() {
        initMocks(this);

        processor = new SessionRequestProcessor(applicationAccessor);
        processor.getMessageHandlerMap().put("1.0", jsonMessageHandler);
    }

    @Test
    public void shouldRegisterItselfForRequestProcessing() {
        verify(applicationAccessor).registerProcessorFor(SessionRequestProcessor.PUT_INTO_SESSION, processor);
        verify(applicationAccessor).registerProcessorFor(SessionRequestProcessor.GET_FROM_SESSION, processor);
        verify(applicationAccessor).registerProcessorFor(SessionRequestProcessor.REMOVE_FROM_SESSION, processor);
    }

    @Test
    public void shouldHandleIncorrectAPIVersion() {
        GoApiResponse response = processor.process(pluginDescriptor, getGoPluginApiRequest("1.1", null, null));
        assertThat(response.responseCode(), is(500));
    }

    @Test
    public void shouldPutIntoSession() {
        String pluginId = "plugin-id-1";
        String requestBody = "expected-request";
        Map<String, String> sessionData = new HashMap<String, String>();
        sessionData.put("k1", "v1");
        sessionData.put("k2", "v2");
        when(jsonMessageHandler.requestMessageSessionPut(requestBody)).thenReturn(new SessionData(pluginId, sessionData));

        SessionRequestProcessor applicationAccessorSpy = spy(processor);
        doReturn(session).when(applicationAccessorSpy).getUserSession();

        GoApiResponse response = applicationAccessorSpy.process(pluginDescriptor, getGoPluginApiRequest("1.0", SessionRequestProcessor.PUT_INTO_SESSION, requestBody));

        assertThat(response.responseCode(), is(200));
        verify(session).setAttribute(pluginId, sessionData);
    }

    @Test
    public void shouldHandleSessionNotFoundDuringPutIntoSession() {
        String pluginId = "plugin-id-1";
        String requestBody = "expected-request";
        when(jsonMessageHandler.requestMessageSessionPut(requestBody)).thenReturn(new SessionData(pluginId, null));

        SessionRequestProcessor applicationAccessorSpy = spy(processor);
        doReturn(null).when(applicationAccessorSpy).getUserSession();

        GoApiResponse response = applicationAccessorSpy.process(pluginDescriptor, getGoPluginApiRequest("1.0", SessionRequestProcessor.PUT_INTO_SESSION, requestBody));

        assertThat(response.responseCode(), is(500));
    }

    @Test
    public void shouldGetFromSession() {
        String pluginId = "plugin-id-1";
        Map<String, String> sessionData = new HashMap<String, String>();
        sessionData.put("k3", "v3");
        sessionData.put("k4", "v4");
        when(session.getAttribute(pluginId)).thenReturn(sessionData);

        String requestBody = "expected-request";
        when(jsonMessageHandler.requestMessageSessionGetAndRemove(requestBody)).thenReturn(pluginId);

        SessionRequestProcessor applicationAccessorSpy = spy(processor);
        doReturn(session).when(applicationAccessorSpy).getUserSession();

        GoApiResponse response = applicationAccessorSpy.process(pluginDescriptor, getGoPluginApiRequest("1.0", SessionRequestProcessor.GET_FROM_SESSION, requestBody));

        assertThat(response.responseCode(), is(200));
        assertEquals(JsonHelper.fromJson(response.responseBody(), Map.class), sessionData);
        verify(session).getAttribute(pluginId);
    }

    @Test
    public void shouldHandleSessionNotFoundDuringGetFromSession() {
        String pluginId = "plugin-id-1";
        String requestBody = "expected-request";
        when(jsonMessageHandler.requestMessageSessionGetAndRemove(requestBody)).thenReturn(pluginId);

        SessionRequestProcessor applicationAccessorSpy = spy(processor);
        doReturn(null).when(applicationAccessorSpy).getUserSession();

        GoApiResponse response = applicationAccessorSpy.process(pluginDescriptor, getGoPluginApiRequest("1.0", SessionRequestProcessor.GET_FROM_SESSION, requestBody));

        assertThat(response.responseCode(), is(500));
    }

    @Test
    public void shouldRemoveSession() {
        String pluginId = "plugin-id-1";
        String requestBody = "expected-request";
        when(jsonMessageHandler.requestMessageSessionGetAndRemove(requestBody)).thenReturn(pluginId);

        SessionRequestProcessor applicationAccessorSpy = spy(processor);
        doReturn(session).when(applicationAccessorSpy).getUserSession();

        GoApiResponse response = applicationAccessorSpy.process(pluginDescriptor, getGoPluginApiRequest("1.0", SessionRequestProcessor.REMOVE_FROM_SESSION, requestBody));

        assertThat(response.responseCode(), is(200));
        verify(session).removeAttribute(pluginId);
    }

    @Test
    public void shouldHandleSessionNotFoundDuringRemoveFromSession() {
        String pluginId = "plugin-id-1";
        String requestBody = "expected-request";
        when(jsonMessageHandler.requestMessageSessionGetAndRemove(requestBody)).thenReturn(pluginId);

        SessionRequestProcessor applicationAccessorSpy = spy(processor);
        doReturn(null).when(applicationAccessorSpy).getUserSession();

        GoApiResponse response = applicationAccessorSpy.process(pluginDescriptor, getGoPluginApiRequest("1.0", SessionRequestProcessor.REMOVE_FROM_SESSION, requestBody));

        assertThat(response.responseCode(), is(500));
    }

    private GoApiRequest getGoPluginApiRequest(final String apiVersion, final String authenticateUserRequest, final String requestBody) {
        return new GoApiRequest() {
            @Override
            public String api() {
                return authenticateUserRequest;
            }

            @Override
            public String apiVersion() {
                return apiVersion;
            }

            @Override
            public GoPluginIdentifier pluginIdentifier() {
                return null;
            }

            @Override
            public Map<String, String> requestParameters() {
                return null;
            }

            @Override
            public Map<String, String> requestHeaders() {
                return null;
            }

            @Override
            public String requestBody() {
                return requestBody;
            }
        };
    }
}
