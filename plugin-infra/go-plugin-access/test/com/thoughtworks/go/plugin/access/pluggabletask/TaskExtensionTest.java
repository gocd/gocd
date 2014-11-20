package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static junitx.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskExtensionTest {
    @Test
    public void shouldReturnCorrectTaskExtensionImplForAPIBasedTaskPlugin() {
        PluginManager pluginManager = mock(PluginManager.class);
        String apiBasedPluginId = "APi-task";
        TaskExtension taskExtension = new TaskExtension(pluginManager);

        when(pluginManager.hasReferenceFor(Task.class, apiBasedPluginId)).thenReturn(true);

        assertTrue(taskExtension.getImpl(apiBasedPluginId) instanceof ApiBasedTaskExtension);
    }

    @Test
    public void shouldReturnMessageBasedTaskExtensionForMessageBasedTaskPlugin() {
        PluginManager pluginManager = mock(PluginManager.class);
        String messageBasedPluginId = "messageBased-task";
        TaskExtension taskExtension = new TaskExtension(pluginManager);

        when(pluginManager.hasReferenceFor(Task.class, messageBasedPluginId)).thenReturn(false);
        when(pluginManager.hasReferenceFor(GoPlugin.class, messageBasedPluginId)).thenReturn(true);

        Assert.assertTrue(taskExtension.getImpl(messageBasedPluginId) instanceof MessageBasedTaskExtension);
    }

    @Test
    public void shouldThrowExceptionIfPluginDoesNotImplementEitherMessageOrApiBasedExtension() {
        PluginManager pluginManager = mock(PluginManager.class);
        String pluginId = "messageBased-task";
        when(pluginManager.hasReferenceFor(Task.class, pluginId)).thenReturn(false);
        when(pluginManager.hasReferenceFor(GoPlugin.class, pluginId)).thenReturn(false);

        TaskExtension taskExtension = new TaskExtension(pluginManager);
        try {
            taskExtension.getImpl(pluginId);
            fail("Should throw exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Plugin should use either message-based or api-based extension. Plugin-id: " + pluginId));
        }
    }
}