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
package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluginSettingsMetadataLoaderTest {
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private SCMExtension scmExtension;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private TaskExtension taskExtension;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private NotificationExtension notificationExtension;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private ConfigRepoExtension configRepoExtension;
    @Mock
    private PluginManager pluginManager;

    private PluginSettingsMetadataLoader metadataLoader;

    @BeforeEach
    public void setUp() {
        when(packageRepositoryExtension.extensionName()).thenReturn(PluginConstants.PACKAGE_MATERIAL_EXTENSION);
        when(scmExtension.extensionName()).thenReturn(PluginConstants.SCM_EXTENSION);
        when(notificationExtension.extensionName()).thenReturn(PluginConstants.NOTIFICATION_EXTENSION);
        when(configRepoExtension.extensionName()).thenReturn(PluginConstants.CONFIG_REPO_EXTENSION);
        when(taskExtension.extensionName()).thenReturn(PluginConstants.PLUGGABLE_TASK_EXTENSION);

        List<GoPluginExtension> extensions = List.of(packageRepositoryExtension, scmExtension, notificationExtension, configRepoExtension, taskExtension);
        metadataLoader = new PluginSettingsMetadataLoader(extensions, pluginManager);

        PluginSettingsMetadataStore.getInstance().clear();
    }

    @AfterEach
    public void tearDown() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldFetchPluginSettingsMetadataForPluginBasedOnPluginId() {
        List<AbstractExtension> everyExtensionExceptTask = List.of(packageRepositoryExtension, scmExtension, notificationExtension, configRepoExtension);

        for (GoPluginExtension extension : everyExtensionExceptTask) {
            PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
            configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

            GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(UUID.randomUUID().toString()).isBundledPlugin(true).build();
            setupSettingsResponses(extension, pluginDescriptor.id(), configuration, "template");

            metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

            verifyMetadataForPlugin(pluginDescriptor.id());
        }
    }

    @Test
    public void shouldNotFetchPluginSettingsMetadataForTaskPlugin() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(UUID.randomUUID().toString()).isBundledPlugin(true).build();
        setupSettingsResponses(taskExtension, pluginDescriptor.id(), configuration, "template");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        verify(taskExtension, never()).getPluginSettingsConfiguration(pluginDescriptor.id());
        verify(taskExtension, never()).getPluginSettingsView(pluginDescriptor.id());
        assertThat(PluginSettingsMetadataStore.getInstance().configuration(pluginDescriptor.id())).isNull();
    }

    @Test
    public void shouldNotStoreMetadataIfConfigurationIsMissing() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("plugin-id").isBundledPlugin(true).build();
        setupSettingsResponses(packageRepositoryExtension, pluginDescriptor.id(), null, "template");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id())).isFalse();
    }

    @Test
    public void shouldNotStoreMetadataIfViewTemplateIsMissing() {
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("plugin-id").isBundledPlugin(true).build();
        setupSettingsResponses(packageRepositoryExtension, pluginDescriptor.id(), null, null);

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id())).isFalse();
    }

    @Test
    public void shouldRegisterAsPluginFrameworkStartListener() {
        verify(pluginManager).addPluginChangeListener(metadataLoader);
    }

    @Test
    public void shouldRemoveMetadataOnPluginUnLoadedCallback() {
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("plugin-id").isBundledPlugin(true).build();
        PluginSettingsMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), PluginConstants.NOTIFICATION_EXTENSION, new PluginSettingsConfiguration(), "template");

        metadataLoader.pluginUnLoaded(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id())).isFalse();
    }

    @Test
    public void shouldFailWhenAPluginWithMultipleExtensionsHasMoreThanOneExtensionRespondingWithSettings() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        String pluginId = "plugin-id";
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginId).build();

        setupSettingsResponses(notificationExtension, pluginId, configuration, "view");
        setupSettingsResponses(packageRepositoryExtension, pluginId, configuration, "view");

        assertThatThrownBy(() -> metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plugin with ID: plugin-id has more than one extension which supports plugin settings");

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id())).isFalse();
    }

    @Test
    public void shouldNotFailWhenAPluginWithMultipleExtensionsHasMoreThanOneExtensionRespondingWithSettings_BUT_OnlyOneIsValid() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        String pluginId = "plugin-id";
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginId).build();

        setupSettingsResponses(notificationExtension, pluginId, configuration, null);
        setupSettingsResponses(packageRepositoryExtension, pluginId, configuration, "view");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId)).isTrue();
        assertThat(PluginSettingsMetadataStore.getInstance().configuration(pluginId)).isEqualTo(configuration);
        assertThat(PluginSettingsMetadataStore.getInstance().template(pluginId)).isEqualTo("view");
        assertThat(PluginSettingsMetadataStore.getInstance().extensionWhichCanHandleSettings(pluginId)).isEqualTo(PluginConstants.PACKAGE_MATERIAL_EXTENSION);
    }

    @Test
    public void shouldNotFailWhenAPluginWithMultipleExtensionsHasMoreThanOneExtensionRespondingWithSettings_BUT_NoneIsValid() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        String pluginId = "plugin-id";
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginId).build();

        setupSettingsResponses(notificationExtension, pluginId, configuration, null);
        setupSettingsResponses(packageRepositoryExtension, pluginId, null, "view");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId)).isFalse();
    }

    @Test
    public void shouldNotFailWhenAPluginWithMultipleExtensionsHasMoreThanOneExtensionRespondingWithSettings_BUT_OneIsValidAndOtherThrowsException() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        String pluginId = "plugin-id";
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginId).build();

        setupSettingsResponses(notificationExtension, pluginId, configuration, "view");

        when(packageRepositoryExtension.canHandlePlugin(pluginId)).thenReturn(false);

        when(scmExtension.canHandlePlugin(pluginId)).thenReturn(true);
        when(scmExtension.getPluginSettingsConfiguration(pluginId)).thenThrow(new RuntimeException("Ouch!"));
        when(scmExtension.getPluginSettingsView(pluginId)).thenReturn("view");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId)).isTrue();
        verify(packageRepositoryExtension, never()).getPluginSettingsConfiguration(pluginId);
        verify(packageRepositoryExtension, never()).getPluginSettingsView(pluginId);
    }

    @Test
    public void shouldReturnNullForExtensionWhichCanHandleSettingsIfPluginDoesNotExist() {
        assertThat(PluginSettingsMetadataStore.getInstance().extensionWhichCanHandleSettings("INVALID-PLUGIN")).isNull();
        assertThat(PluginSettingsMetadataStore.getInstance().extensionWhichCanHandleSettings("")).isNull();
    }

    private void verifyMetadataForPlugin(String pluginId) {
        PluginSettingsConfiguration configurationInStore = PluginSettingsMetadataStore.getInstance().configuration(pluginId);
        assertThat(configurationInStore).isNotNull().satisfies(s -> assertThat(s.size()).isEqualTo(1));
        PluginSettingsProperty scmConfiguration = (PluginSettingsProperty) configurationInStore.get("k1");
        assertThat(scmConfiguration.getKey()).isEqualTo("k1");
        assertThat(scmConfiguration.getOption(Property.REQUIRED)).isTrue();
        assertThat(scmConfiguration.getOption(Property.SECURE)).isFalse();
        String template = PluginSettingsMetadataStore.getInstance().template(pluginId);
        assertThat(template).isEqualTo("template");
    }

    private void setupSettingsResponses(GoPluginExtension extension, String pluginId, PluginSettingsConfiguration configuration, String viewTemplate) {
        when(extension.canHandlePlugin(pluginId)).thenReturn(true);
        when(extension.getPluginSettingsConfiguration(pluginId)).thenReturn(configuration);
        when(extension.getPluginSettingsView(pluginId)).thenReturn(viewTemplate);
    }
}
