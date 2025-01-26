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
package com.thoughtworks.go.plugin.access.packagematerial;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RepositoryMetadataStoreTest {
    @BeforeEach
    public void setUp() {
        RepositoryMetadataStoreHelper.clear();
    }

    @AfterEach
    public void tearDown() {
        RepositoryMetadataStoreHelper.clear();
    }

    @Test
    public void shouldPopulateDataCorrectly() {
        PackageConfigurations repositoryConfigurationPut = new PackageConfigurations();
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id", repositoryConfigurationPut);

        assertThat(RepositoryMetadataStore.getInstance().getMetadata("plugin-id")).isEqualTo(repositoryConfigurationPut);
    }

    @Test
    public void shouldReturnNullForMetadataIfPluginIdIsNotProvided() {
        assertNull(RepositoryMetadataStore.getInstance().getMetadata(""));
    }

    @Test
    public void shouldReturnNullForMetadataIfPluginIdIsNonExistent() {
        assertNull(RepositoryMetadataStore.getInstance().getMetadata("non-existent-plugin-id"));
    }

    @Test
    public void shouldAnswerIfKeyHasGivenOption() {
        PackageConfigurations repositoryConfigurationPut = new PackageConfigurations();
        repositoryConfigurationPut.add(new PackageConfiguration("key-one").with(PackageConfiguration.SECURE, true).with(PackageConfiguration.REQUIRED, true));
        repositoryConfigurationPut.add(new PackageConfiguration("key-two"));
        RepositoryMetadataStore metadataStore = RepositoryMetadataStore.getInstance();
        metadataStore.addMetadataFor("plugin-id", repositoryConfigurationPut);

        assertThat(metadataStore.hasOption("plugin-id", "key-one", PackageConfiguration.SECURE)).isEqualTo(true);
        assertThat(metadataStore.hasOption("plugin-id", "key-one", PackageConfiguration.REQUIRED)).isEqualTo(true);
        assertThat(metadataStore.hasOption("plugin-id", "key-one", PackageConfiguration.PART_OF_IDENTITY)).isEqualTo(true);

        assertThat(metadataStore.hasOption("plugin-id", "key-two", PackageConfiguration.SECURE)).isEqualTo(false);
        assertThat(metadataStore.hasOption("plugin-id", "key-two", PackageConfiguration.REQUIRED)).isEqualTo(true);
        assertThat(metadataStore.hasOption("plugin-id", "key-two", PackageConfiguration.PART_OF_IDENTITY)).isEqualTo(true);
    }

    @Test
    public void shouldGetAllPluginIds() {
        RepositoryMetadataStore metadataStore = RepositoryMetadataStore.getInstance();
        metadataStore.addMetadataFor("plugin1", new PackageConfigurations());
        metadataStore.addMetadataFor("plugin2", new PackageConfigurations());
        metadataStore.addMetadataFor("plugin3", new PackageConfigurations());
        assertThat(metadataStore.getPlugins().size()).isEqualTo(3);
        assertThat(metadataStore.getPlugins().contains("plugin1")).isEqualTo(true);
        assertThat(metadataStore.getPlugins().contains("plugin2")).isEqualTo(true);
        assertThat(metadataStore.getPlugins().contains("plugin3")).isEqualTo(true);
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() {
        RepositoryMetadataStore metadataStore = RepositoryMetadataStore.getInstance();

        PackageConfigurations repositoryConfigurationPut = new PackageConfigurations();
        metadataStore.addMetadataFor("plugin-id", repositoryConfigurationPut);

        assertThat(metadataStore.hasPlugin("plugin-id")).isEqualTo(true);
        assertThat(metadataStore.hasPlugin("some-plugin-which-does-not-exist")).isEqualTo(false);
    }
}
