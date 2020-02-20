/*
 * Copyright 2020 ThoughtWorks, Inc.
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

/**
 * @understands how to run a local development mode agent so we can develop live
 * Set the following before running the main method:
 * Working directory: <project-path>/agent
 * VM arguments: -Djava.awt.headless=true
 * classpath: Use classpath of 'gocd.development-utility.development-agent.main'.
 */

public class DevelopmentAgent {
    public static void main(String[] args) throws Exception {
        new ProcessRunner().command("curl", "--insecure", "http://localhost:8153/go/admin/agent-plugins.zip", "--fail", "--silent", "--output", "agent-plugins.zip").failOnError(false).run();
        new ProcessRunner().command("curl", "--insecure", "http://localhost:8153/go/admin/tfs-impl.jar", "--fail", "--silent", "--output", "tfs-impl.jar").failOnError(false).run();
        new SystemEnvironment().set(SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH, "go-plugin-activator.jar");
        assertActivationJarPresent();
        AgentMain.main("-serverUrl", "http://localhost:8153/go");
    }

    private static void assertActivationJarPresent() {
        if (DevelopmentAgent.class.getResource("/go-plugin-activator.jar") == null) {
            System.err.println("Could not find plugin activator jar, Plugin framework will not be loaded. Hint: Did you run `./gradlew prepare`?");
        }
    }
}
