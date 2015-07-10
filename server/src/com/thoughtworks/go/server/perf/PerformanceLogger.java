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

package com.thoughtworks.go.server.perf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PerformanceLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("PerformanceLogger");
    private final boolean isDebuggingTurnedOn;

    public PerformanceLogger() {
        isDebuggingTurnedOn = LOGGER.isDebugEnabled();
    }

    public void log(String format, Object... arguments) {
        LOGGER.debug(format, arguments);
    }

    public boolean isLoggingTurnedOn() {
        return isDebuggingTurnedOn;
    }
}

/* Use with these log4j settings:
log4j.logger.PerformanceLogger=DEBUG, PerformanceLoggerAppender
log4j.additivity.PerformanceLogger=false
log4j.appender.PerformanceLoggerAppender=org.apache.log4j.RollingFileAppender
log4j.appender.PerformanceLoggerAppender.File=go-perf.log
log4j.appender.PerformanceLoggerAppender.MaxFileSize=10240KB
log4j.appender.PerformanceLoggerAppender.MaxBackupIndex=50
log4j.appender.PerformanceLoggerAppender.layout=org.apache.log4j.PatternLayout
log4j.appender.PerformanceLoggerAppender.layout.conversionPattern=%d{yyyy-MM-dd HH:mm:ss,SSSZ} |%t| %m%n
*/