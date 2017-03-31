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

package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.BuildLogElement;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.pluggabletask.JobConsoleLoggerInternal;
import com.thoughtworks.go.plugin.access.pluggabletask.JsonBasedTaskExtensionHandler_V1;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.*;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginManagerReference;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.builder.pluggableTask.TaskExtensionTestHelper.configWithOneKey;
import static com.thoughtworks.go.domain.builder.pluggableTask.TaskExtensionTestHelper.successfulExecution;
import static com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension.CONFIGURATION_REQUEST;
import static com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension.EXECUTION_REQUEST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluggableTaskBuilderTest {
    @Mock private RunIfConfigs runIfConfigs;
    @Mock private Builder cancelBuilder;
    @Mock private PluggableTask pluggableTask;
    @Mock private PluginManager pluginManager;
    @Mock private EnvironmentVariableContext variableContext;
    @Mock private DefaultGoPublisher goPublisher;
    @Mock private BuildLogElement buildLogElement;
    @Mock private GoPluginDescriptor pluginDescriptor;
    @Mock private SystemEnvironment systemEnvironment;

    private TaskExtension taskExtension;
    private static final String TEST_PLUGIN_ID = "test-plugin-id";
    private static final String WORKING_DIR = "test-directory";
    private static final String FULL_PATH_TO_WORKING_DIRECTORY = new File("/full/path/to/working-dir").getAbsolutePath();

    @Before
    public void setUp() {
        initMocks(this);
        PluginManagerReference.reference().setPluginManager(pluginManager);
        when(pluggableTask.getPluginConfiguration()).thenReturn(new PluginConfiguration(TEST_PLUGIN_ID, "1.0"));
        HashMap<String, Map<String, String>> pluginConfig = new HashMap<>();
        when(pluggableTask.configAsMap()).thenReturn(pluginConfig);
        taskExtension = new TaskExtension(pluginManager);

        when(pluginManager.getPluginDescriptorFor(TEST_PLUGIN_ID)).thenReturn(mock(GoPluginDescriptor.class));
        when(pluginManager.resolveExtensionVersion(eq(TEST_PLUGIN_ID), any(List.class))).thenReturn(JsonBasedTaskExtensionHandler_V1.VERSION);
    }

    @After
    public void teardown(){
        JobConsoleLoggerInternal.unsetContext();
    }

    @Test
    public void JSONBasedPlugin_shouldInvokeTheTaskExecutorOfThePlugin() throws Exception {
        setupExpectationsForPlugin();

        sendTaskToPluginAndCaptureRequests();

        verify(pluginManager).submitTo(eq(TEST_PLUGIN_ID), requestOfType(EXECUTION_REQUEST));
    }

    @Test
    public void JSONBasedPlugin_shouldResolveWorkingDirBeforeInvokingTheTaskExecutorOfThePlugin() throws Exception {
        setupExpectationsForPlugin();

        List<GoPluginApiRequest> requestsSentToPlugin = sendTaskToPluginAndCaptureRequests();
        GoPluginApiRequest executeTaskRequest = requestsSentToPlugin.get(1);

        Map<String, Object> requestBodyMap = TaskExtensionTestHelper.fromRequestBody(executeTaskRequest.requestBody());
        Map<String, Object> contextSentToPlugin = (Map<String, Object>) requestBodyMap.get("context");

        assertThat(contextSentToPlugin.get("workingDirectory"), is(FULL_PATH_TO_WORKING_DIRECTORY));
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
        File fullPathToWorkDir = new File("/path/to/work/dir", workingDir);
        when(systemEnvironment.resolveAgentWorkingDirectory(new File(workingDir))).thenReturn(fullPathToWorkDir);

        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, task, TEST_PLUGIN_ID, workingDir);
        TaskExecutionContext taskExecutionContext = taskBuilder.buildTaskContext(buildLogElement, goPublisher, variableContext, systemEnvironment);

        assertThat(taskExecutionContext instanceof PluggableTaskContext, is(true));
        assertThat(taskExecutionContext.workingDir(), is(fullPathToWorkDir.getAbsolutePath()));
    }

    @Test
    public void shouldPublishErrorMessageIfPluginThrowsAnException() throws CruiseControlException {
        setupExpectationsForPlugin();

        PluggableTask task = mock(PluggableTask.class);
        when(task.getPluginConfiguration()).thenReturn(new PluginConfiguration());
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected ExecutionResult executeTask(Task task, BuildLogElement buildLogElement, DefaultGoPublisher publisher,
                                                  EnvironmentVariableContext environmentVariableContext, SystemEnvironment systemEnvironment) {
                throw new RuntimeException("err");
            }
        };
        when(pluginManager.doOn(eq(Task.class), eq(TEST_PLUGIN_ID), any(ActionWithReturn.class))).thenAnswer(new Answer<ExecutionResult>() {
            @Override
            public ExecutionResult answer(InvocationOnMock invocationOnMock) throws Throwable {
                ActionWithReturn<Task, ExecutionResult> actionWithReturn = (ActionWithReturn<Task, ExecutionResult>) invocationOnMock.getArguments()[2];
                return actionWithReturn.execute(mock(Task.class), pluginDescriptor);
            }
        });

        try {
            taskBuilder.build(buildLogElement, goPublisher, variableContext, systemEnvironment, taskExtension);
            fail("expected exception to be thrown");
        } catch (Exception e) {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(goPublisher).consumeLine(captor.capture());
            String error = "Error: err";
            assertThat(captor.getValue(), is(error));
            assertThat(e.getMessage(), is(new RuntimeException("err").toString()));
        }
    }

    @Test
    public void shouldPublishErrorMessageIfPluginReturnsAFailureResponse() throws CruiseControlException {
        setupExpectationsForPlugin();

        PluggableTask task = mock(PluggableTask.class);
        when(task.getPluginConfiguration()).thenReturn(new PluginConfiguration());
        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, "test-directory") {
            @Override
            protected ExecutionResult executeTask(Task task, BuildLogElement buildLogElement,
                                                  DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext,
                                                  SystemEnvironment systemEnvironment) {
                return ExecutionResult.failure("err");
            }
        };
        when(pluginManager.doOn(eq(Task.class), eq(TEST_PLUGIN_ID), any(ActionWithReturn.class))).thenAnswer(new Answer<ExecutionResult>() {
            @Override
            public ExecutionResult answer(InvocationOnMock invocationOnMock) throws Throwable {
                ActionWithReturn<Task, ExecutionResult> actionWithReturn = (ActionWithReturn<Task, ExecutionResult>) invocationOnMock.getArguments()[2];
                return actionWithReturn.execute(mock(Task.class), pluginDescriptor);
            }
        });

        try {
            taskBuilder.build(buildLogElement, goPublisher, variableContext, systemEnvironment, taskExtension);
            fail("expected exception to be thrown");
        } catch (Exception e) {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(goPublisher).consumeLine(captor.capture());
            assertThat(captor.getValue(), is("err"));
            assertThat(e.getMessage(), is("err"));
        }
    }

    @Test
    public void shouldRegisterTaskConfigDuringExecutionAndUnregisterOnSuccessfulCompletion() throws CruiseControlException {
        final PluggableTaskBuilder builder = spy(new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, "", ""));
        taskExtension = mock(TaskExtension.class);
        when(taskExtension.execute(eq(TEST_PLUGIN_ID), any(ActionWithReturn.class))).thenReturn(ExecutionResult.success("yay"));

        builder.build(buildLogElement, goPublisher, variableContext, systemEnvironment, taskExtension);
        assertThat(ReflectionUtil.getStaticField(JobConsoleLogger.class, "context"), is(nullValue()));
    }

    @Test
    public void shouldUnsetTaskExecutionContextFromJobConsoleLoggerWhenTaskExecutionFails() throws CruiseControlException {
        final PluggableTaskBuilder builder = spy(new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, "", ""));

        taskExtension = mock(TaskExtension.class);
        when(taskExtension.execute(eq(TEST_PLUGIN_ID), any(ActionWithReturn.class))).thenReturn(ExecutionResult.failure("oh no"));

        try {
            builder.build(buildLogElement, goPublisher, variableContext, systemEnvironment, taskExtension);
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
            builder.build(buildLogElement, goPublisher, variableContext, systemEnvironment, taskExtension);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(ReflectionUtil.getStaticField(JobConsoleLogger.class, "context"), is(nullValue()));
        }
    }

    private void setupExpectationsForPlugin() {
        when(pluginManager.hasReferenceFor(Task.class, TEST_PLUGIN_ID)).thenReturn(false);
        when(pluginManager.isPluginOfType(TaskExtension.TASK_EXTENSION, TEST_PLUGIN_ID)).thenReturn(true);
    }

    private List<GoPluginApiRequest> sendTaskToPluginAndCaptureRequests() throws CruiseControlException {
        when(systemEnvironment.resolveAgentWorkingDirectory(new File(WORKING_DIR))).thenReturn(new File(FULL_PATH_TO_WORKING_DIRECTORY));

        when(pluginManager.submitTo(eq(TEST_PLUGIN_ID), requestOfType(CONFIGURATION_REQUEST))).thenReturn(configWithOneKey());
        when(pluginManager.submitTo(eq(TEST_PLUGIN_ID), requestOfType(EXECUTION_REQUEST))).thenReturn(successfulExecution());


        PluggableTaskBuilder taskBuilder = new PluggableTaskBuilder(runIfConfigs, cancelBuilder, pluggableTask, TEST_PLUGIN_ID, WORKING_DIR);
        taskBuilder.build(buildLogElement, goPublisher, variableContext, systemEnvironment, taskExtension);


        ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        verify(pluginManager, times(2)).submitTo(eq(TEST_PLUGIN_ID), requestArgumentCaptor.capture());
        return requestArgumentCaptor.getAllValues();
    }

    private GoPluginApiRequest requestOfType(final String requestType) {
        return argThat(new ArgumentMatcher<GoPluginApiRequest>() {
            @Override
            public boolean matches(Object o) {
                return ((GoPluginApiRequest) o).requestName().equals(requestType);
            }
        });
    }
}
