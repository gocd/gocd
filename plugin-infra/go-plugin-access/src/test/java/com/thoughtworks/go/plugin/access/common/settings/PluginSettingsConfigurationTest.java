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
package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginSettingsConfigurationTest {
    @Test
    public void shouldSortConfigurationPropertiesBasedOnDisplayOrder() {
        PluginSettingsProperty p3 = createProperty("k3", 3);
        PluginSettingsProperty p0 = createProperty("k0", 0);
        PluginSettingsProperty p2 = createProperty("k2", 2);
        PluginSettingsProperty p1 = createProperty("k1", 1);
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(p3);
        configuration.add(p0);
        configuration.add(p2);
        configuration.add(p1);

        List<? extends Property> properties = configuration.list();
        assertThat(properties.get(0).getKey()).isEqualTo("k0");
        assertThat(properties.get(1).getKey()).isEqualTo("k1");
        assertThat(properties.get(2).getKey()).isEqualTo("k2");
        assertThat(properties.get(3).getKey()).isEqualTo("k3");
    }

    private PluginSettingsProperty createProperty(String key, int order) {
        PluginSettingsProperty property = new PluginSettingsProperty(key);
        property.with(Property.DISPLAY_ORDER, order);
        return property;
    }
}
