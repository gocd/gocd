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

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

public class AgentOutputAppender {
    private static final String LOG_DIR = "LOG_DIR";

    enum Outstream {
        STDOUT(ConsoleAppender.SYSTEM_OUT),
        STDERR(ConsoleAppender.SYSTEM_ERR);

        private final String name;

        Outstream(String name) {
            this.name = name;
        }
    }

    private static final PatternLayout LAYOUT = new PatternLayout("%m%n");
    private final List<WriterAppender> appenders = new ArrayList<>();

    public AgentOutputAppender(String file) throws IOException {
        appenders.add(rollingAppender(file));
    }

    public void writeTo(Outstream target) {
        appenders.add(new ConsoleAppender(LAYOUT, target.name));
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
        RollingFileAppender rollingFileAppender = new RollingFileAppender(LAYOUT, getEffectiveLogDirectory(file), true);
        rollingFileAppender.setMaxBackupIndex(4);
        rollingFileAppender.setMaxFileSize("5000KB");
        return rollingFileAppender;
    }

    private static String getEffectiveLogDirectory(String file) {
        String logDir = System.getenv(LOG_DIR);

        if (isBlank(logDir)) {
            return file;
        } else {
            return logDir + "/" + file;
        }
    }
}
