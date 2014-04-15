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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @understands how to run a local development mode agent so we can develop live
 * Set the following before running the main method:
 * Working directory: <project-path>/agent
 * classpath: Use classpath of current module.
 */

public class DevelopmentAgent {
    public static void main(String[] args) throws Exception {
        runProcess("curl", "http://localhost:8153/go/admin/agent-plugins.zip", "-o", "agent-plugins.zip");
        copyActivatorJarToClassPath();
        new SystemEnvironment().set(SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH, "go-plugin-activator.jar");
        AgentMain.main("https://localhost:8154/go");
    }

    private static void runProcess(String... command) throws IOException, InterruptedException {
        System.out.println("Trying to run command: " + Arrays.toString(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        int exitCode = process.waitFor();

        System.out.println("Finished command: " + Arrays.toString(command) + ". Exit code: " + exitCode);
        if (exitCode != 0)
            throw new RuntimeException(String.format("Command exited with code %s. \n Exception: %s", exitCode, IOUtils.toString(process.getErrorStream())));
    }

    private static void copyActivatorJarToClassPath() throws IOException {
        File activatorJar = new File("../plugin-infra/go-plugin-activator/target/go-plugin-activator.jar");
        FileUtils.copyFileToDirectory(activatorJar, new File("target/classes"));
    }
}
