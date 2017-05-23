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
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.util.command.ProcessRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import static com.thoughtworks.go.server.util.GoLauncher.DEFAULT_LOG4J_CONFIGURATION_FILE;
import static org.hibernate.cfg.Environment.GENERATE_STATISTICS;

/**
 * @understands how to run a local development mode webserver so we can develop live
 * Set the following before running the main method:
 * Working directory: <project-path>/server
 * VM arguments: -Xms512m -Xmx1024m -Djava.awt.headless=true
 * classpath: Use classpath of 'development-server'
 */

public class DevelopmentServer {
    public static void main(String[] args) throws Exception {
        LogConfigurator logConfigurator = new LogConfigurator(DEFAULT_LOG4J_CONFIGURATION_FILE);
        logConfigurator.initialize();
        copyDbFiles();
        File webApp = new File("webapp");
        if (!webApp.exists()) {
            throw new RuntimeException("No webapp found in " + webApp.getAbsolutePath());
        }

        copyActivatorJarToClassPath();
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.setProperty(GENERATE_STATISTICS, "true");

        systemEnvironment.setProperty(SystemEnvironment.PARENT_LOADER_PRIORITY, "true");
        systemEnvironment.setProperty(SystemEnvironment.CRUISE_SERVER_WAR_PROPERTY, webApp.getAbsolutePath());
        systemEnvironment.set(SystemEnvironment.PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS, 5);

        systemEnvironment.set(SystemEnvironment.DEFAULT_PLUGINS_ZIP, "/plugins.zip");
        systemEnvironment.setProperty(GoConstants.I18N_CACHE_LIFE, "0"); //0 means reload when stale
        systemEnvironment.set(SystemEnvironment.GO_SERVER_MODE, "development");
        setupPeriodicGC(systemEnvironment);
        setupPlugins();
        GoServer server = new GoServer();
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.toString(false));
        try {
            server.startServer();

            String hostName = systemEnvironment.getListenHost();
            if (hostName == null) {
                hostName = "localhost";
            }

            System.out.println("Go server dashboard started on http://" + hostName + ":" + systemEnvironment.getServerPort());
            System.out.println("* credentials: \"admin\" / \"badger\"");
        } catch (Exception e) {
            System.err.println("Failed to start Go server. Exception:");
            e.printStackTrace();
        }
    }

    private static void setupPlugins() throws IOException, InterruptedException {
        File pluginsDist = new File("../tw-go-plugins/dist/");
        if (!pluginsDist.exists()) {
            pluginsDist.mkdirs();
        }
        File passwordFilePluginJar = new File(pluginsDist, "filebased-authentication-plugin.jar");
        if (passwordFilePluginJar.exists()) {
            System.out.println("Found a local copy of passwordFile plugin, using it.");
        } else {
            new ProcessRunner().command("curl", "-L", "https://github.com/gocd/filebased-authentication-plugin/releases/download/v0.1/filebased-authentication-plugin-0.0.1-SNAPSHOT.jar", "--output", passwordFilePluginJar.getAbsolutePath()).failOnError(true).run();
        }
        new ZipUtil().zipFolderContents(pluginsDist, new File(classpath(), "plugins.zip"));
    }

    private static void setupPeriodicGC(SystemEnvironment systemEnvironment) {
        systemEnvironment.set(SystemEnvironment.GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD, 100L);
        systemEnvironment.set(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC, true);
        systemEnvironment.set(SystemEnvironment.GO_CONFIG_REPO_GC_AGGRESSIVE, true);
        systemEnvironment.setProperty("go.config.repo.gc.cron", "0 0/1 * 1/1 * ?");
        systemEnvironment.setProperty("go.config.repo.gc.check.interval", "10000");
    }

    private static void copyDbFiles() throws IOException {
        FileUtils.copyDirectoryToDirectory(new File("db/migrate/h2deltas"), new File("db/"));
        if (!new File("db/h2db/cruise.h2.db").exists()) {
            FileUtils.copyDirectoryToDirectory(new File("db/dbtemplate/h2db"), new File("db/"));
        }
    }

    private static void copyActivatorJarToClassPath() throws IOException {
        File activatorJar = new File("../plugin-infra/go-plugin-activator/target/libs/").listFiles((FileFilter) new WildcardFileFilter("go-plugin-activator-*.jar"))[0];
        new SystemEnvironment().set(SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH, "go-plugin-activator.jar");

        if (activatorJar.exists()) {
            FileUtils.copyFile(activatorJar, new File(classpath(), "go-plugin-activator.jar"));
        } else {
            System.err.println("Could not find plugin activator jar, Plugin framework will not be loaded.");
        }
    }

    private static File classpath() {
        return new File("target/classes/main");
    }
}
