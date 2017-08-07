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

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.net.URL;

public class LogConfigurator {
    private final String configDir;
    private final String childLog4jConfigFile;

    public LogConfigurator(String childLog4jConfigFile) {
        this(new SystemEnvironment().getConfigDir(), childLog4jConfigFile);
    }

    LogConfigurator(String configDir, String childLog4jConfigFile) {
        this.configDir = configDir;
        this.childLog4jConfigFile = childLog4jConfigFile;
    }

    public void initialize() {
        File log4jFile = new File(configDir, childLog4jConfigFile);

        if (log4jFile.exists()) {
            System.err.println("Using log4j configuration from " + log4jFile);

            if (log4jFile.getName().endsWith(".xml")) {
                initializeFromXMLFile(log4jFile);
            } else {
                initializeFromPropertiesFile(log4jFile);
            }
        } else {
            System.err.println("Could not find file `" + log4jFile + "'. Attempting to load from classpath.");
            String resourcePath = "config/" + childLog4jConfigFile;
            URL resource = getClass().getClassLoader().getResource(resourcePath);
            if (resource == null) {
                System.err.println("Could not find classpath resource `" + resourcePath + "'. Falling back to using a default log4j configuration that writes to stdout.");
                configureDefaultLogging();
            } else {
                System.err.println("Using classpath resource `" + resourcePath + "'.");
                if (childLog4jConfigFile.endsWith(".xml")) {
                    initializeFromXMLResource(resource);
                } else {
                    initializeFromPropertyResource(resource);
                }
            }
        }
    }

    protected void initializeFromPropertyResource(URL resource) {
        PropertyConfigurator.configure(resource);
    }

    protected void initializeFromXMLResource(URL resource) {
        DOMConfigurator.configure(resource);
    }

    protected void initializeFromPropertiesFile(File log4jFile) {
        PropertyConfigurator.configureAndWatch(log4jFile.getAbsolutePath(), 5000);
    }

    protected void initializeFromXMLFile(File log4jFile) {
        DOMConfigurator.configureAndWatch(log4jFile.getAbsolutePath(), 5000);
    }

    protected void configureDefaultLogging() {
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d{ISO8601} [%-9t] %-5p %-16c{4}:%L %x- %m%n")));
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    public void shutdown() {
        LogManager.shutdown();
    }
}
