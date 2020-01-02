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
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PluginSettingsPropertyTest {
    @Test
    public void validatePropertyDefaults() {
        PluginSettingsProperty property = new PluginSettingsProperty("Test-Property");
        assertThat(property.getOptions().size(), is(4));
        assertThat(property.getOption(Property.REQUIRED), is(true));
        assertThat(property.getOption(Property.SECURE), is(false));
        assertThat(property.getOption(Property.DISPLAY_NAME), is("Test-Property"));
        assertThat(property.getOption(Property.DISPLAY_ORDER), is(0));

        property = new PluginSettingsProperty("Test-Property", "Dummy Value");
        assertThat(property.getOptions().size(), is(4));
        assertThat(property.getOption(Property.REQUIRED), is(true));
        assertThat(property.getOption(Property.SECURE), is(false));
        assertThat(property.getOption(Property.DISPLAY_NAME), is("Test-Property"));
        assertThat(property.getOption(Property.DISPLAY_ORDER), is(0));
    }

    @Test
    public void shouldCompareTwoPropertiesBasedOnOrder() {
        PluginSettingsProperty p1 = createProperty("Test-Property", 1);
        PluginSettingsProperty p2 = createProperty("Test-Property", 0);
        assertThat(p1.compareTo(p2), is(1));
    }

    private PluginSettingsProperty createProperty(String key, int order) {
        PluginSettingsProperty property = new PluginSettingsProperty(key);
        property.with(Property.DISPLAY_ORDER, order);
        return property;
    }
}
