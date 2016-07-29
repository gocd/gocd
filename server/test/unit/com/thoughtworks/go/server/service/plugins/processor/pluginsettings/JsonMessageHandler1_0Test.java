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

package com.thoughtworks.go.server.service.plugins.processor.pluginsettings;

import com.thoughtworks.go.server.domain.PluginSettings;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JsonMessageHandler1_0Test {
    private JsonMessageHandler1_0 messageHandler;

    @Before
    public void setUp() {
        messageHandler = new JsonMessageHandler1_0();
    }

    @Test
    public void shouldHandleResponseMessageForPluginSettingsGet() {
        PluginSettings pluginSettings = new PluginSettings("plugin-id-1");
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        pluginSettings.populateSettingsMap(map);
        assertThat(messageHandler.responseMessagePluginSettingsGet(pluginSettings), is("{\"k1\":\"v1\",\"k2\":\"v2\"}"));
    }

    @Test
    public void shouldHandleNullResponseMessageForPluginSettingsGet() {
        assertThat(messageHandler.responseMessagePluginSettingsGet(new PluginSettings("plugin-id-2")), is(nullValue()));
    }

}
