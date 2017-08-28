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

import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.*;

import java.io.File;
import java.io.IOException;

public class DefaultPluginLoggingService implements LoggingService {
    private static Logger loggingServiceLogger = Logger.getLogger(DefaultPluginLoggingService.class);
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
        getLogger(pluginId, loggerName).info(message);
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
        return Logger.getLogger(PLUGIN_LOGGER_PREFIX + "." + pluginId + "." + loggerName);
    }

    private boolean alreadyInitialized(String pluginId) {
        return Logger.getLogger(PLUGIN_LOGGER_PREFIX + "." + pluginId).getAllAppenders().hasMoreElements();
    }

    private void initializeLoggerForPluginId(String pluginId) {
        if (alreadyInitialized(pluginId)) {
            return;
        }

        synchronized (pluginId.intern()) {
            if (alreadyInitialized(pluginId)) {
                return;
            }
            FileAppender pluginAppender = getAppender(pluginId);

            Logger logger = Logger.getLogger(PLUGIN_LOGGER_PREFIX + "." + pluginId);
            logger.setAdditivity(false);
            logger.setLevel(systemEnvironment.pluginLoggingLevel(pluginId));
            logger.addAppender(pluginAppender);

            if (systemEnvironment.consoleOutToStdout()) {
                ConsoleAppender consoleAppender = new ConsoleAppender(new PatternLayout("%d{ISO8601} %5p [%t] %c{1}:%L [plugin-" + pluginId + "] - %m%n"));
                logger.setAdditivity(false);
                logger.setLevel(systemEnvironment.pluginLoggingLevel(pluginId));
                logger.addAppender(consoleAppender);
            }

            loggingServiceLogger.debug("Plugin with ID: " + pluginId + " will log to: " + pluginAppender.getFile());
        }
    }

    private FileAppender getAppender(String pluginId) {
        try {
            String logDirectory = getCurrentLogDirectory();
            File pluginLogFileLocation = new File(logDirectory, pluginLogFileName(pluginId));
            return new RollingFileAppender(new PatternLayout("%d{ISO8601} %5p [%t] %c{1}:%L - %m%n"), pluginLogFileLocation.getPath(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            String fileName = fileAppender.getFile();
            return new File(fileName).getAbsoluteFile().getParent();
        } catch (Exception e) {
            return ".";
        }
    }

    FileAppender getGoServerLogFileAppender() {
        return (FileAppender) Logger.getRootLogger().getAppender("FileAppender");
    }
}
