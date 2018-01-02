/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.TasksTest;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.pluggableTask.PluggableTaskBuilder;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PluggableTaskBuilderCreatorTest {
    private static final String DEFAULT_WORKING_DIRECTORY = "default/working/directory";
    private static final String PIPELINE_LABEL = "label";
    private Pipeline pipeline = TasksTest.pipelineStub(PIPELINE_LABEL, DEFAULT_WORKING_DIRECTORY);
    private PluggableTask pluggableTask;
    private PluggableTaskBuilderCreator pluggableTaskBuilderCreator;
    private ExecTaskBuilder execTaskBuilder;
    private BuilderFactory builderFactory;
    private UpstreamPipelineResolver resolver;

    @Before
    public void setup() throws Exception {
        pluggableTask = new PluggableTask(new PluginConfiguration("test-plugin-id", "13.4"), new Configuration());
        pluggableTaskBuilderCreator = new PluggableTaskBuilderCreator(mock(TaskExtension.class));
        execTaskBuilder = new ExecTaskBuilder();
        builderFactory = mock(BuilderFactory.class);
        resolver = mock(UpstreamPipelineResolver.class);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldCreatePluggableTaskBuilder() throws Exception {
        when(builderFactory.builderFor(pluggableTask.cancelTask(), pipeline, resolver)).thenReturn(null);
        Builder builder = pluggableTaskBuilderCreator.createBuilder(builderFactory, pluggableTask, new Pipeline(), resolver);
        assertThat(builder != null, is(true));
        assertThat(builder instanceof PluggableTaskBuilder, is(true));
    }

    @Test
    public void shouldReturnBuilderWithCancelBuilderIfOnCancelDefined() throws Exception {
        ExecTask cancelExecTask = new ExecTask();
        Builder builderForCancelTask = execTaskBuilder.createBuilder(builderFactory, cancelExecTask, pipeline, resolver);
        pluggableTask.setCancelTask(cancelExecTask);
        when(builderFactory.builderFor(cancelExecTask, pipeline, resolver)).thenReturn(builderForCancelTask);
        Builder expected = expectedBuilder(pluggableTask, builderForCancelTask);
        Builder actual = pluggableTaskBuilderCreator.createBuilder(builderFactory, pluggableTask, pipeline, resolver);
        assertThat(actual, Is.is(expected));
    }

    @Test
    public void shouldCreateBuilderWithAReasonableDescription() throws Exception {
        Builder builder = pluggableTaskBuilderCreator.createBuilder(builderFactory, pluggableTask, pipeline, resolver);
        assertThat(builder.getDescription(), is("Plugin with ID: test-plugin-id"));
    }

    private Builder expectedBuilder(PluggableTask pluggableTask, Builder builderForCancelTask) {
        Builder expected = pluggableTaskBuilderCreator.createBuilder(builderFactory, pluggableTask, pipeline, resolver);
        expected.setCancelBuilder(builderForCancelTask);
        return expected;
    }
}
