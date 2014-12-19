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

package com.thoughtworks.go.server;

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.OperatingSystem;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.util.command.ProcessRunner;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @understands how to run a local development mode webserver so we can develop live
 * Set the following before running the main method:
 * Working directory: <project-path>/server
 * VM arguments: -Xms512m -Xmx1024m -XX:PermSize=400m -Djava.awt.headless=true
 * classpath: Use classpath of 'development-server'
 */

public class DevelopmentServer {
    public static void main(String[] args) throws Exception {
        copyDbFiles();
        File webApp = new File("webapp");
        if (!webApp.exists()) {
            throw new RuntimeException("No webapp found in " + webApp.getAbsolutePath());
        }

        copyActivatorJarToClassPath();
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.setProperty(SystemEnvironment.PARENT_LOADER_PRIORITY, "true");
        systemEnvironment.setProperty(SystemEnvironment.CRUISE_SERVER_WAR_PROPERTY, webApp.getAbsolutePath());

        systemEnvironment.set(SystemEnvironment.DEFAULT_PLUGINS_ZIP, "/plugins.zip");
        systemEnvironment.setProperty(GoConstants.I18N_CACHE_LIFE, "0"); //0 means reload when stale
        File pluginsDist = new File("../tw-go-plugins/dist/");
        if (!pluginsDist.exists()) {
            pluginsDist.mkdirs();
        }
        new ZipUtil().zipFolderContents(pluginsDist, new File(classpath(), "plugins.zip"));
        GoServer server = new GoServer();
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.toString(false));
        try {
            server.startServer();

            String hostName = systemEnvironment.getListenHost();
            if(hostName == null){
                hostName = "localhost";
            }

            System.out.println("Go server dashboard started on http://" + hostName + ":" + systemEnvironment.getServerPort());
            System.out.println("* credentials: \"admin\" / \"badger\"");
        } catch (Exception e) {
            System.err.println("Failed to start Go server. Exception:");
            e.printStackTrace();
        }
    }

    private static void copyDbFiles() throws IOException {
        FileUtils.copyDirectoryToDirectory(new File("db/migrate/h2deltas"), new File("db/"));
        if (!new File("db/h2db/cruise.h2.db").exists()) {
            FileUtils.copyDirectoryToDirectory(new File("db/dbtemplate/h2db"), new File("db/"));
        }
    }

    private static void copyActivatorJarToClassPath() throws IOException {
        File activatorJarFromTarget = new File("../plugin-infra/go-plugin-activator/target/go-plugin-activator.jar");
        File activatorJarFromMaven = new File(System.getProperty("user.home") + "/.m2/repository/com/thoughtworks/go/go-plugin-activator/1.0/go-plugin-activator-1.0.jar");
        File activatorJar = activatorJarFromTarget.exists() ? activatorJarFromTarget : activatorJarFromMaven;
        new SystemEnvironment().set(SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH, "go-plugin-activator.jar");

        if (activatorJar.exists()) {
            FileUtils.copyFileToDirectory(activatorJar, classpath());
        } else {
            System.err.println("Could not find plugin activator jar, Plugin framework will not be loaded.");
        }
    }

    private static File classpath() {
        return new File("target/classes");
    }
}
