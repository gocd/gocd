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

package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.notification.v1.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class NotificationExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";
    public static final String RESPONSE_BODY = "expected-response";

    @Mock
    private PluginManager pluginManager;
    @Mock
    private PluginSettingsJsonMessageHandler1_0 pluginSettingsJSONMessageHandler;
    @Mock
    private JsonMessageHandler1_0 jsonMessageHandler;

    private NotificationExtension notificationExtension;
    private PluginSettingsConfiguration pluginSettingsConfiguration;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        notificationExtension = new NotificationExtension(pluginManager);
        notificationExtension.getPluginSettingsMessageHandlerMap().put("1.0", pluginSettingsJSONMessageHandler);
        notificationExtension.getMessageHandlerMap().put("1.0", jsonMessageHandler);

        pluginSettingsConfiguration = new PluginSettingsConfiguration();
        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, Arrays.asList("1.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(NotificationExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(RESPONSE_BODY));
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsConfiguration() throws Exception {
        PluginSettingsConfiguration deserializedResponse = new PluginSettingsConfiguration();
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsConfiguration(RESPONSE_BODY)).thenReturn(deserializedResponse);

        PluginSettingsConfiguration response = notificationExtension.getPluginSettingsConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), NotificationExtension.EXTENSION_NAME, "1.0", PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_CONFIGURATION, null);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsConfiguration(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsView() throws Exception {
        String deserializedResponse = "";
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsView(RESPONSE_BODY)).thenReturn(deserializedResponse);

        String response = notificationExtension.getPluginSettingsView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), NotificationExtension.EXTENSION_NAME, "1.0", PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_VIEW, null);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsView(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToValidatePluginSettings() throws Exception {
        String requestBody = "expected-request";
        when(pluginSettingsJSONMessageHandler.requestMessageForPluginSettingsValidation(pluginSettingsConfiguration)).thenReturn(requestBody);
        ValidationResult deserializedResponse = new ValidationResult();
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsValidation(RESPONSE_BODY)).thenReturn(deserializedResponse);

        ValidationResult response = notificationExtension.validatePluginSettings(PLUGIN_ID, pluginSettingsConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), NotificationExtension.EXTENSION_NAME, "1.0", PluginSettingsConstants.REQUEST_VALIDATE_PLUGIN_SETTINGS, requestBody);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsValidation(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetNotificationsInterestedIn() throws Exception {
        List<String> response = Arrays.asList(new String[]{"pipeline-status", "stage-status"});
        when(jsonMessageHandler.responseMessageForNotificationsInterestedIn(RESPONSE_BODY)).thenReturn(response);

        List<String> deserializedResponse = notificationExtension.getNotificationsOfInterestFor(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), NotificationExtension.EXTENSION_NAME, "1.0", NotificationExtension.REQUEST_NOTIFICATIONS_INTERESTED_IN, null);
        verify(jsonMessageHandler).responseMessageForNotificationsInterestedIn(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToNotify() throws Exception {
        Result response = new Result();
        String notificationName = "notification-name";
        String jsonResponse = "json-response";
        StageNotificationData stageNotificationData = new StageNotificationData(new Stage(), BuildCause.createWithEmptyModifications(), "group");
        when(jsonMessageHandler.requestMessageForNotify(stageNotificationData)).thenReturn(jsonResponse);
        when(jsonMessageHandler.responseMessageForNotify(RESPONSE_BODY)).thenReturn(response);

        Result deserializedResponse = notificationExtension.notify(PLUGIN_ID, notificationName, stageNotificationData);

        assertRequest(requestArgumentCaptor.getValue(), NotificationExtension.EXTENSION_NAME, "1.0", notificationName, jsonResponse);
        verify(jsonMessageHandler).responseMessageForNotify(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThat(goPluginApiRequest.requestBody(), is(requestBody));
    }
}