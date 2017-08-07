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

package com.thoughtworks.go.server;

import com.thoughtworks.go.logging.LogConfigurator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static com.thoughtworks.go.server.util.GoLauncher.DEFAULT_LOG4J_CONFIGURATION_FILE;

public class LoggingInitializer implements ServletContextListener {

    private LogConfigurator logConfigurator;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logConfigurator = new LogConfigurator(DEFAULT_LOG4J_CONFIGURATION_FILE);
        logConfigurator.initialize();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logConfigurator.shutdown();
    }
}
