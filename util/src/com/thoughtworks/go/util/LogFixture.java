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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.thoughtworks.go.logging.LogHelper;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.ListUtil.join;

public class LogFixture implements Closeable {

    private final ListAppender appender;
    private Logger logger;

    private LogFixture(Class aClass, Level level) {
        this((Logger) LoggerFactory.getLogger(aClass), level);
    }

    private LogFixture(Logger logger, Level level) {
        this.logger = logger;
        this.appender = new ListAppender(LogHelper.encoder("%level %msg%n"));
        this.appender.start();
        logger.addAppender(appender);
        logger.setLevel(level);
    }

    public static LogFixture logFixtureFor(Class aClass, Level level) {
        return new LogFixture(aClass, level);
    }

    public static LogFixture logFixtureForRootLogger(Level level) {
        return new LogFixture(LogHelper.rootLogger(), level);
    }

    public static LogFixture logFixtureForLogger(String name) {
        return new LogFixture((Logger) LoggerFactory.getLogger(name), Level.ALL);
    }

    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }

    public List<String> getFormattedMessages() {
        return appender.getFormattedEvents();
    }

    public List<String> getRawMessages() {
        return appender.getRawMessages();
    }

    public void clear() {
        appender.clear();
    }

    public String getLog() {
        return join(getFormattedMessages());
    }

    public synchronized boolean contains(Level level, String message) {
        for (ILoggingEvent event : appender.getRawEvents()) {
            if (event.getLevel().equals(level) && event.getFormattedMessage().contains(message)) {
                return true;
            }
        }
        return false;
    }


    private class ListAppender extends AppenderBase<ILoggingEvent> {

        private final PatternLayoutEncoder encoder;
        private List<ILoggingEvent> events = new ArrayList<>();

        ListAppender(PatternLayoutEncoder encoder) {
            this.encoder = encoder;
        }

        protected void append(ILoggingEvent e) {
            events.add(e);
        }

        public List<ILoggingEvent> getRawEvents() {
            return events;
        }

        public void clear() {
            events.clear();
        }

        List<String> getFormattedEvents() {
            ArrayList<String> strings = new ArrayList<>();
            for (ILoggingEvent event : events) {
                strings.add(new String(encoder.encode(event)));
            }
            return strings;
        }

        List<String> getRawMessages() {
            ArrayList<String> strings = new ArrayList<>();
            for (ILoggingEvent event : events) {
                strings.add(event.getFormattedMessage());
            }
            return strings;
        }

        @Override
        public void stop() {
            super.stop();
            clear();
        }
    }
}
