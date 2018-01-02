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

package com.thoughtworks.go.agent;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.thoughtworks.go.logging.LogHelper;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isNotBlank;

class AgentOutputAppender {
    enum Outstream {
        STDOUT(ConsoleTarget.SystemOut, "stdout"),
        STDERR(ConsoleTarget.SystemErr, "stderr");

        private final ConsoleTarget target;
        private final String marker;

        Outstream(ConsoleTarget target, String marker) {
            this.target = target;
            this.marker = marker;
        }
    }

    private final List<OutputStreamAppender<ILoggingEvent>> appenders = new ArrayList<>();

    AgentOutputAppender(String file) throws IOException {
        appenders.add(rollingAppender(file));
    }

    void writeTo(Outstream target) {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setTarget(target.target.getName());
        appender.setEncoder(LogHelper.encoder("%date{ISO8601} [" + target.marker + "] - %msg%n"));
        appender.start();
        appenders.add(appender);
    }

    private void write(String message, Exception throwable) {
        for (OutputStreamAppender<ILoggingEvent> appender : appenders) {
            Logger logger = (Logger) LoggerFactory.getLogger(AgentOutputAppender.class);
            appender.doAppend(new LoggingEvent("", logger, Level.ERROR, message, throwable, null));
        }
    }

    void write(String line) {
        write(line, null);
    }

    void close() {
        appenders.forEach(OutputStreamAppender::stop);
    }

    private RollingFileAppender<ILoggingEvent> rollingAppender(String file) throws IOException {
        RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
        rollingFileAppender.setEncoder(LogHelper.encoder("%date{ISO8601} - %msg%n"));
        rollingFileAppender.setContext(LogHelper.LOGGER_CONTEXT);
        rollingFileAppender.setFile(getEffectiveLogDirectory(file));
        rollingFileAppender.setName(UUID.randomUUID().toString());

        LogHelper.rollingPolicyForAppender(
                rollingFileAppender,
                "5 MB",
                "20 MB",
                4
        );
        rollingFileAppender.start();
        return rollingFileAppender;
    }

    private static String getEffectiveLogDirectory(String file) {
        return getLogDir() + "/" + file;
    }

    private static String getLogDir() {
        List<String> logDirs = Arrays.asList(
                System.getProperty("gocd.agent.log.dir"),
                System.getenv("LOG_DIR"),
                "logs"
        );

        for (String logDir : logDirs) {
            if (isNotBlank(logDir)) {
                return logDir;
            }
        }

        throw new IllegalStateException("Could not find a log directory to log to.");
    }
}
