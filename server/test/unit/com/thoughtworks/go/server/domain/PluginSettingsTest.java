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

package com.thoughtworks.go.server.domain;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo;
import com.thoughtworks.go.security.GoCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PluginSettingsTest {
    public static final String PLUGIN_ID = "plugin-id";

    @Test
    public void shouldPopulateSettingsMapFromPluginFromDB() {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("k1", "v1");
        configuration.put("k2", "");
        configuration.put("k3", null);
        Plugin plugin = new Plugin(PLUGIN_ID, toJSON(configuration));

        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        pluginSettings.populateSettingsMap(plugin);

        assertThat(pluginSettings.getPluginSettingsKeys().size(), is(3));
        assertThat(pluginSettings.getValueFor("k1"), is("v1"));
        assertThat(pluginSettings.getValueFor("k2"), is(""));
        assertThat(pluginSettings.getValueFor("k3"), is(nullValue()));
    }

    @Test
    public void shouldPopulateSettingsMapFromPluginFromConfiguration() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("k1", "v1"));
        configuration.add(new PluginSettingsProperty("k2", ""));
        configuration.add(new PluginSettingsProperty("k3", null));

        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        pluginSettings.populateSettingsMap(configuration);

        assertThat(pluginSettings.getValueFor("k1"), is(""));
        assertThat(pluginSettings.getValueFor("k2"), is(""));
        assertThat(pluginSettings.getValueFor("k3"), is(""));
    }

    @Test
    public void shouldPopulateSettingsMapFromKeyValueMap() {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("k1", "v1");
        parameterMap.put("k2", "");
        parameterMap.put("k3", null);

        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        pluginSettings.populateSettingsMap(parameterMap);

        assertThat(pluginSettings.getValueFor("k1"), is("v1"));
        assertThat(pluginSettings.getValueFor("k2"), is(""));
        assertThat(pluginSettings.getValueFor("k3"), is(nullValue()));
    }

    @Test
    public void shouldGetSettingsMapAsKeyValueMap() {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("k1", "v1");
        parameterMap.put("k2", "");
        parameterMap.put("k3", null);

        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        pluginSettings.populateSettingsMap(parameterMap);

        Map<String, String> settingsAsKeyValuePair = pluginSettings.getSettingsAsKeyValuePair();
        assertThat(settingsAsKeyValuePair.size(), is(3));
        assertThat(settingsAsKeyValuePair.get("k1"), is("v1"));
        assertThat(settingsAsKeyValuePair.get("k2"), is(""));
        assertThat(settingsAsKeyValuePair.get("k3"), is(nullValue()));
    }

    @Test
    public void shouldPopulateSettingsMapWithErrorsCorrectly() {
        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("k1", "v1");

        pluginSettings.populateSettingsMap(parameterMap);

        pluginSettings.populateErrorMessageFor("k1", "e1");

        assertThat(pluginSettings.getErrorFor("k1"), is(Arrays.asList("e1")));
    }

    @Test
    public void shouldProvidePluginSettingsAsAWeirdMapForView() {
        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("k1", "v1");

        pluginSettings.populateSettingsMap(parameterMap);

        pluginSettings.populateErrorMessageFor("k1", "e1");

        HashMap<String, Map<String, String>> expectedMap = new HashMap<>();
        HashMap<String, String> valuesAndErrorsMap = new HashMap<>();
        valuesAndErrorsMap.put("value", "v1");
        valuesAndErrorsMap.put("errors", "[e1]");
        expectedMap.put("k1", valuesAndErrorsMap);

        Map<String, Map<String, String>> settingsMap = pluginSettings.getSettingsMap();

        assertThat(settingsMap, is(expectedMap));
    }

    @Test
    public void shouldPopulateHasErrorsCorrectly() {
        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        assertThat(pluginSettings.hasErrors(), is(false));

        pluginSettings.populateErrorMessageFor("k1", "e1");
        assertThat(pluginSettings.hasErrors(), is(true));
    }

    @Test
    public void shouldCreatePluginFromConfigurationCorrectly() {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("k1", "v1");
        parameterMap.put("k2", "");
        parameterMap.put("k3", null);
        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        pluginSettings.populateSettingsMap(parameterMap);

        PluginSettingsConfiguration configuration = pluginSettings.toPluginSettingsConfiguration();

        assertThat(configuration.size(), is(3));
        assertThat(configuration.get("k1").getValue(), is("v1"));
        assertThat(configuration.get("k2").getValue(), is(""));
        assertThat(configuration.get("k3").getValue(), is(nullValue()));
    }

    @Test
    public void shouldAddConfigurationsToSettingsMapCorrectly() throws InvalidCipherTextException {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("k1", new Metadata(true, false)));
        pluginConfigurations.add(new PluginConfiguration("k2", new Metadata(true, true)));
        ConfigRepoPluginInfo pluginInfo = new ConfigRepoPluginInfo(null, new PluggableInstanceSettings(pluginConfigurations));

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1")));
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("k2"), new EncryptedConfigurationValue(new GoCipher().encrypt("v2"))));

        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        pluginSettings.addConfigurations(pluginInfo, configurationProperties);

        PluginSettingsConfiguration pluginSettingsProperties = pluginSettings.toPluginSettingsConfiguration();
        assertThat(pluginSettingsProperties.size(), is(2));
        assertThat(pluginSettingsProperties.get("k1").getValue(), is("v1"));
        assertThat(pluginSettingsProperties.get("k2").getValue(), is("v2"));
    }

    @Test
    public void shouldEncryptedValuesForSecureProperties() throws InvalidCipherTextException {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("k1", new Metadata(true, false)));
        pluginConfigurations.add(new PluginConfiguration("k2", new Metadata(true, true)));
        ConfigRepoPluginInfo pluginInfo = new ConfigRepoPluginInfo(null, new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty configProperty1 = new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1"));
        ConfigurationProperty configProperty2 = new ConfigurationProperty(new ConfigurationKey("k2"), new EncryptedConfigurationValue(new GoCipher().encrypt("v2")));
        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(configProperty1);
        configurationProperties.add(configProperty2);

        PluginSettings pluginSettings = new PluginSettings(PLUGIN_ID);
        pluginSettings.addConfigurations(pluginInfo, configurationProperties);

        List<ConfigurationProperty> pluginSettingsProperties = pluginSettings.getSecurePluginSettingsProperties(pluginInfo);
        assertThat(pluginSettingsProperties.size(), is(2));
        assertThat(pluginSettingsProperties.get(0), is(configProperty1));
        assertThat(pluginSettingsProperties.get(1), is(configProperty2));
    }

    private String toJSON(Map<String, String> map) {
        return new GsonBuilder().serializeNulls().create().toJson(map);
    }
}
