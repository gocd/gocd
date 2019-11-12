/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.FileAppender;
import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.thoughtworks.go.util.LogFixture.logFixtureForRootLogger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

class DefaultPluginLoggingServiceIntegrationTest {
    private static OSChecker WINDOWS = new OSChecker(OSChecker.WINDOWS);
    private DefaultPluginLoggingService pluginLoggingService;
    private Map<Integer, String> plugins;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    void setUp() {
        this.plugins = new HashMap<>();

        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.pluginLoggingLevel(any(String.class))).thenReturn(Level.INFO);

        pluginLoggingService = new DefaultPluginLoggingService(systemEnvironment);
    }

    @AfterEach
    void tearDown() {
        for (Integer pluginIndex : plugins.keySet()) {
            FileUtils.deleteQuietly(pluginLog(pluginIndex));
        }
    }

    @Test
    void shouldNotLogPluginMessagesToRootLogger() {
        try (LogFixture fixture = logFixtureForRootLogger(Level.INFO)) {
            DefaultPluginLoggingService service = new DefaultPluginLoggingService(systemEnvironment);
            service.info(pluginID(1), "LoggingClass", "this-message-should-not-go-to-root-logger");

            String failureMessage = "Expected no messages to be logged to root logger. Found: " + fixture.getFormattedMessages();
            assertThat(fixture.getFormattedMessages().size()).as(failureMessage).isEqualTo(0);
        }
    }

    @Test
    void shouldGetLogLocationFromRootLoggerFileAppender() {
        DefaultPluginLoggingService service = new DefaultPluginLoggingService(systemEnvironment);
        DefaultPluginLoggingService spy = Mockito.spy(service);

        spy.info(pluginID(1), "LoggingClass", "message");

        verify(spy).getCurrentLogDirectory();
    }

    @Test
    void shouldGetCurrentLogDirectoryByLookingAtFileAppenderOfRootLogger() {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileAppender fileAppender = new FileAppender();
        fileAppender.setFile("/var/log/go-server/go-server.log");

        DefaultPluginLoggingService service = Mockito.spy(new DefaultPluginLoggingService(systemEnvironment));
        doReturn(fileAppender).when(service).getGoServerLogFileAppender();

        String currentLogDirectory = service.getCurrentLogDirectory();

        assertThat(currentLogDirectory).isEqualTo("/var/log/go-server");
    }

    @Test
    void shouldNotLogDebugMessagesByDefaultSinceTheDefaultLoggingLevelIsInfo() throws IOException {
        pluginLoggingService.debug(pluginID(1), "LoggingClass", "message");

        assertThat(FileUtils.readFileToString(pluginLog(1), Charset.defaultCharset())).isEqualTo("");
    }

    @Test
    void shouldLogNonDebugMessagesByDefaultSinceTheDefaultLoggingLevelIsInfo() throws IOException {
        pluginLoggingService.info(pluginID(1), "LoggingClass", "info");
        pluginLoggingService.warn(pluginID(1), "LoggingClass", "warn");
        pluginLoggingService.error(pluginID(1), "LoggingClass", "error");

        assertNumberOfMessagesInLog(pluginLog(1), 3);
        assertMessageInLog(pluginLog(1), "INFO", "LoggingClass", "info");
        assertMessageInLog(pluginLog(1), "WARN", "LoggingClass", "warn");
        assertMessageInLog(pluginLog(1), "ERROR", "LoggingClass", "error");
    }

    @Test
    void shouldLogThrowableDetailsAlongwithMessage() throws IOException {
        Throwable throwable = new RuntimeException("oops");
        throwable.setStackTrace(new StackTraceElement[]{new StackTraceElement("class", "method", "field", 20)});

        pluginLoggingService.error(pluginID(1), "LoggingClass", "error", throwable);

        assertMessageInLog(pluginLog(1), "ERROR", "LoggingClass", "error", "java\\.lang\\.RuntimeException:\\soops[\\s\\S]*at\\sclass\\.method\\(field:20\\)[\\s\\S]*$");
    }

    @Test
    void shouldUsePluginLogFileForAllLogMessagesOfASinglePlugin() throws IOException {
        pluginLoggingService.info(pluginID(1), "LoggingClass", "info1");
        pluginLoggingService.warn(pluginID(1), "SomeOtherClass", "info2");

        assertNumberOfMessagesInLog(pluginLog(1), 2);
        assertMessageInLog(pluginLog(1), "INFO", "LoggingClass", "info1");
        assertMessageInLog(pluginLog(1), "WARN", "SomeOtherClass", "info2");
    }

    @Test
    void shouldLogMessagesOfDifferentPluginsToTheirOwnLogFiles() throws IOException {
        pluginLoggingService.info(pluginID(1), "LoggingClass", "info1");
        pluginLoggingService.info(pluginID(2), "SomeOtherClass", "info2");

        assertNumberOfMessagesInLog(pluginLog(1), 1);
        assertMessageInLog(pluginLog(1), "INFO", "LoggingClass", "info1");

        assertNumberOfMessagesInLog(pluginLog(2), 1);
        assertMessageInLog(pluginLog(2), "INFO", "SomeOtherClass", "info2");
    }

    @Test
    @Timeout(value = 10)
    void shouldAllowLoggingAcrossMultipleThreadsAndPlugins() throws IOException, InterruptedException {
        Thread thread1 = createThreadFor(pluginID(1), "1");
        Thread thread2 = createThreadFor(pluginID(2), "2");
        Thread thread3 = createThreadFor(pluginID(1), "3");
        Thread thread4 = createThreadFor(pluginID(2), "4");

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        assertNumberOfMessagesInLog(pluginLog(1), 200);
        assertNumberOfMessagesInLog(pluginLog(2), 200);
    }

    @Test
    void shouldAllowSettingLoggingLevelPerPlugin() throws IOException {
        when(systemEnvironment.pluginLoggingLevel(pluginID(1))).thenReturn(Level.WARN);

        pluginLoggingService.debug(pluginID(1), "LoggingClass", "debug");
        pluginLoggingService.info(pluginID(1), "LoggingClass", "info");
        pluginLoggingService.warn(pluginID(1), "LoggingClass", "warn");
        pluginLoggingService.error(pluginID(1), "SomeOtherClass", "error");

        assertNumberOfMessagesInLog(pluginLog(1), 2);
        assertMessageInLog(pluginLog(1), "WARN", "LoggingClass", "warn");
        assertMessageInLog(pluginLog(1), "ERROR", "SomeOtherClass", "error");
    }

    private Thread createThreadFor(final String pluginId, final String threadIdentifier) {
        return new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    pluginLoggingService.info(pluginId, "LoggingClass", "info-" + threadIdentifier + "-" + i);

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    private void assertMessageInLog(File pluginLogFile, String expectedLoggingLevel, String loggerName, String expectedLogMessage) throws IOException {
        List linesInLog = FileUtils.readLines(pluginLogFile, Charset.defaultCharset());
        for (Object line : linesInLog) {
            if (((String) line).matches(String.format("^.*%s\\s+\\[%s\\] %s:.* - %s$", expectedLoggingLevel, Thread.currentThread().getName(), loggerName, expectedLogMessage))) {
                return;
            }
        }
        fail(String.format("None of the lines matched level:%s message:'%s'. Lines were: %s", expectedLoggingLevel, expectedLogMessage, linesInLog));
    }

    private void assertMessageInLog(File pluginLogFile, String loggingLevel, String loggerName, String message, String stackTracePattern) throws IOException {
        String fileContent = FileUtils.readFileToString(pluginLogFile, Charset.defaultCharset());
        if (fileContent.matches(String.format("^.*%s\\s\\[%s\\]\\s%s:.*\\s-\\s%s[\\s\\S]*%s", loggingLevel, Thread.currentThread().getName(), loggerName, message, stackTracePattern))) {
            return;
        }
        fail(String.format("Message not found in log file. File content is: %s", fileContent));

    }

    private void assertNumberOfMessagesInLog(File pluginLogFile, int size) throws IOException {
        assertThat(FileUtils.readLines(pluginLogFile, Charset.defaultCharset()).size()).isEqualTo(size);
    }

    private String pluginID(int pluginIndex) {
        if (plugins.containsKey(pluginIndex)) {
            return plugins.get(pluginIndex);
        }

        int randomPluginId = new Random().nextInt();
        String pluginId = "plugin-" + randomPluginId;
        plugins.put(pluginIndex, pluginId);

        return pluginId;
    }

    private File pluginLog(int pluginIndex) {
        File pluginLogFile = pluginLoggingService.pluginLogFile(pluginID(pluginIndex));
        pluginLogFile.deleteOnExit();
        return pluginLogFile;
    }
}
