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
package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.StubGoPublisher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static com.thoughtworks.go.config.RunIfConfig.FAILED;
import static org.assertj.core.api.Assertions.assertThat;

class BuilderTest {
    private StubGoPublisher goPublisher = new StubGoPublisher();

    private EnvironmentVariableContext environmentVariableContext;

    @BeforeEach
    void setUp() throws Exception {
        environmentVariableContext = new EnvironmentVariableContext();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldReportErrorWhenCancelCommandDoesNotExist() {

        StubBuilder stubBuilder = new StubBuilder();

        CommandBuilder cancelBuilder = new CommandBuilder("echo2", "cancel task", new File("."),
                new RunIfConfigs(FAILED), stubBuilder,
                "");

        CommandBuilder builder = new CommandBuilder("echo", "normal task", new File("."), new RunIfConfigs(FAILED),
                cancelBuilder,
                "");
        builder.cancel(goPublisher, new EnvironmentVariableContext(), null, null, "utf-8");

        assertThat(goPublisher.getMessage()).contains("Error happened while attempting to execute 'echo2 cancel task'");
    }

    @Test
    void shouldRunCancelBuilderWhenCanceled() {
        StubBuilder stubBuilder = new StubBuilder();
        CommandBuilder builder = new CommandBuilder("echo", "", new File("."), new RunIfConfigs(FAILED), stubBuilder,
                "");
        builder.cancel(goPublisher, environmentVariableContext, null, null, "utf-8");
        assertThat(stubBuilder.wasCalled).isTrue();
    }

    @Test
    void shouldLogToConsoleOutWhenCanceling() {
        StubBuilder stubBuilder = new StubBuilder();
        CommandBuilder builder = new CommandBuilder("echo", "", new File("."), new RunIfConfigs(FAILED), stubBuilder,
                "");
        builder.cancel(goPublisher, environmentVariableContext, null, null, "utf-8");

        assertThat(goPublisher.getMessage()).contains("On Cancel Task");
        assertThat(goPublisher.getMessage()).contains("On Cancel Task completed");
    }

}
