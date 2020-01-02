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

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StubGoPublisher;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AntTaskBuilderTest {
    private AntTask antTask;
    private static final String DEFAULT_WORKING_DIRECTORY = "default/working/directory";
    private static final String PIPELINE_LABEL = "label";
    private Pipeline pipeline = ExecTaskBuilderTest.pipelineStub(PIPELINE_LABEL, DEFAULT_WORKING_DIRECTORY);

    private UpstreamPipelineResolver resolver;
    private AntTaskBuilder antTaskBuilder;
    private BuilderFactory builderFactory;
    private ExecTaskBuilder execTaskBuilder;
    private TaskExtension taskEntension;

    @BeforeEach
    void setup() {
        antTask = new AntTask();
        antTaskBuilder = new AntTaskBuilder();
        execTaskBuilder = new ExecTaskBuilder();
        builderFactory = mock(BuilderFactory.class);
        resolver = mock(UpstreamPipelineResolver.class);
        taskEntension = mock(TaskExtension.class);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test
    void shouldUseAbsoluteWorkingDirectoryWhenItIsSet() {
        final File absoluteFile = new File("me/antdirectory").getAbsoluteFile();
        antTask.setWorkingDirectory(absoluteFile.getPath());

        CommandBuilder builder = (CommandBuilder) antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        assertThat(builder.getWorkingDir()).isEqualTo(absoluteFile);
    }

    @Test
    void shouldUseDefaultWorkingDirectoryWhenItIsNotSet() {
        File workingDir = new File(DEFAULT_WORKING_DIRECTORY);
        CommandBuilder builder = (CommandBuilder) antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        assertThat(builder.getWorkingDir()).isEqualTo(workingDir);
    }

    @Test
    void shouldFailWhenTargetDoesNotExist() {
        String target = "not-exist-target";
        String buildXml = "./build.xml";
        antTask.setBuildFile(buildXml);
        antTask.setTarget(target);
        Builder builder = antTaskBuilder.createBuilder(builderFactory, antTask, ExecTaskBuilderTest.pipelineStub(PIPELINE_LABEL, "."), resolver);

        try {
            builder.build(new StubGoPublisher(), new EnvironmentVariableContext(), taskEntension, null, null, "utf-8");
        } catch (CruiseControlException e) {
            assertThat(e.getMessage()).contains("Build failed. Command ant reported [BUILD FAILED].");
        }
    }

    @Test
    void shouldPrependDefaultWorkingDirectoryIfRelativeAntHomeIsUsed() {
        antTask.setWorkingDirectory("lib");
        File baseDir = new File(DEFAULT_WORKING_DIRECTORY);
        CommandBuilder builder = (CommandBuilder) antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        assertThat(builder.getWorkingDir()).isEqualTo(new File(baseDir, "lib"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void antTaskShouldNormalizeWorkingDirectory() {
        AntTask task = new AntTask();
        task.setWorkingDirectory("folder1\\folder2");

        CommandBuilder commandBuilder = (CommandBuilder) antTaskBuilder.createBuilder(builderFactory, task, ExecTaskBuilderTest.pipelineStub("label", "/var/cruise-agent/pipelines/cruise"), resolver);

        assertThat(commandBuilder.getWorkingDir().getPath()).isEqualTo("/var/cruise-agent/pipelines/cruise/folder1/folder2");
    }

    @Test
    void shouldReturnBuilderWithCancelBuilderIfOnCancelDefined() {
        ExecTask cancelTask = new ExecTask();
        Builder builderForCancelTask = execTaskBuilder.createBuilder(builderFactory, cancelTask, pipeline, resolver);

        AntTask antTask = new AntTask();
        antTask.setCancelTask(cancelTask);
        when(builderFactory.builderFor(cancelTask, pipeline, resolver)).thenReturn(builderForCancelTask);

        Builder expected = expectedBuilder(antTask, builderForCancelTask);

        Builder actualBuilder = antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        assertThat(actualBuilder).isEqualTo(expected);
    }

    private Builder expectedBuilder(AntTask antTask, Builder builderForCancelTask) {
        Builder expected = antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        expected.setCancelBuilder(builderForCancelTask);
        return expected;
    }
}
