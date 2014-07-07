package com.thoughtworks.go.plugin.api.logging;

import com.thoughtworks.go.plugin.internal.api.LoggingService;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
}