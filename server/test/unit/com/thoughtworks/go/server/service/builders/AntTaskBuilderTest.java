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

package com.thoughtworks.go.server.service.builders;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StubGoPublisher;
import com.thoughtworks.go.domain.TasksTest;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(JunitExtRunner.class)
public class AntTaskBuilderTest {
    private AntTask antTask;
    private static final String DEFAULT_WORKING_DIRECTORY = "default/working/directory";
    private static final String PIPELINE_LABEL = "label";
    private Pipeline pipeline = TasksTest.pipelineStub(PIPELINE_LABEL, DEFAULT_WORKING_DIRECTORY);

    private UpstreamPipelineResolver resolver;
    private AntTaskBuilder antTaskBuilder;
    private BuilderFactory builderFactory;
    private ExecTaskBuilder execTaskBuilder;
    private TaskExtension taskEntension;

    @Before
    public void setup() throws Exception {
        antTask = new AntTask();
        antTaskBuilder = new AntTaskBuilder();
        execTaskBuilder = new ExecTaskBuilder();
        builderFactory = mock(BuilderFactory.class);
        resolver = mock(UpstreamPipelineResolver.class);
        taskEntension = mock(TaskExtension.class);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldUseAbsoluteWorkingDirectoryWhenItIsSet() throws Exception {
        final File absoluteFile = new File("me/antdirectory").getAbsoluteFile();
        antTask.setWorkingDirectory(absoluteFile.getPath());

        CommandBuilder builder = (CommandBuilder) antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        assertThat(builder.getWorkingDir(), is(absoluteFile));
    }

    @Test
    public void shouldUseDefaultWorkingDirectoryWhenItIsNotSet() throws Exception {
        File workingDir = new File(DEFAULT_WORKING_DIRECTORY);
        CommandBuilder builder = (CommandBuilder) antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        assertThat(builder.getWorkingDir(), is(workingDir));
    }

    @Test
    public void shouldFailWhenTargetDoesNotExist() throws Exception {
        String target = "not-exist-target";
        String buildXml = "./build.xml";
        antTask.setBuildFile(buildXml);
        antTask.setTarget(target);
        Builder builder = antTaskBuilder.createBuilder(builderFactory, antTask, TasksTest.pipelineStub(PIPELINE_LABEL, "."), resolver);

        try {
            builder.build(new StubGoPublisher(), new EnvironmentVariableContext(), taskEntension);
        } catch (CruiseControlException e) {
            assertThat(e.getMessage(), containsString("Build failed. Command ant reported [BUILD FAILED]."));
        }
    }

    @Test
    public void shouldPrependDefaultWorkingDirectoryIfRelativeAntHomeIsUsed() throws Exception {
        antTask.setWorkingDirectory("lib");
        File baseDir = new File(DEFAULT_WORKING_DIRECTORY);
        CommandBuilder builder = (CommandBuilder) antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        assertThat(builder.getWorkingDir(), is(new File(baseDir, "lib")));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void antTaskShouldNormalizeWorkingDirectory() throws Exception {
        AntTask task = new AntTask();
        task.setWorkingDirectory("folder1\\folder2");

        CommandBuilder commandBuilder = (CommandBuilder) antTaskBuilder.createBuilder(builderFactory, task, TasksTest.pipelineStub("label", "/var/cruise-agent/pipelines/cruise"), resolver);

        assertThat(commandBuilder.getWorkingDir().getPath(), is("/var/cruise-agent/pipelines/cruise/folder1/folder2"));
    }

    @Test
    public void shouldReturnBuilderWithCancelBuilderIfOnCancelDefined() throws Exception {
        ExecTask cancelTask = new ExecTask();
        Builder builderForCancelTask = execTaskBuilder.createBuilder(builderFactory, cancelTask, pipeline, resolver);

        AntTask antTask = new AntTask();
        antTask.setCancelTask(cancelTask);
        when(builderFactory.builderFor(cancelTask, pipeline, resolver)).thenReturn(builderForCancelTask);

        Builder expected = expectedBuilder(antTask, builderForCancelTask);

        Builder actualBuilder = antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        assertThat(actualBuilder, is(expected));
    }

    private Builder expectedBuilder(AntTask antTask, Builder builderForCancelTask) {
        Builder expected = antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, resolver);
        expected.setCancelBuilder(builderForCancelTask);
        return expected;
    }
}
