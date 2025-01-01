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
package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.NOTIFICATION_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PluginSettingsMetadataStoreTest {
    @BeforeEach
    public void setUp() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @AfterEach
    public void tearDown() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldPopulateDataCorrectly() {
        String existingPluginId = "plugin-id";
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        String template = "template-value";
        PluginSettingsMetadataStore.getInstance().addMetadataFor(existingPluginId, NOTIFICATION_EXTENSION, configuration, template);

        assertThat(PluginSettingsMetadataStore.getInstance().configuration(existingPluginId)).isEqualTo(configuration);
        assertThat(PluginSettingsMetadataStore.getInstance().template(existingPluginId)).isEqualTo(template);

        String nonExistingPluginId = "some-plugin-which-does-not-exist";
        assertThat(PluginSettingsMetadataStore.getInstance().configuration(nonExistingPluginId)).isNull();
        assertThat(PluginSettingsMetadataStore.getInstance().template(nonExistingPluginId)).isNull();
    }

    @Test
    public void shouldRemoveDataCorrectly() {
        String pluginId = "plugin-id";
        PluginSettingsMetadataStore.getInstance().addMetadataFor(pluginId, NOTIFICATION_EXTENSION, new PluginSettingsConfiguration(), "template-value");
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId)).isTrue();

        PluginSettingsMetadataStore.getInstance().removeMetadataFor(pluginId);
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId)).isFalse();
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        String template = "template-value";
        PluginSettingsMetadataStore.getInstance().addMetadataFor("plugin-id", NOTIFICATION_EXTENSION, configuration, template);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin("plugin-id")).isTrue();
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin("some-plugin-which-does-not-exist")).isFalse();
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
