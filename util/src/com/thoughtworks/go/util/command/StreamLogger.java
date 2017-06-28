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

package com.thoughtworks.go.util.command;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.InputStream;

/**
 * Logs the content of a Stream line by line.
 */
public final class StreamLogger implements StreamConsumer {
    private Logger logger;
    private Level level;

    private StreamLogger(Logger log, Level level) {
        this.logger = log;
        this.level = level;
    }

    public static StreamConsumer getInfoLogger(Logger log) {
        return new StreamLogger(log, Level.INFO);
    }

    public static StreamPumper getInfoPumper(Logger log, InputStream info) {
        return new StreamPumper(info, new StreamLogger(log, Level.INFO));
    }

    public static StreamPumper getInfoPumper(Logger log, Process process) {
        return getInfoPumper(log, process.getInputStream());
    }

    public static StreamConsumer getWarnLogger(Logger log) {
        return new StreamLogger(log, Level.WARN);
    }

    public static StreamPumper getWarnPumper(Logger log, InputStream warn) {
        return new StreamPumper(warn, new StreamLogger(log, Level.WARN));
    }

    public static StreamPumper getWarnPumper(Logger log, Process process) {
        return getWarnPumper(log, process.getErrorStream());
    }

    /** {@inheritDoc} */
    public void consumeLine(String line) {
        switch(level){
            case ERROR:
                logger.error(line);
                break;
            case WARN:
                logger.warn(line);
                break;
            case INFO:
                logger.info(line);
                break;
            case DEBUG:
                logger.debug(line);
                break;
            case TRACE:
                logger.trace(line);
                break;
        }

    }
}