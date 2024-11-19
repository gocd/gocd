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

import static org.assertj.core.api.Assertions.assertThat;

public class PluginSettingsPropertyTest {
    @Test
    public void validatePropertyDefaults() {
        PluginSettingsProperty property = new PluginSettingsProperty("Test-Property");
        assertThat(property.getOptions().size()).isEqualTo(4);
        assertThat(property.getOption(Property.REQUIRED)).isTrue();
        assertThat(property.getOption(Property.SECURE)).isFalse();
        assertThat(property.getOption(Property.DISPLAY_NAME)).isEqualTo("Test-Property");
        assertThat(property.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);

        property = new PluginSettingsProperty("Test-Property", "Dummy Value");
        assertThat(property.getOptions().size()).isEqualTo(4);
        assertThat(property.getOption(Property.REQUIRED)).isTrue();
        assertThat(property.getOption(Property.SECURE)).isFalse();
        assertThat(property.getOption(Property.DISPLAY_NAME)).isEqualTo("Test-Property");
        assertThat(property.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);
    }

    @Test
    public void shouldCompareTwoPropertiesBasedOnOrder() {
        PluginSettingsProperty p1 = createProperty("Test-Property", 1);
        PluginSettingsProperty p2 = createProperty("Test-Property", 0);
        assertThat(p1.compareTo(p2)).isEqualTo(1);
    }

    private PluginSettingsProperty createProperty(String key, int order) {
        PluginSettingsProperty property = new PluginSettingsProperty(key);
        property.with(Property.DISPLAY_ORDER, order);
        return property;
    }
}
