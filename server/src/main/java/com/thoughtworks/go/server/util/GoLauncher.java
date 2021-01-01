/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.logging.LogConfigurator;
import com.thoughtworks.go.server.GoServer;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;

import java.io.File;


public final class GoLauncher {

    public static final String DEFAULT_LOGBACK_CONFIGURATION_FILE = "logback.xml";

    private GoLauncher() {
    }

    public static void main(String[] args) {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.toString(true));
        LogConfigurator logConfigurator = new LogConfigurator(DEFAULT_LOGBACK_CONFIGURATION_FILE);
        logConfigurator.initialize();

        try {
            cleanupTempFiles();
            new GoServer().go();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to start GoCD server. Please check the logs.");
            e.printStackTrace();
            System.exit(1);
        }
    }


    private static void cleanupTempFiles() {
        FileUtils.deleteQuietly(new File("agent-bootstrapper.jar"));
        FileUtils.deleteQuietly(new File("agent.jar"));
        FileUtils.deleteQuietly(new File("agent-launcher.jar"));
        FileUtils.deleteQuietly(new File("config.properties"));
        FileUtils.deleteQuietly(new File("historical_jars"));
        FileUtils.deleteQuietly(new File(new SystemEnvironment().getConfigDir(), "agentkeystore"));
        FileUtils.deleteQuietly(new File(new SystemEnvironment().getConfigDir(), "gadget_truststore.jks"));
        FileUtils.deleteQuietly(new File(new SystemEnvironment().getConfigDir(), "config.properties"));
        FileUtils.deleteQuietly(new File(new SystemEnvironment().getConfigDir(), "go-config-before-migration-91.xml"));
        FileUtils.deleteQuietly(new File(new SystemEnvironment().getConfigDir(), "go-config-before-migration-92.xml"));
    }

}
