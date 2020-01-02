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
package com.thoughtworks.go.plugin.access.packagematerial;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PackageMetadataStoreTest {
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
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id", packageConfigurations);

        assertThat(PackageMetadataStore.getInstance().getMetadata("plugin-id"), is(packageConfigurations));
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() throws Exception {
        PackageMetadataStore metadataStore = PackageMetadataStore.getInstance();

        PackageConfigurations packageConfigurations = new PackageConfigurations();
        metadataStore.addMetadataFor("plugin-id", packageConfigurations);

        assertThat(metadataStore.hasPlugin("plugin-id"), is(true));
        assertThat(metadataStore.hasPlugin("some-plugin-which-does-not-exist"), is(false));
    }
}
