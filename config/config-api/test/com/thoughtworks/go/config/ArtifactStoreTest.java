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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import org.junit.Test;

import java.util.Arrays;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class ArtifactStoreTest {
    @Test
    public void addConfigurations_shouldAddConfigurationsWithValue() {
        ConfigurationProperty property = create("username", false, "some_value");

        ArtifactStore artifactStore = new ArtifactStore("id", "plugin_id");
        artifactStore.addConfigurations(Arrays.asList(property));

        assertThat(artifactStore.size(), is(1));
        assertThat(artifactStore, contains(create("username", false, "some_value")));
    }

    @Test
    public void addConfigurations_shouldAddConfigurationsWithEncryptedValue() {
        ConfigurationProperty property = create("username", true, "some_value");

        ArtifactStore artifactStore = new ArtifactStore("id", "plugin_id");
        artifactStore.addConfigurations(Arrays.asList(property));

        assertThat(artifactStore.size(), is(1));
        assertThat(artifactStore, contains(create("username", true, "some_value")));
    }

    @Test
    public void shouldReturnObjectDescription() {
        assertThat(new ArtifactStore().getObjectDescription(), is("Artifact store"));
    }

    @Test
    public void shouldNotAllowNullPluginIdOrProfileId() {
        ArtifactStore store = new ArtifactStore();

        store.validate(null);
        assertThat(store.errors().size(), is(2));
        assertThat(store.errors().on(ArtifactStore.PLUGIN_ID), is("Artifact store cannot have a blank plugin id."));
        assertThat(store.errors().on(ArtifactStore.ID), is("Artifact store cannot have a blank id."));
    }

    @Test
    public void shouldValidateElasticPluginIdPattern() throws Exception {
        ArtifactStore store = new ArtifactStore("!123", "docker");
        store.validate(null);
        assertThat(store.errors().size(), is(1));
        assertThat(store.errors().on(ArtifactStore.ID), is("Invalid id '!123'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateConfigPropertyNameUniqueness() {
        ConfigurationProperty prop1 = ConfigurationPropertyMother.create("USERNAME");
        ConfigurationProperty prop2 = ConfigurationPropertyMother.create("USERNAME");
        ArtifactStore store = new ArtifactStore("s3.plugin", "cd.go.s3.plugin", prop1, prop2);

        store.validate(null);

        assertThat(store.errors().size(), is(0));

        assertThat(prop1.errors().size(), is(1));
        assertThat(prop2.errors().size(), is(1));

        assertThat(prop1.errors().on(ConfigurationProperty.CONFIGURATION_KEY), is("Duplicate key 'USERNAME' found for Artifact store 's3.plugin'"));
        assertThat(prop2.errors().on(ConfigurationProperty.CONFIGURATION_KEY), is("Duplicate key 'USERNAME' found for Artifact store 's3.plugin'"));
    }
}