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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class BuilderFactoryTest {
    private UpstreamPipelineResolver pipelineResolver;
    private BuilderFactory builderFactory;
    private static AntTaskBuilder antTaskBuilder = mock(AntTaskBuilder.class);
    private static ExecTaskBuilder execTaskBuilder = mock(ExecTaskBuilder.class);
    private static NantTaskBuilder nantTaskBuilder = mock(NantTaskBuilder.class);
    private static RakeTaskBuilder rakeTaskBuilder = mock(RakeTaskBuilder.class);
    private static KillAllChildProcessTaskBuilder killAllChildProcessTaskBuilder = mock(KillAllChildProcessTaskBuilder.class);
    private static FetchTaskBuilder fetchTaskBuilder = mock(FetchTaskBuilder.class);
    private static NullTaskBuilder nullTaskBuilder = mock(NullTaskBuilder.class);
    private static PluggableTaskBuilderCreator pluggableTaskBuilderCreator = mock(PluggableTaskBuilderCreator.class);

    @DataPoint public static TaskDataPoint<AntTask> antDataPoint = new TaskDataPoint<>(new AntTask(), antTaskBuilder);
    @DataPoint public static TaskDataPoint<ExecTask> execDataPoint = new TaskDataPoint<>(new ExecTask(), execTaskBuilder);
    @DataPoint public static TaskDataPoint<NantTask> nantDataPoint = new TaskDataPoint<>(new NantTask(), nantTaskBuilder);
    @DataPoint public static TaskDataPoint<RakeTask> rakeDataPoint = new TaskDataPoint<>(new RakeTask(), rakeTaskBuilder);
    @DataPoint public static TaskDataPoint<AbstractFetchTask> fetchDataPoint = new TaskDataPoint<>(new FetchTask(), fetchTaskBuilder);
    @DataPoint public static TaskDataPoint<NullTask> nullDataPoint = new TaskDataPoint<>(new NullTask(), nullTaskBuilder);
    @DataPoint public static TaskDataPoint<PluggableTask> pluggableTaskDataPoint = new TaskDataPoint<>(new PluggableTask(), pluggableTaskBuilderCreator);
    @DataPoint public static TaskDataPoint<KillAllChildProcessTask> killAllChildProcessTaskTaskDataPoint = new TaskDataPoint<>(new KillAllChildProcessTask(), killAllChildProcessTaskBuilder);

    @Before
    public void setUp() throws Exception {
        pipelineResolver = mock(UpstreamPipelineResolver.class);
        builderFactory = new BuilderFactory(antTaskBuilder, execTaskBuilder, nantTaskBuilder, rakeTaskBuilder, pluggableTaskBuilderCreator, killAllChildProcessTaskBuilder, fetchTaskBuilder, nullTaskBuilder);
    }

    @Theory
    public void shouldCreateABuilderUsingTheCorrectTaskBuilderForATask(TaskDataPoint taskDataPoint)  {
        assertBuilderForTask(taskDataPoint.task, taskDataPoint.taskBuilder);
    }

    @Test
    public void shouldFailIfCalledWithSomeRandomTypeOfTask()  {
        Task task = someRandomNonStandardTask();

        try {
            Pipeline pipeline = PipelineMother.pipeline("pipeline1", StageMother.custom("stage1"));
            builderFactory.builderFor(task, pipeline, pipelineResolver);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Unexpected type of task: " + task.getClass()));
        }
    }

    @Test
    public void shouldCreateABuilderForEachTypeOfTaskWhichExists()  {
        Pipeline pipeline = PipelineMother.pipeline("pipeline1", StageMother.custom("stage1"));
        AntTask antTask = new AntTask();
        NantTask nantTask = new NantTask();
        RakeTask rakeTask = new RakeTask();
        PluggableTask pluggableTask = new PluggableTask();

        Builder expectedBuilderForAntTask = myFakeBuilder();
        Builder expectedBuilderForNantTask = myFakeBuilder();
        Builder expectedBuilderForRakeTask = myFakeBuilder();
        Builder expectedBuilderForPluggableTask = myFakeBuilder();

        when(antTaskBuilder.createBuilder(builderFactory, antTask, pipeline, pipelineResolver)).thenReturn(expectedBuilderForAntTask);
        when(nantTaskBuilder.createBuilder(builderFactory, nantTask, pipeline, pipelineResolver)).thenReturn(expectedBuilderForNantTask);
        when(rakeTaskBuilder.createBuilder(builderFactory, rakeTask, pipeline, pipelineResolver)).thenReturn(expectedBuilderForRakeTask);
        when(pluggableTaskBuilderCreator.createBuilder(builderFactory, pluggableTask, pipeline, pipelineResolver)).thenReturn(expectedBuilderForPluggableTask);

        List<Builder> builders = builderFactory.buildersForTasks(pipeline, listOf(antTask, nantTask, rakeTask,pluggableTask), pipelineResolver);

        assertThat(builders.size(), is(4));
        assertThat(builders.get(0), is(expectedBuilderForAntTask));
        assertThat(builders.get(1), is(expectedBuilderForNantTask));
        assertThat(builders.get(2), is(expectedBuilderForRakeTask));
        assertThat(builders.get(3), is(expectedBuilderForPluggableTask));
    }

    private void assertBuilderForTask(Task task, TaskBuilder expectedBuilderToBeUsed) {
        Pipeline pipeline = PipelineMother.pipeline("pipeline1", StageMother.custom("stage1"));

        Builder expectedBuilder = myFakeBuilder();
        when(expectedBuilderToBeUsed.createBuilder(builderFactory, task, pipeline, pipelineResolver)).thenReturn(expectedBuilder);

        Builder builder = builderFactory.builderFor(task, pipeline, pipelineResolver);

        assertThat(builder, is(expectedBuilder));
    }

    private Builder myFakeBuilder() {
        return new Builder(null, null, null) {
            @Override
            public void build(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, String consoleLogCharset) throws CruiseControlException {
            }

            @Override
            public String toString() {
                return "A fake builder " + super.toString();
            }
        };
    }

    private BuildTask someRandomNonStandardTask() {
        return new BuildTask() {

            @Override
            public String getTaskType() {
                return "build";
            }

            @Override
            public String getTypeForDisplay() {
                return null;
            }

            @Override
            public String command() {
                return null;
            }

            @Override
            public String arguments() {
                return null;
            }
        };
    }

    private List<Task> listOf(Task... tasks) {
        return Arrays.asList(tasks);
    }

    private static class TaskDataPoint<T extends Task> {
        private final T task;
        private final TaskBuilder<T> taskBuilder;

        public TaskDataPoint(T task, TaskBuilder<T> taskBuilder) {
            this.task = task;
            this.taskBuilder = taskBuilder;
        }
    }
}
