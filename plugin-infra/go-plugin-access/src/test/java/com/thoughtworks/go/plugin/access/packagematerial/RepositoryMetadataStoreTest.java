/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.packagematerial;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class RepositoryMetadataStoreTest {
    @Before
    public void setUp() throws Exception {
        RepositoryMetadataStoreHelper.clear();
    }

    @After
    public void tearDown() throws Exception {
        RepositoryMetadataStoreHelper.clear();
    }

    @Test
    public void shouldPopulateDataCorrectly() throws Exception {
        PackageConfigurations repositoryConfigurationPut = new PackageConfigurations();
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id", repositoryConfigurationPut);

        assertThat(RepositoryMetadataStore.getInstance().getMetadata("plugin-id"), is(repositoryConfigurationPut));
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
    public void shouldAnswerIfKeyHasGivenOption() throws Exception {
        PackageConfigurations repositoryConfigurationPut = new PackageConfigurations();
        repositoryConfigurationPut.add(new PackageConfiguration("key-one").with(PackageConfiguration.SECURE, true).with(PackageConfiguration.REQUIRED, true));
        repositoryConfigurationPut.add(new PackageConfiguration("key-two"));
        RepositoryMetadataStore metadataStore = RepositoryMetadataStore.getInstance();
        metadataStore.addMetadataFor("plugin-id", repositoryConfigurationPut);

        assertThat(metadataStore.hasOption("plugin-id", "key-one", PackageConfiguration.SECURE),is(true));
        assertThat(metadataStore.hasOption("plugin-id", "key-one", PackageConfiguration.REQUIRED),is(true));
        assertThat(metadataStore.hasOption("plugin-id", "key-one", PackageConfiguration.PART_OF_IDENTITY),is(true));

        assertThat(metadataStore.hasOption("plugin-id", "key-two", PackageConfiguration.SECURE),is(false));
        assertThat(metadataStore.hasOption("plugin-id", "key-two", PackageConfiguration.REQUIRED),is(true));
        assertThat(metadataStore.hasOption("plugin-id", "key-two", PackageConfiguration.PART_OF_IDENTITY),is(true));
    }

    @Test
    public void shouldGetAllPluginIds() throws Exception {
        RepositoryMetadataStore metadataStore = RepositoryMetadataStore.getInstance();
        metadataStore.addMetadataFor("plugin1", new PackageConfigurations());
        metadataStore.addMetadataFor("plugin2", new PackageConfigurations());
        metadataStore.addMetadataFor("plugin3", new PackageConfigurations());
        assertThat(metadataStore.getPlugins().size(), is(3));
        assertThat(metadataStore.getPlugins().contains("plugin1"), is(true));
        assertThat(metadataStore.getPlugins().contains("plugin2"), is(true));
        assertThat(metadataStore.getPlugins().contains("plugin3"), is(true));
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() throws Exception {
        RepositoryMetadataStore metadataStore = RepositoryMetadataStore.getInstance();

        PackageConfigurations repositoryConfigurationPut = new PackageConfigurations();
        metadataStore.addMetadataFor("plugin-id", repositoryConfigurationPut);

        assertThat(metadataStore.hasPlugin("plugin-id"), is(true));
        assertThat(metadataStore.hasPlugin("some-plugin-which-does-not-exist"), is(false));
    }
}
