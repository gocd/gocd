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

import java.io.File;
import java.io.IOException;

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;

/**
 * @understands how to run a local development mode webserver so we can develop live
 * Set the following before running the main method:
 * Working directory: <project-path>/server
 * VM arguments: -Xms512m -Xmx1024m -XX:PermSize=400m
 * classpath: Use classpath of current module.
 */

public class DevelopmentServer {

    public static void main(String[] args) throws Exception {
        FileUtils.copyDirectoryToDirectory(new File("target/webapp/stylesheets/css_sass"), new File("webapp/stylesheets"));
        File path = new File("target/classes");
        File webapp = new File("webapp");
        if (!webapp.exists()) {
            throw new RuntimeException("No webapp found in " + webapp.getAbsolutePath());
        }
        if (!path.exists()) {
            throw new Exception("run b clean cruise:prepare");
        }

        copyActivatorJarToClassPath();

        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.setProperty(SystemEnvironment.PARENT_LOADER_PRIORITY, "true");
        systemEnvironment.setProperty(SystemEnvironment.CRUISE_SERVER_WAR_PROPERTY, webapp.getAbsolutePath());
        systemEnvironment.set(SystemEnvironment.DEFAULT_PLUGINS_ZIP, "/plugins.zip");
        systemEnvironment.set(SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH, "go-plugin-activator.jar");
        systemEnvironment.setProperty(GoConstants.I18N_CACHE_LIFE, "0"); //0 means reload when stale
        File pluginsDist = new File("../tw-go-plugins/dist/");
        if (pluginsDist.exists()) {
            new ZipUtil().zipFolderContents(pluginsDist, new File(path, "plugins.zip"));
        }
        GoServer server = new GoServer();
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.toString(false));
        try {
            server.startServer();
        } catch (Exception e) {
            System.err.println("Failed to start Go server. Exception:");
            e.printStackTrace();
        }
    }

    private static void copyActivatorJarToClassPath() throws IOException {
        File activatorJar = new File("../plugin-infra/go-plugin-activator/target/go-plugin-activator.jar");
        FileUtils.copyFileToDirectory(activatorJar, new File("target/classes"));
    }
}
