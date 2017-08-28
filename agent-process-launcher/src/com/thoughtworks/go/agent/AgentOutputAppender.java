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

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

public class AgentOutputAppender {

    enum Outstream {
        STDOUT(ConsoleAppender.SYSTEM_OUT, "stdout"),
        STDERR(ConsoleAppender.SYSTEM_ERR, "stderr");

        private final String name;
        private final String marker;

        Outstream(String name, String marker) {
            this.name = name;
            this.marker = marker;
        }
    }

    private final List<WriterAppender> appenders = new ArrayList<>();

    public AgentOutputAppender(String file) throws IOException {
        appenders.add(rollingAppender(file));
    }

    public void writeTo(Outstream target) {
        appenders.add(new ConsoleAppender(new PatternLayout(target.marker + ": %m%n"), target.name));
    }

    public void write(String message, Exception throwable) {
        for (WriterAppender appender : appenders) {
            Logger logger = Logger.getLogger(AgentOutputAppender.class);
            appender.append(new LoggingEvent("", logger, Level.ERROR, message, throwable));
        }
    }

    public void write(String line) {
        write(line, null);
    }

    public void close() {
        for (WriterAppender appender : appenders) {
            appender.close();
        }
    }

    private RollingFileAppender rollingAppender(String file) throws IOException {
        RollingFileAppender rollingFileAppender = new RollingFileAppender(new PatternLayout("%m%n"), getEffectiveLogDirectory(file), true);
        rollingFileAppender.setMaxBackupIndex(4);
        rollingFileAppender.setMaxFileSize("5000KB");
        return rollingFileAppender;
    }

    private static String getEffectiveLogDirectory(String file) {
        String logDir = new SystemEnvironment().getPropertyImpl("go.agent.log.dir");

        if (isBlank(logDir)) {
            return file;
        } else {
            return logDir + "/" + file;
        }
    }
}
