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
package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.NOTIFICATION_EXTENSION;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class PluginSettingsMetadataStoreTest {
    @Before
    public void setUp() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @After
    public void tearDown() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldPopulateDataCorrectly() {
        String existingPluginId = "plugin-id";
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        String template = "template-value";
        PluginSettingsMetadataStore.getInstance().addMetadataFor(existingPluginId, NOTIFICATION_EXTENSION, configuration, template);

        assertThat(PluginSettingsMetadataStore.getInstance().configuration(existingPluginId), is(configuration));
        assertThat(PluginSettingsMetadataStore.getInstance().template(existingPluginId), is(template));

        String nonExistingPluginId = "some-plugin-which-does-not-exist";
        assertThat(PluginSettingsMetadataStore.getInstance().configuration(nonExistingPluginId), is(nullValue()));
        assertThat(PluginSettingsMetadataStore.getInstance().template(nonExistingPluginId), is(nullValue()));
    }

    @Test
    public void shouldRemoveDataCorrectly() {
        String pluginId = "plugin-id";
        PluginSettingsMetadataStore.getInstance().addMetadataFor(pluginId, NOTIFICATION_EXTENSION, new PluginSettingsConfiguration(), "template-value");
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId), is(true));

        PluginSettingsMetadataStore.getInstance().removeMetadataFor(pluginId);
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId), is(false));
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        String template = "template-value";
        PluginSettingsMetadataStore.getInstance().addMetadataFor("plugin-id", NOTIFICATION_EXTENSION, configuration, template);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin("plugin-id"), is(true));
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin("some-plugin-which-does-not-exist"), is(false));
    }

    @Test
    public void shouldCheckIfPluginSettingsConfigurationHasOption() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        PluginSettingsProperty p1 = createProperty("k1", true);
        PluginSettingsProperty p2 = createProperty("k2", false);
        configuration.add(p1);
        configuration.add(p2);

        PluginSettingsMetadataStore.getInstance().addMetadataFor("plugin-id", NOTIFICATION_EXTENSION, configuration, "template-value");

        assertTrue(PluginSettingsMetadataStore.getInstance().hasOption("plugin-id", "k1", Property.REQUIRED));
        assertFalse(PluginSettingsMetadataStore.getInstance().hasOption("plugin-id", "k2", Property.REQUIRED));
    }

    private PluginSettingsProperty createProperty(String key, boolean isRequired) {
        PluginSettingsProperty property = new PluginSettingsProperty(key);
        property.with(Property.REQUIRED, isRequired);
        return property;
    }
}
