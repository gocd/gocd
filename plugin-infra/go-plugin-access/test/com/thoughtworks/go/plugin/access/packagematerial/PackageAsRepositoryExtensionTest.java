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

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PackageAsRepositoryExtensionTest {

    public static final String PLUGIN_ID = "plugin-id";
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private PluginManager pluginManager;
    private PackageAsRepositoryExtensionContract contract;

    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        packageAsRepositoryExtension = spy(new PackageAsRepositoryExtension(pluginManager));
        contract = mock(PackageAsRepositoryExtensionContract.class);
        doReturn(contract).when(packageAsRepositoryExtension).resolveBy(PLUGIN_ID);
    }

    @Test
    public void shouldResolveToApiBasedPackageRepositoryExtension() throws Exception {
        String pluginId = "plugin-id";
        when(pluginManager.hasReferenceFor(PackageMaterialProvider.class, pluginId)).thenReturn(true);
        packageAsRepositoryExtension = new PackageAsRepositoryExtension(pluginManager);
        PackageAsRepositoryExtensionContract contract = packageAsRepositoryExtension.resolveBy(pluginId);
        assertThat(contract, IsInstanceOf.instanceOf(ApiBasedPackageRepositoryExtension.class));
    }

    @Test
    public void shouldResolveToJsonBasedPackageRepositoryExtension() throws Exception {
        String pluginId = "plugin-id";
        when(pluginManager.hasReferenceFor(PackageMaterialProvider.class, pluginId)).thenReturn(false);
        packageAsRepositoryExtension = new PackageAsRepositoryExtension(pluginManager);
        PackageAsRepositoryExtensionContract contract = packageAsRepositoryExtension.resolveBy(pluginId);
        assertThat(contract, IsInstanceOf.instanceOf(JsonBasedPackageRepositoryExtension.class));
    }

    @Test
    public void shouldGetPluginSettingsConfiguration() throws Exception {
        packageAsRepositoryExtension.getPluginSettingsConfiguration(PLUGIN_ID);
        verify(contract).getPluginSettingsConfiguration(PLUGIN_ID);
    }

    @Test
    public void shouldGetPluginSettingsView() throws Exception {
        packageAsRepositoryExtension.getPluginSettingsView(PLUGIN_ID);
        verify(contract).getPluginSettingsView(PLUGIN_ID);
    }

    @Test
    public void shouldValidatePluginSettings() throws Exception {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        packageAsRepositoryExtension.validatePluginSettings(PLUGIN_ID, configuration);
        verify(contract).validatePluginSettings(PLUGIN_ID, configuration);
    }

    @Test
    public void shouldGetRepositoryConfiguration() throws Exception {
        packageAsRepositoryExtension.getRepositoryConfiguration(PLUGIN_ID);
        verify(contract).getRepositoryConfiguration(PLUGIN_ID);
    }

    @Test
    public void shouldGetPackageConfiguration() throws Exception {
        packageAsRepositoryExtension.getPackageConfiguration(PLUGIN_ID);
        verify(contract).getPackageConfiguration(PLUGIN_ID);
    }

    @Test
    public void shouldCheckIfRepositoryConfigurationValid() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        packageAsRepositoryExtension.isRepositoryConfigurationValid(PLUGIN_ID, repositoryConfiguration);
        verify(contract).isRepositoryConfigurationValid(PLUGIN_ID, repositoryConfiguration);
    }

    @Test
    public void shouldCheckIfPackageConfigurationValid() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new PackageConfiguration();
        packageAsRepositoryExtension.isPackageConfigurationValid(PLUGIN_ID, packageConfiguration, repositoryConfiguration);
        verify(contract).isPackageConfigurationValid(PLUGIN_ID, packageConfiguration, repositoryConfiguration);
    }

    @Test
    public void shouldCheckRepositoryConnection() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        packageAsRepositoryExtension.checkConnectionToRepository(PLUGIN_ID, repositoryConfiguration);
        verify(contract).checkConnectionToRepository(PLUGIN_ID, repositoryConfiguration);
    }

    @Test
    public void shouldCheckPackageConnection() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new PackageConfiguration();
        packageAsRepositoryExtension.checkConnectionToPackage(PLUGIN_ID, packageConfiguration, repositoryConfiguration);
        verify(contract).checkConnectionToPackage(PLUGIN_ID, packageConfiguration, repositoryConfiguration);
    }

    @Test
    public void shouldGetLatestRevision() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new PackageConfiguration();
        packageAsRepositoryExtension.getLatestRevision(PLUGIN_ID, packageConfiguration, repositoryConfiguration);
        verify(contract).getLatestRevision(PLUGIN_ID, packageConfiguration, repositoryConfiguration);
    }

    @Test
    public void shouldGetLatestModificationSince() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new PackageConfiguration();
        PackageRevision packageRevision = new PackageRevision("", null, "");
        packageAsRepositoryExtension.latestModificationSince(PLUGIN_ID, packageConfiguration, repositoryConfiguration, packageRevision);
        verify(contract).latestModificationSince(PLUGIN_ID, packageConfiguration, repositoryConfiguration, packageRevision);
    }

    @Test
    public void shouldFindPluginToBeOfTypePackageRepository() throws Exception {
        when(pluginManager.hasReferenceFor(PackageMaterialProvider.class, "plugin-one")).thenReturn(true);
        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, "plugin-two")).thenReturn(true);
        assertThat(packageAsRepositoryExtension.canHandlePlugin("plugin-one"), is(true));
        assertThat(packageAsRepositoryExtension.canHandlePlugin("plugin-two"), is(true));
    }

    @Test
    public void shouldNotFindPluginToBeOfTypePackageRepository() throws Exception {
        when(pluginManager.hasReferenceFor(PackageMaterialProvider.class, "plugin-one")).thenReturn(false);
        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, "plugin-two")).thenReturn(false);
        assertThat(packageAsRepositoryExtension.canHandlePlugin("plugin-one"), is(false));
        assertThat(packageAsRepositoryExtension.canHandlePlugin("plugin-two"), is(false));
    }
}
