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
package com.thoughtworks.go.plugin.access;

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PluginRequestHelperTest {

    private PluginManager pluginManager;
    private PluginRequestHelper helper;
    private boolean[] isSuccessInvoked;
    private String pluginId = "pid";
    private GoPluginApiResponse response;
    private final String requestName = "req";
    private final String extensionName = "some-extension";
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        pluginManager = mock(PluginManager.class);
        helper = new PluginRequestHelper(pluginManager, asList("1.0"), extensionName);
        isSuccessInvoked = new boolean[]{false};
        response = mock(GoPluginApiResponse.class);
        when(pluginManager.isPluginOfType(extensionName, pluginId)).thenReturn(true);
    }

    @Test
    public void shouldNotInvokeSuccessBlockOnFailureResponse() {
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.INTERNAL_ERROR);
        when(response.responseBody()).thenReturn("junk");
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);
        try {
            helper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Object>() {
                @Override
                public Object onSuccess(String responseBody, String resolvedExtensionVersion) {
                    isSuccessInvoked[0] = true;
                    return null;
                }
            });
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Interaction with plugin with id 'pid' implementing 'some-extension' extension failed while requesting for 'req'. Reason: [The plugin sent a response that could not be understood by Go. Plugin returned with code '500' and the following response: 'junk']"));
            assertThat(e.getCause().getMessage(), is("The plugin sent a response that could not be understood by Go. Plugin returned with code '500' and the following response: 'junk'"));
            assertFalse(isSuccessInvoked[0]);
            verify(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));
        }
    }

    @Test
    public void shouldInvokeSuccessBlockOnSuccessfulResponse() {
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);

        helper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Object>() {
            @Override
            public Object onSuccess(String responseBody, String resolvedExtensionVersion) {
                isSuccessInvoked[0] = true;
                return null;
            }
        });
        assertTrue(isSuccessInvoked[0]);
        verify(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));
    }

    @Test
    public void shouldErrorOutOnValidationFailure() {
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.VALIDATION_ERROR);
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);

        thrown.expect(RuntimeException.class);

        helper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Object>() {
            @Override
            public Object onSuccess(String responseBody, String resolvedExtensionVersion) {
                isSuccessInvoked[0] = true;
                return null;
            }
        });
    }

    @Test
    public void shouldConstructTheRequest() {
        final String requestBody = "request_body";
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);

        final GoPluginApiRequest[] generatedRequest = {null};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                generatedRequest[0] = (GoPluginApiRequest) invocationOnMock.getArguments()[1];
                return response;
            }
        }).when(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));


        helper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Object>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return requestBody;
            }
        });
        assertThat(generatedRequest[0].requestBody(), is(requestBody));
        assertThat(generatedRequest[0].extension(), is(extensionName));
        assertThat(generatedRequest[0].requestName(), is(requestName));
        assertTrue(generatedRequest[0].requestParameters().isEmpty());
    }

    @Test
    public void shouldConstructTheRequestWithRequestParams() {
        final String requestBody = "request_body";
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);

        final GoPluginApiRequest[] generatedRequest = {null};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                generatedRequest[0] = (GoPluginApiRequest) invocationOnMock.getArguments()[1];
                return response;
            }
        }).when(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));


        helper.submitRequest(pluginId, requestName, new PluginInteractionCallback<Object>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return requestBody;
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                final HashMap params = new HashMap();
                params.put("p1", "v1");
                params.put("p2", "v2");
                return params;
            }

            @Override
            public Map<String, String> requestHeaders(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public Object onSuccess(String responseBody, String resolvedExtensionVersion) {
                return null;
            }
        });

        assertThat(generatedRequest[0].requestBody(), is(requestBody));
        assertThat(generatedRequest[0].extension(), is(extensionName));
        assertThat(generatedRequest[0].requestName(), is(requestName));
        assertThat(generatedRequest[0].requestParameters().size(), is(2));
        assertThat(generatedRequest[0].requestParameters().get("p1"), is("v1"));
        assertThat(generatedRequest[0].requestParameters().get("p2"), is("v2"));
    }

    @Test
    public void shouldConstructTheRequestWithRequestHeaders() {
        final String requestBody = "request_body";
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);

        final GoPluginApiRequest[] generatedRequest = {null};
        doAnswer(invocationOnMock -> {
            generatedRequest[0] = (GoPluginApiRequest) invocationOnMock.getArguments()[1];
            return response;
        }).when(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));


        helper.submitRequest(pluginId, requestName, new PluginInteractionCallback<Object>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return requestBody;
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public Map<String, String> requestHeaders(String resolvedExtensionVersion) {
                final Map<String, String> headers = new HashMap();
                headers.put("HEADER-1", "HEADER-VALUE-1");
                headers.put("HEADER-2", "HEADER-VALUE-2");
                return headers;
            }

            @Override
            public Object onSuccess(String responseBody, String resolvedExtensionVersion) {
                return null;
            }
        });

        assertThat(generatedRequest[0].requestBody(), is(requestBody));
        assertThat(generatedRequest[0].extension(), is(extensionName));
        assertThat(generatedRequest[0].requestName(), is(requestName));
        assertThat(generatedRequest[0].requestHeaders().size(), is(2));
        assertThat(generatedRequest[0].requestHeaders().get("HEADER-1"), is("HEADER-VALUE-1"));
        assertThat(generatedRequest[0].requestHeaders().get("HEADER-2"), is("HEADER-VALUE-2"));
    }
}