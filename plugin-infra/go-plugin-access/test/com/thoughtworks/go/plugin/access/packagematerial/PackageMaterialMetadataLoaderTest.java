/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PackageMaterialMetadataLoaderTest {

    private PackageMaterialMetadataLoader metadataLoader;
    private GoPluginDescriptor pluginDescriptor;
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        pluginDescriptor = new GoPluginDescriptor("plugin-id", "1.0", null, null, null, true);
        pluginManager = mock(PluginManager.class);
        packageAsRepositoryExtension = mock(PackageAsRepositoryExtension.class);
        metadataLoader = new PackageMaterialMetadataLoader(pluginManager, packageAsRepositoryExtension);

        RepositoryMetadataStore.getInstance().removeMetadata(pluginDescriptor.id());
        PackageMetadataStore.getInstance().removeMetadata(pluginDescriptor.id());
    }

    @Test
    public void shouldFetchPackageMetadataForPluginsWhichImplementPackageRepositoryMaterialExtensionPoint() {
        RepositoryConfiguration expectedRepoConfigurations = new RepositoryConfiguration();
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration expectedPackageConfigurations = new PackageConfiguration();

        when(packageAsRepositoryExtension.getRepositoryConfiguration(pluginDescriptor.id())).thenReturn(expectedRepoConfigurations);
        when(packageAsRepositoryExtension.getPackageConfiguration(pluginDescriptor.id())).thenReturn(expectedPackageConfigurations);


        metadataLoader.fetchRepositoryAndPackageMetaData(pluginDescriptor);

        assertThat(RepositoryMetadataStore.getInstance().getMetadata(pluginDescriptor.id()).getRepositoryConfiguration(), is(expectedRepoConfigurations));
        assertThat(PackageMetadataStore.getInstance().getMetadata(pluginDescriptor.id()).getPackageConfiguration(), is(expectedPackageConfigurations));
    }

    @Test
    public void shouldThrowExceptionWhenNullRepositoryConfigurationReturned() {
        when(packageAsRepositoryExtension.getRepositoryConfiguration(pluginDescriptor.id())).thenReturn(null);
        try {
            metadataLoader.fetchRepositoryAndPackageMetaData(pluginDescriptor);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Plugin[plugin-id] returned null repository configuration"));
        }
        assertThat(RepositoryMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), nullValue());
        assertThat(PackageMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), nullValue());
    }

    @Test
    public void shouldThrowExceptionWhenNullPackageConfigurationReturned() {
        when(packageAsRepositoryExtension.getPackageConfiguration(pluginDescriptor.id())).thenReturn(null);
        try {
            metadataLoader.fetchRepositoryAndPackageMetaData(pluginDescriptor);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Plugin[plugin-id] returned null repository configuration"));
        }
        assertThat(RepositoryMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), nullValue());
        assertThat(PackageMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), nullValue());
    }

    @Test
    public void shouldRegisterAsPluginFrameworkStartListener() throws Exception {
        metadataLoader = new PackageMaterialMetadataLoader(pluginManager, packageAsRepositoryExtension);
        verify(pluginManager).addPluginChangeListener(metadataLoader, PackageMaterialProvider.class, GoPlugin.class);
    }

    @Test
    public void shouldFetchMetadataOnPluginLoadedCallback() throws Exception {
        PackageMaterialMetadataLoader spy = spy(metadataLoader);
        doNothing().when(spy).fetchRepositoryAndPackageMetaData(pluginDescriptor);
        when(packageAsRepositoryExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(true);
        spy.pluginLoaded(pluginDescriptor);
        verify(spy).fetchRepositoryAndPackageMetaData(pluginDescriptor);
    }

    @Test
    public void shouldNotTryToFetchMetadataOnPluginLoadedCallback() throws Exception {
        PackageMaterialMetadataLoader spy = spy(metadataLoader);
        when(packageAsRepositoryExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(false);
        spy.pluginLoaded(pluginDescriptor);
        verify(spy, never()).fetchRepositoryAndPackageMetaData(pluginDescriptor);
    }

    @Test
    public void shouldRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), new PackageConfigurations());
        when(packageAsRepositoryExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(true);
        metadataLoader.pluginUnLoaded(pluginDescriptor);
        assertThat(RepositoryMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), is(nullValue()));
        assertThat(PackageMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), is(nullValue()));
    }
}
