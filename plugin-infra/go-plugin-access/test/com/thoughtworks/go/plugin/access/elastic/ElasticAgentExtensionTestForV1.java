/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;


import com.thoughtworks.go.plugin.infra.PluginManager;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticAgentExtensionTestForV1 {
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
    }

    @Test
    public void shouldSerializePluginSettingsToJSON() throws Exception {
        String pluginId = "plugin_id";
        HashMap<String, String> pluginSettings = new HashMap<>();
        pluginSettings.put("key1", "val1");
        pluginSettings.put("key2", "val2");
        PluginManager pluginManager = mock(PluginManager.class);

        ElasticAgentExtension elasticAgentExtension = new ElasticAgentExtension(pluginManager);

        when(pluginManager.resolveExtensionVersion(pluginId, Arrays.asList("1.0", "2.0"))).thenReturn("1.0");
        String pluginSettingsJSON = elasticAgentExtension.pluginSettingsJSON(pluginId, pluginSettings);

        assertThat(pluginSettingsJSON, is("{\"key1\":\"val1\",\"key2\":\"val2\"}"));
    }
}
