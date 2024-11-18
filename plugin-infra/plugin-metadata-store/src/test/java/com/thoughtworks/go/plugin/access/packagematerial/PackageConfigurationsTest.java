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
package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageConfigurationsTest {
    @Test
    public void shouldGetAllPackagesSortedByDisplayOrder() {
        PackageConfiguration c1 = new PackageConfiguration("k1").with(PackageConfiguration.DISPLAY_ORDER, 2);
        PackageConfiguration c2 = new PackageConfiguration("k2").with(PackageConfiguration.DISPLAY_ORDER, 0);
        PackageConfiguration c3 = new PackageConfiguration("k3").with(PackageConfiguration.DISPLAY_ORDER, 1);
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(c1);
        packageConfigurations.add(c2);
        packageConfigurations.add(c3);
        assertThat(packageConfigurations.list().get(0)).isEqualTo(c2);
        assertThat(packageConfigurations.list().get(1)).isEqualTo(c3);
        assertThat(packageConfigurations.list().get(2)).isEqualTo(c1);
    }

    @Test
    public void shouldConstructPackageConfigurationFromApiRepositoryConfiguration() {

        RepositoryConfiguration configuration = new RepositoryConfiguration();
        configuration.add(new PackageMaterialProperty("k1", "v1").with(Property.SECURE, Boolean.TRUE));

        PackageConfigurations packageConfigurations = new PackageConfigurations(configuration);
        assertThat(packageConfigurations.list().size()).isEqualTo(1);
        assertThat(packageConfigurations.list().get(0).getKey()).isEqualTo("k1");
        assertThat(packageConfigurations.list().get(0).getValue()).isEqualTo("v1");
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.REQUIRED)).isEqualTo(true);
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.PART_OF_IDENTITY)).isEqualTo(true);
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.SECURE)).isEqualTo(true);
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.DISPLAY_NAME)).isEqualTo("");
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.DISPLAY_ORDER)).isEqualTo(0);
    }

    @Test
    public void shouldConstructPackageConfigurationFromApiPackageConfiguration() {

        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration configuration = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        configuration.add(new PackageMaterialProperty("k1", "v1").with(Property.SECURE, Boolean.TRUE));

        PackageConfigurations packageConfigurations = new PackageConfigurations(configuration);
        assertThat(packageConfigurations.list().size()).isEqualTo(1);
        assertThat(packageConfigurations.list().get(0).getKey()).isEqualTo("k1");
        assertThat(packageConfigurations.list().get(0).getValue()).isEqualTo("v1");
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.REQUIRED)).isEqualTo(true);
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.PART_OF_IDENTITY)).isEqualTo(true);
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.SECURE)).isEqualTo(true);
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.DISPLAY_NAME)).isEqualTo("");
        assertThat(packageConfigurations.list().get(0).getOption(PackageConfiguration.DISPLAY_ORDER)).isEqualTo(0);
    }
}
