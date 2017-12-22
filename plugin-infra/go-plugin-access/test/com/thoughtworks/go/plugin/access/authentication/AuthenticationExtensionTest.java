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

package com.thoughtworks.go.plugin.access.authentication;

import com.thoughtworks.go.plugin.access.authentication.models.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthenticationExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";
    public static final String REQUEST_BODY = "expected-request";
    public static final String RESPONSE_BODY = "expected-response";

    @Mock
    private PluginManager pluginManager;
    @Mock
    private PluginSettingsJsonMessageHandler1_0 pluginSettingsJSONMessageHandler;
    @Mock
    private JsonMessageHandler1_0 jsonMessageHandler;

    private AuthenticationExtension authenticationExtension;
    private PluginSettingsConfiguration pluginSettingsConfiguration;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        authenticationExtension = new AuthenticationExtension(pluginManager);
        authenticationExtension.getPluginSettingsMessageHandlerMap().put("1.0", pluginSettingsJSONMessageHandler);
        authenticationExtension.getMessageHandlerMap().put("1.0", jsonMessageHandler);

        pluginSettingsConfiguration = new PluginSettingsConfiguration();
        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, Arrays.asList("1.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(AuthenticationExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(RESPONSE_BODY));
    }

    @Test
    public void shouldExtendAbstractExtension() throws Exception {
        assertTrue(authenticationExtension instanceof AbstractExtension);
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsConfiguration() throws Exception {
        PluginSettingsConfiguration deserializedResponse = new PluginSettingsConfiguration();
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsConfiguration(RESPONSE_BODY)).thenReturn(deserializedResponse);

        PluginSettingsConfiguration response = authenticationExtension.getPluginSettingsConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AuthenticationExtension.EXTENSION_NAME, "1.0", PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_CONFIGURATION, null);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsConfiguration(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsView() throws Exception {
        String deserializedResponse = "";
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsView(RESPONSE_BODY)).thenReturn(deserializedResponse);

        String response = authenticationExtension.getPluginSettingsView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AuthenticationExtension.EXTENSION_NAME, "1.0", PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_VIEW, null);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsView(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToValidatePluginSettings() throws Exception {
        String requestBody = "expected-request";
        when(pluginSettingsJSONMessageHandler.requestMessageForPluginSettingsValidation(pluginSettingsConfiguration)).thenReturn(requestBody);
        ValidationResult deserializedResponse = new ValidationResult();
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsValidation(RESPONSE_BODY)).thenReturn(deserializedResponse);

        ValidationResult response = authenticationExtension.validatePluginSettings(PLUGIN_ID, pluginSettingsConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), AuthenticationExtension.EXTENSION_NAME, "1.0", PluginSettingsConstants.REQUEST_VALIDATE_PLUGIN_SETTINGS, requestBody);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsValidation(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetPluginConfiguration() throws Exception {
        AuthenticationPluginConfiguration response = new AuthenticationPluginConfiguration("name", "image-url", true, true);
        when(jsonMessageHandler.responseMessageForPluginConfiguration(RESPONSE_BODY)).thenReturn(response);

        AuthenticationPluginConfiguration deserializedResponse = authenticationExtension.getPluginConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AuthenticationExtension.EXTENSION_NAME, "1.0", AuthenticationExtension.REQUEST_PLUGIN_CONFIGURATION, null);
        verify(jsonMessageHandler).responseMessageForPluginConfiguration(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToAuthenticateUser() throws Exception {
        String username = "username";
        String password = "password";
        when(jsonMessageHandler.requestMessageForAuthenticateUser(username, password)).thenReturn(REQUEST_BODY);
        User response = new User("username", "display-name", "email-id");
        when(jsonMessageHandler.responseMessageForAuthenticateUser(RESPONSE_BODY)).thenReturn(response);

        User deserializedResponse = authenticationExtension.authenticateUser(PLUGIN_ID, username, password);

        assertRequest(requestArgumentCaptor.getValue(), AuthenticationExtension.EXTENSION_NAME, "1.0", AuthenticationExtension.REQUEST_AUTHENTICATE_USER, REQUEST_BODY);
        verify(jsonMessageHandler).responseMessageForAuthenticateUser(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToSearchUser() throws Exception {
        String searchTerm = "search-term";
        when(jsonMessageHandler.requestMessageForSearchUser(searchTerm)).thenReturn(REQUEST_BODY);
        List<User> response = new ArrayList<>();
        when(jsonMessageHandler.responseMessageForSearchUser(RESPONSE_BODY)).thenReturn(response);

        List<User> deserializedResponse = authenticationExtension.searchUser(PLUGIN_ID, searchTerm);

        assertRequest(requestArgumentCaptor.getValue(), AuthenticationExtension.EXTENSION_NAME, "1.0", AuthenticationExtension.REQUEST_SEARCH_USER, REQUEST_BODY);
        verify(jsonMessageHandler).responseMessageForSearchUser(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldSerializePluginSettingsToJSON() throws Exception {
        String pluginId = "plugin_id";
        HashMap<String, String> pluginSettings = new HashMap<>();
        pluginSettings.put("key1", "val1");
        pluginSettings.put("key2", "val2");

        when(pluginManager.resolveExtensionVersion(pluginId, authenticationExtension.goSupportedVersions())).thenReturn("1.0");

        String pluginSettingsJSON = authenticationExtension.pluginSettingsJSON(pluginId, pluginSettings);

        assertThat(pluginSettingsJSON, is("{\"key1\":\"val1\",\"key2\":\"val2\"}"));
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThat(goPluginApiRequest.requestBody(), is(requestBody));
    }
}
