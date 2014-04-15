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

package com.tw.go.task.curl;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.*;

import java.io.IOException;

public class CurlTaskExecutor implements TaskExecutor {

    public static final String CURLED_FILE = "index.txt";

    @Override
    public ExecutionResult execute(TaskConfig config, TaskExecutionContext taskEnvironment) {
        String value = config.getValue(CurlTask.URL_PROPERTY);
        try {
            return runCommand(taskEnvironment, value);
        } catch (Exception e) {
            return ExecutionResult.failure("Failed to download file from URL: " + value, e);
        }
    }

    private ExecutionResult runCommand(TaskExecutionContext taskContext, String url) throws IOException, InterruptedException {
        ProcessBuilder curl = new ProcessBuilder("curl", "-o",taskContext.workingDir()+"/"+CURLED_FILE, url);

        Console console = taskContext.console();
        console.printLine("Launching command: " + curl.command());

        EnvironmentVariables environment = taskContext.environment();
        curl.environment().putAll(environment.asMap());
        console.printEnvironment(curl.environment(), environment.secureEnvSpecifier());

        Process curlProcess = curl.start();
        console.readErrorOf(curlProcess.getErrorStream());
        console.readOutputOf(curlProcess.getInputStream());

        int exitCode = curlProcess.waitFor();
        curlProcess.destroy();

        if (exitCode != 0) {
            return ExecutionResult.failure("Failed downloading file. Please check the output");
        }

        return ExecutionResult.success("Downloaded file: " + CURLED_FILE);
    }
}
