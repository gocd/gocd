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
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.domain.TasksTest;
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
import static com.thoughtworks.go.util.TestUtils.isSamePath;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(JunitExtRunner.class)
public class ExecTaskBuilderTest {
    private UpstreamPipelineResolver resolver;
    private ExecTaskBuilder execTaskBuilder;
    private BuilderFactory builderFactory;

    @Before
    public void setUp() {
        resolver = mock(UpstreamPipelineResolver.class);
        builderFactory = mock(BuilderFactory.class);
        execTaskBuilder = new ExecTaskBuilder();
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test public void shouldUseProjectDefaultWorkingDirectoryIfNotSpecified() throws Exception {
        ExecTask task = new ExecTask("command", "", (String) null);
        final File defaultWorkingDir = new File("foo");

        CommandBuilder builder = (CommandBuilder) execTaskBuilder.createBuilder(builderFactory, task, TasksTest.pipelineStub("label", "foo"), resolver);

        assertThat(builder.getWorkingDir(), isSamePath(defaultWorkingDir));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldNormalizeWorkingDirectory() throws Exception {
        ExecTask execTask = new ExecTask("ant", "", "folder\\child");

        CommandBuilder builder = (CommandBuilder) execTaskBuilder.createBuilder(builderFactory, execTask, TasksTest.pipelineStub("label", "."), resolver);

        assertThat(builder.getWorkingDir().getPath(), is("./folder/child"));
    }
}
