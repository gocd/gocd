/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
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

    @BeforeEach
    void setup() {
        pluginManager = mock(PluginManager.class);
        helper = new PluginRequestHelper(pluginManager, asList("1.0"), extensionName);
        isSuccessInvoked = new boolean[]{false};
        response = mock(GoPluginApiResponse.class);
        when(pluginManager.isPluginOfType(extensionName, pluginId)).thenReturn(true);
    }

    @Test
    void shouldNotInvokeSuccessBlockOnFailureResponse() {
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.INTERNAL_ERROR);
        when(response.responseBody()).thenReturn("junk");
        when(pluginManager.submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class))).thenReturn(response);
        try {
            helper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Object>() {
                @Override
                public Object onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                    isSuccessInvoked[0] = true;
                    return null;
                }
            });
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("The plugin sent a response that could not be understood by Go. Plugin returned with code '500' and the following response: 'junk'");
            assertThat(isSuccessInvoked[0]).isFalse();
            verify(pluginManager).submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class));
        }
    }

    @Test
    void shouldInvokeSuccessBlockOnSuccessfulResponse() {
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);
        when(pluginManager.submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class))).thenReturn(response);

        helper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Object>() {
            @Override
            public Object onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                isSuccessInvoked[0] = true;
                return null;
            }
        });
        assertThat(isSuccessInvoked[0]).isTrue();
        verify(pluginManager).submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class));
    }

    @Test
    void shouldErrorOutOnValidationFailure() {
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.VALIDATION_ERROR);
        when(pluginManager.submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class))).thenReturn(response);

        thrown.expect(RuntimeException.class);

        helper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Object>() {
            @Override
            public Object onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                isSuccessInvoked[0] = true;
                return null;
            }
        });
    }

    @Test
    void shouldConstructTheRequest() {
        final String requestBody = "request_body";
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);

        final GoPluginApiRequest[] generatedRequest = {null};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                generatedRequest[0] = (GoPluginApiRequest) invocationOnMock.getArguments()[2];
                return response;
            }
        }).when(pluginManager).submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class));


        helper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Object>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return requestBody;
            }
        });
        assertThat(generatedRequest[0].requestBody()).isEqualTo(requestBody);
        assertThat(generatedRequest[0].extension()).isEqualTo(extensionName);
        assertThat(generatedRequest[0].requestName()).isEqualTo(requestName);
        assertThat(generatedRequest[0].requestParameters().isEmpty()).isTrue();
    }

    @Test
    void shouldConstructTheRequestWithRequestParams() {
        final String requestBody = "request_body";
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);

        final GoPluginApiRequest[] generatedRequest = {null};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                generatedRequest[0] = (GoPluginApiRequest) invocationOnMock.getArguments()[2];
                return response;
            }
        }).when(pluginManager).submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class));


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
            public Object onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public void onFailure(int responseCode, String responseBody, String resolvedExtensionVersion) {
            }
        });

        assertThat(generatedRequest[0].requestBody()).isEqualTo(requestBody);
        assertThat(generatedRequest[0].extension()).isEqualTo(extensionName);
        assertThat(generatedRequest[0].requestName()).isEqualTo(requestName);
        assertThat(generatedRequest[0].requestParameters().size()).isEqualTo(2);
        assertThat(generatedRequest[0].requestParameters().get("p1")).isEqualTo("v1");
        assertThat(generatedRequest[0].requestParameters().get("p2")).isEqualTo("v2");
    }

    @Test
    void shouldConstructTheRequestWithRequestHeaders() {
        final String requestBody = "request_body";
        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);

        final GoPluginApiRequest[] generatedRequest = {null};
        doAnswer(invocationOnMock -> {
            generatedRequest[0] = (GoPluginApiRequest) invocationOnMock.getArguments()[2];
            return response;
        }).when(pluginManager).submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class));


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
            public Object onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public void onFailure(int responseCode, String responseBody, String resolvedExtensionVersion) {
            }
        });

        assertThat(generatedRequest[0].requestBody()).isEqualTo(requestBody);
        assertThat(generatedRequest[0].extension()).isEqualTo(extensionName);
        assertThat(generatedRequest[0].requestName()).isEqualTo(requestName);
        assertThat(generatedRequest[0].requestHeaders().size()).isEqualTo(2);
        assertThat(generatedRequest[0].requestHeaders().get("HEADER-1")).isEqualTo("HEADER-VALUE-1");
        assertThat(generatedRequest[0].requestHeaders().get("HEADER-2")).isEqualTo("HEADER-VALUE-2");
    }

    @Test
    void shouldInvokeOnFailureCallbackWhenResponseCodeOtherThan200() {
        PluginInteractionCallback pluginInteractionCallback = mock(PluginInteractionCallback.class);

        when(response.responseCode()).thenReturn(400);
        when(response.responseBody()).thenReturn("Error response");
        when(pluginManager.submitTo(eq(pluginId), eq(extensionName), any(GoPluginApiRequest.class))).thenReturn(response);
        when(pluginManager.resolveExtensionVersion(eq(pluginId), eq(extensionName), anyList())).thenReturn("1.0");

        assertThatCode(() -> helper.submitRequest(pluginId, requestName, pluginInteractionCallback))
                .isInstanceOf(RuntimeException.class);

        verify(pluginInteractionCallback).onFailure(400, "Error response", "1.0");
    }
}