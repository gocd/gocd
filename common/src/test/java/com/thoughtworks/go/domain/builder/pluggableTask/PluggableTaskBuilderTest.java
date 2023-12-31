/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluggableTaskBuilderTest {

    public static final String TEST_PLUGIN_ID = "test-plugin-id";
    @Mock
    private RunIfConfigs runIfConfigs;
    @Mock
    private Builder cancelBuilder;
    @Mock(strictness = Mock.Strictness.LENIENT)
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
    public void shouldInvokeTheTaskExecutorOfThePlugin() {
        final int[] executeTaskCalled = new int[1];
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected ExecutionResult executeTask(Task task, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, Charset consoleLogCharset) {
                executeTaskCalled[0]++;
                return ExecutionResult.success("Test succeeded");
            }
        };

        taskBuilder.build(goPublisher, variableContext, taskExtension, null, null, UTF_8);

        assertThat(executeTaskCalled[0]).isEqualTo(1);
    }

    @Test
    public void shouldBuildExecutorConfigPlusExecutionContextAndInvokeTheTaskExecutorWithIt() {
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
            protected TaskExecutionContext buildTaskContext(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, Charset consoleLogCharset) {
                return taskExecutionContext;
            }
        };

        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        when(taskExecutor.execute(executorTaskConfig, taskExecutionContext)).thenReturn(new ExecutionResult());
        when(task.executor()).thenReturn(taskExecutor);

        taskBuilder.executeTask(task, null, null, UTF_8);

        verify(task).config();
        verify(task).executor();
        verify(taskExecutor).execute(executorTaskConfig, taskExecutionContext);

        assertThat((TaskExecutionContext) ReflectionUtil.getStaticField(JobConsoleLogger.class, "context")).isNotNull();
    }

    @Test
    public void shouldReturnDefaultValueInExecConfigWhenNoConfigValueIsProvided() {
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
        assertThat(config.getValue(propertyName)).isEqualTo(defaultValue);
    }

    @Test
    public void shouldReturnDefaultValueInExecConfigWhenConfigValueIsNull() {
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
        assertThat(config.getValue(propertyName)).isEqualTo(defaultValue);
    }

    @Test
    public void shouldReturnDefaultValueInExecConfigWhenConfigValueIsEmptyString() {
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
        assertThat(config.getValue(propertyName)).isEqualTo(defaultValue);
    }

    @Test
    public void shouldReturnConfigValueInExecConfig() {
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
        assertThat(config.getValue(propertyName)).isEqualTo(configValue.get("value"));
    }

    @Test
    public void shouldReturnPluggableTaskContext() {
        PluggableTask task = mock(PluggableTask.class);
        when(task.getPluginConfiguration()).thenReturn(new PluginConfiguration());

        String workingDir = "test-directory";
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, task, TEST_PLUGIN_ID, workingDir);
        TaskExecutionContext taskExecutionContext = taskBuilder.buildTaskContext(goPublisher, variableContext, UTF_8);

        assertThat(taskExecutionContext instanceof PluggableTaskContext).isEqualTo(true);
        assertThat(taskExecutionContext.workingDir()).isEqualTo(workingDir);
    }

    @Test
    public void shouldPublishErrorMessageIfPluginThrowsAnException() {
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected ExecutionResult executeTask(Task task, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, Charset consoleLogCharset) {
                throw new RuntimeException("err");
            }
        };


        assertThatThrownBy(() -> taskBuilder.build(goPublisher, variableContext, taskExtension, null, null, UTF_8))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("err");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(goPublisher).taggedConsumeLine(eq(DefaultGoPublisher.ERR), captor.capture());
        assertThat(captor.getValue()).isEqualTo("Error: err");
    }

    @Test
    public void shouldPublishErrorMessageIfPluginReturnsAFailureResponse() {
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected ExecutionResult executeTask(Task task, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, Charset consoleLogCharset) {
                return ExecutionResult.failure("err");
            }
        };

        assertThatThrownBy(() -> taskBuilder.build(goPublisher, variableContext, taskExtension, null, null, UTF_8))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("err");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(goPublisher).taggedConsumeLine(eq(DefaultGoPublisher.ERR), captor.capture());
        assertThat(captor.getValue()).isEqualTo("err");
    }

    @Test
    public void shouldRegisterTaskConfigDuringExecutionAndUnregisterOnSuccessfulCompletion() {
        final PluggableTaskBuilder builder = spy(new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, "", ""));
        taskExtension = mock(TaskExtension.class);
        when(taskExtension.execute(eq(TEST_PLUGIN_ID), any())).thenReturn(ExecutionResult.success("yay"));

        builder.build(goPublisher, variableContext, taskExtension, null, null, UTF_8);
        assertThat((TaskExecutionContext) ReflectionUtil.getStaticField(JobConsoleLogger.class, "context")).isNull();
    }

    @Test
    public void shouldUnsetTaskExecutionContextFromJobConsoleLoggerWhenTaskExecutionFails() {
        final PluggableTaskBuilder builder = spy(new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, "", ""));

        taskExtension = mock(TaskExtension.class);
        when(taskExtension.execute(eq(TEST_PLUGIN_ID), any())).thenReturn(ExecutionResult.failure("oh no"));

        assertThatThrownBy(() -> builder.build(goPublisher, variableContext, taskExtension, null, null, UTF_8))
            .hasMessage("oh no");
        assertThat((TaskExecutionContext) ReflectionUtil.getStaticField(JobConsoleLogger.class, "context")).isNull();
    }

    @Test
    public void shouldUnsetTaskExecutionContextFromJobConsoleLoggerWhenTaskExecutionThrowsException() {
        final PluggableTaskBuilder builder = spy(new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, "", ""));

        taskExtension = mock(TaskExtension.class);

        when(taskExtension.execute(eq(TEST_PLUGIN_ID), any())).thenThrow(new RuntimeException("something"));
        assertThatThrownBy(() -> builder.build(goPublisher, variableContext, taskExtension, null, null, UTF_8))
            .hasMessage("something");
        assertThat((TaskExecutionContext) ReflectionUtil.getStaticField(JobConsoleLogger.class, "context")).isNull();
    }
}
