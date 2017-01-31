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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.helper.MetadataStoreHelper;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PackageRepositoryPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.thoughtworks.go.server.service.plugins.builder.PackageRepositoryPluginInfoBuilder.configurations;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageRepositoryPluginInfoBuilderTest {

    @Before
    public void setUp() throws Exception {
        MetadataStoreHelper.clear();
    }

    @After
    public void tearDown() throws Exception {
        MetadataStoreHelper.clear();
    }

    @Test
    public void pluginInfoFor_ShouldProvidePluginInfoForAPlugin() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        PluginManager pluginManager = mock(PluginManager.class);
        PackageMetadataStore packageMetadataStore = mock(PackageMetadataStore.class);
        RepositoryMetadataStore repositoryMetadataStore = mock(RepositoryMetadataStore.class);

        when(packageMetadataStore.getPlugins()).thenReturn(Collections.singletonList(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(new com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration("key1"));
        packageConfigurations.add(new com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration("key2"));

        PackageConfigurations repoConfigurations = new PackageConfigurations();
        repoConfigurations.add(new com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration("key1"));
        repoConfigurations.add(new com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration("key2"));

        when(packageMetadataStore.getMetadata(plugin.id())).thenReturn(packageConfigurations);
        when(repositoryMetadataStore.getMetadata(plugin.id())).thenReturn(repoConfigurations);

        PackageRepositoryPluginInfoBuilder builder = new PackageRepositoryPluginInfoBuilder(pluginManager, packageMetadataStore, repositoryMetadataStore);
        PackageRepositoryPluginInfo pluginInfo = builder.pluginInfoFor(plugin.id());

        PackageRepositoryPluginInfo expectedPluginInfo = new PackageRepositoryPluginInfo(plugin, new PluggableInstanceSettings(configurations(packageConfigurations)), new PluggableInstanceSettings(configurations(repoConfigurations)));
        assertEquals(expectedPluginInfo, pluginInfo);
    }

    @Test
    public void pluginInfoFor_ShouldReturnNullWhenPluginIsNotFound() throws Exception {
        PluginManager pluginManager = mock(PluginManager.class);
        PackageMetadataStore packageMetadataStore = mock(PackageMetadataStore.class);
        RepositoryMetadataStore repositoryMetadataStore = mock(RepositoryMetadataStore.class);

        PackageRepositoryPluginInfoBuilder builder = new PackageRepositoryPluginInfoBuilder(pluginManager, packageMetadataStore, repositoryMetadataStore);
        PackageRepositoryPluginInfo pluginInfo = builder.pluginInfoFor("docker-plugin");
        assertEquals(null, pluginInfo);
    }

    @Test
    public void allPluginInfos_ShouldReturnAListOfAllPluginInfos() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);


        PluginManager pluginManager = mock(PluginManager.class);
        PackageMetadataStore packageMetadataStore = mock(PackageMetadataStore.class);
        RepositoryMetadataStore repositoryMetadataStore = mock(RepositoryMetadataStore.class);

        when(packageMetadataStore.getPlugins()).thenReturn(Collections.singletonList(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(new com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration("key1"));
        packageConfigurations.add(new com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration("key2"));

        PackageConfigurations repoConfigurations = new PackageConfigurations();
        repoConfigurations.add(new com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration("key1"));
        repoConfigurations.add(new com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration("key2"));

        when(packageMetadataStore.getMetadata(plugin.id())).thenReturn(packageConfigurations);
        when(repositoryMetadataStore.getMetadata(plugin.id())).thenReturn(repoConfigurations);

        PackageRepositoryPluginInfoBuilder builder = new PackageRepositoryPluginInfoBuilder(pluginManager, packageMetadataStore, repositoryMetadataStore);
        Collection<PackageRepositoryPluginInfo> pluginInfos = builder.allPluginInfos();

        PackageRepositoryPluginInfo expectedPluginInfo = new PackageRepositoryPluginInfo(plugin, new PluggableInstanceSettings(configurations(packageConfigurations)), new PluggableInstanceSettings(configurations(repoConfigurations)));
        assertEquals(Arrays.asList(expectedPluginInfo), pluginInfos);
    }

}