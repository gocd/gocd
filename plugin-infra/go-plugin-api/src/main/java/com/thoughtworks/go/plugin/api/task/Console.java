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

package com.thoughtworks.go.plugin.api.task;

import java.io.InputStream;
import java.util.Map;

/**
 * Used to write information of a task run, out to the build log.
 */
@Deprecated
//Will be moved to internal scope
public interface Console {
    /**
     * Print a line out to the build log.
     *
     * @param line Line to write.
     */
    void printLine(String line);

    /**
     * Setup the console to read the input stream as standard error.
     * <p></p>
     * This is used to connect the output of a process, to the build log. This is usually used as:
     * console.readErrorOf(process.getErrorStream());
     * <p></p>
     * where the "process" object is of type {@link java.lang.Process}.
     *
     * @param in The input stream to read as standard error.
     */
    void readErrorOf(InputStream in);

    /**
     * Setup the console to read the input stream as standard output.
     * <p></p>
     * This is used to connect the output of a process, to the build log. This is usually used as:
     * console.readOutputOf(process.getInputStream());
     * <p></p>
     * where the "process" object is of type {@link java.lang.Process}.
     *
     * @param in The input stream to read as standard output.
     */
    void readOutputOf(InputStream in);

    /**
     * Print details about the environment specified in the argument into the build log.
     *
     * @param environment Environment to print details of.
     * @param secureEnvVarSpecifier {@link com.thoughtworks.go.plugin.api.task.Console.SecureEnvVarSpecifier}
     */
    void printEnvironment(Map<String, String> environment, SecureEnvVarSpecifier secureEnvVarSpecifier);

    /**
     * Used to specify which environment variables are secure and shouldn't be printed literally.
     */
    interface SecureEnvVarSpecifier {
        public boolean isSecure(String variableName);
    }
}
