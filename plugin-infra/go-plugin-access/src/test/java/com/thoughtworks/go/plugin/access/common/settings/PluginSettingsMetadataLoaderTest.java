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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluginSettingsMetadataLoaderTest {
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock(lenient = true)
    private SCMExtension scmExtension;
    @Mock(lenient = true)
    private TaskExtension taskExtension;
    @Mock(lenient = true)
    private NotificationExtension notificationExtension;
    @Mock(lenient = true)
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

        List<GoPluginExtension> extensions = asList(packageRepositoryExtension, scmExtension, notificationExtension, configRepoExtension, taskExtension);
        metadataLoader = new PluginSettingsMetadataLoader(extensions, pluginManager);

        PluginSettingsMetadataStore.getInstance().clear();
    }

    @AfterEach
    public void tearDown() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldFetchPluginSettingsMetadataForPluginBasedOnPluginId() throws Exception {
        List<AbstractExtension> everyExtensionExceptTask = asList(packageRepositoryExtension, scmExtension, notificationExtension, configRepoExtension);

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
    public void shouldNotFetchPluginSettingsMetadataForTaskPlugin() throws Exception {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(UUID.randomUUID().toString()).isBundledPlugin(true).build();
        setupSettingsResponses(taskExtension, pluginDescriptor.id(), configuration, "template");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        verify(taskExtension, never()).getPluginSettingsConfiguration(pluginDescriptor.id());
        verify(taskExtension, never()).getPluginSettingsView(pluginDescriptor.id());
        assertThat(PluginSettingsMetadataStore.getInstance().configuration(pluginDescriptor.id()), is(nullValue()));
    }

    @Test
    public void shouldNotStoreMetadataIfConfigurationIsMissing() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("plugin-id").isBundledPlugin(true).build();
        setupSettingsResponses(packageRepositoryExtension, pluginDescriptor.id(), null, "template");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id()), is(false));
    }

    @Test
    public void shouldNotStoreMetadataIfViewTemplateIsMissing() {
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("plugin-id").isBundledPlugin(true).build();
        setupSettingsResponses(packageRepositoryExtension, pluginDescriptor.id(), null, null);

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id()), is(false));
    }

    @Test
    public void shouldRegisterAsPluginFrameworkStartListener() throws Exception {
        verify(pluginManager).addPluginChangeListener(metadataLoader);
    }

    @Test
    public void shouldRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("plugin-id").isBundledPlugin(true).build();
        PluginSettingsMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), PluginConstants.NOTIFICATION_EXTENSION, new PluginSettingsConfiguration(), "template");

        metadataLoader.pluginUnLoaded(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id()), is(false));
    }

    @Test
    public void shouldFailWhenAPluginWithMultipleExtensionsHasMoreThanOneExtensionRespondingWithSettings() throws Exception {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        String pluginID = "plugin-id";
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginID).build();

        setupSettingsResponses(notificationExtension, pluginID, configuration, "view");
        setupSettingsResponses(packageRepositoryExtension, pluginID, configuration, "view");

        try {
            metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);
            fail("Should have failed since multiple extensions support plugin settings.");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Plugin with ID: plugin-id has more than one extension which supports plugin settings"));
            assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id()), is(false));
        }
    }

    @Test
    public void shouldNotFailWhenAPluginWithMultipleExtensionsHasMoreThanOneExtensionRespondingWithSettings_BUT_OnlyOneIsValid() throws Exception {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        String pluginID = "plugin-id";
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginID).build();

        setupSettingsResponses(notificationExtension, pluginID, configuration, null);
        setupSettingsResponses(packageRepositoryExtension, pluginID, configuration, "view");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginID), is(true));
        assertThat(PluginSettingsMetadataStore.getInstance().configuration(pluginID), is(configuration));
        assertThat(PluginSettingsMetadataStore.getInstance().template(pluginID), is("view"));
        assertThat(PluginSettingsMetadataStore.getInstance().extensionWhichCanHandleSettings(pluginID), is(PluginConstants.PACKAGE_MATERIAL_EXTENSION));
    }

    @Test
    public void shouldNotFailWhenAPluginWithMultipleExtensionsHasMoreThanOneExtensionRespondingWithSettings_BUT_NoneIsValid() throws Exception {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        String pluginID = "plugin-id";
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginID).build();

        setupSettingsResponses(notificationExtension, pluginID, configuration, null);
        setupSettingsResponses(packageRepositoryExtension, pluginID, null, "view");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginID), is(false));
    }

    @Test
    public void shouldNotFailWhenAPluginWithMultipleExtensionsHasMoreThanOneExtensionRespondingWithSettings_BUT_OneIsValidAndOtherThrowsException() throws Exception {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        String pluginID = "plugin-id";
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginID).build();

        setupSettingsResponses(notificationExtension, pluginID, configuration, "view");

        when(packageRepositoryExtension.canHandlePlugin(pluginID)).thenReturn(false);

        when(scmExtension.canHandlePlugin(pluginID)).thenReturn(true);
        when(scmExtension.getPluginSettingsConfiguration(pluginID)).thenThrow(new RuntimeException("Ouch!"));
        when(scmExtension.getPluginSettingsView(pluginID)).thenReturn("view");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginID), is(true));
        verify(packageRepositoryExtension, never()).getPluginSettingsConfiguration(pluginID);
        verify(packageRepositoryExtension, never()).getPluginSettingsView(pluginID);
    }

    @Test
    public void shouldReturnNullForExtensionWhichCanHandleSettingsIfPluginDoesNotExist() throws Exception {
        assertThat(PluginSettingsMetadataStore.getInstance().extensionWhichCanHandleSettings("INVALID-PLUGIN"), is(nullValue()));
        assertThat(PluginSettingsMetadataStore.getInstance().extensionWhichCanHandleSettings(""), is(nullValue()));
    }

    private void verifyMetadataForPlugin(String pluginId) {
        PluginSettingsConfiguration configurationInStore = PluginSettingsMetadataStore.getInstance().configuration(pluginId);
        assertThat(configurationInStore.size(), is(1));
        PluginSettingsProperty scmConfiguration = (PluginSettingsProperty) configurationInStore.get("k1");
        assertThat(scmConfiguration.getKey(), is("k1"));
        assertThat(scmConfiguration.getOption(Property.REQUIRED), is(true));
        assertThat(scmConfiguration.getOption(Property.SECURE), is(false));
        String template = PluginSettingsMetadataStore.getInstance().template(pluginId);
        assertThat(template, is("template"));
    }

    private void setupSettingsResponses(GoPluginExtension extension, String pluginID, PluginSettingsConfiguration configuration, String viewTemplate) {
        when(extension.canHandlePlugin(pluginID)).thenReturn(true);
        when(extension.getPluginSettingsConfiguration(pluginID)).thenReturn(configuration);
        when(extension.getPluginSettingsView(pluginID)).thenReturn(viewTemplate);
    }
}
