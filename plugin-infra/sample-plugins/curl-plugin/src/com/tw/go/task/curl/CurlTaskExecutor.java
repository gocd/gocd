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
        try {
            return runCommand(taskEnvironment, config);
        } catch (Exception e) {
            return ExecutionResult.failure("Failed to download file from URL: " + config.getValue(CurlTask.URL_PROPERTY), e);
        }
    }

    private ExecutionResult runCommand(TaskExecutionContext taskContext, TaskConfig taskConfig) throws IOException, InterruptedException {
        ProcessBuilder curl = createCurlCommandWithOptions(taskContext, taskConfig);

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

    private ProcessBuilder createCurlCommandWithOptions(TaskExecutionContext taskContext, TaskConfig taskConfig) {
        String url = taskConfig.getValue(CurlTask.URL_PROPERTY);
        String secureConnection = taskConfig.getValue(CurlTask.SECURE_CONNECTION_PROPERTY).equals("yes") ? "" : "--insecure";
        String requestType = taskConfig.getValue(CurlTask.REQUEST_PROPERTY);

        return new ProcessBuilder("curl", requestType, secureConnection, "-o", taskContext.workingDir() + "/" + CURLED_FILE, url);
    }
}
