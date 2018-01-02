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

package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.util.command.StreamPumper;
import com.thoughtworks.go.work.DefaultGoPublisher;

import java.io.InputStream;
import java.util.Map;

public class PluggableTaskConsole implements Console {
    public static final String MASK_VALUE = "********";
    private final DefaultGoPublisher publisher;

    public PluggableTaskConsole(DefaultGoPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void printLine(String line) {
        publisher.consumeLine(line);
    }

    @Override
    public void readErrorOf(InputStream in) {
        StreamPumper.pump(in, publisher, "", null);
    }

    @Override
    public void readOutputOf(InputStream in) {
        StreamPumper.pump(in, publisher, "", null);
    }

    @Override
    public void printEnvironment(Map<String, String> environment, SecureEnvVarSpecifier secureEnvVarSpecifier) {
        publisher.consumeLine("Environment variables: ");
        for (String key : environment.keySet()) {
            boolean secure = secureEnvVarSpecifier.isSecure(key);
            publisher.consumeLine(String.format("Name= %s  Value= %s", key, secure ? MASK_VALUE : environment.get(key)));
        }
    }
}
