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
package com.thoughtworks.go.server.service.plugins.processor.pluginsettings.v1;


import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MessageHandlerForPluginSettingsRequestProcessor1_0Test {

    @Test
    public void shouldSerializePluginSettingsMapToJSON() {
        MessageHandlerForPluginSettingsRequestProcessor1_0 processor = new MessageHandlerForPluginSettingsRequestProcessor1_0();
        Map pluginSettings = new HashMap<String, String>();

        pluginSettings.put("k1", "v1");
        pluginSettings.put("k2", "v2");

        assertThat(processor.pluginSettingsToJSON(pluginSettings), is("{\"k1\":\"v1\",\"k2\":\"v2\"}"));
    }
}
