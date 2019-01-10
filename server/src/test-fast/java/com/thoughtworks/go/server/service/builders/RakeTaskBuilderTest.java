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
import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(JunitExtRunner.class)
public class RakeTaskBuilderTest {
    private UpstreamPipelineResolver resolver;
    private BuilderFactory builderFactory;
    private RakeTaskBuilder rakeTaskBuilder;

    @Before
    public void setUp() {
        resolver = mock(UpstreamPipelineResolver.class);
        builderFactory = mock(BuilderFactory.class);
        rakeTaskBuilder = new RakeTaskBuilder();
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void rakeTaskShouldNormalizeWorkingDirectory() throws Exception {
        RakeTask task = new RakeTask();
        task.setWorkingDirectory("folder1\\folder2");
        CommandBuilder commandBuilder = (CommandBuilder) rakeTaskBuilder.createBuilder(builderFactory, task, ExecTaskBuilderTest.pipelineStub("label", "/var/cruise-agent/pipelines/cruise"), resolver);
        assertThat(commandBuilder.getWorkingDir().getPath(), is("/var/cruise-agent/pipelines/cruise/folder1/folder2"));
    }
}
