/*
 * Copyright Thoughtworks, Inc.
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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.thoughtworks.go.logging.LogHelper;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.Closeable;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static ch.qos.logback.classic.Level.convertAnSLF4JLevel;

public class LogFixture implements Closeable {

    private final ListAppender appender;
    private final Logger logger;

    private LogFixture(Class<?> aClass, Level level) {
        this((Logger) LoggerFactory.getLogger(aClass), level);
    }

    private LogFixture(Logger logger, Level level) {
        this.logger = logger;
        this.appender = new ListAppender(LogHelper.encoder("%level %msg%n"));
        this.appender.start();
        logger.addAppender(appender);
        logger.setLevel(convertAnSLF4JLevel(level));
    }

    public static LogFixture logFixtureFor(Class<?> aClass, Level level) {
        return new LogFixture(aClass, level);
    }

    public static LogFixture logFixtureForRootLogger(Level level) {
        return new LogFixture(LogHelper.rootLogger(), level);
    }

    @SuppressWarnings("unused") // Used by slf4j_logger_spec.rb
    public static LogFixture logFixtureForLogger(String name) {
        return new LogFixture((Logger) LoggerFactory.getLogger(name), Level.TRACE);
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }

    public List<String> getFormattedMessages() {
        return appender.getFormattedMessages();
    }

    public List<String> getRawMessages() {
        return appender.getRawMessages();
    }

    public void clear() {
        appender.clear();
    }

    public String getLog() {
        return String.join(", ", getFormattedMessages());
    }

    public synchronized boolean contains(Level level, String message) {
        ch.qos.logback.classic.Level expectedLevel = convertAnSLF4JLevel(level);
        return appender
            .getRawEvents()
            .stream()
            .anyMatch(event -> event.getLevel() == expectedLevel && event.getFormattedMessage().contains(message));
    }

    private static class ListAppender extends AppenderBase<ILoggingEvent> {

        private final PatternLayoutEncoder encoder;
        private final Queue<ILoggingEvent> events = new ConcurrentLinkedQueue<>();

        ListAppender(PatternLayoutEncoder encoder) {
            this.encoder = encoder;
        }

        @Override
        protected void append(ILoggingEvent e) {
            events.add(e);
        }

        Queue<ILoggingEvent> getRawEvents() {
            return events;
        }

        public void clear() {
            events.clear();
        }

        List<String> getFormattedMessages() {
            return events.stream().map(event -> new String(encoder.encode(event))).toList();
        }

        List<String> getRawMessages() {
            return events.stream().map(ILoggingEvent::getFormattedMessage).toList();
        }

        @Override
        public void stop() {
            super.stop();
            clear();
        }
    }
}
