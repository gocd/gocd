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
import ch.qos.logback.classic.jul.LevelChangePropagator;
import com.thoughtworks.go.logging.LogConfigurator;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Routes java.util.logging (used by e.g. the PostgreSQL JDBC driver, JAXB and Jakarta Mail) to slf4j/logback,
 * so such logging respects the logback configuration rather than going to JUL's default stderr handler.
 */
@UtilityClass
public final class ServerLogging {
    private static final String DEFAULT_LOGBACK_CONFIGURATION_FILE = "logback.xml";

    public static void initialize() {
        new LogConfigurator(DEFAULT_LOGBACK_CONFIGURATION_FILE).initialize();
        redirectJavaUtilLogging();
    }

    @VisibleForTesting
    static void redirectJavaUtilLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
            // Propagates logback level changes to JUL loggers so disabled JUL log statements are cheap
            LevelChangePropagator propagator = new LevelChangePropagator();
            propagator.setContext(loggerContext);
            propagator.setResetJUL(true);
            propagator.start();
            loggerContext.addListener(propagator);
        }
    }
}
