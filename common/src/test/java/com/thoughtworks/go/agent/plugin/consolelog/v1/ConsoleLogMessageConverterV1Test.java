/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.plugin.consolelog.v1;

import com.thoughtworks.go.agent.plugin.consolelog.ConsoleLogMessage;
import com.thoughtworks.go.agent.plugin.consolelog.LogLevel;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsoleLogMessageConverterV1Test {
    @Test
    public void shouldConvertConsoleLogMessageDTOToConsoleLogMessage() {
        final ConsoleLogMessageDTO consoleLogMessageDTO = mock(ConsoleLogMessageDTO.class);
        when(consoleLogMessageDTO.getLogLevel()).thenReturn("info");
        when(consoleLogMessageDTO.getMessage()).thenReturn("some message.");

        final ConsoleLogMessage consoleLogMessage = new ConsoleLogMessageConverterV1().toConsoleLogMessage(consoleLogMessageDTO);

        assertThat(consoleLogMessage.getLogLevel(), is(LogLevel.INFO));
        assertThat(consoleLogMessage.getMessage(), is("some message."));
    }
}