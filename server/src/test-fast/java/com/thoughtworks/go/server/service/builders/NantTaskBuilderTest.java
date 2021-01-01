/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.NantTask;
import com.thoughtworks.go.domain.NullPipeline;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class NantTaskBuilderTest {
    private Pipeline pipeline = new NullPipeline();
    private UpstreamPipelineResolver resolver;
    private NantTaskBuilder nantTaskBuilder;
    private BuilderFactory builderFactory;

    @BeforeEach
    void setUp() {
        resolver = mock(UpstreamPipelineResolver.class);
        builderFactory = mock(BuilderFactory.class);
        nantTaskBuilder = new NantTaskBuilder();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test
    void shouldSetTargetWhenTargetIsSpecified() {
        NantTask nantTask = new NantTask();
        nantTask.setTarget("unit-test");
        CommandBuilder commandBuilder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(commandBuilder.getArgs()).isEqualTo("unit-test");
    }

    @Test
    void shouldUseDefaultWorkingDirectoryByDefault() {
        NantTask nantTask = new NantTask();
        CommandBuilder commandBuilder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, ExecTaskBuilderTest.pipelineStub("label", "/cruise"), resolver);
        assertThat(commandBuilder.getWorkingDir()).isEqualTo(new File("/cruise"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldUseAbsoluteNantPathIfAbsoluteNantPathIsSpecifiedOnLinux() {
        NantTask nantTask = new NantTask();
        nantTask.setNantPath("/usr/bin");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(new File(builder.getCommand())).isEqualTo(new File("/usr/bin/nant"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldUseAbsoluteNantPathIfAbsoluteNantPathIsSpecifiedOnWindows() {
        NantTask nantTask = new NantTask();
        nantTask.setNantPath("c:\\nantdir");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(new File(builder.getCommand())).isEqualTo(new File("c:\\nantdir\\nant"));
    }

    @Test
    void shouldJoinNantPathWithWorkingDirectoryIfRelativeNantPathIsSpecified() {
        NantTask nantTask = new NantTask();
        nantTask.setNantPath("lib");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(new File(builder.getCommand())).isEqualTo(new File("lib/nant"));
    }

    @Test
    void shouldDealWithSpacesInNantPath() {
        NantTask nantTask = new NantTask();
        nantTask.setNantPath("lib/nant 1.0");
        nantTask.setBuildFile("ccnet default.build");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(new File(builder.getCommand())).isEqualTo(new File("lib/nant 1.0/nant"));
        assertThat(builder.getArgs()).isEqualTo("-buildfile:\"ccnet default.build\"");
    }


    @Test
    @DisabledOnOs(OS.WINDOWS)
    void nantTaskShouldNormalizeWorkingDirectory() {
        NantTask nantTask = new NantTask();
        nantTask.setWorkingDirectory("folder1\\folder2");
        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, ExecTaskBuilderTest.pipelineStub("label", "/var/cruise-agent/pipelines/cruise"), resolver);
        assertThat(builder.getWorkingDir()).isEqualTo(new File("/var/cruise-agent/pipelines/cruise/folder1/folder2"));
    }

    @Test
    void shouldSetNAntWorkingDirectoryAbsolutelyIfSpecified() {
        final File absoluteFile = new File("project").getAbsoluteFile();
        NantTask task = new NantTask();
        task.setWorkingDirectory(absoluteFile.getPath());

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, task, pipeline, resolver);
        assertThat(builder.getWorkingDir()).isEqualTo(absoluteFile);
    }

    @Test
    void nantTaskShouldNormalizeBuildFile() {
        NantTask task = new NantTask();
        task.setBuildFile("pavan\\build.xml");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, task, pipeline, resolver);

        assertThat(builder.getArgs()).isEqualTo("-buildfile:\"pavan/build.xml\"");
    }

}
