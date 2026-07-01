/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.plugin.activation.DefaultGoPluginActivator;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemErr;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
public class LoggerTest {

    @SystemStub SystemOut systemOut;
    @SystemStub SystemErr systemErr;

    private LoggingService loggingService;

    @BeforeEach
    void setUp() {
        loggingService = mock(LoggingService.class);
        DefaultGoPluginActivator.pluginId = "some-test-plugin";
        Logger.initialize(loggingService);
    }

    @AfterEach
    void tearDown() {
        Logger.initialize(null);
        DefaultGoPluginActivator.pluginId = null;
    }

    @Test
    public void shouldLogMessageWithPluginId() {
        Logger.getLoggerFor(this.getClass()).debug("message");
        verify(loggingService).debug("some-test-plugin", this.getClass().getName(), "message");

        Logger.getLoggerFor(this.getClass()).info("message");
        verify(loggingService).info("some-test-plugin", this.getClass().getName(), "message");

        Logger.getLoggerFor(this.getClass()).warn("message");
        verify(loggingService).warn("some-test-plugin", this.getClass().getName(), "message");

        Logger.getLoggerFor(this.getClass()).error("message");
        verify(loggingService).error("some-test-plugin", this.getClass().getName(), "message");

        assertThat(systemOutLines()).isEmpty();
        assertThat(systemErrLines()).isEmpty();
    }

    @Test
    public void shouldHandleMissingPluginId() {
        DefaultGoPluginActivator.pluginId = null;
        Logger.getLoggerFor(this.getClass()).info("message");

        verify(loggingService).info("UNKNOWN", this.getClass().getName(), "message");

        assertThat(systemOutLines()).isEmpty();
        assertThat(systemErrLines()).isEmpty();
    }

    @Test
    public void shouldHandleInvalidPluginIdDuringReflection() {
        DefaultGoPluginActivator.pluginId = 123L; // not a string
        Logger.getLoggerFor(this.getClass()).info("message");

        verify(loggingService).info("UNKNOWN", this.getClass().getName(), "message");

        assertThat(systemOutLines()).isEmpty();
        assertThat(systemErrLines()).singleElement(STRING)
            .startsWith("Could not find pluginId for logger: class com.thoughtworks.go.plugin.api.logging.LoggerTest due to java.lang.ClassCastException");
    }

    @Test
    public void shouldHandleInvalidPluginIdForLoggerClassWithBadClassLoader() {
        Class<?> badLoggerClass = String.class;
        Logger.getLoggerFor(badLoggerClass).info("message");

        verify(loggingService).info("UNKNOWN", badLoggerClass.getName(),"message");

        assertThat(systemOutLines()).isEmpty();
        assertThat(systemErrLines()).singleElement(STRING)
            .startsWith("Could not find pluginId for logger: class java.lang.String due to java.lang.NullPointerException");
    }

    @Test
    public void shouldLogMessageWithException() {
        RuntimeException exception = new RuntimeException("error");
        Logger.getLoggerFor(this.getClass()).error("message", exception);

        verify(loggingService).error("some-test-plugin", this.getClass().getName(), "message", exception);

        assertThat(systemOutLines()).isEmpty();
        assertThat(systemErrLines()).isEmpty();
    }

    @Test
    public void shouldLogAMessageUsingProvidedLoggerPluginId() {
        Logger.getLoggerFor(this.getClass(), "some-other-plugin").error("message");

        verify(loggingService).error("some-other-plugin", this.getClass().getName(), "message");

        assertThat(systemOutLines()).isEmpty();
        assertThat(systemErrLines()).isEmpty();
    }

    @Test
    public void shouldFallbackToStdStreams() {
        Logger.initialize(null);

        Logger.getLoggerFor(this.getClass()).debug("message");
        verifyNoInteractions(loggingService);

        assertThat(systemOutLines()).containsExactly("message");
        assertThat(systemErrLines()).isEmpty();
    }

    private Stream<String> systemOutLines() {
        return systemOut.getLines().filter(line -> !line.isBlank());
    }

    private Stream<String> systemErrLines() {
        return systemErr.getLines().filter(line -> !line.isBlank())
            .filter(line -> !line.matches("^(WARNING|Mockito).*"));
    }
}
