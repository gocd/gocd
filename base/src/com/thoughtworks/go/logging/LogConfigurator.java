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

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

public class LogConfigurator {
    private final String configDir;
    private final String childLogbackConfigFile;
    private final ILoggerFactory loggerFactory;

    public LogConfigurator(String childLogbackConfigFile) {
        this(new SystemEnvironment().getConfigDir(), childLogbackConfigFile);
    }

    LogConfigurator(String configDir, String childLogbackConfigFile) {
        this.configDir = configDir;
        this.childLogbackConfigFile = childLogbackConfigFile;
        this.loggerFactory = LoggerFactory.getILoggerFactory();
    }

    public void runWithLogger(Runnable runnable) {
        try {
            initialize();
            runnable.run();
        } finally {
            shutdown();
        }
    }

    public <T> T runWithLogger(Supplier<T> supplier) {
        try {
            initialize();
            return supplier.get();
        } finally {
            shutdown();
        }
    }

    public void initialize() {
        if ((!(loggerFactory instanceof LoggerContext))) {
            System.err.println("Unable to initialize logback. It seems that slf4j is bound to an unexpected backend " + loggerFactory.getClass().getName());
            return;
        }

        File logbackFile = new File(configDir, childLogbackConfigFile);

        if (logbackFile.exists()) {
            System.err.println("Using logback configuration from file " + logbackFile);
            configureWith(logbackFile);
        } else {
            System.err.println("Could not find file `" + logbackFile + "'. Attempting to load from classpath.");
            String resourcePath = "config/" + childLogbackConfigFile;
            URL resource = getClass().getClassLoader().getResource(resourcePath);

            if (resource == null) {
                System.err.println("Could not find classpath resource `" + resourcePath + "'. Falling back to using a default logback configuration that writes to stdout.");
                configureDefaultLogging();
            } else {
                System.err.println("Using classpath resource `" + resource + "'.");
                configureWith(resource);
            }
        }
    }

    protected void configureDefaultLogging() {
        ((LoggerContext) loggerFactory).reset();
        // reset will cause log level to be set to debug, so we set it to something more useful
        LogHelper.rootLogger().setLevel(Level.INFO);
        new BasicConfigurator().configure((LoggerContext) loggerFactory);
    }

    protected void configureWith(URL resource) {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext((LoggerContext) loggerFactory);
        ((LoggerContext) loggerFactory).reset();

        // the statusManager keeps a copy of all logback status messages even after reset, so we clear that
        ((LoggerContext) loggerFactory).getStatusManager().clear();
        try {
            configurator.doConfigure(resource);
        } catch (JoranException ignore) {
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings((Context) loggerFactory);
    }

    private void configureWith(File logbackFile) {
        try {
            configureWith(logbackFile.toURI().toURL());
        } catch (MalformedURLException ignore) {
        }
    }

    public void shutdown() {
        if (loggerFactory instanceof LoggerContext) {
            ((LoggerContext) loggerFactory).stop();
        } else {
            System.err.println("Unable to shutdown logback. It seems that slf4j is bound to an unexpected backend " + loggerFactory.getClass().getName());
        }
    }
}
