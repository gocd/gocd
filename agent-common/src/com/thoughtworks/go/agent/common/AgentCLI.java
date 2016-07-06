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

package com.thoughtworks.go.agent.common;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import java.io.PrintStream;

public class AgentCLI {
    private final PrintStream stderr;
    private final SystemExitter exitter;

    public interface SystemExitter {
        void exit(int status);
    }

    public AgentCLI() {
        this(System.err, new SystemExitter() {
            @Override
            public void exit(int status) {
                System.exit(status);
            }
        });
    }

    public AgentCLI(PrintStream stderr, SystemExitter exitter) {
        this.stderr = stderr;
        this.exitter = exitter;
    }

    public AgentBootstrapperArgs parse(String... args) {
        AgentBootstrapperArgs result = new AgentBootstrapperArgs();
        try {
            new JCommander(result, args);

            if (result.help) {
                printUsageAndExit(0);
            }

            return result;
        } catch (ParameterException e) {
            stderr.println(e.getMessage());
            printUsageAndExit(1);
        }

        return null;
    }

    private void printUsageAndExit(int exitCode) {
        StringBuilder out = new StringBuilder();
        JCommander jCommander = new JCommander(new AgentBootstrapperArgs());
        jCommander.setProgramName("java -jar agent-bootstrapper.jar");
        jCommander.usage(out);
        stderr.print(out);
        exit(exitCode);
    }

    public void exit(int status) {
        exitter.exit(status);
    }
}
