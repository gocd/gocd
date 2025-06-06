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
package com.thoughtworks.go.server;

import com.thoughtworks.go.logging.LogConfigurator;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.thoughtworks.go.server.util.GoLauncher.DEFAULT_LOGBACK_CONFIGURATION_FILE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hibernate.cfg.Environment.GENERATE_STATISTICS;

/**
 * Understands how to run a local development mode webserver so we can develop live
 * Set the following before running the main method:
 * Working directory: <project-path>/server
 * VM arguments:
 * -Xms512m -Xmx1024m -Djava.awt.headless=true
 * classpath: Use classpath of 'gocd.development-utility.development-server.main'
 */

public class DevelopmentServer {
    public static void main(String[] args) throws Exception {
        LogConfigurator logConfigurator = new LogConfigurator(DEFAULT_LOGBACK_CONFIGURATION_FILE);
        logConfigurator.initialize();
        copyPluginAssets();
        File webApp = new File("src/main/webapp");
        if (!webApp.exists()) {
            throw new RuntimeException("No webapp found in " + webApp.getAbsolutePath());
        }

        assertActivationJarPresent();
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.setProperty(GENERATE_STATISTICS, "true");

        systemEnvironment.setProperty(SystemEnvironment.PARENT_LOADER_PRIORITY, "true");
        systemEnvironment.setProperty(SystemEnvironment.CRUISE_SERVER_WAR_PROPERTY, webApp.getAbsolutePath());
        systemEnvironment.set(SystemEnvironment.PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS, 5);

        systemEnvironment.set(SystemEnvironment.DEFAULT_PLUGINS_ZIP, "/plugins.zip");
        systemEnvironment.set(SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH, "go-plugin-activator.jar");
        systemEnvironment.set(SystemEnvironment.GO_SERVER_MODE, "development");
        setupPeriodicGC(systemEnvironment);
        assertPluginsZipExists();
        GoServer server = new GoServer();
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.toString(false));
        try {
            server.startServer();

            String hostName = systemEnvironment.getListenHost();
            if (hostName == null) {
                hostName = "localhost";
            }

            System.out.println("GoCD server dashboard started on http://" + hostName + ":" + systemEnvironment.getServerPort());
            System.out.println("* credentials: \"admin\" / \"badger\"");
        } catch (Exception e) {
            System.err.println("Failed to start GoCD server. Exception:");
            e.printStackTrace();
        }
    }

    private static void assertPluginsZipExists() {
        if (DevelopmentServer.class.getResource("/plugins.zip") == null) {
            throw new IllegalArgumentException("Could not find plugins.zip. Hint: Did you run `./gradlew prepare`?");
        }
    }

    private static void setupPeriodicGC(SystemEnvironment systemEnvironment) {
        systemEnvironment.set(SystemEnvironment.GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD, 100L);
        systemEnvironment.set(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC, true);
        systemEnvironment.set(SystemEnvironment.GO_CONFIG_REPO_GC_AGGRESSIVE, true);
        systemEnvironment.setProperty("go.config.repo.gc.cron", "0 0/1 * 1/1 * ?");
        systemEnvironment.setProperty("go.config.repo.gc.check.interval", "10000");
    }

    private static void copyPluginAssets() throws IOException {
        Path classPathRoot = Path.of(DevelopmentServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        Files.copy(Path.of("src/main/webapp/WEB-INF/rails/webpack/rails-shared/plugin-endpoint.js"), classPathRoot.resolve("plugin-endpoint.js"), REPLACE_EXISTING);
    }

    private static void assertActivationJarPresent() {
        if (DevelopmentServer.class.getResource("/go-plugin-activator.jar") == null) {
            System.err.println("Could not find plugin activator jar, Plugin framework will not be loaded. Hint: Did you run `./gradlew prepare`?");
        }
    }

}
