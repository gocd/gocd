/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.ProcessRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * @understands how to run a local development mode agent so we can develop live
 * Set the following before running the main method:
 * Working directory: <project-path>/agent
 * VM arguments: -Djava.awt.headless=true
 * classpath: Use classpath of 'development-agent'.
 */

public class DevelopmentAgent {
    public static void main(String[] args) throws Exception {
        new ProcessRunner().command("curl", "http://localhost:8153/go/admin/agent-plugins.zip", "-o", "agent-plugins.zip").failOnError(false).run();
        copyActivatorJarToClassPath();
        AgentMain.main("-serverUrl", "https://localhost:8154/go");
    }

    private static void copyActivatorJarToClassPath() throws IOException {
        File activatorJar = new File("../plugin-infra/go-plugin-activator/target/libs/").listFiles((FileFilter) new WildcardFileFilter("go-plugin-activator-*.jar"))[0];
        new SystemEnvironment().set(SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH, activatorJar.getName());
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.set(SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH, "go-plugin-activator.jar");
        systemEnvironment.set(SystemEnvironment.PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS, 5);

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
