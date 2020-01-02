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
package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.notification.NotificationMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskMetadataStore;
import com.thoughtworks.go.plugin.access.secrets.SecretsMetadataStore;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo;
import com.thoughtworks.go.plugin.domain.pluggabletask.PluggableTaskPluginInfo;
import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class DefaultPluginInfoFinderTest {
    @Mock
    private PluginManager pluginManager;

    private DefaultPluginInfoFinder finder;
    private PluggableTaskMetadataStore taskMetadataStore;
    private NotificationMetadataStore notificationMetadataStore;
    private SecretsMetadataStore secretsMetadataStore;
    private final String SECRET_PLUGIN_ID = "secret-plugin-id";

    @Before
    public void setUp() throws Exception {
        com.thoughtworks.go.ClearSingleton.clearSingletons();
        taskMetadataStore = PluggableTaskMetadataStore.instance();
        notificationMetadataStore = NotificationMetadataStore.instance();
        secretsMetadataStore = SecretsMetadataStore.instance();

        finder = new DefaultPluginInfoFinder(pluginManager);
    }

    @After
    public void tearDown() throws Exception {
        com.thoughtworks.go.ClearSingleton.clearSingletons();
    }

    @Test
    public void shouldCombineMultipleExtensionsInASinglePluginIntoOne() {
        taskMetadataStore.setPluginInfo(taskPluginInfo("plugin-1"));
        taskMetadataStore.setPluginInfo(taskPluginInfo("plugin-2"));
        notificationMetadataStore.setPluginInfo(notificationPluginInfo("plugin-1"));
        notificationMetadataStore.setPluginInfo(notificationPluginInfo("plugin-3"));

        Collection<CombinedPluginInfo> allPluginInfos = finder.allPluginInfos(null);

        assertThat(allPluginInfos.size(), is(4));
        assertThat(allPluginInfos, containsInAnyOrder(
                combinedPluginInfo(taskPluginInfo("plugin-1"), notificationPluginInfo("plugin-1")),
                combinedPluginInfo(taskPluginInfo("plugin-2")),
                combinedPluginInfo(notificationPluginInfo("plugin-3")),
                combinedPluginInfo(secretsPluginInfo(SECRET_PLUGIN_ID))));
    }

    @Test
    public void shouldFetchAllPluginInfosForAType() {
        taskMetadataStore.setPluginInfo(taskPluginInfo("plugin-1"));
        taskMetadataStore.setPluginInfo(taskPluginInfo("plugin-2"));
        notificationMetadataStore.setPluginInfo(notificationPluginInfo("plugin-1"));
        notificationMetadataStore.setPluginInfo(notificationPluginInfo("plugin-3"));

        Collection<CombinedPluginInfo> allTaskPluginInfos = finder.allPluginInfos(PluginConstants.PLUGGABLE_TASK_EXTENSION);

        assertThat(allTaskPluginInfos.size(), is(2));
        assertThat(allTaskPluginInfos, containsInAnyOrder(
                combinedPluginInfo(taskPluginInfo("plugin-1")),
                combinedPluginInfo(taskPluginInfo("plugin-2"))));

        Collection<CombinedPluginInfo> allSCMPluginInfos = finder.allPluginInfos(PluginConstants.SCM_EXTENSION);
        assertThat(allSCMPluginInfos, is(emptyList()));
    }

    @Test
    public void shouldGetPluginInfoForASpecifiedPluginID() {
        taskMetadataStore.setPluginInfo(taskPluginInfo("plugin-1"));
        taskMetadataStore.setPluginInfo(taskPluginInfo("plugin-2"));
        notificationMetadataStore.setPluginInfo(notificationPluginInfo("plugin-1"));
        notificationMetadataStore.setPluginInfo(notificationPluginInfo("plugin-3"));

        assertThat(finder.pluginInfoFor("plugin-1"), is(combinedPluginInfo(taskPluginInfo("plugin-1"), notificationPluginInfo("plugin-1"))));
        assertThat(finder.pluginInfoFor("plugin-2"), is(combinedPluginInfo(taskPluginInfo("plugin-2"))));
        assertThat(finder.pluginInfoFor("plugin-NON-EXISTENT"), is(nullValue()));
    }

    @Test
    public void shouldGetPluginInfosForSecrets() {
        SecretsPluginInfo pluginInfo = secretsPluginInfo(SECRET_PLUGIN_ID);
        secretsMetadataStore.setPluginInfo(pluginInfo);

        assertThat(finder.pluginInfoFor(SECRET_PLUGIN_ID), is(new CombinedPluginInfo(pluginInfo)));
        assertThat(finder.pluginInfoFor("non-existent-plugin-id"), is(nullValue()));
    }

    private PluggableInstanceSettings settings(String someConfigurationSettingKeyName) {
        PluginConfiguration configuration = new PluginConfiguration(someConfigurationSettingKeyName, new Metadata(false, false));

        return new PluggableInstanceSettings(singletonList(configuration));
    }

    private SecretsPluginInfo secretsPluginInfo(String pluginID) {
        return new SecretsPluginInfo(getDescriptor(pluginID), settings("key"), null);
    }


    private NotificationPluginInfo notificationPluginInfo(String pluginID) {
        return new NotificationPluginInfo(getDescriptor(pluginID), settings("key2"));
    }

    private PluggableTaskPluginInfo taskPluginInfo(String pluginID) {
        return new PluggableTaskPluginInfo(getDescriptor(pluginID), "Plugin 1", settings("key1"));
    }

    private CombinedPluginInfo combinedPluginInfo(PluginInfo... pluginInfos) {
        return new CombinedPluginInfo(Arrays.asList(pluginInfos));
    }

    private GoPluginDescriptor getDescriptor(String pluginID) {
        GoPluginDescriptor.Vendor vendor = new GoPluginDescriptor.Vendor("vendor-name", "vendor-url");
        GoPluginDescriptor.About about = GoPluginDescriptor.About.builder().name("author").version("1.0").vendor(vendor).targetGoVersion("17.12").description("some description").build();
        return GoPluginDescriptor.builder()
                .id(pluginID)
                .version("1.0")
                .pluginJarFileLocation("/path/to/plugin")
                .about(about)
                .bundleLocation(new File("/path/to/bundle"))
                .build();
    }
}
