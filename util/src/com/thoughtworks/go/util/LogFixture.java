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

package com.thoughtworks.go.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class LogFixture extends AppenderSkeleton implements Closeable {

    private List<String> messages = new ArrayList<>();
    private List<LoggingEvent> events = new ArrayList<>();
    private Logger logger;

    private LogFixture(Class aClass, Level level) {
        this(Logger.getLogger(aClass), level);
    }

    private LogFixture(Logger logger, Level level) {
        this.logger = logger;
        logger.addAppender(this);
        logger.setLevel(level);
    }

    public static LogFixture logFixtureFor(Class aClass, Level level) {
        return new LogFixture(aClass, level);
    }

    public static LogFixture logFixtureForRootLogger(Level level) {
        return new LogFixture(Logger.getRootLogger(), level);
    }

    public static LogFixture logFixtureForRails() {
        return new LogFixture(Logger.getLogger("com.thoughtworks.go.server.Rails"), Level.ALL);
    }

    public void close() {
        logger.removeAppender(this);
    }

    protected synchronized void append(LoggingEvent event) {
        events.add(event);
        messages.add(event.getRenderedMessage());
    }


    public boolean requiresLayout() {
        return false;
    }

    public String[] getMessages() {
        return messages.toArray(new String[messages.size()]);
    }

    public void clear() {
        messages.clear();
    }

    public String getLog() {
        return ArrayUtil.join(getMessages());
    }

    public synchronized boolean contains(Level level, String message) {
        for (LoggingEvent event : events) {
            if (event.getLevel().equals(level) && event.getMessage().toString().contains(message)) {
                return true;
            }
        }
        return false;
    }

    public synchronized String allLogs() {
        StringBuilder builder = new StringBuilder();
        for (LoggingEvent event : events) {
            builder.append(event.getLevel()).append(" - ").append(event.getMessage()).append("\n");
            if (event.getThrowableInformation() != null) {
                for (String s : event.getThrowableStrRep()) {
                    builder.append(s).append("\n");
                }
            }
        }
        return builder.toString();
    }

}
