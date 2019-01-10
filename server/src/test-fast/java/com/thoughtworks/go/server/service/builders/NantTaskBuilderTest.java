/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.NantTask;
import com.thoughtworks.go.domain.NullPipeline;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(JunitExtRunner.class)
public class NantTaskBuilderTest {
    Pipeline pipeline = new NullPipeline();
    private UpstreamPipelineResolver resolver;
    private NantTaskBuilder nantTaskBuilder;
    private BuilderFactory builderFactory;

    @Before
    public void setUp() {
        resolver = mock(UpstreamPipelineResolver.class);
        builderFactory = mock(BuilderFactory.class);
        nantTaskBuilder = new NantTaskBuilder();
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldSetTargetWhenTargetIsSpecified() throws Exception {
        NantTask nantTask = new NantTask();
        nantTask.setTarget("unit-test");
        CommandBuilder commandBuilder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(commandBuilder.getArgs(), is("unit-test"));
    }

    @Test
    public void shouldUseDefaultWorkingDirectoryByDefault() throws Exception {
        NantTask nantTask = new NantTask();
        CommandBuilder commandBuilder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, ExecTaskBuilderTest.pipelineStub("label", "/cruise"), resolver);
        assertThat(commandBuilder.getWorkingDir(), is(new File("/cruise")));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldUseAbsoluteNantPathIfAbsoluteNantPathIsSpecifiedOnLinux() throws Exception {
        NantTask nantTask = new NantTask();
        nantTask.setNantPath("/usr/bin");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(new File(builder.getCommand()), is(new File("/usr/bin/nant")));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldUseAbsoluteNantPathIfAbsoluteNantPathIsSpecifiedOnWindows() throws Exception {
        NantTask nantTask = new NantTask();
        nantTask.setNantPath("c:\\nantdir");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(new File(builder.getCommand()), is(new File("c:\\nantdir\\nant")));
    }

    @Test
    public void shouldJoinNantPathWithWorkingDirectoryIfRelativeNantPathIsSpecified() throws Exception {
        NantTask nantTask = new NantTask();
        nantTask.setNantPath("lib");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(new File(builder.getCommand()), is(new File("lib/nant")));
    }

    @Test
    public void shouldDealWithSpacesInNantPath() throws Exception {
        NantTask nantTask = new NantTask();
        nantTask.setNantPath("lib/nant 1.0");
        nantTask.setBuildFile("ccnet default.build");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, resolver);
        assertThat(new File(builder.getCommand()), is(new File("lib/nant 1.0/nant")));
        assertThat(builder.getArgs(), is("-buildfile:\"ccnet default.build\""));
    }


    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void nantTaskShouldNormalizeWorkingDirectory() throws Exception {
        NantTask nantTask = new NantTask();
        nantTask.setWorkingDirectory("folder1\\folder2");
        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, nantTask, ExecTaskBuilderTest.pipelineStub("label", "/var/cruise-agent/pipelines/cruise"), resolver);
        assertThat(builder.getWorkingDir(), is(new File("/var/cruise-agent/pipelines/cruise/folder1/folder2")));
    }

    @Test
    public void shouldSetNAntWorkingDirectoryAbsolutelyIfSpecified() throws Exception {
        final File absoluteFile = new File("project").getAbsoluteFile();
        NantTask task = new NantTask();
        task.setWorkingDirectory(absoluteFile.getPath());

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, task, pipeline, resolver);
        assertThat(builder.getWorkingDir(), is(absoluteFile));
    }

    @Test
    public void nantTaskShouldNormalizeBuildFile() throws Exception {
        NantTask task = new NantTask();
        task.setBuildFile("pavan\\build.xml");

        CommandBuilder builder = (CommandBuilder) nantTaskBuilder.createBuilder(builderFactory, task, pipeline, resolver);

        assertThat(builder.getArgs(), is("-buildfile:\"pavan/build.xml\""));
    }

}
