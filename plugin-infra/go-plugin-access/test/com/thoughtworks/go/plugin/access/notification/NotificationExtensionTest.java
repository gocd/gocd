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

package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class NotificationExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";
    public static final Map REQUEST_BODY = new HashMap();
    public static final String RESPONSE_BODY = "expected-response";

    @Mock
    private PluginManager pluginManager;
    @Mock
    private JsonMessageHandler1_0 jsonMessageHandler;

    private NotificationExtension notificationExtension;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        REQUEST_BODY.put("key", "value");

        notificationExtension = new NotificationExtension(pluginManager);
        notificationExtension.getMessageHandlerMap().put("1.0", jsonMessageHandler);

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, Arrays.asList("1.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(NotificationExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(RESPONSE_BODY));
    }

    @Test
    public void shouldTalkToPluginToGetNotificationsInterestedIn() throws Exception {
        List<String> response = Arrays.asList(new String[]{"pipeline-status", "stage-status"});
        when(jsonMessageHandler.responseMessageForNotificationsInterestedIn(RESPONSE_BODY)).thenReturn(response);

        List<String> deserializedResponse = notificationExtension.getNotificationsInterestedIn(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), NotificationExtension.EXTENSION_NAME, "1.0", NotificationExtension.REQUEST_NOTIFICATIONS_INTERESTED_IN, null);
        verify(jsonMessageHandler).responseMessageForNotificationsInterestedIn(RESPONSE_BODY);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToNotify() throws Exception {
        Result response = new Result();
        String notificationName = "notification-name";
        String jsonResponse = "json-response";
        when(jsonMessageHandler.requestMessageForNotify(notificationName, REQUEST_BODY)).thenReturn(jsonResponse);
        when(jsonMessageHandler.responseMessageForNotify(RESPONSE_BODY)).thenReturn(response);

        Result deserializedResponse = notificationExtension.notify(PLUGIN_ID, notificationName, REQUEST_BODY);

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