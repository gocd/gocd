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
package com.thoughtworks.go.server.util;

import ch.qos.logback.classic.LoggerContext;
import com.thoughtworks.go.util.LogFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.event.Level;

import java.util.logging.Logger;

import static com.thoughtworks.go.util.LogFixture.logFixtureForLogger;
import static org.assertj.core.api.Assertions.assertThat;

public class ServerLoggingTest {

    @BeforeAll
    static void redirectJavaUtilLogging() {
        ServerLogging.redirectJavaUtilLogging();
    }

    @AfterAll
    static void uninstallBridge() {
        SLF4JBridgeHandler.uninstall();
    }

    @Test
    public void shouldRedirectJavaUtilLoggingToLogback() {
        try (LogFixture fixture = logFixtureForLogger("test.jul.redirect")) {
            Logger.getLogger("test.jul.redirect").warning("warning via java.util.logging");

            assertThat(fixture.contains(Level.WARN, "warning via java.util.logging")).isTrue();
        }
    }

    @Test
    public void shouldPropagateLogbackLevelsToJavaUtilLoggingSoDisabledLogStatementsAreCheap() {
        Logger julLogger = Logger.getLogger("test.jul.levels");
        ch.qos.logback.classic.Logger logbackLogger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("test.jul.levels");
        try {
            logbackLogger.setLevel(ch.qos.logback.classic.Level.WARN);
            assertThat(julLogger.isLoggable(java.util.logging.Level.INFO)).isFalse();
            assertThat(julLogger.isLoggable(java.util.logging.Level.WARNING)).isTrue();

            logbackLogger.setLevel(ch.qos.logback.classic.Level.TRACE);
            assertThat(julLogger.isLoggable(java.util.logging.Level.FINEST)).isTrue();
        } finally {
            logbackLogger.setLevel(null);
        }
    }
}
