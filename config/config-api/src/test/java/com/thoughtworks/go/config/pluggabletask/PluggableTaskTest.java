/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config.pluggabletask;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.OnCancelConfig;
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.security.GoCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class PluggableTaskTest {
    @Test
    public void testConfigAsMap() throws Exception {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("test-plugin-id", "13.4");

        GoCipher cipher = new GoCipher();
        List<String> keys = List.of("Avengers 1", "Avengers 2", "Avengers 3", "Avengers 4");
        List<String> values = List.of("Iron man", "Hulk", "Thor", "Captain America");

        Configuration configuration = new Configuration(
                new ConfigurationProperty(new ConfigurationKey(keys.get(0)), new ConfigurationValue(values.get(0))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(1)), new ConfigurationValue(values.get(1))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(2)), new ConfigurationValue(values.get(2))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(3)), null, new EncryptedConfigurationValue(cipher.encrypt(values.get(3))), cipher)
        );

        PluggableTask task = new PluggableTask(pluginConfiguration, configuration);

        Map<String, Map<String, String>> configMap = task.configAsMap();
        assertThat(configMap.keySet().size()).isEqualTo(keys.size());
        assertThat(configMap.values().size()).isEqualTo(values.size());
        assertThat(configMap.keySet().containsAll(keys)).isTrue();
        for (int i = 0; i < keys.size(); i++) {
            assertThat(configMap.get(keys.get(i)).get(PluggableTask.VALUE_KEY)).isEqualTo(values.get(i));
        }
    }

    @Test
    public void shouldReturnTrueWhenPluginConfigurationForTwoPluggableTasksIsExactlyTheSame() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("test-plugin-1", "1.0");
        PluggableTask pluggableTask1 = new PluggableTask(pluginConfiguration, new Configuration());
        PluggableTask pluggableTask2 = new PluggableTask(pluginConfiguration, new Configuration());
        assertTrue(pluggableTask1.hasSameTypeAs(pluggableTask2));
    }

    @Test
    public void shouldReturnFalseWhenPluginConfigurationForTwoPluggableTasksIsDifferent() {
        PluginConfiguration pluginConfiguration1 = new PluginConfiguration("test-plugin-1", "1.0");
        PluginConfiguration pluginConfiguration2 = new PluginConfiguration("test-plugin-2", "1.0");
        PluggableTask pluggableTask1 = new PluggableTask(pluginConfiguration1, new Configuration());
        PluggableTask pluggableTask2 = new PluggableTask(pluginConfiguration2, new Configuration());
        assertFalse(pluggableTask1.hasSameTypeAs(pluggableTask2));
    }

    @Test
    public void shouldReturnFalseWhenPluggableTaskIsComparedWithAnyOtherTask() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("test-plugin-1", "1.0");
        PluggableTask pluggableTask = new PluggableTask(pluginConfiguration, new Configuration());
        AntTask antTask = new AntTask();
        assertFalse(pluggableTask.hasSameTypeAs(antTask));
    }

    @Test
    public void taskTypeShouldBeSanitizedToHaveNoSpecialCharacters() {
        assertThat(new PluggableTask(new PluginConfiguration("abc.def", "1"), new Configuration()).getTaskType()).isEqualTo("pluggable_task_abc_def");
        assertThat(new PluggableTask(new PluginConfiguration("abc_def", "1"), new Configuration()).getTaskType()).isEqualTo("pluggable_task_abc_def");
        assertThat(new PluggableTask(new PluginConfiguration("abcdef", "1"), new Configuration()).getTaskType()).isEqualTo("pluggable_task_abcdef");
        assertThat(new PluggableTask(new PluginConfiguration("abc#def", "1"), new Configuration()).getTaskType()).isEqualTo("pluggable_task_abc_def");
        assertThat(new PluggableTask(new PluginConfiguration("abc#__def", "1"), new Configuration()).getTaskType()).isEqualTo("pluggable_task_abc___def");
        assertThat(new PluggableTask(new PluginConfiguration("Abc#dEF", "1"), new Configuration()).getTaskType()).isEqualTo("pluggable_task_Abc_dEF");
        assertThat(new PluggableTask(new PluginConfiguration("1234567890#ABCDEF", "1"), new Configuration()).getTaskType()).isEqualTo("pluggable_task_1234567890_ABCDEF");
    }

    @Test
    public void shouldPopulatePropertiesForDisplay() {
        Configuration configuration = new Configuration(
                ConfigurationPropertyMother.create("KEY1", false, "value1"),
                ConfigurationPropertyMother.create("Key2", false, "value2"),
                ConfigurationPropertyMother.create("key3", true, "encryptedValue1"));

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);

        List<TaskProperty> propertiesForDisplay = task.getPropertiesForDisplay();

        assertThat(propertiesForDisplay.size()).isEqualTo(3);
        assertProperty(propertiesForDisplay.get(0), "KEY1", "value1", "key1");
        assertProperty(propertiesForDisplay.get(1), "Key2", "value2", "key2");
        assertProperty(propertiesForDisplay.get(2), "key3", "****", "key3");
    }

    @AfterEach
    public void teardown() {
        for (String pluginId : PluggableTaskConfigStore.store().pluginIds()) {
            PluggableTaskConfigStore.store().removePreferenceFor(pluginId);
        }
    }

    @Test
    public void shouldPopulatePropertiesForDisplayRetainingOrderAndDisplayNameIfConfigured() {
        Task taskDetails = mock(Task.class);
        TaskConfig taskConfig = new TaskConfig();
        addProperty(taskConfig, "KEY2", "Key 2", 1);
        addProperty(taskConfig, "KEY1", "Key 1", 0);
        addProperty(taskConfig, "KEY3", "Key 3", 2);
        when(taskDetails.config()).thenReturn(taskConfig);
        when(taskDetails.view()).thenReturn(mock(TaskView.class));

        String pluginId = "plugin_with_all_details";
        PluggableTaskConfigStore.store().setPreferenceFor(pluginId, new TaskPreference(taskDetails));

        Configuration configuration = new Configuration(
                ConfigurationPropertyMother.create("KEY3", true, "encryptedValue1"),
                ConfigurationPropertyMother.create("KEY1", false, "value1"),
                ConfigurationPropertyMother.create("KEY2", false, "value2")
        );

        PluggableTask task = new PluggableTask(new PluginConfiguration(pluginId, "1"), configuration);

        List<TaskProperty> propertiesForDisplay = task.getPropertiesForDisplay();

        assertThat(propertiesForDisplay.size()).isEqualTo(3);
        assertProperty(propertiesForDisplay.get(0), "Key 1", "value1", "key1");
        assertProperty(propertiesForDisplay.get(1), "Key 2", "value2", "key2");
        assertProperty(propertiesForDisplay.get(2), "Key 3", "****", "key3");
    }

    @Test
    public void shouldGetOnlyConfiguredPropertiesIfACertainPropertyDefinedByPluginIsNotConfiguredByUser() {
        Task taskDetails = mock(Task.class);
        TaskConfig taskConfig = new TaskConfig();
        addProperty(taskConfig, "KEY2", "Key 2", 1);
        addProperty(taskConfig, "KEY1", "Key 1", 0);
        addProperty(taskConfig, "KEY3", "Key 3", 2);
        when(taskDetails.config()).thenReturn(taskConfig);
        when(taskDetails.view()).thenReturn(mock(TaskView.class));

        String pluginId = "plugin_with_all_details";
        PluggableTaskConfigStore.store().setPreferenceFor(pluginId, new TaskPreference(taskDetails));

        Configuration configuration = new Configuration(
                ConfigurationPropertyMother.create("KEY1", false, "value1"),
                ConfigurationPropertyMother.create("KEY2", false, "value2")
        );

        PluggableTask task = new PluggableTask(new PluginConfiguration(pluginId, "1"), configuration);

        List<TaskProperty> propertiesForDisplay = task.getPropertiesForDisplay();

        assertThat(propertiesForDisplay.size()).isEqualTo(2);
        assertProperty(propertiesForDisplay.get(0), "Key 1", "value1", "key1");
        assertProperty(propertiesForDisplay.get(1), "Key 2", "value2", "key2");
    }

    private void addProperty(TaskConfig taskConfig, String key, String displayName, int displayOrder) {
        TaskConfigProperty property = taskConfig.addProperty(key);
        property.with(Property.DISPLAY_NAME, displayName);
        property.with(Property.DISPLAY_ORDER, displayOrder);
    }

    @Test
    public void shouldPopulateItselfFromConfigAttributesMap() {
        TaskPreference taskPreference = mock(TaskPreference.class);
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("Key2"));
        PluggableTaskConfigStore.store().setPreferenceFor("abc.def", taskPreference);

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);
        Map<String, String> attributeMap = Map.of("KEY1", "value1", "Key2", "value2");

        TaskConfig taskConfig = new TaskConfig();
        TaskProperty property1 = new TaskProperty("KEY1", "value1");
        TaskProperty property2 = new TaskProperty("Key2", "value2");
        taskConfig.addProperty(property1.getName());
        taskConfig.addProperty(property2.getName());

        when(taskPreference.getConfig()).thenReturn(taskConfig);

        task.setTaskConfigAttributes(attributeMap);

        assertThat(task.configAsMap().get("KEY1").get(PluggableTask.VALUE_KEY)).isEqualTo("value1");
        assertThat(task.configAsMap().get("Key2").get(PluggableTask.VALUE_KEY)).isEqualTo("value2");
    }

    @Test
    public void shouldHandleSecureConfigurations() {
        TaskPreference taskPreference = mock(TaskPreference.class);
        Configuration configuration = new Configuration();
        PluggableTaskConfigStore.store().setPreferenceFor("abc.def", taskPreference);

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);
        Map<String, String> attributeMap = Map.of("KEY1", "value1");

        TaskConfig taskConfig = new TaskConfig();
        taskConfig.addProperty("KEY1").with(Property.SECURE, true);

        when(taskPreference.getConfig()).thenReturn(taskConfig);

        task.setTaskConfigAttributes(attributeMap);

        assertThat(task.getConfiguration().size()).isEqualTo(1);
        assertTrue(task.getConfiguration().first().isSecure());
        assertThat(task.getConfiguration().first().getValue()).isEqualTo("value1");
    }

    @Test
    public void shouldNotOverwriteValuesIfTheyAreNotAvailableInConfigAttributesMap() {
        TaskPreference taskPreference = mock(TaskPreference.class);
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("Key2"));
        PluggableTaskConfigStore.store().setPreferenceFor("abc.def", taskPreference);

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);
        Map<String, String> attributeMap = Map.of("KEY1", "value1");

        TaskConfig taskConfig = new TaskConfig();
        TaskProperty property1 = new TaskProperty("KEY1", "value1");
        TaskProperty property2 = new TaskProperty("Key2", null);
        taskConfig.addProperty(property1.getName());
        taskConfig.addProperty(property2.getName());

        when(taskPreference.getConfig()).thenReturn(taskConfig);

        task.setTaskConfigAttributes(attributeMap);

        assertThat(task.configAsMap().get("KEY1").get(PluggableTask.VALUE_KEY)).isEqualTo("value1");
        assertThat(task.configAsMap().get("Key2").get(PluggableTask.VALUE_KEY)).isNull();
    }

    @Test
    public void shouldIgnoreKeysPresentInConfigAttributesMapButNotPresentInConfigStore() {
        TaskPreference taskPreference = mock(TaskPreference.class);
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        PluggableTaskConfigStore.store().setPreferenceFor("abc.def", taskPreference);

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);
        Map<String, String> attributeMap = Map.of("KEY1", "value1", "Key2", "value2");

        TaskConfig taskConfig = new TaskConfig();
        TaskProperty property1 = new TaskProperty("KEY1", "value1");
        taskConfig.addProperty(property1.getName());

        when(taskPreference.getConfig()).thenReturn(taskConfig);

        task.setTaskConfigAttributes(attributeMap);

        assertThat(task.configAsMap().get("KEY1").get(PluggableTask.VALUE_KEY)).isEqualTo("value1");
        assertFalse(task.configAsMap().containsKey("Key2"));
    }

    @Test
    public void shouldAddPropertyComingFromAttributesMapIfPresentInConfigStoreEvenIfItISNotPresentInCurrentConfiguration() {
        TaskPreference taskPreference = mock(TaskPreference.class);
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        PluggableTaskConfigStore.store().setPreferenceFor("abc.def", taskPreference);

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);
        Map<String, String> attributeMap = Map.of("KEY1", "value1", "Key2", "value2");

        TaskConfig taskConfig = new TaskConfig();
        TaskProperty property1 = new TaskProperty("KEY1", "value1");
        TaskProperty property2 = new TaskProperty("Key2", "value2");
        taskConfig.addProperty(property1.getName());
        taskConfig.addProperty(property2.getName());

        when(taskPreference.getConfig()).thenReturn(taskConfig);

        task.setTaskConfigAttributes(attributeMap);

        assertThat(task.configAsMap().get("KEY1").get(PluggableTask.VALUE_KEY)).isEqualTo("value1");
        assertThat(task.configAsMap().get("Key2").get(PluggableTask.VALUE_KEY)).isEqualTo("value2");
    }

    @Test
    public void shouldAddConfigurationProperties() {
        List<ConfigurationProperty> configurationProperties = List.of(ConfigurationPropertyMother.create("key", "value", "encValue"), new ConfigurationProperty());
        PluginConfiguration pluginConfiguration = new PluginConfiguration("github.pr", "1.1");
        TaskPreference taskPreference = mock(TaskPreference.class);
        TaskConfig taskConfig = new TaskConfig();
        Configuration configuration = new Configuration();
        Property property = new Property("key");
        property.with(Property.SECURE, false);

        PluggableTaskConfigStore.store().setPreferenceFor(pluginConfiguration.getId(), taskPreference);
        taskConfig.addProperty("key");

        when(taskPreference.getConfig()).thenReturn(taskConfig);

        PluggableTask pluggableTask = new PluggableTask(pluginConfiguration, configuration);
        pluggableTask.addConfigurations(configurationProperties);

        assertThat(configuration.size()).isEqualTo(2);
    }

    @Test
    public void shouldAddConfigurationPropertiesForAInvalidPlugin() {
        List<ConfigurationProperty> configurationProperties = List.of(ConfigurationPropertyMother.create("key", "value", "encValue"));
        PluginConfiguration pluginConfiguration = new PluginConfiguration("does_not_exist", "1.1");

        Configuration configuration = new Configuration();

        PluggableTask pluggableTask = new PluggableTask(pluginConfiguration, configuration);
        pluggableTask.addConfigurations(configurationProperties);

        assertThat(configuration.size()).isEqualTo(1);
    }

    @Test
    public void isValidShouldVerifyIfPluginIdIsValid() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("does_not_exist", "1.1");
        Configuration configuration = new Configuration();
        PluggableTask pluggableTask = new PluggableTask(pluginConfiguration, configuration);

        pluggableTask.isValid();

        assertThat(pluggableTask.errors().get("pluggable_task").get(0)).isEqualTo("Could not find plugin for given pluggable id:[does_not_exist].");
    }

    @Test
    public void isValidShouldVerifyForValidConfigurationProperties() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("github.pr", "1.1");
        Configuration configuration = mock(Configuration.class);

        PluggableTaskConfigStore.store().setPreferenceFor(pluginConfiguration.getId(), mock(TaskPreference.class));
        when(configuration.hasErrors()).thenReturn(true);

        PluggableTask pluggableTask = new PluggableTask(pluginConfiguration, configuration);

        assertFalse(pluggableTask.isValid());

        verify(configuration).validateTree();
        verify(configuration).hasErrors();
    }

    @Test
    public void shouldBeAbleToGetTaskConfigRepresentation() {
        List<ConfigurationProperty> configurationProperties = List.of(ConfigurationPropertyMother.create("source", false, "src_dir"),
                ConfigurationPropertyMother.create("destination", false, "des_dir"));

        Configuration configuration = new Configuration();
        configuration.addAll(configurationProperties);

        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");
        PluggableTask pluggableTask = new PluggableTask(pluginConfiguration, configuration);

        TaskConfig taskConfig = pluggableTask.toTaskConfig();

        assertThat(taskConfig.size()).isEqualTo(2);
        assertThat(taskConfig.get("source").getValue()).isEqualTo("src_dir");
        assertThat(taskConfig.get("destination").getValue()).isEqualTo("des_dir");
    }

    @Test
    public void validateTreeShouldVerifyIfOnCancelTasksHasErrors() {
        PluggableTask pluggableTask = new PluggableTask(new PluginConfiguration(), new Configuration());
        pluggableTask.onCancelConfig = mock(OnCancelConfig.class);
        com.thoughtworks.go.domain.Task cancelTask = mock(com.thoughtworks.go.domain.Task.class);

        when(pluggableTask.onCancelConfig.getTask()).thenReturn(cancelTask);
        when(cancelTask.hasCancelTask()).thenReturn(false);
        when(pluggableTask.onCancelConfig.validateTree(null)).thenReturn(false);

        assertFalse(pluggableTask.validateTree(null));
    }

    @Test
    public void validateTreeShouldVerifyIfCancelTasksHasNestedCancelTask() {
        PluggableTask pluggableTask = new PluggableTask(new PluginConfiguration(), new Configuration());
        pluggableTask.onCancelConfig = mock(OnCancelConfig.class);
        com.thoughtworks.go.domain.Task cancelTask = mock(com.thoughtworks.go.domain.Task.class);

        when(pluggableTask.onCancelConfig.getTask()).thenReturn(cancelTask);
        when(cancelTask.hasCancelTask()).thenReturn(true);
        when(pluggableTask.onCancelConfig.validateTree(null)).thenReturn(true);

        assertFalse(pluggableTask.validateTree(null));
        assertThat(pluggableTask.errors().get("onCancelConfig").get(0)).isEqualTo("Cannot nest 'oncancel' within a cancel task");
    }

    @Test
    public void validateTreeShouldVerifyIfPluggableTaskHasErrors() {
        PluggableTask pluggableTask = new PluggableTask(new PluginConfiguration(), new Configuration());
        pluggableTask.addError("task", "invalid plugin");

        assertFalse(pluggableTask.validateTree(null));
    }

    @Test
    public void validateTreeShouldVerifyIfConfigurationHasErrors() {
        Configuration configuration = mock(Configuration.class);

        PluggableTask pluggableTask = new PluggableTask(new PluginConfiguration(), configuration);

        when(configuration.hasErrors()).thenReturn(true);

        assertFalse(pluggableTask.validateTree(null));
    }

    @Test
    public void postConstructShouldHandleSecureConfigurationForConfigurationProperties() {
        TaskPreference taskPreference = mock(TaskPreference.class);
        ConfigurationProperty configurationProperty = ConfigurationPropertyMother.create("KEY1");
        Configuration configuration = new Configuration(configurationProperty);
        PluggableTaskConfigStore.store().setPreferenceFor("abc.def", taskPreference);

        TaskConfig taskConfig = new TaskConfig();
        taskConfig.addProperty("KEY1").with(Property.SECURE, true);
        when(taskPreference.getConfig()).thenReturn(taskConfig);

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);

        assertFalse(configurationProperty.isSecure());

        task.applyPluginMetadata();
        assertTrue(configurationProperty.isSecure());
    }

    @Test
    public void postConstructShouldDoNothingForPluggableTaskWithoutCorrespondingPlugin() {
        ConfigurationProperty configurationProperty = ConfigurationPropertyMother.create("KEY1");
        Configuration configuration = new Configuration(configurationProperty);

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);

        assertFalse(configurationProperty.isSecure());

        task.applyPluginMetadata();
        assertFalse(configurationProperty.isSecure());
    }

    @Test
    public void postConstructShouldDoNothingForAInvalidConfigurationProperty() {
        TaskPreference taskPreference = mock(TaskPreference.class);
        ConfigurationProperty configurationProperty = ConfigurationPropertyMother.create("KEY1");
        Configuration configuration = new Configuration(configurationProperty);
        PluggableTaskConfigStore.store().setPreferenceFor("abc.def", taskPreference);

        TaskConfig taskConfig = new TaskConfig();
        when(taskPreference.getConfig()).thenReturn(taskConfig);

        PluggableTask task = new PluggableTask(new PluginConfiguration("abc.def", "1"), configuration);

        assertFalse(configurationProperty.isSecure());

        task.applyPluginMetadata();
        assertFalse(configurationProperty.isSecure());
    }

    private void assertProperty(TaskProperty taskProperty, String name, String value, String cssClass) {
        assertThat(taskProperty.getName()).isEqualTo(name);
        assertThat(taskProperty.getValue()).isEqualTo(value);
        assertThat(taskProperty.getCssClass()).isEqualTo(cssClass);
    }
}
