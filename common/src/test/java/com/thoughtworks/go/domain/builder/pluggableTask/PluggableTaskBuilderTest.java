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
package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.pluggabletask.JobConsoleLoggerInternal;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.*;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluggableTaskBuilderTest {

    public static final String TEST_PLUGIN_ID = "test-plugin-id";
    @Mock
    private RunIfConfigs runIfConfigs;
    @Mock
    private Builder cancelBuilder;
    @Mock(lenient = true)
    private PluggableTask pluggableTask;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private ExtensionsRegistry extensionsRegistry;
    @Mock
    private EnvironmentVariableContext variableContext;
    @Mock
    private DefaultGoPublisher goPublisher;
    private TaskExtension taskExtension;

    @BeforeEach
    public void setUp() {
        PluginManagerReference.reference().setPluginManager(pluginManager);
        when(pluggableTask.getPluginConfiguration()).thenReturn(new PluginConfiguration(TEST_PLUGIN_ID, "1.0"));
        HashMap<String, Map<String, String>> pluginConfig = new HashMap<>();
        when(pluggableTask.configAsMap()).thenReturn(pluginConfig);
        taskExtension = new TaskExtension(pluginManager, extensionsRegistry);
    }

    @AfterEach
    public void teardown() {
        JobConsoleLoggerInternal.unsetContext();
    }

    @Test
    public void shouldInvokeTheTaskExecutorOfThePlugin() throws Exception {
        final int[] executeTaskCalled = new int[1];
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected ExecutionResult executeTask(Task task, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
                executeTaskCalled[0]++;
                return ExecutionResult.success("Test succeeded");
            }
        };

        taskBuilder.build(goPublisher, variableContext, taskExtension, null, null, "utf-8");

        assertThat(executeTaskCalled[0], is(1));
    }

    @Test
    public void shouldBuildExecutorConfigPlusExecutionContextAndInvokeTheTaskExecutorWithIt() throws Exception {
        Task task = mock(Task.class);

        TaskConfig defaultTaskConfig = mock(TaskConfig.class);
        when(task.config()).thenReturn(defaultTaskConfig);

        final TaskConfig executorTaskConfig = mock(TaskConfig.class);
        final TaskExecutionContext taskExecutionContext = mock(TaskExecutionContext.class);
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected TaskConfig buildTaskConfig(TaskConfig config) {
                return executorTaskConfig;
            }

            @Override
            protected TaskExecutionContext buildTaskContext(DefaultGoPublisher publisher,
                                                            EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
                return taskExecutionContext;
            }
        };

        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        when(taskExecutor.execute(executorTaskConfig, taskExecutionContext)).thenReturn(new ExecutionResult());
        when(task.executor()).thenReturn(taskExecutor);

        taskBuilder.executeTask(task, null, null, "utf-8");

        verify(task).config();
        verify(task).executor();
        verify(taskExecutor).execute(executorTaskConfig, taskExecutionContext);

        assertThat(ReflectionUtil.getStaticField(JobConsoleLogger.class, "context"), is(not(nullValue())));
    }

    @Test
    public void shouldReturnDefaultValueInExecConfigWhenNoConfigValueIsProvided() throws Exception {
        Map<String, Map<String, String>> configMap = new HashMap<>();
        PluggableTask task = mock(PluggableTask.class);
        when(task.getPluginConfiguration()).thenReturn(new PluginConfiguration());
        when(task.configAsMap()).thenReturn(configMap);

        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, task, TEST_PLUGIN_ID, "test-directory");

        TaskConfig defaultTaskConfig = new TaskConfig();
        String propertyName = "URL";
        String defaultValue = "ABC.TXT";

        defaultTaskConfig.addProperty(propertyName).withDefault(defaultValue);

        TaskConfig config = taskBuilder.buildTaskConfig(defaultTaskConfig);
        assertThat(config.getValue(propertyName), is(defaultValue));
    }

    @Test
    public void shouldReturnDefaultValueInExecConfigWhenConfigValueIsNull() throws Exception {
        TaskConfig defaultTaskConfig = new TaskConfig();
        String propertyName = "URL";
        String defaultValue = "ABC.TXT";

        Map<String, Map<String, String>> configMap = new HashMap<>();
        configMap.put(propertyName, null);

        PluggableTask task = mock(PluggableTask.class);
        when(task.getPluginConfiguration()).thenReturn(new PluginConfiguration());
        when(task.configAsMap()).thenReturn(configMap);

        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, task, TEST_PLUGIN_ID, "test-directory");

        defaultTaskConfig.addProperty(propertyName).withDefault(defaultValue);

        TaskConfig config = taskBuilder.buildTaskConfig(defaultTaskConfig);
        assertThat(config.getValue(propertyName), is(defaultValue));
    }

    @Test
    public void shouldReturnDefaultValueInExecConfigWhenConfigValueIsEmptyString() throws Exception {
        TaskConfig defaultTaskConfig = new TaskConfig();
        String propertyName = "URL";
        String defaultValue = "ABC.TXT";

        Map<String, Map<String, String>> configMap = new HashMap<>();
        HashMap<String, String> configValue = new HashMap<>();
        configValue.put("value", "");

        configMap.put(propertyName, configValue);

        PluggableTask task = mock(PluggableTask.class);
        when(task.getPluginConfiguration()).thenReturn(new PluginConfiguration());
        when(task.configAsMap()).thenReturn(configMap);

        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, task, TEST_PLUGIN_ID, "test-directory");

        defaultTaskConfig.addProperty(propertyName).withDefault(defaultValue);

        TaskConfig config = taskBuilder.buildTaskConfig(defaultTaskConfig);
        assertThat(config.getValue(propertyName), is(defaultValue));
    }

    @Test
    public void shouldReturnConfigValueInExecConfig() throws Exception {
        TaskConfig defaultTaskConfig = new TaskConfig();
        String propertyName = "URL";
        String defaultValue = "ABC.TXT";
        HashMap<String, String> configValue = new HashMap<>();
        configValue.put("value", "XYZ.TXT");

        Map<String, Map<String, String>> configMap = new HashMap<>();
        configMap.put(propertyName, configValue);

        PluggableTask task = mock(PluggableTask.class);
        when(task.getPluginConfiguration()).thenReturn(new PluginConfiguration());
        when(task.configAsMap()).thenReturn(configMap);

        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, task, TEST_PLUGIN_ID, "test-directory");

        defaultTaskConfig.addProperty(propertyName).withDefault(defaultValue);

        TaskConfig config = taskBuilder.buildTaskConfig(defaultTaskConfig);
        assertThat(config.getValue(propertyName), is(configValue.get("value")));
    }

    @Test
    public void shouldReturnPluggableTaskContext() throws Exception {
        PluggableTask task = mock(PluggableTask.class);
        when(task.getPluginConfiguration()).thenReturn(new PluginConfiguration());

        String workingDir = "test-directory";
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, task, TEST_PLUGIN_ID, workingDir);
        TaskExecutionContext taskExecutionContext = taskBuilder.buildTaskContext(goPublisher, variableContext, "utf-8");

        assertThat(taskExecutionContext instanceof PluggableTaskContext, is(true));
        assertThat(taskExecutionContext.workingDir(), is(workingDir));
    }

    @Test
    public void shouldPublishErrorMessageIfPluginThrowsAnException() throws CruiseControlException {
        PluggableTask task = mock(PluggableTask.class);
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected ExecutionResult executeTask(Task task, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
                throw new RuntimeException("err");
            }
        };

        try {
            taskBuilder.build(goPublisher, variableContext, taskExtension, null, null, "utf-8");
            fail("expected exception to be thrown");
        } catch (Exception e) {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(goPublisher).taggedConsumeLine(eq(DefaultGoPublisher.ERR), captor.capture());
            String error = "Error: err";
            assertThat(captor.getValue(), is(error));
            assertThat(e.getMessage(), is(new RuntimeException("err").toString()));
        }
    }

    @Test
    public void shouldPublishErrorMessageIfPluginReturnsAFailureResponse() throws CruiseControlException {
        PluggableTask task = mock(PluggableTask.class);
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected ExecutionResult executeTask(Task task, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
                return ExecutionResult.failure("err");
            }
        };

        try {
            taskBuilder.build(goPublisher, variableContext, taskExtension, null, null, "utf-8");
            fail("expected exception to be thrown");
        } catch (Exception e) {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(goPublisher).taggedConsumeLine(eq(DefaultGoPublisher.ERR), captor.capture());
            assertThat(captor.getValue(), is("err"));
            assertThat(e.getMessage(), is("err"));
        }
    }

    @Test
    public void shouldRegisterTaskConfigDuringExecutionAndUnregisterOnSuccessfulCompletion() throws CruiseControlException {
        final PluggableTaskBuilder builder = spy(new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, "", ""));
        taskExtension = mock(TaskExtension.class);
        when(taskExtension.execute(eq(TEST_PLUGIN_ID), any(ActionWithReturn.class))).thenReturn(ExecutionResult.success("yay"));

        builder.build(goPublisher, variableContext, taskExtension, null, null, "utf-8");
        assertThat(ReflectionUtil.getStaticField(JobConsoleLogger.class, "context"), is(nullValue()));
    }

    @Test
    public void shouldUnsetTaskExecutionContextFromJobConsoleLoggerWhenTaskExecutionFails() throws CruiseControlException {
        final PluggableTaskBuilder builder = spy(new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, "", ""));

        taskExtension = mock(TaskExtension.class);
        when(taskExtension.execute(eq(TEST_PLUGIN_ID), any(ActionWithReturn.class))).thenReturn(ExecutionResult.failure("oh no"));

        try {
            builder.build(goPublisher, variableContext, taskExtension, null, null, "utf-8");
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(ReflectionUtil.getStaticField(JobConsoleLogger.class, "context"), is(nullValue()));
        }
    }

    @Test
    public void shouldUnsetTaskExecutionContextFromJobConsoleLoggerWhenTaskExecutionThrowsException() throws CruiseControlException {
        final PluggableTaskBuilder builder = spy(new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, "", ""));

        taskExtension = mock(TaskExtension.class);

        when(taskExtension.execute(eq(TEST_PLUGIN_ID), any(ActionWithReturn.class))).thenThrow(new RuntimeException("something"));
        try {
            builder.build(goPublisher, variableContext, taskExtension, null, null, "utf-8");
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(ReflectionUtil.getStaticField(JobConsoleLogger.class, "context"), is(nullValue()));
        }
    }
}
