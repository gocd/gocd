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

package com.thoughtworks.go.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;

public class LogHelper {

    public static final LoggerContext LOGGER_CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();

    public static PatternLayoutEncoder encoder() {
        return encoder("%date{ISO8601} %-5level [%thread] %logger{0}:%line - %msg%n");
    }

    public static PatternLayoutEncoder encoder(String pattern) {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(LOGGER_CONTEXT);
        encoder.setPattern(pattern);
        encoder.start();
        return encoder;
    }


    public static void rollingPolicyForAppender(RollingFileAppender rollingFileAppender, String maxFileSize, String totalSizeCap, int maxHistory) {
        SizeAndTimeBasedRollingPolicy rollingPolicy = new SizeAndTimeBasedRollingPolicy();
        rollingPolicy.setContext(LOGGER_CONTEXT);
        rollingPolicy.setMaxHistory(maxHistory);
        rollingPolicy.setMaxFileSize(FileSize.valueOf(maxFileSize));
        rollingPolicy.setTotalSizeCap(FileSize.valueOf(totalSizeCap));
        rollingPolicy.setFileNamePattern(rollingFileAppender.rawFileProperty() + ".%d{yyyy-MM-dd}.%i.gz");
        rollingPolicy.setParent(rollingFileAppender);
        rollingFileAppender.setRollingPolicy(rollingPolicy);
        rollingPolicy.start();
    }

    public static ch.qos.logback.classic.Logger rootLogger() {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    }
}
