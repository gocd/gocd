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
package com.thoughtworks.go.remote.work.artifact;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConsoleLogMessageTest {

    @Test
    public void shouldDeserializeJsonWithInfoLogLevel() {
        final ConsoleLogMessage consoleLogMessage = ConsoleLogMessage.fromJSON("{\"logLevel\":\"INFO\",\"message\":\"This is info message.\"}");

        assertNotNull(consoleLogMessage);
        assertThat(consoleLogMessage.getLogLevel(), is(ConsoleLogMessage.LogLevel.INFO));
        assertThat(consoleLogMessage.getMessage(), is("This is info message."));
    }


    @Test
    public void shouldDeserializeJsonWithErrorLogLevel() {
        final ConsoleLogMessage consoleLogMessage = ConsoleLogMessage.fromJSON("{\"logLevel\":\"ERROR\",\"message\":\"This is error.\"}");

        assertNotNull(consoleLogMessage);
        assertThat(consoleLogMessage.getLogLevel(), is(ConsoleLogMessage.LogLevel.ERROR));
        assertThat(consoleLogMessage.getMessage(), is("This is error."));
    }
}
