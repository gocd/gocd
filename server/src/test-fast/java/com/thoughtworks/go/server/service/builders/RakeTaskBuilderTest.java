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
package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RakeTaskBuilderTest {
    private UpstreamPipelineResolver resolver;
    private BuilderFactory builderFactory;
    private RakeTaskBuilder rakeTaskBuilder;

    @BeforeEach
    void setUp() {
        resolver = mock(UpstreamPipelineResolver.class);
        builderFactory = mock(BuilderFactory.class);
        rakeTaskBuilder = new RakeTaskBuilder();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void rakeTaskShouldNormalizeWorkingDirectory() {
        RakeTask task = new RakeTask();
        task.setWorkingDirectory("folder1\\folder2");
        CommandBuilder commandBuilder = (CommandBuilder) rakeTaskBuilder.createBuilder(builderFactory, task, ExecTaskBuilderTest.pipelineStub("label", "/var/cruise-agent/pipelines/cruise"), resolver);
        assertThat(commandBuilder.getWorkingDir().getPath()).isEqualTo("/var/cruise-agent/pipelines/cruise/folder1/folder2");
    }
}
