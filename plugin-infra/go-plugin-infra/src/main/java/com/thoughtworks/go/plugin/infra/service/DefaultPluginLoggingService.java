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

package com.thoughtworks.go.plugin.infra.service;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.thoughtworks.go.logging.LogHelper;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.thoughtworks.go.logging.LogHelper.rootLogger;

public class DefaultPluginLoggingService implements LoggingService {
    private static Logger loggingServiceLogger = LoggerFactory.getLogger(DefaultPluginLoggingService.class);
    private static int MAX_LENGTH_OF_PLUGIN_FILENAME = 200;
    private static final String PLUGIN_LOGGER_PREFIX = "plugin";

    private final SystemEnvironment systemEnvironment;

    public DefaultPluginLoggingService(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public void debug(String pluginId, String loggerName, String message) {
        getLogger(pluginId, loggerName).debug(message);
    }

    @Override
    public void debug(String pluginId, String loggerName, String message, Throwable throwable) {
        getLogger(pluginId, loggerName).debug(message, throwable);
    }

    @Override
    public void info(String pluginId, String loggerName, String message) {
        Logger logger = getLogger(pluginId, loggerName);
        logger.info(message);
    }

    @Override
    public void info(String pluginId, String loggerName, String message, Throwable throwable) {
        getLogger(pluginId, loggerName).info(message, throwable);
    }

    @Override
    public void warn(String pluginId, String loggerName, String message) {
        getLogger(pluginId, loggerName).warn(message);
    }

    @Override
    public void warn(String pluginId, String loggerName, String message, Throwable throwable) {
        getLogger(pluginId, loggerName).warn(message, throwable);
    }

    @Override
    public void error(String pluginId, String loggerName, String message) {
        getLogger(pluginId, loggerName).error(message);
    }

    @Override
    public void error(String pluginId, String loggerName, String message, Throwable throwable) {
        getLogger(pluginId, loggerName).error(message, throwable);
    }

    private Logger getLogger(String pluginId, String loggerName) {
        initializeLoggerForPluginId(pluginId);
        return LoggerFactory.getLogger(PLUGIN_LOGGER_PREFIX + "." + pluginId + "." + loggerName);
    }

    private boolean alreadyInitialized(String pluginId) {
        return ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PLUGIN_LOGGER_PREFIX + "." + pluginId)).getAppender(rollingFileAppenderName(pluginId)) != null;
    }

    private void initializeLoggerForPluginId(String pluginId) {
        if (alreadyInitialized(pluginId)) {
            return;
        }

        synchronized (pluginId.intern()) {
            if (alreadyInitialized(pluginId)) {
                return;
            }
            FileAppender<ILoggingEvent> pluginAppender = getAppender(pluginId);

            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PLUGIN_LOGGER_PREFIX + "." + pluginId);
            logger.setAdditive(false);
            logger.setLevel(systemEnvironment.pluginLoggingLevel(pluginId));
            logger.addAppender(pluginAppender);

            if (systemEnvironment.consoleOutToStdout()) {
                ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
                consoleAppender.setEncoder(LogHelper.encoder("%d{ISO8601} %5p [%t] %c{1}:%L [plugin-" + pluginId + "] - %m%n"));
                logger.setAdditive(false);
                logger.setLevel(systemEnvironment.pluginLoggingLevel(pluginId));
                consoleAppender.start();
                logger.addAppender(consoleAppender);
            }

            loggingServiceLogger.debug("Plugin with ID: " + pluginId + " will log to: " + pluginAppender.rawFileProperty());
        }
    }

    private FileAppender<ILoggingEvent> getAppender(String pluginId) {
        File pluginLogFileLocation = pluginLogFile(pluginId);

        RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
        rollingFileAppender.setEncoder(LogHelper.encoder());
        rollingFileAppender.setContext(LogHelper.LOGGER_CONTEXT);
        rollingFileAppender.setFile(pluginLogFileLocation.getPath());
        rollingFileAppender.setName(rollingFileAppenderName(pluginId));

        LogHelper.rollingPolicyForAppender(
                rollingFileAppender,
                "5 MB",
                "20 MB",
                7
        );

        rollingFileAppender.start();
        return rollingFileAppender;
    }

    File pluginLogFile(String pluginId) {
        return new File(getCurrentLogDirectory(), pluginLogFileName(pluginId));
    }

    private String rollingFileAppenderName(String pluginId) {
        return "rollingFileAppender-" + pluginId;
    }

    static String pluginLogFileName(String pluginId) {
        String untrimmedLogFileName = PLUGIN_LOGGER_PREFIX + "-" + pluginId + ".log";
        int lengthOfNameExcludingPluginId = untrimmedLogFileName.length() - pluginId.length();
        int lengthThatThatPluginIdShouldBeTrimmedTo = Math.min(pluginId.length(), MAX_LENGTH_OF_PLUGIN_FILENAME - lengthOfNameExcludingPluginId);

        return String.format("%s-%s.log", PLUGIN_LOGGER_PREFIX, pluginId.substring(0, lengthThatThatPluginIdShouldBeTrimmedTo));
    }

    String getCurrentLogDirectory() {
        try {
            FileAppender fileAppender = getGoServerLogFileAppender();
            String fileName = fileAppender.rawFileProperty();
            return new File(fileName).getAbsoluteFile().getParent();
        } catch (Exception e) {
            return ".";
        }
    }

    FileAppender getGoServerLogFileAppender() {
        return (FileAppender) rootLogger().getAppender("FileAppender");
    }

}
