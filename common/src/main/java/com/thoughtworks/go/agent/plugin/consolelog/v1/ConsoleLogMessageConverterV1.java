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
import com.thoughtworks.go.agent.plugin.consolelog.ConsoleLogMessageConverter;
import com.thoughtworks.go.agent.plugin.consolelog.LogLevel;

public class ConsoleLogMessageConverterV1 implements ConsoleLogMessageConverter<ConsoleLogMessageDTO> {
    @Override
    public ConsoleLogMessage toConsoleLogMessage(ConsoleLogMessageDTO consoleLogMessageDTO) {
        final LogLevel logLevel = LogLevel.valueOf(consoleLogMessageDTO.getLogLevel().toUpperCase());
        return new ConsoleLogMessage(consoleLogMessageDTO.getMessage(), logLevel);
    }
}
