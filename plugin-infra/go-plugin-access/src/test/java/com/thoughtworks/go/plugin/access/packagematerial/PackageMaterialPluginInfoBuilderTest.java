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
package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.domain.packagematerial.PackageMaterialPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageMaterialPluginInfoBuilderTest {
    private PackageRepositoryExtension extension;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        extension = mock(PackageRepositoryExtension.class);

        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageSettings = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        packageSettings.add(new PackageMaterialProperty("username", null).with(Property.REQUIRED, true).with(Property.SECURE, false).with(Property.PART_OF_IDENTITY, false).with(Property.DISPLAY_NAME, "foo").with(Property.DISPLAY_ORDER, 1));
        packageSettings.add(new PackageMaterialProperty("password", null).with(Property.REQUIRED, true).with(Property.SECURE, true).with(Property.DISPLAY_ORDER, 2));

        RepositoryConfiguration repoSettings = new RepositoryConfiguration();
        repoSettings.add(new PackageMaterialProperty("foo", null).with(Property.REQUIRED, true).with(Property.SECURE, false).with(Property.DISPLAY_ORDER, 1));
        repoSettings.add(new PackageMaterialProperty("bar", null).with(Property.REQUIRED, true).with(Property.SECURE, true).with(Property.DISPLAY_ORDER, 2));

        when(extension.getPackageConfiguration("plugin1")).thenReturn(packageSettings);
        when(extension.getRepositoryConfiguration("plugin1")).thenReturn(repoSettings);
        when(extension.getPluginSettingsView("plugin1")).thenReturn("some-html");
        PluginSettingsConfiguration pluginSettingsConfiguration = new PluginSettingsConfiguration();
        pluginSettingsConfiguration.add(new PluginSettingsProperty("k1", null).with(Property.REQUIRED, true).with(Property.SECURE, false).with(Property.DISPLAY_ORDER, 3));
        when(extension.getPluginSettingsConfiguration("plugin1")).thenReturn(pluginSettingsConfiguration);
    }

    @Test
    public void shouldBuildPluginInfo() throws Exception {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        PackageMaterialPluginInfo pluginInfo = new PackageMaterialPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        List<PluginConfiguration> packageSettings = Arrays.asList(
                new PluginConfiguration("username", new PackageMaterialMetadata(true, false, false, "foo", 1)),
                new PluginConfiguration("password", new PackageMaterialMetadata(true, true, true, "", 2))
        );

        List<PluginConfiguration> repoSettings = Arrays.asList(
                new PluginConfiguration("foo", new PackageMaterialMetadata(true, false, true, "", 1)),
                new PluginConfiguration("bar", new PackageMaterialMetadata(true, true, true, "", 2))
        );

        List<PluginConfiguration> pluginSettings = Arrays.asList(new PluginConfiguration("k1", new Metadata(true, false)));

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("package-repository"));

        assertThat(pluginInfo.getPackageSettings(), is(new PluggableInstanceSettings(packageSettings, null)));
        assertThat(pluginInfo.getRepositorySettings(), is(new PluggableInstanceSettings(repoSettings, null)));
        assertThat(pluginInfo.getPluginSettings(), is(new PluggableInstanceSettings(pluginSettings, new PluginView("some-html"))));
    }

    @Test
    public void shouldThrowAnExceptionWhenRepoConfigProvidedByPluginIsNull() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        when(extension.getRepositoryConfiguration("plugin1")).thenReturn(null);
        thrown.expectMessage("Plugin[plugin1] returned null repository configuration");
        new PackageMaterialPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        new PackageMaterialPluginInfoBuilder(extension).pluginInfoFor(descriptor);
    }

    @Test
    public void shouldThrowAnExceptionWhenPackageConfigProvidedByPluginIsNull() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        when(extension.getPackageConfiguration("plugin1")).thenReturn(null);
        thrown.expectMessage("Plugin[plugin1] returned null package configuration");
        new PackageMaterialPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        new PackageMaterialPluginInfoBuilder(extension).pluginInfoFor(descriptor);
    }
}
