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

package com.thoughtworks.go.agent.bootstrapper;


import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

final public class BootstrapperLoggingHelper {
    private BootstrapperLoggingHelper() {
    }

    private static final PatternLayout LOG4J_PATTERN =
            new PatternLayout("%d{ISO8601} [%-9t] %-5p %-16c{4}:%L %x- %m%n");

    public static void initLog4j() {
        // If there are no appenders, then Log4J is not configured, so we create a default one.
        if (!LogManager.getRootLogger().getAllAppenders().hasMoreElements()) {
            setupDefaultLog4j();
        }
    }

    private static void setupDefaultLog4j() {
        String logFile = System.getenv("LOG_FILE");
        System.out.println("logFile Environment Variable= " + logFile);
        try {
            if (logFile == null) {
                logFile = "go-agent-bootstrapper.log";
            }
            System.out.println("Logging to " + logFile);
            BasicConfigurator.configure(new FileAppender(LOG4J_PATTERN, logFile));
            Logger.getRootLogger().setLevel(Level.INFO);
        } catch (IOException e) {
            BasicConfigurator.configure(new ConsoleAppender(LOG4J_PATTERN));
            Logger.getRootLogger().setLevel(Level.INFO);
            Log LOG = LogFactory.getLog(BootstrapperLoggingHelper.class);
            LOG.warn("Unable to initialize log4j file-appender: " + logFile, e);
            LOG.warn("Using console-appender instead");
        }

    }
}
