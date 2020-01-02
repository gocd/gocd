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
package com.thoughtworks.go.plugin.api.logging;

import com.thoughtworks.go.plugin.internal.api.LoggingService;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LoggerTest {
    @Test
    public void shouldLogMessageWithException() {
        LoggingService loggingService = mock(LoggingService.class);

        Logger.initialize(loggingService);
        Logger logger = Logger.getLoggerFor(this.getClass());

        RuntimeException exception = new RuntimeException("error");
        logger.error("message", exception);

        verify(loggingService).error(anyString(), eq(this.getClass().getName()), eq("message"), eq(exception));
    }

    @Test
    public void shouldLogAMessageUsingTheLoggerPluginID() {
        LoggingService loggingService = mock(LoggingService.class);
        Logger.initialize(loggingService);

        Logger logger = Logger.getLoggerFor(this.getClass(), "someOtherPluginID");

        logger.error("message");

        verify(loggingService).error(eq("someOtherPluginID"), eq(this.getClass().getName()), eq("message"));
    }
}
