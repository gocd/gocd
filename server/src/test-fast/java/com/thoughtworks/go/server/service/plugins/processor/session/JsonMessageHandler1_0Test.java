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

package com.thoughtworks.go.server.service.plugins.processor.session;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class JsonMessageHandler1_0Test {
    private JsonMessageHandler1_0 messageHandler;

    @Before
    public void setUp() {
        messageHandler = new JsonMessageHandler1_0();
    }

    @Test
    public void shouldHandleGetSessionData() {
        SessionData sessionData = messageHandler.requestMessageSessionPut("{\"plugin-id\":\"plugin-id-1\",\"session-data\":{\"k1\":\"v1\",\"k2\":\"v2\"}}");

        assertThat(sessionData.getPluginId(), is("plugin-id-1"));
        HashMap<String, String> expected = new HashMap<>();
        expected.put("k1", "v1");
        expected.put("k2", "v2");
        assertEquals(sessionData.getSessionData(), expected);
    }

    @Test
    public void shouldHandleGetSessionDataWithMissingData() {
        SessionData sessionData = messageHandler.requestMessageSessionPut("{\"plugin-id\":\"plugin-id-1\"}");

        assertThat(sessionData.getPluginId(), is("plugin-id-1"));
        assertThat(sessionData.getSessionData(), is(nullValue()));
    }

    @Test
    public void shouldHandleGetPluginIdMessage() {
        assertThat(messageHandler.requestMessageSessionGetAndRemove("{\"plugin-id\":\"plugin-id-1\"}"), is("plugin-id-1"));
    }

    @Test
    public void shouldHandleIncorrectDataForPutIntoSession() {
        assertErrorMessageForPutIntoSession("{}", "'plugin-id' cannot be empty");
        assertErrorMessageForPutIntoSession("{\"plugin-id\":true}", "'plugin-id' should of string type");
        assertErrorMessageForPutIntoSession("{\"plugin-id\":\"plugin-id-1\",\"session-data\":[]}", "'sessionData' should of map type");
    }

    @Test
    public void shouldHandleIncorrectDataForGetAndRemoveFromSession() {
        assertErrorMessageForGetAndRemoveFromSession("{}", "'plugin-id' cannot be empty");
        assertErrorMessageForGetAndRemoveFromSession("{\"plugin-id\":true}", "'plugin-id' should of string type");
    }

    private void assertErrorMessageForPutIntoSession(String requestBody, String message) {
        try {
            messageHandler.requestMessageSessionPut(requestBody);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    private void assertErrorMessageForGetAndRemoveFromSession(String requestBody, String message) {
        try {
            messageHandler.requestMessageSessionGetAndRemove(requestBody);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(message));
        }
    }
}
