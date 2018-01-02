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

package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginSettingsMetadataLoaderTest {
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;
    @Mock
    private NotificationExtension notificationExtension;
    @Mock
    private ConfigRepoExtension configRepoExtension;
    @Mock
    private PluginManager pluginManager;
    private PluginSettingsMetadataLoader metadataLoader;
    private GoPluginDescriptor pluginDescriptor;
    private List<GoPluginExtension> extensions;

    @Before
    public void setUp() {
        initMocks(this);
        pluginDescriptor = new GoPluginDescriptor("plugin-id", "1.0", null, null, null, true);

        extensions = Arrays.asList(packageRepositoryExtension, scmExtension, taskExtension, notificationExtension, configRepoExtension);
        metadataLoader = new PluginSettingsMetadataLoader(extensions, pluginManager);

        PluginSettingsMetadataStore.getInstance().clear();
    }

    @After
    public void tearDown() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldFetchPluginSettingsMetadataForPluginBasedOnPluginId() throws Exception {
        for (GoPluginExtension extension : extensions) {

            PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
            configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

            pluginDescriptor = new GoPluginDescriptor(UUID.randomUUID().toString(), "1.0", null, null, null, true);

            when(extension.canHandlePlugin(pluginDescriptor.id())).thenReturn(true);
            when(extension.extensionName()).thenReturn("extension-name");
            when(extension.getPluginSettingsConfiguration(pluginDescriptor.id())).thenReturn(configuration);
            when(extension.getPluginSettingsView(pluginDescriptor.id())).thenReturn("template");

            metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

            verifyMetadataForPlugin(pluginDescriptor.id());
        }
    }

    @Test
    public void shouldNotFetchPluginSettingsMetadataForTaskPlugin() throws Exception {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        pluginDescriptor = new GoPluginDescriptor(UUID.randomUUID().toString(), "1.0", null, null, null, true);

        when(taskExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(true);
        when(taskExtension.extensionName()).thenReturn("task");
        when(taskExtension.getPluginSettingsView(pluginDescriptor.id())).thenReturn("template");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        verify(taskExtension, never()).getPluginSettingsConfiguration(pluginDescriptor.id());
        PluginSettingsConfiguration configurationInStore = PluginSettingsMetadataStore.getInstance().configuration(pluginDescriptor.id());
        assertNull(configurationInStore);
    }

    @Test
    public void shouldNotStoreMetadataIfConfigurationIsMissing() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1").with(Property.REQUIRED, true).with(Property.SECURE, false));

        when(packageRepositoryExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(true);
        when(packageRepositoryExtension.getPluginSettingsConfiguration(pluginDescriptor.id())).thenReturn(null);
        when(packageRepositoryExtension.getPluginSettingsView(pluginDescriptor.id())).thenReturn("template");

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id()), is(false));
    }

    @Test
    public void shouldNotStoreMetadataIfTemplateIsMissing() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();

        when(packageRepositoryExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(true);
        when(packageRepositoryExtension.getPluginSettingsConfiguration(pluginDescriptor.id())).thenReturn(configuration);
        when(packageRepositoryExtension.getPluginSettingsView(pluginDescriptor.id())).thenReturn(null);

        metadataLoader.fetchPluginSettingsMetaData(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id()), is(false));
    }

    @Test
    public void shouldRegisterAsPluginFrameworkStartListener() throws Exception {
        verify(pluginManager).addPluginChangeListener(metadataLoader, GoPlugin.class);
    }

    @Test
    public void shouldRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        PluginSettingsMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), new PluginSettingsConfiguration(), "template");

        metadataLoader.pluginUnLoaded(pluginDescriptor);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginDescriptor.id()), is(false));
    }

    private void verifyMetadataForPlugin(String pluginId) {
        PluginSettingsConfiguration configurationInStore = PluginSettingsMetadataStore.getInstance().configuration(pluginId);
        assertThat(configurationInStore.size(), is(1));
        PluginSettingsProperty scmConfiguration = (PluginSettingsProperty) configurationInStore.get("k1");
        assertThat(scmConfiguration.getKey(), is("k1"));
        assertThat(scmConfiguration.getOption(Property.REQUIRED), is(true));
        assertThat(scmConfiguration.getOption(Property.SECURE), is(false));
        String template = PluginSettingsMetadataStore.getInstance().template(pluginDescriptor.id());
        assertThat(template, is("template"));
    }
}
