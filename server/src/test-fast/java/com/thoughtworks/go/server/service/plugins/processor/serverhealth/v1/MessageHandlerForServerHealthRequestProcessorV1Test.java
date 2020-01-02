/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.plugins.processor.serverhealth.v1;

import com.google.gson.Gson;
import com.thoughtworks.go.server.service.plugins.processor.serverhealth.PluginHealthMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class MessageHandlerForServerHealthRequestProcessorV1Test {
    private MessageHandlerForServerHealthRequestProcessorV1 processor;

    @Before
    public void setUp() {
        processor = new MessageHandlerForServerHealthRequestProcessorV1();
    }

    @Test
    public void shouldDeserializeProperMessage() {
        Map<String, Object> message1 = m("type", "warning", "message", "message 1");
        Map<String, Object> message2 = m("type", "error", "message", "message 2");
        Map<String, Object> message3 = m("type", "misspelled-type", "message", "message 3");
        String input = new Gson().toJson(a(message1, message2, message3));

        List<PluginHealthMessage> messages = processor.deserializeServerHealthMessages(input);

        assertThat(messages.get(0).isWarning(), is(true));
        assertThat(messages.get(0).message(), is("message 1"));

        assertThat(messages.get(1).isWarning(), is(false));
        assertThat(messages.get(1).message(), is("message 2"));

        assertThat(messages.get(2).isWarning(), is(false));
        assertThat(messages.get(2).message(), is("message 3"));
    }

    @Test
    public void shouldDeserializeToEmptyListProperly() {
        assertTrue(processor.deserializeServerHealthMessages("[]").isEmpty());
    }

    @Test
    public void shouldFailIfMessageIsInvalid() {
        try {
            processor.deserializeServerHealthMessages("Invalid JSON");
            fail("Should have failed to deserialize");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to deserialize"));
        }
    }
}
