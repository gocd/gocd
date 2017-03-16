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

package com.thoughtworks.go.agent.common.util;

import com.thoughtworks.go.util.StringUtil;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.util.Enumeration;

public class LoggingHelper {

    private static final PatternLayout LOG4J_PATTERN = new PatternLayout("%d{ISO8601} [%-9t] %-5p %-16c{4}:%L %x- %m%n");
    private static final PatternLayout LOG4J_CONSOLE_PATTERN = new PatternLayout("%m%n");
    public static final String LOG_DIR = "LOG_DIR";
    private static String logDirectory = getEffectiveLogDirectory();
    private static final Filter CONSOLE_NDC_REJECT_FILTER = new Filter() {
        @Override public int decide(LoggingEvent event) {
            return CONSOLE_NDC.STDOUT.toString().equals(event.getNDC())
                    || CONSOLE_NDC.STDERR.toString().equals(event.getNDC()) ? DENY : ACCEPT;
        }
    };

    public static enum CONSOLE_NDC {STDOUT, STDERR};

    private static Appender createFileAppender(String logOutputFilename) {
        RollingFileAppender appender;
        try {
            appender = new RollingFileAppender(LOG4J_PATTERN, logFilePath(logOutputFilename), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        appender.setMaxBackupIndex(4);
        appender.setMaxFileSize("5000KB");
        return appender;
    }

    private static String logFilePath(String logOutputFilename) {
        if (!StringUtil.isBlank(logDirectory)) {
            return String.format("%s/%s", logDirectory, logOutputFilename);
        }
        return logOutputFilename;
    }

    private static String getEffectiveLogDirectory() {
        return StringUtil.isBlank(System.getProperty(LOG_DIR)) ? System.getenv(LOG_DIR) : System.getProperty(LOG_DIR);
    }

    public static Appender createConsoleFileAppender(final CONSOLE_NDC ndc, String consoleFileName) {
        Appender consoleAppender = createFileAppender(consoleFileName);
        consoleAppender.setName(ndc.name());
        consoleAppender.setLayout(LOG4J_CONSOLE_PATTERN);
        //Console NDC acceptance filter
        consoleAppender.addFilter(new org.apache.log4j.spi.Filter() {
            @Override public int decide(LoggingEvent event) {
                return event.getNDC() == ndc.toString() ? ACCEPT : DENY;
            }
        });
        addConsoleNDCLogRemoverFilter();
        return consoleAppender;
    }

    private static void addConsoleNDCLogRemoverFilter() {
        Enumeration allAppenders = Logger.getRootLogger().getAllAppenders();

        while (allAppenders.hasMoreElements()) {
            Appender appender = (Appender) allAppenders.nextElement();
            if (isConsoleNdcAppender(appender)) {
                continue;
            }
            Filter filter = getConsoleNDCFilter(appender);
            if (filter != CONSOLE_NDC_REJECT_FILTER) {
                appender.addFilter(CONSOLE_NDC_REJECT_FILTER);
            }
        }
    }

    private static Filter getConsoleNDCFilter(Appender appender) {
        Filter filter = appender.getFilter();
        while (filter != null) {
            if (filter == CONSOLE_NDC_REJECT_FILTER) {
                break;
            }
            filter = filter.getNext();
        }
        return filter;
    }

    private static boolean isConsoleNdcAppender(Appender appender) {
        return CONSOLE_NDC.STDOUT.name().equals(appender.getName())
                || CONSOLE_NDC.STDERR.name().equals(appender.getName());
    }

}
