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
package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.NOTIFICATION_EXTENSION;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class NotificationExtensionTestBase {
    private static final String PLUGIN_ID = "plugin-id";
    private static final String RESPONSE_BODY = "expected-response";

    private NotificationExtension notificationExtension;
    private PluginSettingsConfiguration pluginSettingsConfiguration;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Mock(lenient = true)
    protected PluginManager pluginManager;
    @Mock
    protected ExtensionsRegistry extensionsRegistry;

    @BeforeEach
    public void setUp() throws Exception {
        notificationExtension = new NotificationExtension(pluginManager, extensionsRegistry);
        notificationExtension.getPluginSettingsMessageHandlerMap().put(apiVersion(), pluginSettingsJSONMessageHandler());
        notificationExtension.getMessageHandlerMap().put(apiVersion(), jsonMessageHandler());

        pluginSettingsConfiguration = new PluginSettingsConfiguration();
        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, NOTIFICATION_EXTENSION, NotificationExtension.goSupportedVersions)).thenReturn(apiVersion());
        when(pluginManager.isPluginOfType(NOTIFICATION_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(NOTIFICATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(RESPONSE_BODY));
    }

    protected abstract String apiVersion();

    protected abstract PluginSettingsJsonMessageHandler pluginSettingsJSONMessageHandler();

    protected abstract JsonMessageHandler jsonMessageHandler();

    @Test
    public void shouldExtendAbstractExtension() {
        assertThat(notificationExtension, instanceOf(AbstractExtension.class));
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsConfiguration() {
        PluginSettingsConfiguration deserializedResponse = new PluginSettingsConfiguration();
        when(pluginSettingsJSONMessageHandler().responseMessageForPluginSettingsConfiguration(RESPONSE_BODY)).thenReturn(deserializedResponse);

        PluginSettingsConfiguration response = notificationExtension.getPluginSettingsConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), NOTIFICATION_EXTENSION, apiVersion(), PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_CONFIGURATION, null);
        verify(pluginSettingsJSONMessageHandler()).responseMessageForPluginSettingsConfiguration(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsView() throws Exception {
        String deserializedResponse = "";
        when(pluginSettingsJSONMessageHandler().responseMessageForPluginSettingsView(RESPONSE_BODY)).thenReturn(deserializedResponse);

        String response = notificationExtension.getPluginSettingsView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), NOTIFICATION_EXTENSION, apiVersion(), PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_VIEW, null);
        verify(pluginSettingsJSONMessageHandler()).responseMessageForPluginSettingsView(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToValidatePluginSettings() throws Exception {
        String requestBody = "expected-request";
        when(pluginSettingsJSONMessageHandler().requestMessageForPluginSettingsValidation(pluginSettingsConfiguration)).thenReturn(requestBody);
        ValidationResult deserializedResponse = new ValidationResult();
        when(pluginSettingsJSONMessageHandler().responseMessageForPluginSettingsValidation(RESPONSE_BODY)).thenReturn(deserializedResponse);

        ValidationResult response = notificationExtension.validatePluginSettings(PLUGIN_ID, pluginSettingsConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), NOTIFICATION_EXTENSION, apiVersion(), PluginSettingsConstants.REQUEST_VALIDATE_PLUGIN_SETTINGS, requestBody);
        verify(pluginSettingsJSONMessageHandler()).responseMessageForPluginSettingsValidation(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetNotificationsInterestedIn() throws Exception {
        List<String> response = asList("pipeline-status", "stage-status");
        when(jsonMessageHandler().responseMessageForNotificationsInterestedIn(RESPONSE_BODY)).thenReturn(response);

        List<String> deserializedResponse = notificationExtension.getNotificationsOfInterestFor(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), NOTIFICATION_EXTENSION, apiVersion(), NotificationExtension.REQUEST_NOTIFICATIONS_INTERESTED_IN, null);
        verify(jsonMessageHandler()).responseMessageForNotificationsInterestedIn(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToNotify() throws Exception {
        Result response = new Result();
        String notificationName = "notification-name";
        String jsonResponse = "json-response";
        StageNotificationData stageNotificationData = new StageNotificationData(new Stage(), BuildCause.createWithEmptyModifications(), "group");
        when(jsonMessageHandler().requestMessageForNotify(stageNotificationData)).thenReturn(jsonResponse);
        when(jsonMessageHandler().responseMessageForNotify(RESPONSE_BODY)).thenReturn(response);

        Result deserializedResponse = notificationExtension.notify(PLUGIN_ID, notificationName, stageNotificationData);

        assertRequest(requestArgumentCaptor.getValue(), NOTIFICATION_EXTENSION, apiVersion(), notificationName, jsonResponse);
        verify(jsonMessageHandler()).responseMessageForNotify(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThat(goPluginApiRequest.requestBody(), is(requestBody));
    }
}
