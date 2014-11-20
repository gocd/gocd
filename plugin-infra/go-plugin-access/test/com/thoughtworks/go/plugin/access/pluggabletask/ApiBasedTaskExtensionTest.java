package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ApiBasedTaskExtensionTest {
    @Test
    public void shouldExecuteTheTask() {
        PluginManager pluginManager = mock(PluginManager.class);
        ApiBasedTaskExtension extension = new ApiBasedTaskExtension(pluginManager);
        String pluginId = "plugin-id";

        when(pluginManager.doOn(eq(Task.class), eq(pluginId), any(ActionWithReturn.class))).thenReturn(ExecutionResult.failure("failed"));

        ExecutionResult executionResult = extension.execute(pluginId, mock(ActionWithReturn.class));

        verify(pluginManager).doOn(eq(Task.class), eq(pluginId), any(ActionWithReturn.class));
        assertThat(executionResult.getMessagesForDisplay(), is("failed"));
        assertFalse(executionResult.isSuccessful());
    }
}