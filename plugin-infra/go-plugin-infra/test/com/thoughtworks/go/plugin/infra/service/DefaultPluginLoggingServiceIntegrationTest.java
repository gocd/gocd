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

package com.thoughtworks.go.plugin.infra.service;

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.*;

import static com.thoughtworks.go.plugin.infra.service.DefaultPluginLoggingService.pluginLogFileName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class DefaultPluginLoggingServiceIntegrationTest {
    private static OSChecker WINDOWS = new OSChecker(OSChecker.WINDOWS);
    private DefaultPluginLoggingService pluginLoggingService;
    private Map<Integer, String> plugins;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        this.plugins = new HashMap<Integer, String>();

        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.pluginLoggingLevel(any(String.class))).thenReturn(Level.INFO);

        pluginLoggingService = new DefaultPluginLoggingService(systemEnvironment);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.shutdown();
        for (Integer pluginIndex : plugins.keySet()) {
            FileUtils.deleteQuietly(pluginLog(pluginIndex));
        }
    }

    @Test
    public void shouldNotLogPluginMessagesToRootLogger() throws Exception {
        LogFixture appender = LogFixture.startListening(Level.INFO);
        Logger.getRootLogger().addAppender(appender);

        DefaultPluginLoggingService service = new DefaultPluginLoggingService(systemEnvironment);
        service.info(pluginID(1), "LoggingClass", "this-message-should-not-go-to-root-logger");

        String failureMessage = "Expected no messages to be logged to root logger. Found: " + Arrays.toString(appender.getMessages());
        assertThat(failureMessage, appender.getMessages().length, is(0));
    }

    @Test
    public void shouldGetLogLocationFromRootLoggerFileAppender() throws Exception {
        DefaultPluginLoggingService service = new DefaultPluginLoggingService(systemEnvironment);
        DefaultPluginLoggingService spy = Mockito.spy(service);

        spy.info(pluginID(1), "LoggingClass", "message");

        verify(spy).getCurrentLogDirectory();
    }

    @Test
    public void shouldGetCurrentLogDirectoryByLookingAtFileAppenderOfRootLogger() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileAppender fileAppender = mock(FileAppender.class);
        when(fileAppender.getFile()).thenReturn("/var/log/go-server/go-server.log");

        DefaultPluginLoggingService service = Mockito.spy(new DefaultPluginLoggingService(systemEnvironment));
        doReturn(fileAppender).when(service).getGoServerLogFileAppender();

        String currentLogDirectory = service.getCurrentLogDirectory();

        assertThat(currentLogDirectory, is("/var/log/go-server"));
    }

    @Test
    public void shouldNotLogDebugMessagesByDefaultSinceTheDefaultLoggingLevelIsInfo() throws Exception {
        pluginLoggingService.debug(pluginID(1), "LoggingClass", "message");

        assertThat(FileUtils.readFileToString(pluginLog(1)), is(""));
    }

    @Test
    public void shouldLogNonDebugMessagesByDefaultSinceTheDefaultLoggingLevelIsInfo() throws Exception {
        pluginLoggingService.info(pluginID(1), "LoggingClass", "info");
        pluginLoggingService.warn(pluginID(1), "LoggingClass", "warn");
        pluginLoggingService.error(pluginID(1), "LoggingClass", "error");

        assertNumberOfMessagesInLog(pluginLog(1), 3);
        assertMessageInLog(pluginLog(1), "INFO", "LoggingClass", "info");
        assertMessageInLog(pluginLog(1), "WARN", "LoggingClass", "warn");
        assertMessageInLog(pluginLog(1), "ERROR", "LoggingClass", "error");
    }

    @Test
    public void shouldLogThrowableDetailsAlongwithMessage() throws Exception {
        Throwable throwable = new RuntimeException("oops");
        throwable.setStackTrace(new StackTraceElement[]{new StackTraceElement("class", "method", "field", 20)});

        pluginLoggingService.error(pluginID(1), "LoggingClass", "error", throwable);

        assertMessageInLog(pluginLog(1), "ERROR", "LoggingClass", "error", "java\\.lang\\.RuntimeException:\\soops[\\s\\S]*at\\sclass\\.method\\(field:20\\)[\\s\\S]*$");
    }

    @Test
    public void shouldUsePluginLogFileForAllLogMessagesOfASinglePlugin() throws Exception {
        pluginLoggingService.info(pluginID(1), "LoggingClass", "info1");
        pluginLoggingService.warn(pluginID(1), "SomeOtherClass", "info2");

        assertNumberOfMessagesInLog(pluginLog(1), 2);
        assertMessageInLog(pluginLog(1), "INFO", "LoggingClass", "info1");
        assertMessageInLog(pluginLog(1), "WARN", "SomeOtherClass", "info2");
    }

    @Test
    public void shouldLogMessagesOfDifferentPluginsToTheirOwnLogFiles() throws Exception {
        pluginLoggingService.info(pluginID(1), "LoggingClass", "info1");
        pluginLoggingService.info(pluginID(2), "SomeOtherClass", "info2");

        assertNumberOfMessagesInLog(pluginLog(1), 1);
        assertMessageInLog(pluginLog(1), "INFO", "LoggingClass", "info1");

        assertNumberOfMessagesInLog(pluginLog(2), 1);
        assertMessageInLog(pluginLog(2), "INFO", "SomeOtherClass", "info2");
    }

    @Test(timeout = 10 * 1000)
    public void shouldAllowLoggingAcrossMultipleThreadsAndPlugins() throws Exception {
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
    public void shouldAllowSettingLoggingLevelPerPlugin() throws Exception {
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

    private void assertMessageInLog(File pluginLogFile, String expectedLoggingLevel, String loggerName, String expectedLogMessage) throws Exception {
        List linesInLog = FileUtils.readLines(pluginLogFile);
        for (Object line : linesInLog) {
            if (((String)line).matches(String.format("^.*%s \\[%s\\] %s:.* - %s$", expectedLoggingLevel, Thread.currentThread().getName(), loggerName, expectedLogMessage))) {
                return;
            }
        }
        fail(String.format("None of the lines matched level:%s message:'%s'. Lines were: %s", expectedLoggingLevel, expectedLogMessage, linesInLog));
    }

    private void assertMessageInLog(File pluginLogFile, String loggingLevel, String loggerName, String message, String stackTracePattern) throws Exception {
        String fileContent = FileUtils.readFileToString(pluginLogFile);
        if (fileContent.matches(String.format("^.*%s\\s\\[%s\\]\\s%s:.*\\s-\\s%s[\\s\\S]*%s", loggingLevel, Thread.currentThread().getName(), loggerName, message, stackTracePattern))) {
            return;
        }
        fail(String.format("Message not found in log file. File content is: %s", fileContent));

    }

    private void assertNumberOfMessagesInLog(File pluginLogFile, int size) throws Exception {
        assertThat(FileUtils.readLines(pluginLogFile).size(), is(size));
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
        File pluginLogFile = new File(pluginLogFileName(pluginID(pluginIndex)));
        pluginLogFile.deleteOnExit();
        return pluginLogFile;
    }
}
